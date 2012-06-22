package org.nlogo.extensions.nw.jung

import org.nlogo.agent.ArrayAgentSet
import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.mapAsJavaMapConverter
import scala.collection.JavaConverters.mapAsScalaMapConverter
import scala.collection.mutable
import org.nlogo.agent.Agent
import org.nlogo.agent.Link
import org.nlogo.agent.Turtle
import edu.uci.ics.jung.algorithms.cluster.BicomponentClusterer
import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath
import edu.uci.ics.jung.algorithms.util.KMeansClusterer
import edu.uci.ics.jung.algorithms.importance.AbstractRanker
import edu.uci.ics.jung.algorithms.importance.BetweennessCentrality
import edu.uci.ics.jung.algorithms.scoring.PageRank
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality
import org.nlogo.api.ExtensionException
import edu.uci.ics.jung.algorithms.filters.KNeighborhoodFilter
import java.util.Random

// TODO: catch exceptions from Jung and give meaningful error messages 

trait Ranker {
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
    }).getOrElse(throw new ExtensionException(agent + " is not a member of this result set"))

  def get(agent: Agent) = getFrom(agent, turtleScores, linkScores)
}

trait Algorithms {
  self: Graph =>

  private def functionToTransformer[I, O](f: Function1[I, O]) =
    new org.apache.commons.collections15.Transformer[I, O] {
      override def transform(i: I) = f(i)
    }

  lazy private val dijkstraMemo: mutable.Map[String, RichDijkstra] = mutable.Map()

  def dijkstraShortestPath =
    dijkstraMemo.getOrElseUpdate("1.0", new RichDijkstra(_ => 1.0))

  def dijkstraShortestPath(variable: String) =
    dijkstraMemo.getOrElseUpdate(variable,
      new RichDijkstra(_.getTurtleOrLinkVariable(variable).asInstanceOf[Double]))

  class RichDijkstra(weightFunction: Function1[Link, java.lang.Number])
    extends DijkstraShortestPath(self, functionToTransformer(weightFunction), true) {

    def meanLinkPathLength: Option[Double] = {

      // Build a seq of all optional lengths for paths from all nodes to all nodes,
      // where None means that there is no path between two nodes
      val pathLengths = for {
        source <- nlg.turtles.toSeq // toSeq makes sure we don't get a set
        target <- nlg.turtles.toSeq
        if target != source
        path = getPath(source, target)
        weights = path.asScala.map(weightFunction(_).doubleValue)
      } yield Option(weights).filterNot(_.isEmpty).map(_.sum)

      for {
        allLengths <- Option(pathLengths)
        if allLengths.nonEmpty // exclude the empty graph, returns None
        if allLengths.forall(_.isDefined) // exclude disconnected graphs, returns None
        lengths = allLengths.flatten
      } yield lengths.sum / lengths.size.toDouble

    }

  }

  lazy val betweennessCentrality = new BetweennessCentrality(this) with Ranker
  lazy val eigenvectorCentrality = new PageRank(this, 0.0) {
    evaluate()
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current snapshot")
      getVertexScore(turtle)
    }
  }
  lazy val closenessCentrality = new ClosenessCentrality(this) {
    def getScore(turtle: Turtle) = {
      if (!graph.containsVertex(turtle))
        throw new ExtensionException(turtle + " is not a member of the current snapshot")
      val res = getVertexScore(turtle)
      if (res.isNaN)
        Double.box(0.0) // for isolates
      else res
    }

  }

  lazy val kMeansClusterer = new KMeansClusterer[Turtle] {
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

  def kNeighborhood(source: Turtle, radius: Int, edgeType: KNeighborhoodFilter.EdgeType) = {
    val agents =
      if (radius == 0)
        // Jung's algorithm doesn't return the source turtle for radius 0, but we do want it
        Array[Agent](source)
      else
        new KNeighborhoodFilter(source, radius, edgeType)
          .transform(this.asSparseGraph) // TODO: ugly hack; fix when we fork jung
          .getVertices
          .asScala
          .toArray[Agent]
    new ArrayAgentSet(classOf[Turtle], agents, nlg.world)
  }
}

trait UndirectedAlgorithms extends Algorithms {
  self: UndirectedGraph =>
  lazy val bicomponentClusterer = new BicomponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
  lazy val weakComponentClusterer = new WeakComponentClusterer[Turtle, Link] {
    def clusters = transform(self).asScala.toSeq.map(_.asScala.toSeq)
  }
}

trait DirectedAlgorithms extends Algorithms {
  self: DirectedGraph =>
}
