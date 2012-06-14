# The "New" NetLogo Network Extension

This is a new, experimental, version of the Network Extension that is currently bundled with NetLogo (see https://github.com/NetLogo/Network-Extension for the current version of the extension.)

This version of the extension is **not** pre-installed in NetLogo 5.0.1. To use it, you will need to either build it yourself (see below) or download it from ***HERE***.

(For help with extensions in general, see the NetLogo User Manual.)

This extension is at a very early stage of development.  Users are invited to experiment with it and report any issues they might find [here on GitHub](https://github.com/nicolaspayette/netlogo-network/issues?state=open), but it should not be used for production code.

The source code for the extension is currently hosted online at
https://github.com/nicolaspayette/netlogo-network.

## Changes

Compared to the current extension, this new version offers:

- **Improved performance and functionality of existing features**: pathfinding primitives are now faster and allow taking edge weights into account.
- **Centrality measures**: calculate the betweenness centrality, closeness centrality and eigenvector centrality of the nodes in your network.
- **Clusterers**: find bicomponent and weak component clusters in your network.
- **Clique finder**: find all maximal cliques or the biggest maximal clique in your network.
- **Generators**: generate many different kinds of networks, namely, preferential attachment, random, small world, 2D lattice, ring, star, and wheel networks.
- **Import/Export**: save and load your networks using plain text matrix files.

There is also more to come in the future. This is just a very preliminary version of what we have in mind. Future versions will include import/export to other formats like [GraphML](http://graphml.graphdrawing.org/) and [Pajek](http://pajek.imfm.si/doku.php), some new network layouts, and more algorithms and measures.

To provide all this functionality, the Network Extension is relying on two external, popular and well-tested network librairies: [Jung](http://jung.sourceforge.net/) and [JGraphT](https://github.com/jgrapht/jgrapht).

## Usage



## Primitives

### network:in-link-radius, network:in-out-link-radius, network:in-in-link-radius

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-link-radius RADIUS LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-out-link-radius RADIUS LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `TURTLESET network:in-in-link-radius RADIUS LINK-BREED`

example: `ask one-of bankers [ show other bankers network:in-link-radius 5 friendships ]`

Returns the set of turtles within the given distance (number of links followed)
of the calling turtle.
Searches breadth-first from the calling turtle,
following links of the given link breed.

The `in-link-radius` form works with undirected links.  The other two
forms work with directed links; `out` or `in` specifies whether links
are followed in the normal direction (`out`), or in reverse (`in`).

### network:link-distance

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-distance TURTLE LINK-BREED`

example: `ask one-of-bankers [ show network:link-distance the-best-banker friendships ]`

Finds the distance to the destination turtle (number of links followed).
Searches breadth-first from the calling turtle,
following links of the given link breed.

Reports false if no path exists.

### network:link-path, link-path-turtles

![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-path TURTLE LINK-BREED`  
![turtle](https://github.com/NetLogo/Network-Extension/raw/master/turtle.gif) `network:link-path-turtles TURTLE LINK-BREED`

example: `ask banker1 [ show network:link-path banker3 friendships ]`
->   [(link 1 2) (link 2 3)]

example:`ask banker1 [ show network:link-path-turtles banker3 friendships ]`
->   [(banker 1) (banker 2) (banker 3)]
 
Reports a list of turtles or links following the shortest path from the calling
turtle to the destination turtle.

Reports an empty list if no path exists.

If `network:link-path-turtles` is used, the calling turtle and the
destination are included in the list.

Searches breadth-first from the calling turtle,
following links of the given link breed.

Follows links at the same depth in random order.  If there are
multiple shortest paths, a different path may be returned on
subsequent calls, depending on the random choices made during search.

### network:mean-link-path-length

`network:mean-link-path-length TURTLE-SET LINK-BREED`

Reports the average shortest-path length between all distinct pairs of
nodes in the given set of turtles, following links of the given link
breed.

Reports false unless paths exist between all pairs.

## Transition guide



## Building

The extension is written in Scala (version 2.9.1).

Unless you are compiling the extension from a `extensions/nw` under a NetLogo source code distribution, you will need to use the `NETLOGO` environment variable to point to your NetLogo directory (containing `NetLogo.jar`) and the `SCALA_HOME` variable to point to your Scala 2.9.1 installation. For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0.1 SCALA_HOME=/usr/local/scala-2.9.1.final make

Unless they are already present, the Makefile will download the needed Jung and JGraphT jar files from the Internet. Note that the extension requires a modified version of `jung-algorithms-2.0.2` (namely, `jung-algorithms-2.0.2-nlfork-0.1.jar`) which will be downloaded from the CCL server.

If compilation succeeds, `nw.jar` will be created. To use the extension, this file and all the other jars will need to be in the `extensions/nw` folder under your NetLogo installation.

## Credits

The first versions of the network primitives were written by Forrest Stonedahl. They were then ported to a NetLogo 5.0 extension by Seth Tisue.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo network extension is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
