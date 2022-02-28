import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension)
enablePlugins(ExtensionDocumentationPlugin)

name       := "nw"
version    := "3.7.9"
isSnapshot := true

netLogoExtName      := "nw"
netLogoClassManager := "org.nlogo.extensions.nw.NetworkExtension"
netLogoTarget       := NetLogoExtension.directoryTarget(baseDirectory.value)
netLogoZipSources   := false
netLogoVersion      := "6.2.2"
netLogoTestExtras   += baseDirectory.value / "test"

scalaVersion := "2.12.12"
scalaSource in Compile := baseDirectory.value / "src" / "main"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-feature", "-encoding", "us-ascii")

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
    intransitive,
  "com.typesafe" % "config" % "1.3.1" % Test,
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)
