// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.nlogo.agent.{World, Agent, Link, Turtle}
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.NetworkExtensionUtil.LinkToRichLink
import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer
import org.nlogo.extensions.nw.util.TurtleSetsConverters.toTurtleSets

import edu.uci.ics.jung.{ algorithms => jungalg }
import org.nlogo.agent.World.VariableWatcher

trait Algorithms {
  self: Graph =>

  val weightedGraphCaches: mutable.Map[String, jungalg.shortestpath.DijkstraShortestPath[Turtle, Link]] = new mutable.HashMap[String, jungalg.shortestpath.DijkstraShortestPath[Turtle, Link]]()

  val cacheInvalidator: World.VariableWatcher = new VariableWatcher {
    def update(agent: Agent, variable: String, value: scala.Any) = agent match {
      case link: Link => if (gc.links contains link) {
        weightedGraphCaches.remove(variable)
        gc.world.deleteWatcher(variable, cacheInvalidator)
      }
    }
  }

  def weightedDijkstraShortestPath(variable: String) = {
    getOrCreateCache(variable)
  }

  def getOrCreateCache(variable: String) = {
    weightedGraphCaches.getOrElseUpdate(variable, {
      val weightFunction = (link: Link) => {
        val value = link.getBreedOrLinkVariable(variable)
        try value.asInstanceOf[java.lang.Number]
        catch {
          case e: Exception => throw new ExtensionException("Weight variable must be numeric.")
        }
      }
      gc.world.addWatcher(variable, cacheInvalidator)
      new jungalg.shortestpath.DijkstraShortestPath(self, weightFunction, true)
    })
  }

  object BetweennessCentrality extends jungalg.importance.BetweennessCentrality(self) {

    private def toScoreMap[A <: Agent](m: java.util.Map[A, Number]) =
      m.asScala.map(x => x._1 -> x._2.doubleValue).toMap
    lazy val turtleScores = toScoreMap(getVertexRankScores(getRankScoreKey))
    lazy val linkScores = toScoreMap(getEdgeRankScores(getRankScoreKey))

    setRemoveRankScoresOnFinalize(false)
    evaluate()

    private def getFrom(agent: Agent, tScores: Map[Turtle, Double], lScores: Map[Link, Double]) =
      (agent match {
        case t: Turtle => tScores.get(t)
        case l: Link   => lScores.get(l)
        case _         => None
      }).getOrElse(throw new ExtensionException(agent + " is not a member of the current graph context."))

    def get(agent: Agent) = getFrom(agent, turtleScores, linkScores)
  }

  object EigenvectorCentrality extends jungalg.scoring.PageRank(this, 0.0) {
    evaluate()
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current graph context.")
      getVertexScore(turtle)
    }
  }

  object ClosenessCentrality extends jungalg.scoring.ClosenessCentrality(this) {
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current graph context.")
      val res = getVertexScore(turtle)
      if (res.isNaN)
        Double.box(0.0) // for isolates
      else res
    }
  }

  object WeakComponentClusterer
    extends jungalg.cluster.WeakComponentClusterer[Turtle, Link] {
    def clusters(rng: java.util.Random) = toTurtleSets(transform(self).asScala, gc.world, rng)
  }

}

trait UndirectedAlgorithms extends Algorithms {
  self: UndirectedGraph =>
  object BicomponentClusterer
    extends jungalg.cluster.BicomponentClusterer[Turtle, Link] {
    def clusters(rng: java.util.Random) = toTurtleSets(transform(self).asScala, gc.world, rng)
  }
}

trait DirectedAlgorithms extends Algorithms {
  self: DirectedGraph =>
}
