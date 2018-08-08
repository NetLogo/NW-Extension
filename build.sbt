import org.nlogo.build.NetLogoExtension

scalaVersion := "2.12.0"

enablePlugins(NetLogoExtension)
enablePlugins(org.nlogo.build.ExtensionDocumentationPlugin)

name := "nw"

version := "1.1.0"

netLogoExtName      := "nw"

netLogoClassManager := "org.nlogo.extensions.nw.NetworkExtension"

netLogoTarget :=
  NetLogoExtension.directoryTarget(baseDirectory.value)

netLogoZipSources := false

scalaSource in Compile := baseDirectory.value / "src" / "main"

scalaSource in Test := baseDirectory.value / "src" / "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-feature",
                      "-encoding", "us-ascii")

libraryDependencies ++= Seq(
  "net.sf.jgrapht" % "jgrapht" % "0.8.3",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt" % "colt" % "1.2.0",
  "net.sf.jung" % "jung-algorithms" % "2.0.1",
  "net.sf.jung" % "jung-api" % "2.0.1",
  "net.sf.jung" % "jung-graph-impl" % "2.0.1",
  "net.sf.jung" % "jung-io" % "2.0.1",
  "org.gephi"   % "gephi-toolkit" % "0.8.2"
    from "https://s3.amazonaws.com/ccl-artifacts/gephi-toolkit-0.8.2-all.jar"
    intransitive
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.0" % "test",
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
    ((baseDirectory.value / "test").allPaths).filter { f =>
      f.getName.contains(".") && ! f.getName.endsWith(".scala")
    }
  for (file <- testResources.get)
    IO.copyFile(file, nwDirectory.value / "test" / IO.relativize(baseDirectory.value / "test", file).get)
}

test in Test := {
  moveToNwDir.value
  (test in Test).value
  IO.delete(nwDirectory.value)
}

netLogoVersion := "6.0.2-M1"
