// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw.jung

import java.util.Random
import scala.Option.option2Iterable
import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
import org.nlogo.agent.Agent
import org.nlogo.agent.ArrayAgentSet
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import org.nlogo.extensions.nw.NetworkExtensionUtil.LinkToRichLink
import org.nlogo.extensions.nw.NetworkExtensionUtil.functionToTransformer
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

  lazy private val dijkstraMemo: mutable.Map[String, RichDijkstra] = mutable.Map()

  def dijkstraShortestPath =
    dijkstraMemo.getOrElseUpdate("1.0", new RichDijkstra(_ => 1.0))

  def dijkstraShortestPath(variable: String) = {
    val weightFunction = (link: Link) => {
      val value = link.getBreedOrLinkVariable(variable)
      try value.asInstanceOf[java.lang.Number]
      catch {
        case e: Exception => throw new ExtensionException("Weight variable must be numeric.")
      }
    }
    dijkstraMemo.getOrElseUpdate(variable, new RichDijkstra(weightFunction))
  }

  class RichDijkstra(weightFunction: Function1[Link, java.lang.Number])
    extends jungalg.shortestpath.DijkstraShortestPath(self, weightFunction, true) {

    def meanLinkPathLength: Option[Double] = {
      import scala.util.control.Breaks._
      var sum = 0.0
      var n = 0
      breakable {
        for {
          source <- nlg.turtles
          target <- nlg.turtles
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

    override def getDistance(source: Turtle, target: Turtle) =
      try super.getDistance(source, target)
      catch { case e: Exception => throw new ExtensionException(e) }
  }

  object BetweennessCentrality
    extends jungalg.importance.BetweennessCentrality(this)
    with Ranker

  object EigenvectorCentrality extends jungalg.scoring.PageRank(this, 0.0) {
    evaluate()
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current snapshot")
      getVertexScore(turtle)
    }
  }

  object ClosenessCentrality extends jungalg.scoring.ClosenessCentrality(this) {
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current snapshot")
      val res = getVertexScore(turtle)
      if (res.isNaN)
        Double.box(0.0) // for isolates
      else res
    }
  }

  object KMeansClusterer extends jungalg.util.KMeansClusterer[Turtle] {
    lazy val locations =
      self.nlg.turtles.map(t => t -> Array(t.xcor, t.ycor)).toMap.asJava

    def clusters(nbClusters: Int, maxIterations: Int, convergenceThreshold: Double, rng: Random) =
      if (nlg.turtles.nonEmpty) {
        rand = rng
        setMaxIterations(maxIterations)
        setConvergenceThreshold(convergenceThreshold)
        cluster(locations, nbClusters).asScala.map(_.keySet.asScala.toSeq).toSeq
      } else Seq()
  }

  def kNeighborhood(
    source: Turtle,
    radius: Int,
    edgeType: jungalg.filters.KNeighborhoodFilter.EdgeType) = {
    val agents = new jungalg.filters.KNeighborhoodFilter(source, radius, edgeType)
      .transform(this.asSparseGraph) // TODO: ugly hack; fix when we fork jung
      .getVertices
      .asScala
      .toSet
      .+(source) // make sure source is there, as Jung doesn't include isolates
      .toArray[Agent]
    new ArrayAgentSet(classOf[Turtle], agents, nlg.world)
  }

  object WeakComponentClusterer extends jungalg.cluster.WeakComponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }

}

trait UndirectedAlgorithms extends Algorithms {
  self: UndirectedGraph =>
  object BicomponentClusterer extends jungalg.cluster.BicomponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
}

trait DirectedAlgorithms extends Algorithms {
  self: DirectedGraph =>
}
