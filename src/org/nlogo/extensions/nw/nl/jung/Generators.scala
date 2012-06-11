package org.nlogo.extensions.nw.nl.jung

import org.nlogo.agent.AgentSet

import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator
import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator
import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator
import edu.uci.ics.jung.algorithms.generators.random.KleinbergSmallWorldGenerator
import edu.uci.ics.jung.algorithms.generators.Lattice2DGenerator

class Generator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  val rng = turtleBreed.world.mainRNG

  lazy val graphFactory = DummyGraph.factory
  lazy val undirectedGraphFactory = DummyGraph.undirectedFactory
  lazy val edgeFactory = DummyGraph.edgeFactory
  lazy val vertexFactory = DummyGraph.vertexFactory

  def lattice2D(rowCount: Int, colCount: Int, isToroidal: Boolean) =
    DummyGraph.importToNetLogo(new Lattice2DGenerator(
      graphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, isToroidal)
      .create, turtleBreed, linkBreed)

  def barabasiAlbert(nbVertices: Int) = {
    val gen = new BarabasiAlbertGenerator(
      graphFactory, vertexFactory, edgeFactory,
      1, 1, new java.util.HashSet[DummyGraph.Vertex])
    gen.setRandom(rng)
    while (gen.create.getVertexCount < nbVertices)
      gen.evolveGraph(1)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed)
  }

  def erdosRenyi(nbVertices: Int, connexionProbability: Double) = {
    val gen = new ErdosRenyiGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      nbVertices, connexionProbability)
    gen.setRandom(rng)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed)
  }

  def kleinbergSmallWorld(rowCount: Int, colCount: Int,
    clusteringExponent: Double, isToroidal: Boolean) = {
    val gen = new KleinbergSmallWorldGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, clusteringExponent, isToroidal)
    gen.setRandom(rng)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed)
  }

}

