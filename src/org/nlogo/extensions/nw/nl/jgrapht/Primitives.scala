package org.nlogo.extensions.nw.nl.jgrapht

import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.api.ScalaConversions.toRichAny
import org.nlogo.api.ScalaConversions.toRichSeq
import org.nlogo.api.Syntax.AgentsetType
import org.nlogo.api.Syntax.BooleanType
import org.nlogo.api.Syntax.CommandTaskType
import org.nlogo.api.Syntax.LinksetType
import org.nlogo.api.Syntax.ListType
import org.nlogo.api.Syntax.NumberType
import org.nlogo.api.Syntax.OptionalType
import org.nlogo.api.Syntax.StringType
import org.nlogo.api.Syntax.TurtleType
import org.nlogo.api.Syntax.TurtlesetType
import org.nlogo.api.Syntax.commandSyntax
import org.nlogo.api.Syntax.reporterSyntax
import org.nlogo.api.Argument
import org.nlogo.api.Context
import org.nlogo.api.DefaultCommand
import org.nlogo.api.DefaultReporter
import org.nlogo.api.LogoList
import org.nlogo.api.Turtle
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToNetLogoAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentSetToRichAgentSet
import org.nlogo.extensions.nw.NetworkExtensionUtil.AgentToNetLogoAgent
import org.nlogo.extensions.nw.NetworkExtensionUtil.TurtleToNetLogoTurtle
import org.nlogo.extensions.nw.NetworkExtension
import org.nlogo.extensions.nw.StaticNetLogoGraph
import org.nlogo.nvm.ExtensionContext
import scala.collection.JavaConverters._

trait Primitives {
  self: NetworkExtension =>

  object MaximalCliques extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asJGraphTGraph
        .bronKerboschCliqueFinder
        .cliques
        .toLogoList
    }
  }

  object BiggestMaximalClique extends DefaultReporter {
    override def getSyntax = reporterSyntax(ListType)
    override def report(args: Array[Argument], context: Context) = {
      getGraph(context).asJGraphTGraph
        .bronKerboschCliqueFinder
        .biggestClique
        .toLogoList
    }
  }

}