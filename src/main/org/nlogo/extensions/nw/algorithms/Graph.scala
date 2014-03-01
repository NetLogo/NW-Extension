package org.nlogo.extensions.nw.algorithms

import collection.mutable
import org.nlogo.agent.{Turtle, Link}

trait Graph {
  private val preds: Map[(Turtle, Turtle), mutable.ArrayBuffer[Turtle]] =
    Map().withDefault((pair) => mutable.ArrayBuffer())
  private val dist: mutable.Map[(Turtle, Turtle), Int] = mutable.Map()
  private val traversals: mutable.Map[Turtle, Iterator[Turtle]] = mutable.Map()

  val rng: scala.util.Random
  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle]

  private def getTraversal(source: Turtle): Iterator[Turtle] = {
    traversals.getOrElseUpdate(source, cachingBFS(source))
  }
  private def cachedPath(source: Turtle, dest: Turtle): Option[List[Turtle]] = {
    if (source == dest) {
      Some(List())
    } else {
      val availablePreds = preds((source, dest))
      if (availablePreds.nonEmpty) {
        val pred = availablePreds(rng.nextInt(availablePreds.length))
        cachedPath(source, pred) map {pred :: _ }
      } else {
        None
      }
    }
  }

  def path(source: Turtle, dest: Turtle): Option[Iterable[Turtle]] = {
    cachedPath(source,dest) orElse {
      getTraversal(source) find { _==dest }
      cachedPath(source, dest)
    }
  }

  def distance(source: Turtle, dest: Turtle): Option[Int] = {
    dist get (source, dest) orElse {
      getTraversal(source) find { _==dest }
      dist get (source, dest)
    }
  }

  /*
  This allows us to calculate and store the min spanning tree of start lazily.
  As it traverses the tree, it stores the predecessor and distance information.
  Although the iterator returns one turtle at a time, data about turtles is
  computed a layer at a time so that the cache ends up with complete predecessor
  information for any turtle appearing there. This is crucial or else this class
  will thinks it's done computing paths for a certain pair when it has not.
   */
  private def cachingBFS(start: Turtle): Iterator[Turtle] = {
    dist((start, start)) = 0
    Iterator.iterate(List(start))((last) => {
      var layer: List[Turtle] = List()
      for {
        node <- last
        distance = dist((start, node))
        neighbor <- neighbors(node, true, false, true)
      } {
        if (!dist.contains((start, neighbor))) {
          dist((start, neighbor)) = distance + 1
          layer = neighbor :: layer
        }
        if (dist((start, neighbor)) == distance + 1) {
          preds((start, neighbor)).append(node)
        }
      }
      layer
    }).takeWhile(_.nonEmpty).flatten
  }
}
