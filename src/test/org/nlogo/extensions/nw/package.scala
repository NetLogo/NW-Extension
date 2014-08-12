// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions

import scala.collection.JavaConverters._

import org.nlogo.workspace.AbstractWorkspace

package object nw {

  def networkExtension(ws: AbstractWorkspace) =
    ws.getExtensionManager.loadedExtensions.asScala.collect {
      case ext: NetworkExtension => ext
    }.head

}
