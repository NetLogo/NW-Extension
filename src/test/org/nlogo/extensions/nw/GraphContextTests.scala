// (C) Uri Wilensky. https://github.com/NetLogo/NW-Extension

package org.nlogo.extensions.nw

import org.nlogo.headless.HeadlessWorkspace
import org.scalatest.FunSuite
import org.scalatest.GivenWhenThen

class SetContextTestSuite extends FunSuite with GivenWhenThen {
  test("avoid dangling links in context") {
    val ws: HeadlessWorkspace = HeadlessWorkspace.newInstance
    try {

      given("two mice linked together and a frog linked to one mouse")
      ws.initForTesting(1, "extensions [nw]\n" + HeadlessWorkspace.TestDeclarations)
      ws.command("create-mice 2")
      ws.command("create-frogs 1")
      ws.command("ask one-of mice [ create-links-with other turtles ]")
      when("we set the context to `mice links`")
      ws.command("nw:set-context mice links")

      val gc = networkExtension(ws).getGraphContext(ws.world)

      then("the context should contain the two mice")
      expect(2)(gc.turtles.size)
      and("only the one link between them")
      expect(1)(gc.links.size)

      and("the set of allEdges from all the turtles should be the same as `links`")
      expect(gc.turtles.flatMap(gc.allEdges))(gc.links)

    } finally ws.dispose()
  }
}
