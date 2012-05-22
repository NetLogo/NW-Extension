ifeq ($(origin NETLOGO), undefined)
  NETLOGO=../..
endif

ifeq ($(origin SCALA_HOME), undefined)
  SCALA_HOME=../..
endif

ifneq (,$(findstring CYGWIN,$(shell uname -s)))
  COLON=\;
  SCALA_HOME := `cygpath -up "$(SCALA_HOME)"`
else
  COLON=:
endif

SRCS=$(wildcard src/org/nlogo/extensions/nw/*.scala src/org/nlogo/extensions/nw/nl/jung/*.scala src/org/nlogo/extensions/nw/nl/jgrapht/*.scala)

JARSPATH=jung-api-2.0.1.jar$(COLON)jung-graph-impl-2.0.1.jar$(COLON)jung-algorithms-2.0.1.jar$(COLON)collections-generic-4.01.jar$(COLON)colt-1.2.0.jar$(COLON)jung-io-2.0.1.jar$(COLON)jgrapht-jdk1.6.jar

network.jar network.jar.pack.gz: $(SRCS) manifest.txt Makefile
	mkdir -p classes
	$(SCALA_HOME)/bin/scalac -deprecation -unchecked -encoding us-ascii -classpath $(NETLOGO)/NetLogo.jar$(COLON)$(JARSPATH) -d classes $(SRCS)
	jar cmf manifest.txt network.jar -C classes .
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip network.jar.pack.gz network.jar

network.zip: network.jar
	rm -rf network
	mkdir network
	cp -rp network.jar network.jar.pack.gz README.md Makefile src manifest.txt tests.txt network
	zip -rv network.zip network
	rm -rf network
