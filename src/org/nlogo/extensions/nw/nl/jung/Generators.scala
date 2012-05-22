package org.nlogo.extensions.nw.nl.jung

import org.nlogo.agent.AgentSet

import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator
import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator
import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator
import edu.uci.ics.jung.algorithms.generators.random.KleinbergSmallWorldGenerator
import edu.uci.ics.jung.algorithms.generators.Lattice2DGenerator

/* TODO: VERY IMPORTANT!!! change RNG
 * The current generators use the java random number generator.
 * We need to fork Jung and modify them so we can plug in our own
 * Note: KleinbergSmallWorldGenerator is already OK
 */
class Generator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  val rng = turtleBreed.world.mainRNG

  lazy val graphFactory = DummyGraph.factory
  lazy val undirectedGraphFactory = DummyGraph.undirectedFactory
  lazy val edgeFactory = DummyGraph.edgeFactory
  lazy val vertexFactory = DummyGraph.vertexFactory

  def eppsteinPowerLaw(nbVertices: Int, nbEdges: Int, nbIterations: Int) =
    DummyGraph.importToNetLogo(new EppsteinPowerLawGenerator(
      graphFactory, vertexFactory, edgeFactory,
      nbVertices, nbEdges, nbIterations)
      .create, turtleBreed, linkBreed)

  def lattice2D(rowCount: Int, colCount: Int, isToroidal: Boolean) =
    DummyGraph.importToNetLogo(new Lattice2DGenerator(
      graphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, isToroidal)
      .create, turtleBreed, linkBreed)

  def barabasiAlbert(
    initialNbVertices: Int,
    nbEdgesPerIteration: Int,
    nbIterations: Int) = {
    val gen = new BarabasiAlbertGenerator(
      graphFactory, vertexFactory, edgeFactory,
      initialNbVertices, nbEdgesPerIteration, new java.util.HashSet[DummyGraph.Vertex])
    gen.evolveGraph(nbIterations)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed)
  }

  def erdosRenyi(nbVertices: Int, connexionProbability: Double) =
    DummyGraph.importToNetLogo(new ErdosRenyiGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      nbVertices, connexionProbability)
      .create, turtleBreed, linkBreed)

  def kleinbergSmallWorld(rowCount: Int, colCount: Int,
    clusteringExponent: Double, isToroidal: Boolean) = {
    val gen = new KleinbergSmallWorldGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, clusteringExponent, isToroidal)
    gen.setRandom(rng)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed)
  }

}

