package org.nlogo.extensions.nw.nl.jgrapht

import org.nlogo.extensions.nw.nl
import org.jgrapht.alg._
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import scala.collection.JavaConverters._

trait Algorithms {
  self: nl.jgrapht.Graph =>

  lazy val bronKerboschCliqueFinder = new BronKerboschCliqueFinder(this) {
    private def toScala(cliques: java.util.Collection[java.util.Set[Turtle]]) =
      cliques.asScala.map(_.asScala.view.toSeq).view.toSeq
    def cliques = toScala(getAllMaximalCliques)
    def biggestClique = {
      val cliques = toScala(getBiggestMaximalCliques)
      cliques.size match {
        case 0 => Seq[Turtle]()
        case 1 => cliques(0)
        case n => cliques(nlg.rng.nextInt(n))
      }
    }
  }
  // bamsix40
}