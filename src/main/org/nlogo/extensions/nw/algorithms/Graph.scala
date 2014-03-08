package org.nlogo.extensions.nw.algorithms

import collection.mutable
import org.nlogo.agent.{Turtle, Link}
import scala.collection.mutable.ArrayBuffer

trait Graph {
  private val predecessors: mutable.Map[(Turtle, Turtle), mutable.ArrayBuffer[Turtle]] = mutable.Map()
  private val distances: mutable.Map[(Turtle, Turtle), Int] = mutable.Map()
  private val traversals: mutable.Map[Turtle, Iterator[Turtle]] = mutable.Map()

  val rng: scala.util.Random
  def neighbors(turtle: Turtle, includeUn: Boolean, includeIn: Boolean, includeOut: Boolean): Iterable[Turtle]

  private def getTraversal(source: Turtle): Iterator[Turtle] = {
    traversals.getOrElseUpdate(source, cachingBFS(source))
  }

  private def getPredecessor(source: Turtle, dest: Turtle): ArrayBuffer[Turtle] = {
    predecessors.getOrElseUpdate((source, dest), ArrayBuffer[Turtle]())
  }

  private def cachedPath(source: Turtle, dest: Turtle): Option[List[Turtle]] = {
    if (source == dest) {
      Some(List(source))
    } else {
      val availablePredecessors = getPredecessor(source, dest)
      if (availablePredecessors.nonEmpty) {
        val pred = availablePredecessors(rng.nextInt(availablePredecessors.length))
        cachedPath(source, pred) map {dest :: _ }
      } else {
        None
      }
    }
  }

  def path(source: Turtle, dest: Turtle): Option[Iterable[Turtle]] = {
    cachedPath(source,dest) orElse {
      getTraversal(source) find { _==dest }
      cachedPath(source, dest)
    } map { _.reverse }
  }

  def distance(source: Turtle, dest: Turtle): Option[Int] = {
    distances get (source, dest) orElse {
      getTraversal(source) find { _==dest }
      distances get (source, dest)
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
    distances((start, start)) = 0
    Iterator.iterate(List(start))((last) => {
      var layer: List[Turtle] = List()
      for {
        node <- last
        distance = distances((start, node))
        neighbor <- neighbors(node, true, false, true)
      } {
        if (!distances.contains((start, neighbor))) {
          distances((start, neighbor)) = distance + 1
          layer = neighbor :: layer
        }
        if (distances((start, neighbor)) == distance + 1) {
          getPredecessor(start, neighbor).append(node)
        }
      }
      layer
    }).takeWhile(_.nonEmpty).flatten
  }
}
