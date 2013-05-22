// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.control.Breaks.break
import scala.util.control.Breaks.breakable
import org.nlogo.agent.Agent
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.NetworkExtensionUtil.LinkToRichLink
import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer
import org.nlogo.extensions.nw.util.TurtleSetsConverters.toTurtleSet
import org.nlogo.extensions.nw.util.TurtleSetsConverters.toTurtleSets
import edu.uci.ics.jung.{ algorithms => jungalg }

trait Ranker {
  self: jungalg.importance.AbstractRanker[Turtle, Link] =>

  private def toScoreMap[A <: Agent](m: java.util.Map[A, Number]) =
    m.asScala.map(x => x._1 -> x._2.doubleValue).toMap
  lazy val turtleScores = toScoreMap(getVertexRankScores(getRankScoreKey))
  lazy val linkScores = toScoreMap(getEdgeRankScores(getRankScoreKey))

  setRemoveRankScoresOnFinalize(false)
  evaluate

  private def getFrom(agent: Agent, tScores: Map[Turtle, Double], lScores: Map[Link, Double]) =
    (agent match {
      case t: Turtle => tScores.get(t)
      case l: Link   => lScores.get(l)
      case _         => None
    }).getOrElse(throw new ExtensionException(agent + " is not a member of this result set"))

  def get(agent: Agent) = getFrom(agent, turtleScores, linkScores)
}

trait Algorithms {
  self: Graph =>

  val dijkstraShortestPath = new RichDijkstra(_ => 1.0)

  def dijkstraShortestPath(variable: String) = {
    val weightFunction = (link: Link) => {
      val value = link.getBreedOrLinkVariable(variable)
      try value.asInstanceOf[java.lang.Number]
      catch {
        case e: Exception => throw new ExtensionException("Weight variable must be numeric.")
      }
    }
    new RichDijkstra(weightFunction)
  }

  class RichDijkstra(weightFunction: Function1[Link, java.lang.Number])
    extends jungalg.shortestpath.DijkstraShortestPath(self, weightFunction, true) {
    def meanLinkPathLength: Option[Double] = {
      import scala.util.control.Breaks._
      var sum = 0.0
      var n = 0
      breakable {
        for {
          source <- gc.turtles
          target <- gc.turtles
          if target != source
          distance = getDistance(source, target)
        } {
          if (distance == null) {
            sum = Double.NaN
            break
          }
          n += 1
          sum += distance.doubleValue
        }
      }
      Option(sum / n).filterNot(_.isNaN)
    }

    override def getPath(source: Turtle, target: Turtle) =
      try super.getPath(source, target)
      catch { case e: Exception => throw new ExtensionException(e) }

    override def getDistance(source: Turtle, target: Turtle) = {
      try super.getDistance(source, target)
      catch { case e: Exception => throw new ExtensionException(e) }
    }
  }

  object BetweennessCentrality
    extends jungalg.importance.BetweennessCentrality(this)
    with Ranker

  object EigenvectorCentrality extends jungalg.scoring.PageRank(this, 0.0) {
    evaluate()
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current graph")
      getVertexScore(turtle)
    }
  }

  object ClosenessCentrality extends jungalg.scoring.ClosenessCentrality(this) {
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current graph")
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
