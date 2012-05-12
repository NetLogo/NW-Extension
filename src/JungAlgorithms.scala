package org.nlogo.extensions.nw

import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import org.nlogo.agent.Agent
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import Util.normalize
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.algorithms.util.KMeansClusterer
import edu.uci.ics.jung.algorithms.importance.AbstractRanker
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality
import edu.uci.ics.jung.algorithms.cluster.BicomponentClusterer
import edu.uci.ics.jung.algorithms.importance.RandomWalkBetweenness

object Util {
  def normalize[A <: Agent](xs: Map[A, Double]) = {
    val total = xs.values.sum
    xs.map(x => x._1 -> x._2 / total)
  }
}

trait JungRanker {
  self: AbstractRanker[Turtle, Link] =>

  private def toScoreMap[A <: Agent](m: java.util.Map[A, Number]) =
    m.asScala.map(x => x._1 -> x._2.doubleValue).toMap
  lazy val turtleScores = toScoreMap(getVertexRankScores(getRankScoreKey))
  lazy val linkScores = toScoreMap(getEdgeRankScores(getRankScoreKey))
  lazy val normalizedTurtleScores = normalize(turtleScores)
  lazy val normalizedLinkScores = normalize(linkScores)

  setRemoveRankScoresOnFinalize(false)
  evaluate

  private def getFrom(agent: Agent, tScores: Map[Turtle, Double], lScores: Map[Link, Double]) =
    (agent match {
      case t: Turtle => tScores.get(t)
      case l: Link   => lScores.get(l)
      case _         => None
    }).getOrElse(throw new ExtensionException(agent + "is not a member of this result set"))

  def get(agent: Agent) = getFrom(agent, turtleScores, linkScores)
  def getNormalized(agent: Agent) = getFrom(agent, normalizedTurtleScores, normalizedLinkScores)
}

trait JungAlgorithms {
  self: JungGraph =>
  lazy val dijkstraShortestPath = new DijkstraShortestPath(this, nlg.isStatic)
  lazy val betweennessCentrality = new BetweennessCentrality(this) with JungRanker

  lazy val kMeansClusterer = new KMeansClusterer[Turtle] {
    rand = self.nlg.world.mainRNG
    lazy val locations =
      self.nlg.turtles.map(t => t -> Array(t.xcor, t.ycor)).toMap.asJava

    def clusters(nbClusters: Int, maxIterations: Int, convergenceThreshold: Double) = {
      setMaxIterations(maxIterations)
      setConvergenceThreshold(convergenceThreshold)
      cluster(locations, nbClusters).asScala.map(_.keySet.asScala.toSeq).toSeq
    }
  }
}

trait UndirectedJungAlgorithms {
  self: UndirectedJungGraph =>
  lazy val randomWalkBetweenness = new RandomWalkBetweenness(this) with JungRanker

  lazy val bicomponentClusterer = new BicomponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
}

trait DirectedJungAlgorithms {
  self: DirectedJungGraph =>

}
