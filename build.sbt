scalaVersion := "2.9.2"

scalaSource in Compile <<= baseDirectory(_ / "src" / "main")

scalaSource in Test <<= baseDirectory(_ / "src" / "test")

javaSource in Compile <<= baseDirectory(_ / "src" / "main")

javaSource in Test <<= baseDirectory(_ / "src" / "test")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

retrieveManaged := true

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogoLite" % "5.0.3" from
    "http://ccl.northwestern.edu/netlogo/5.0.3/NetLogoLite.jar",
  "jgrapht" % "jgrapht-jdk1.6" % "0.8.3" from
    "http://ccl.northwestern.edu/devel/jgrapht-jdk1.6-0.8.3.jar",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt" % "colt" % "1.2.0",
  "net.sf.jung" % "jung-algorithms" % "2.0.1",
  "net.sf.jung" % "jung-api" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-io" % "2.0.1"
)

artifactName := { (_, _, _) => "nw.jar" }

packageOptions +=
  Package.ManifestAttributes(
    ("Extension-Name", "nw"),
    ("Class-Manager", "org.nlogo.extensions.nw.NetworkExtension"),
    ("NetLogo-Extension-API-Version", "5.0"))


packageBin in Compile <<= (packageBin in Compile, dependencyClasspath in Runtime, baseDirectory, streams) map {
  (jar, classpath, base, s) =>
    IO.copyFile(jar, base / "nw.jar")
    val libraryJarPaths =
      classpath.files.filter{path =>
        path.getName.endsWith(".jar") &&
        path.getName != "scala-library.jar"}
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
          .flatMap{ jar => Seq(jar, jar + ".pack.gz") }
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

cleanFiles <++= baseDirectory { base =>
  Seq(base / "nw.jar",
      base / "nw.jar.pack.gz",
      base / "nw.zip") }
