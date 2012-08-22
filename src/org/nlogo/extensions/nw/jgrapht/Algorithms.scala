package org.nlogo.extensions.nw.jgrapht

import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import scala.collection.JavaConverters._
import org.jgrapht
import java.util.Random

trait Algorithms {
  self: Graph =>

  object BronKerboschCliqueFinder extends jgrapht.alg.BronKerboschCliqueFinder(this) {
    private def toScala(cliques: java.util.Collection[java.util.Set[Turtle]]) =
      cliques.asScala.map(_.asScala.view.toSeq).view.toSeq
    def cliques = toScala(getAllMaximalCliques)
    def biggestClique(rng: Random) = {
      val cliques = toScala(getBiggestMaximalCliques)
      cliques.size match {
        case 0 => Seq[Turtle]()
        case 1 => cliques(0)
        case n => cliques(rng.nextInt(n))
      }
    }
  }

}