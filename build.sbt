scalaVersion := "2.10.4"

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

javaSource in Compile := baseDirectory.value / "src" / "main"

javaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogoHeadless" % "6.0.0-M3" from
    "http://ccl.northwestern.edu/devel/6.0.0-M3/NetLogoHeadless.jar",
  "jgrapht" % "jgrapht-jdk1.6" % "0.8.3" from
    "http://ccl.northwestern.edu/devel/jgrapht-jdk1.6-0.8.3.jar",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt" % "colt" % "1.2.0",
  "net.sf.jung" % "jung-algorithms" % "2.0.1",
  "net.sf.jung" % "jung-api" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-io" % "2.0.1"
)

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogoHeadless-tests" % "6.0.0-M3" from
    "http://ccl.northwestern.edu/devel/6.0.0-M3/NetLogoHeadless-tests.jar",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "asm" % "asm-all" % "3.3.1" % "test"
)

artifactName := { (_, _, _) => "nw.jar" }

packageOptions +=
  Package.ManifestAttributes(
    ("Extension-Name", "nw"),
    ("Class-Manager", "org.nlogo.extensions.nw.NetworkExtension"),
    ("NetLogo-Extension-API-Version", "5.0"))

packageBin in Compile := {
  val jar = (packageBin in Compile).value
  val classpath = (dependencyClasspath in Runtime).value
  val base = baseDirectory.value
  val s = streams.value
  IO.copyFile(jar, base / "nw.jar")
  val libraryJarPaths =
    classpath.files.filter{path =>
      path.getName.endsWith(".jar") &&
      !path.getName.startsWith("scala-library")}
  for(path <- libraryJarPaths) {
    IO.copyFile(path, base / path.getName)
  }
  if(Process("git diff --quiet --exit-code HEAD").! == 0) {
    // copy everything thing we need for distribution in
    // a temporary "nw" directory, which we will then zip
    // before deleting it.
    IO.createDirectory(base / "nw")
    val zipExtras =
      (libraryJarPaths.map(_.getName) :+ "nw.jar")
        .filterNot(_ contains "NetLogo")
        .flatMap{ jar => Seq(jar) }
    for(extra <- zipExtras)
      IO.copyFile(base / extra, base / "nw" / extra)
    for (dir <- Seq("alternate-netlogolite", "demo"))
      IO.copyDirectory(base / dir, base / "nw" / dir)
    Process("zip -r nw.zip nw").!!
    IO.delete(base / "nw")
  }
  else {
    s.log.warn("working tree not clean; no zip archive made")
    IO.delete(base / "nw.zip")
  }
  jar
}

test in Test := {
  val _ = (packageBin in Compile).value
  (test in Test).value
}

cleanFiles ++= {
  val base = baseDirectory.value
  Seq(base / "nw.jar",
      base / "nw.zip")
}
