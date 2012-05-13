package org.nlogo.extensions.nw

import org.apache.commons.collections15.Factory
import edu.uci.ics.jung.graph.Graph
import edu.uci.ics.jung.graph._
import org.nlogo.agent.AgentSet
import org.nlogo.agent.Turtle
import scala.collection.JavaConverters._
import edu.uci.ics.jung.algorithms.generators.random._
import edu.uci.ics.jung.algorithms.generators.Lattice2DGenerator

/* TODO: VERY IMPORTANT!!! change RNG
 * The current generators use the java random number generator.
 * We need to fork Jung and modify them so we can plug in our own
 * Note: KleinbergSmallWorldGenerator is already OK
 */
class JungGraphGenerator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  protected class Edge

  val rng = turtleBreed.world.mainRNG

  lazy val graphFactory = new Factory[Graph[Turtle, Edge]]() {
    def create = new SparseGraph[Turtle, Edge]()
  }

  lazy val undirectedGraphFactory = new Factory[UndirectedGraph[Turtle, Edge]]() {
    def create = new UndirectedSparseGraph[Turtle, Edge]()
  }

  val vertexFactory = new Factory[Turtle]() {
    def create =
      turtleBreed.world.createTurtle(turtleBreed,
        rng.nextInt(14), // color
        rng.nextInt(360)) // heading
  }
  val edgeFactory = new Factory[Edge]() {
    def create = new Edge
  }

  protected def createLinks(graph: Graph[Turtle, Edge]) {
    for {
      headTurtle <- graph.getVertices.asScala.headOption
      linkManager = headTurtle.world.linkManager
      edge <- graph.getEdges.asScala
      endPoints = graph.getEndpoints(edge)
    } linkManager.createLink(endPoints.getFirst, endPoints.getSecond, linkBreed)
  }

  def eppsteinPowerLaw(nbVertices: Int, nbEdges: Int, nbIterations: Int) {
    createLinks(new EppsteinPowerLawGenerator(
      graphFactory, vertexFactory, edgeFactory,
      nbVertices, nbEdges, nbIterations)
      .create)
  }

  def lattice2D(rowCount: Int, colCount: Int, isToroidal: Boolean) {
    createLinks(new Lattice2DGenerator(
      graphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, isToroidal)
      .create)
  }

  def barabasiAlbert(
    initialNbVertices: Int,
    nbEdgesPerIteration: Int,
    nbIterations: Int) {
    val gen = new BarabasiAlbertGenerator(
      graphFactory, vertexFactory, edgeFactory,
      initialNbVertices, nbEdgesPerIteration, new java.util.HashSet[Turtle])
    gen.evolveGraph(nbIterations)
    createLinks(gen.create)
  }

  def erdosRenyi(nbVertices: Int, connexionProbability: Double) {
    createLinks(new ErdosRenyiGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      nbVertices, connexionProbability).create)
  }

  def kleinbergSmallWorld(rowCount: Int, colCount: Int, clusteringExponent: Double, isToroidal: Boolean) {
    val gen = new KleinbergSmallWorldGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, clusteringExponent, isToroidal)
    gen.setRandom(rng)
    createLinks(gen.create)
  }

}

