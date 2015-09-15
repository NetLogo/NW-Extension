scalaVersion := "2.11.7"

enablePlugins(org.nlogo.build.NetLogoExtension)

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-feature",
                      "-encoding", "us-ascii")

resolvers += "Gephi Releases" at "http://nexus.gephi.org/nexus/content/repositories/releases/"

val netLogoJarsOrDependencies =
  Option(System.getProperty("netlogo.jar.url"))
    .orElse(Some("http://ccl.northwestern.edu/netlogo/5.3.0/NetLogo.jar"))
    .map { url =>
      import java.io.File
      import java.net.URI
      val testsUrl = url.stripSuffix(".jar") + "-tests.jar"
      if (url.startsWith("file:"))
        (Seq(new File(new URI(url)), new File(new URI(testsUrl))), Seq())
      else
        (Seq(), Seq(
          "org.nlogo" % "NetLogo" % "5.3.0" from url,
          "org.nlogo" % "NetLogo-tests" % "5.3.0" % "test" from testsUrl))
    }.get

unmanagedJars in Compile ++= netLogoJarsOrDependencies._1

libraryDependencies ++= netLogoJarsOrDependencies._2

libraryDependencies ++= Seq(
  "net.sf.jgrapht" % "jgrapht" % "0.8.3",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt" % "colt" % "1.2.0",
  "net.sf.jung" % "jung-algorithms" % "2.0.1",
  "net.sf.jung" % "jung-api" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-io" % "2.0.1",
  "org.gephi"   % "gephi-toolkit" % "0.8.2" classifier("all") intransitive
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm" % "asm-all" % "3.3.1" % "test"
)

netLogoExtName      := "nw"

netLogoClassManager := "org.nlogo.extensions.nw.NetworkExtension"

val moveToNwDir = taskKey[Unit]("move to nw directory")

val nwDirectory = settingKey[File]("directory that extension is moved to for testing")

nwDirectory := {
  baseDirectory.value / "extensions" / "nw"
}

moveToNwDir := {
  val nwJar = (packageBin in Compile).value
  val base = baseDirectory.value
  IO.createDirectory(nwDirectory.value)
  val allDependencies =
    Attributed.data((dependencyClasspath in Compile).value)
  val zipExtras =
    (allDependencies :+ nwJar)
      .filterNot(_.getName contains "NetLogo")
  for(extra <- zipExtras)
    IO.copyFile(extra, nwDirectory.value / extra.getName)
  for (dir <- Seq("alternate-netlogolite", "demo"))
    IO.copyDirectory(base / dir, nwDirectory.value / dir)
  IO.createDirectory(nwDirectory.value / "test" / "tmp")
  val testResources =
    (baseDirectory.value / "test" ***).filter { f =>
      f.getName.contains(".") && ! f.getName.endsWith(".scala")
    }
  for (file <- testResources.get)
    IO.copyFile(file, nwDirectory.value / "test" / IO.relativize(baseDirectory.value / "test", file).get)
}

packageBin in Compile := {
  val jar = (packageBin in Compile).value
  val nwZip = baseDirectory.value / "nw.zip"
  if (nwZip.exists) {
    IO.unzip(nwZip, baseDirectory.value)
    for (file <- (baseDirectory.value / "nw" ** "*.jar").get)
      IO.copyFile(file, baseDirectory.value / file.getName)
    IO.delete(baseDirectory.value / "nw")
  } else {
    sys.error("No zip file - nw extension not built")
  }
  jar
}

test in Test := {
  moveToNwDir.value
  (test in Test).value
  IO.delete(nwDirectory.value)
}
