package org.nlogo.extensions.nw

import edu.uci.ics.jung.algorithms.cluster._
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.algorithms.util.KMeansClusterer
import org.nlogo.agent.Agent
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import org.nlogo.api.ExtensionException
import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import edu.uci.ics.jung.algorithms.importance.AbstractRanker
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality
import edu.uci.ics.jung.algorithms.importance.RandomWalkBetweenness
import edu.uci.ics.jung.algorithms.scoring._
import org.apache.commons.collections15.Transformer
import edu.uci.ics.jung.graph.Graph

// TODO: catch exceptions from Jung and give meaningful error messages 

trait JungRanker {
  self: AbstractRanker[Turtle, Link] =>

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
    }).getOrElse(throw new ExtensionException(agent + "is not a member of this result set"))

  def get(agent: Agent) = getFrom(agent, turtleScores, linkScores)
}

trait JungAlgorithms {
  self: JungGraph =>
  lazy val dijkstraShortestPath = new DijkstraShortestPath(this, nlg.isStatic)
  lazy val betweennessCentrality = new BetweennessCentrality(this) with JungRanker
  lazy val eigenvectorCentrality = new PageRank(this, 0.0) { evaluate() }
  lazy val closenessCentrality = new ClosenessCentrality(this) {
    override def getVertexScore(turtle: Turtle) =
      Option(super.getVertexScore(turtle))
        .filterNot(_.isNaN).getOrElse(0.0)
  }

  lazy val kMeansClusterer = new KMeansClusterer[Turtle] {
    rand = self.nlg.world.mainRNG
    lazy val locations =
      self.nlg.turtles.map(t => t -> Array(t.xcor, t.ycor)).toMap.asJava

    def clusters(nbClusters: Int, maxIterations: Int, convergenceThreshold: Double) =
      if (nlg.turtles.nonEmpty) {
        setMaxIterations(maxIterations)
        setConvergenceThreshold(convergenceThreshold)
        cluster(locations, nbClusters).asScala.map(_.keySet.asScala.toSeq).toSeq
      } else Seq()
  }

}

trait UndirectedJungAlgorithms {
  self: UndirectedJungGraph =>
  lazy val bicomponentClusterer = new BicomponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
  lazy val weakComponentClusterer = new WeakComponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
}

trait DirectedJungAlgorithms {
  self: DirectedJungGraph =>

}
