package org.nlogo.extensions.nw.jung

import org.nlogo.agent.AgentSet
import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator
import edu.uci.ics.jung.algorithms.generators.random.EppsteinPowerLawGenerator
import edu.uci.ics.jung.algorithms.generators.random.ErdosRenyiGenerator
import edu.uci.ics.jung.algorithms.generators.random.KleinbergSmallWorldGenerator
import edu.uci.ics.jung.algorithms.generators.Lattice2DGenerator
import java.util.Random

class Generator(
  turtleBreed: AgentSet,
  linkBreed: AgentSet) {

  lazy val graphFactory = DummyGraph.factoryFor(linkBreed)
  lazy val undirectedGraphFactory = DummyGraph.undirectedFactory
  lazy val edgeFactory = DummyGraph.edgeFactory
  lazy val vertexFactory = DummyGraph.vertexFactory

  def lattice2D(rowCount: Int, colCount: Int, isToroidal: Boolean, rng: Random) =
    DummyGraph.importToNetLogo(new Lattice2DGenerator(
      graphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, isToroidal)
      .create, turtleBreed, linkBreed, rng)

  def barabasiAlbert(nbVertices: Int, rng: Random) = {
    val gen = new BarabasiAlbertGenerator(
      graphFactory, vertexFactory, edgeFactory,
      1, 1, new java.util.HashSet[DummyGraph.Vertex])
    gen.setRandom(rng)
    while (gen.create.getVertexCount < nbVertices)
      gen.evolveGraph(1)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed, rng, sorted = true)
  }

  def erdosRenyi(nbVertices: Int, connexionProbability: Double, rng: Random) = {
    val gen = new ErdosRenyiGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      nbVertices, connexionProbability)
    gen.setRandom(rng)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed, rng)
  }

  def kleinbergSmallWorld(rowCount: Int, colCount: Int,
    clusteringExponent: Double, isToroidal: Boolean, rng: Random) = {
    val gen = new KleinbergSmallWorldGenerator(
      undirectedGraphFactory, vertexFactory, edgeFactory,
      rowCount, colCount, clusteringExponent, isToroidal)
    gen.setRandom(rng)
    DummyGraph.importToNetLogo(gen.create, turtleBreed, linkBreed, rng)
  }

}

