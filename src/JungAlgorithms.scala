package org.nlogo.extensions.nw

import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import org.nlogo.agent.Agent
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.algorithms.util.KMeansClusterer
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality
import edu.uci.ics.jung.algorithms.importance.AbstractRanker

trait JungRanker {
  self: AbstractRanker[Turtle, Link] =>
  setRemoveRankScoresOnFinalize(false)
  evaluate
  def get(agent: Agent): Double = (agent match {
    case t: Turtle => Option(getVertexRankScores(getRankScoreKey).get(t)).map(_.doubleValue)
    case l: Link   => Option(getEdgeRankScores(getRankScoreKey).get(l)).map(_.doubleValue)
    case _         => None
  }).getOrElse(throw new ExtensionException(agent + "is not a member of this network snapshot"))
}

trait JungAlgorithms {
  self: JungGraph =>
  lazy val dijkstraShortestPath = new DijkstraShortestPath(this, nlg.isStatic)
  lazy val betweennessCentrality = new BetweennessCentrality(this) with JungRanker

  lazy val kMeansClusterer = new KMeansClusterer[Turtle] {
    private var clusters: Option[Map[Turtle, Int]] = None
    rand = self.nlg.world.mainRNG
    lazy val locations =
      self.nlg.turtles.map(t => t -> Array(t.xcor, t.ycor)).toMap.asJava

    def doKMeansClustering(nbClusters: Int, maxIterations: Int, convergenceThreshold: Double) {
      setMaxIterations(maxIterations)
      setConvergenceThreshold(convergenceThreshold)
      clusters = Some((for {
        (c, i) <- cluster(locations, nbClusters).asScala.zipWithIndex
        t <- c.keySet.asScala
      } yield (t, i)).toMap)
    }

    def getCluster(turtle: Turtle) = {
      clusters.map {
        _.getOrElse(turtle, throw new ExtensionException(
          "turtle is not a member of this network snapshot"))
      }.getOrElse(throw new ExtensionException(
        "do-k-means-clustering must be called on this network snapshot beforehand"))
    }
  }
}