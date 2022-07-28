import org.nlogo.build.{ ExtensionDocumentationPlugin, NetLogoExtension }

enablePlugins(NetLogoExtension, ExtensionDocumentationPlugin)

name       := "nw"
version    := "3.7.9"
isSnapshot := true

netLogoExtName      := "nw"
netLogoClassManager := "org.nlogo.extensions.nw.NetworkExtension"
netLogoVersion      := "6.2.2"
netLogoTestExtras   += baseDirectory.value / "test"

scalaVersion := "2.12.12"
scalaSource in Compile := baseDirectory.value / "src" / "main"
scalaSource in Test    := baseDirectory.value / "src" / "test"
scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-feature", "-encoding", "us-ascii", "-Xlint")

resolvers ++= Seq(
  "gephi-thirdparty" at "https://raw.github.com/gephi/gephi/mvn-thirdparty-repo/"
)

libraryDependencies ++= Seq(
  "net.sf.jgrapht"              % "jgrapht"             % "0.8.3",
  "net.sourceforge.collections" % "collections-generic" % "4.01",
  "colt"                        % "colt"                % "1.2.0",
  "net.sf.jung"                 % "jung-algorithms"     % "2.0.1",
  "net.sf.jung"                 % "jung-api"            % "2.0.1",
  "net.sf.jung"                 % "jung-graph-impl"     % "2.0.1",
  "net.sf.jung"                 % "jung-io"             % "2.0.1",
  "org.gephi"                   % "gephi-toolkit"       % "0.9.6" classifier "all"
)
