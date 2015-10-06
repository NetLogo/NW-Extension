import org.nlogo.build.NetLogoExtension

enablePlugins(NetLogoExtension)

scalaVersion := "2.11.7"

name := "nw"

netLogoClassManager := "org.nlogo.extensions.nw.NetworkExtension"

netLogoTarget :=
  NetLogoExtension.directoryTarget(baseDirectory.value)

netLogoZipSources := false

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-feature",
                      "-encoding", "us-ascii")

resolvers += "Gephi Releases" at "http://nexus.gephi.org/nexus/content/repositories/releases/"

val netLogoJarURL =
  Option(System.getProperty("netlogo.jar.url")).getOrElse("http://ccl.northwestern.edu/netlogo/5.3.0/NetLogo.jar")

val netLogoJarsOrDependencies = {
  import java.io.File
  import java.net.URI
  val urlSegments = netLogoJarURL.split("/")
  val lastSegment = urlSegments.last.replaceFirst("NetLogo", "NetLogo-tests")
  val testsUrl = (urlSegments.dropRight(1) :+ lastSegment).mkString("/")
  if (netLogoJarURL.startsWith("file:"))
    Seq(unmanagedJars in Compile ++= Seq(
      new File(new URI(netLogoJarURL)), new File(new URI(testsUrl))))
  else
    Seq(libraryDependencies ++= Seq(
      "org.nlogo" % "NetLogo" % "5.3.0" from netLogoJarURL,
      "org.nlogo" % "NetLogo-tests" % "5.3.0" % "test" from testsUrl))
}

netLogoJarsOrDependencies

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
  "org.ow2.asm" % "asm-all" % "5.0.3" % "test")

val moveToNwDir = taskKey[Unit]("move to nw directory")

val nwDirectory = settingKey[File]("directory that extension is moved to for testing")

nwDirectory := {
  baseDirectory.value / "extensions" / "nw"
}

moveToNwDir := {
  (packageBin in Compile).value
  val testTarget = NetLogoExtension.directoryTarget(nwDirectory.value)
  testTarget.create(NetLogoExtension.netLogoPackagedFiles.value)
  IO.createDirectory(nwDirectory.value / "test" / "tmp")
  val testResources =
    (baseDirectory.value / "test" ***).filter { f =>
      f.getName.contains(".") && ! f.getName.endsWith(".scala")
    }
  for (file <- testResources.get)
    IO.copyFile(file, nwDirectory.value / "test" / IO.relativize(baseDirectory.value / "test", file).get)
}

test in Test := {
  IO.createDirectory(nwDirectory.value)
  moveToNwDir.value
  (test in Test).value
  IO.delete(nwDirectory.value)
}
