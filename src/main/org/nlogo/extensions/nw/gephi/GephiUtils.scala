package org.nlogo.extensions.nw.gephi

import org.nlogo.extensions.nw.NetworkExtension

object GephiUtils {
  // Gephi uses OpenIDE's Lookup system, who's default Lookup uses the current context class loader.
  // However, this is not the extension's class loader, which is what we want it to use. This is the
  // best solution I could come up. Would love to find another one.
  // BCH 1/14/2015
  def withNWLoaderContext[T] = withClassLoaderContext[T](classOf[NetworkExtension].getClassLoader)_

  def withClassLoaderContext[T](loader: ClassLoader)(body: => T) = {
    val oldLoader = Thread.currentThread.getContextClassLoader
    try {
      Thread.currentThread.setContextClassLoader(loader)
      body
    } finally {
      Thread.currentThread.setContextClassLoader(oldLoader)
    }
  }

  // clean up after Gephi!!! See https://github.com/gephi/gephi/issues/854 -- RG 9/18/15
  def shutdownStupidExtraThreads(): Unit = {
    import org.openide.util.Lookup,
      Lookup.Template
    import org.gephi.graph.dhns.core.Dhns
    import org.gephi.data.attributes.model.IndexedAttributeModel
    import org.gephi.data.attributes.event.AttributeEventManager
    import org.gephi.data.attributes.AbstractAttributeModel
    import scala.collection.JavaConversions._

    val indexedAttributeModels = GephiUtils.withNWLoaderContext {
      Lookup.getDefault.lookup(new Template(classOf[IndexedAttributeModel])).allInstances
    }

    val eventManagerField = classOf[AttributeEventManager].getDeclaredField("eventQueue")
    eventManagerField.setAccessible(true)
    indexedAttributeModels.map(eventManagerField.get(_).asInstanceOf[AttributeEventManager]).foreach(_.stop(true))

    val allDhns = withNWLoaderContext {
      Lookup.getDefault.lookup(
        new Template(classOf[Dhns])).allInstances
    }

    allDhns.foreach(_.getEventManager.stop(true))

    val threadSet = Thread.getAllStackTraces().keySet()

    val destructorThread =
      withNWLoaderContext {
        Class.forName("org.gephi.graph.dhns.core.GraphStructure$ViewDestructorThread")
      }
    val runningField = destructorThread.getDeclaredField("running")
    runningField.setAccessible(true)
    threadSet
      .filter(destructorThread.isInstance)
      .foreach { destructorThread => runningField.setBoolean(destructorThread, false) }
  }
}
