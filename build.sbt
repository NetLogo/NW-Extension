scalaVersion := "2.9.2"

scalaSource in Compile <<= baseDirectory(_ / "src")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings",
                      "-encoding", "us-ascii")

libraryDependencies ++= Seq(
  "org.nlogo" % "NetLogoLite" % "5.0.2" from
    "http://ccl.northwestern.edu/netlogo/5.0.2/NetLogoLite.jar",
  "jgrapht" % "jgrapht-jdk1.6" % "0.8.3" from
    "http://ccl.northwestern.edu/devel/jgrapht-jdk1.6-0.8.3.jar",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt" % "colt" % "1.2.0",
  "net.sf.jung" % "jung-algorithms-2.0.2-nlfork" % "0.1" from
    "http://ccl.northwestern.edu/devel/jung-algorithms-2.0.2-nlfork-0.1.jar",
  "net.sf.jung" % "jung-api" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-io" % "2.0.1" exclude("net.sf.jung", "jung-algorithms")
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
    def pack200(name: String) {
      Process("pack200 --modification-time=latest --effort=9 --strip-debug " +
              "--no-keep-file-order --unknown-attribute=strip " +
              name + ".pack.gz " + name).!!
    }
    pack200("nw.jar")
    val libraryJarPaths =
      classpath.files.filter{path =>
        path.getName.endsWith(".jar") &&
        path.getName != "scala-library.jar"}
    for(path <- libraryJarPaths) {
      IO.copyFile(path, base / path.getName)
      pack200(path.getName)
    }
    if(Process("git diff --quiet --exit-code HEAD").! == 0) {
      Process("git archive -o nw.zip --prefix=nw/ HEAD").!!
      IO.createDirectory(base / "nw")
      val zipExtras = 
        (libraryJarPaths.map(_.getName) :+ "nw.jar")
          .filterNot(_ == "NetLogoLite-5.0.2.jar")
          .flatMap{ jar => Seq(jar, jar + ".pack.gz") }
      for(extra <- zipExtras)
        IO.copyFile(base / extra, base / "nw" / extra)
      Process("zip nw.zip " + zipExtras.map("nw/" + _).mkString(" ")).!!
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
