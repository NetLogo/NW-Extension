resolvers += Resolver.url(
  "NetLogo-JVM",
  url("http://dl.bintray.com/content/netlogo/NetLogo-JVM"))(
    Resolver.ivyStylePatterns)

addSbtPlugin("org.nlogo" % "netlogo-extension-plugin" % "3.0")
addSbtPlugin("org.nlogo" % "netlogo-extension-documentation" % "0.7.0")
