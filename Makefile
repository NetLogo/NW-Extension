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
JARS=jung-api-2.0.1.jar jung-graph-impl-2.0.1.jar jung-algorithms-2.0.1.jar collections-generic-4.01.jar colt-1.2.0.jar jung-io-2.0.1.jar jgrapht-jdk1.6.jar
JARSPATH=jung-api-2.0.1.jar$(COLON)jung-graph-impl-2.0.1.jar$(COLON)jung-algorithms-2.0.1.jar$(COLON)collections-generic-4.01.jar$(COLON)colt-1.2.0.jar$(COLON)jung-io-2.0.1.jar$(COLON)jgrapht-jdk1.6.jar

nw.jar nw.jar.pack.gz: $(SRCS) manifest.txt Makefile $(JARS) $(addsuffix .pack.gz, $(JARS))
	mkdir -p classes
	$(SCALA_HOME)/bin/scalac -deprecation -unchecked -encoding us-ascii -classpath $(NETLOGO)/NetLogo.jar$(COLON)$(JARSPATH) -d classes $(SRCS)
	jar cmf manifest.txt nw.jar -C classes .
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip nw.jar.pack.gz nw.jar

nw.zip: nw.jar
	rm -rf nw
	mkdir nw
	cp -rp nw.jar nw.jar.pack.gz README.md Makefile src manifest.txt tests.txt nw
	zip -rv nw.zip nw
	rm -rf nw
	
jung-api-2.0.1.jar jung-api-2.0.1.jar.pack.gz:
	curl -f -S 'http://search.maven.org/remotecontent?filepath=net/sf/jung/jung-api/2.0.1/jung-api-2.0.1.jar' -o jung-api-2.0.1.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip jung-api-2.0.1.jar.pack.gz jung-api-2.0.1.jar
jung-graph-impl-2.0.1.jar jung-graph-impl-2.0.1.jar.pack.gz: 
	curl -f -S 'http://search.maven.org/remotecontent?filepath=net/sf/jung/jung-graph-impl/2.0.1/jung-graph-impl-2.0.1.jar' -o jung-graph-impl-2.0.1.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip jung-graph-impl-2.0.1.jar.pack.gz jung-graph-impl-2.0.1.jar
jung-algorithms-2.0.1.jar jung-algorithms-2.0.1.jar.pack.gz:
	curl -f -S 'http://search.maven.org/remotecontent?filepath=net/sf/jung/jung-algorithms/2.0.1/jung-algorithms-2.0.1.jar' -o jung-algorithms-2.0.1.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip jung-algorithms-2.0.1.jar.pack.gz jung-algorithms-2.0.1.jar
collections-generic-4.01.jar collections-generic-4.01.jar.pack.gz:
	curl -f -S 'http://search.maven.org/remotecontent?filepath=net/sourceforge/collections/collections-generic/4.01/collections-generic-4.01.jar' -o collections-generic-4.01.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip collections-generic-4.01.jar.pack.gz collections-generic-4.01.jar
colt-1.2.0.jar colt-1.2.0.jar.pack.gz:
	curl -f -S 'http://search.maven.org/remotecontent?filepath=colt/colt/1.2.0/colt-1.2.0.jar' -o colt-1.2.0.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip colt-1.2.0.jar.pack.gz colt-1.2.0.jar
jung-io-2.0.1.jar jung-io-2.0.1.jar.pack.gz:
	curl -f -S 'http://search.maven.org/remotecontent?filepath=net/sf/jung/jung-io/2.0.1/jung-io-2.0.1.jar' -o jung-io-2.0.1.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip jung-io-2.0.1.jar.pack.gz jung-io-2.0.1.jar
jgrapht-jdk1.6.jar jgrapht-jdk1.6.jar.pack.gz:
	curl -f -S 'http://ccl.northwestern.edu/devel/jgrapht-jdk1.6.jar' -o jgrapht-jdk1.6.jar
	pack200 --modification-time=latest --effort=9 --strip-debug --no-keep-file-order --unknown-attribute=strip jung-io-2.0.1.jar.pack.gz jung-io-2.0.1.jar
