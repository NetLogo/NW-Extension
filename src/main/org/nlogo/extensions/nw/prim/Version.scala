package org.nlogo.extensions.nw.prim

import org.nlogo.api
import org.nlogo.api.Syntax._
import org.nlogo.extensions.nw.NetworkExtension

class Version(extension: NetworkExtension)
  extends api.DefaultReporter {
  override def getSyntax = reporterSyntax(StringType)
  override def report(args: Array[api.Argument], context: api.Context): AnyRef = extension.version
}