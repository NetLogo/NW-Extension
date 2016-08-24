package org.nlogo.extensions.nw

import org.scalatest.FunSuite
import org.scalatest.GivenWhenThen

import org.nlogo.extensions.nw.algorithms.{Louvain, ClusteringMetrics}

class ClusteringTestSuite extends FunSuite {
  test("merged graphs count weights correctly for mixed multi-graph with self-links") {
    val graph = MixedMultiGraph(Seq(
      (0, 1, false),

      (0, 2, true), // community link 0 -> 1

      (2, 3, false),
      (2, 3, true),

      (2, 4, false), // community link 1 <-> 2

      (4, 5, false),
      (4, 5, false),

      (5, 6, true), // community link 2 -> 3
      (5, 6, true),

      (6, 6, false),

      (6, 7, true), // community link 3 -> 4
      (6, 7, false),

      (7, 7, true)
    ))

    val cs = Seq(Set(0,1), Set(2,3), Set(4,5), Set(6), Set(7)).map(Louvain.Com(_))

    val mGraph = Louvain.MergedGraph(graph, cs)

    // self-links
    assert(mGraph.weight((cs(0), cs(0))) === 2)
    assert(mGraph.weight((cs(1), cs(1))) === 3)
    assert(mGraph.weight((cs(2), cs(2))) === 4)
    assert(mGraph.weight((cs(3), cs(3))) === 2)
    assert(mGraph.weight((cs(4), cs(4))) === 1)

    // between-community links
    assert(mGraph.weight((cs(0), cs(1))) === 1)
    assert(mGraph.weight((cs(1), cs(2))) === 1)
    assert(mGraph.weight((cs(2), cs(3))) === 2)
    assert(mGraph.weight((cs(3), cs(4))) === 2)
  }
}
