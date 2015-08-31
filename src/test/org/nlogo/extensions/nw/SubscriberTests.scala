// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.agent.TreeAgentSet
import org.nlogo.headless.HeadlessWorkspace
import org.scalatest.FunSuite
import org.scalatest.Matchers._
import org.scalatest.GivenWhenThen

class AgentSetChangeSubscribersTestSuite extends FunSuite with GivenWhenThen {

  def getSubscribers(agentSet: AnyRef): collection.Set[AgentSetChangeSubscriber] = {
    val pub = agentSet.asInstanceOf[TreeAgentSet].simpleChangeEventPublisher
    // get private `filters` field using reflection:
    val field = pub.getClass.getDeclaredField("scala$collection$mutable$Publisher$$filters")
    field.setAccessible(true)
    val filters = field.get(pub).asInstanceOf[collection.Map[AgentSetChangeSubscriber, _]]
    filters.keySet // the keySet is mutated with the map, so we can monitor it for changes
  }

  def checkSizes(expectedSizes: (collection.GenTraversable[_], Int)*) =
    for ((xs, n) <- expectedSizes) xs should have size n

  test("subscribers") {
    val ws: HeadlessWorkspace = HeadlessWorkspace.newInstance
    ws.setModelPath(new java.io.File("tests.txt").getPath)
    try {

      Given("a newly initialized workspace")
      ws.initForTesting(1, "extensions [nw]\n" + HeadlessWorkspace.TestDeclarations)

      Then("there should be no subscribers")
      val t = getSubscribers(ws.world.turtles)
      val l = getSubscribers(ws.world.links)
      val f = getSubscribers(ws.world.program.breeds.get("FROGS"))
      val m = getSubscribers(ws.world.program.breeds.get("MICE"))
      val u = getSubscribers(ws.world.program.linkBreeds.get("UNDIRECTED-LINKS"))
      val d = getSubscribers(ws.world.program.linkBreeds.get("DIRECTED-LINKS"))
      checkSizes(t -> 0, l -> 0, f -> 0, m -> 0, u -> 0, d -> 0)

      When("we `nw:set-context turtles links`")
      ws.command("nw:set-context turtles links")
      Then("turtles and links should get one subscriber each")
      checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)

      When("we create a single mouse")
      ws.command("create-mice 1")
      Then("subscriber counts should not change")
      checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)

      When("we create a couple of frogs with undir links to each other")
      ws.command("create-frogs 2 [ create-undirected-links-with other frogs ]")
      Then("subscriber counts should not change")
      checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)

      When("we explicitely get the context")
      ws.report("nw:get-context")
      Then("subscriber counts should not change either")
      checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)

      When("we set the context to the breeds")
      ws.command("nw:set-context frogs undirected-links")
      Then("turtles and links should loose their subscribers and frogs/undir-links should get theirs")
      checkSizes(t -> 0, l -> 0, f -> 1, m -> 0, u -> 1, d -> 0)

      When("we go back to the `turtles links` context")
      ws.command("nw:set-context turtles links")
      Then("only turtles and links should have subscribers")
      checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)

      locally {
        val turtleSub = t.head
        val linkSub = l.head
        When("we use `nw:with-context` to do something innocuous")
        ws.command("nw:with-context frogs undirected-links [" +
          "let d [ nw:distance-to one-of other frogs ] of one-of frogs ]")
        Then("the subscribers counts should not change")
        checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)
        And("the same old subscribers objects should still be subscribed")
        t.head should be theSameInstanceAs turtleSub
        l.head should be theSameInstanceAs linkSub
        When("we do something that verifies the context")
        ws.report("nw:get-context")
        Then("again, the counts and subscribers objects should not change")
        checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)
        t.head should be theSameInstanceAs turtleSub
        l.head should be theSameInstanceAs linkSub
        When("we do something within nw:with-context that should invalidate the cache")
        ws.command("nw:with-context frogs undirected-links [ ask one-of frogs [ die ] ]")
        Then("the subscribers counts should not change")
        checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)
        When("we do something that verifies the context")
        ws.command("let c nw:get-context")
        Then("the subscribers counts should not change")
        checkSizes(t -> 1, l -> 1, f -> 0, m -> 0, u -> 0, d -> 0)
        And("we should have new subscriber objects because of the invalidated context")
        t.head should not be theSameInstanceAs(turtleSub)
        l.head should not be theSameInstanceAs(linkSub)
      }

      When("we clear-all")
      ws.command("clear-all")
      Then("all subscribers should be gone")
      checkSizes(t -> 0, l -> 0, f -> 0, m -> 0, u -> 0, d -> 0)

    } finally ws.dispose()
  }
}
