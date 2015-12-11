# The NetLogo NW Extension for Network Analysis

This is the new extension for network analysis in NetLogo. It replaces the old Network Extension that came bundled with NetLogo (the old extension is [here](https://github.com/NetLogo/Network-Extension).) This extension comes bundled with NetLogo as of NetLogo 5.1.

While this extension is stable and we encourage using this instead of the old extension, we're continuing to add new features to it.  Users are invited to experiment with it and report any issues they might find [here on GitHub](https://github.com/NetLogo/NW-Extension/issues/new) or by submitting bugs to bugs@ccl.northwestern.edu. If there are any features you would like to see added, feel free to request them!

The source code for the extension is hosted online at
https://github.com/NetLogo/NW-Extension.

A much shorter version of this documentation, that can be useful as a cheat sheet, is [available as a PDF file](https://github.com/NetLogo/NW-Extension/blob/master/doc/cheat-sheet/nw-ext-cheat-sheet.pdf?raw=true).

## Index of Primitives

[General](#general)

- [set-context](#set-context), [get-context](#get-context), [with-context](#with-context)

[Path and Distance](#path-and-distance)

- [turtles-in-radius, turtles-in-reverse-radius](#turtles-in-radius-turtles-in-reverse-radius), [distance-to, weighted-distance-to](#distance-to-weighted-distance-to), [path-to, turtles-on-path-to, weighted-path-to, turtles-on-weighted-path-to](#path-to-turtles-on-path-to-weighted-path-to-turtles-on-weighted-path-to), [mean-path-length, mean-weighted-path-length](#mean-path-length-mean-weighted-path-length)

[Centrality](#centrality)

- [betweenness-centrality](#betweenness-centrality), [eigenvector-centrality](#eigenvector-centrality), [page-rank](#page-rank), [closeness-centrality](#closeness-centrality), [weighted-closeness-centrality](#weighted-closeness-centrality)

[Clustering Measures](#clustering-measures)

- [clustering-coefficient](#clustering-coefficient)

[Clusterers](#clusterers)

- [bicomponent-clusters](#bicomponent-clusters), [weak-component-clusters](#weak-component-clusters)

[Cliques](#cliques)

- [maximal-cliques](#maximal-cliques), [biggest-maximal-cliques](#biggest-maximal-cliques)

[Generators](#generators)

- [generate-preferential-attachment](#generate-preferential-attachment), [generate-random](#generate-random), [generate-small-world](#generate-small-world), [generate-lattice-2d](#generate-lattice-2d), [generate-ring](#generate-ring), [generate-star](#generate-star), [generate-wheel, generate-wheel-inward, generate-wheel-outward](#generate-wheel-generate-wheel-inward-generate-wheel-outward)

[Import and Export](#import-and-export)

- [save-matrix](#save-matrix), [load-matrix](#load-matrix), [save-graphml](#save-graphml), [load-graphml](#load-graphml)
- [load, load-dl, load-gdf, load-gexf, load-gml, load-vna](#load)
- [save, save-dl, save-gdf, save-gexf, save-gml, save-vna](#save)

## Changes

Compared to the previous extension, this new version offers:

- **Improved functionality of existing features**: pathfinding primitives now allow taking edge weights into account.
- **Centrality measures**: calculate the betweenness centrality, closeness centrality, PageRank, and eigenvector centrality of the nodes in your network.
- **Clusterers**: find bicomponent and weak component clusters in your network.
- **Clique finder**: find all maximal cliques or the biggest maximal clique in your network.
- **Generators**: generate many different kinds of networks, namely, preferential attachment, random, small world, 2D lattice, ring, star, and wheel networks.
- **Import/Export**: save and load your networks using plain text matrix files, or export them to [GraphML](http://graphml.graphdrawing.org/).

To provide some of this functionality, the Network Extension is relying on two external, popular and well-tested network libraries: [Jung](http://jung.sourceforge.net/) and [JGraphT](https://github.com/jgrapht/jgrapht).

## Usage

The first thing that one needs to understand in order to work with the network extension is how to tell the extension _which_ network to work with. Consider the following example situation:

    breed [ bankers banker ]
    breed [ clients client ]

    undirected-link-breed [ friendships friendship ]
    directed-link-breed [ accounts account ]

Basically, you have bankers and clients. Clients can have accounts with bankers. Bankers can probably have account with other bankers, and anyone can be friends with anyone.

Now we might want to consider this whole thing as one big network. If that is the case, there is nothing special to do: by default, the NW extension primitives consider all turtles and all links to be part of the current network.

We could also, however, be only interested in a subset of the network. Maybe we want to consider only friendship relations. Furthermore, maybe we want to consider only the friendships _between bankers_. After all, having a very high centrality in a network of banker friendships is very different from having a high centrality in a network of client friendships.

To specify such networks, we need to tell the extension _both_ which turtles _and_ which links we are interested in. All the turtles from the specified set of turtles will be included in the network, and only the links from the specified set of links that are between turtles of the specified set will be included. For example, if you ask for `bankers` and `friendships`, even the lonely bankers with no friends will be included, but friendship links between bankers and clients will **not** be included. The way to tell the extension about this is with the [`nw:set-context`](#set-context) primitive, which you must call _prior_ to doing any operations on a network.

Some examples:

- `nw:set-context turtles links` will give you everything: bankers and clients, friendships and accounts, as one big network.
- `nw:set-context turtles friendships` will give you all the bankers and clients and friendships between any of them.
- `nw:set-context bankers friendships` will give you all the bankers, and only friendships between bankers.
- `nw:set-context bankers links` will give you all the bankers, and any links between them, whether these links are friendships or accounts.
- `nw:set-context clients accounts` will give you all the clients, and accounts between each other, but since in our fictional example clients can only have accounts with bankers, this will be a completely disconnected network.

### Special agentsets vs normal agentsets

It must be noted that NetLogo has two types of agentsets that behave slightly differently, and that this has an impact on the way `nw:set-context` works. We will say a few words about these concepts here but, for a thorough understanding, it is highly recommended that you read [the section on agentsets in the NetLogo programming guide](http://ccl.northwestern.edu/netlogo/docs/programming.html#agentsets).

The "special" agentsets in NetLogo are `turtles`, `links` and the different "breed" agentsets. What is special about them is that they can grow: if you create a new turtle, it will be added to the `turtles` agentset. If you have a `bankers` breed and you create a new banker, it will be added to the `bankers` agentset and to the `turtles` agentset. Same goes for links. Other agentsets, such as those created with the `with` primitive (e.g., `turtles with [ color = red ]`) or the `turtle-set` and `link-set` primitives) are never added to. The content of normal agentsets will only change if the agents that they contain die.

To show how different types of agentsets interact with [`nw:set-context`](#set-context), let's create a very simple network:

    clear-all
    create-turtles 3 [ create-links-with other turtles ]

Let's set the context to `turtles` and `links` (which is the default anyway) and use [`nw:get-context`](#get-context) to see what we have:

    nw:set-context turtles links
    show map sort nw:get-context

 We get all three turtles and all three links:

    [[(turtle 0) (turtle 1) (turtle 2)] [(link 0 1) (link 0 2) (link 1 2)]]

Now let's kill one turtle:

    ask one-of turtles [ die ]
    show map sort nw:get-context

As expected, the context is updated to reflect the death of the turtle and of the two links that died with it:

    [[(turtle 0) (turtle 1)] [(link 0 1)]]

What if we now create a new turtle?

    create-turtles 1
    show map sort nw:get-context

Since our context is using the special `turtles` agentset, the new turtle is automatically added:

    [[(turtle 0) (turtle 1) (turtle 3)] [(link 0 1)]]

Now let's demonstrate how it works with normal agentsets. We start over with a new network of red turtles:

    clear-all
    create-turtles 3 [
      create-links-with other turtles
      set color red
    ]

And we set the context to `turtles with [ color = red ])` and `links`

    nw:set-context (turtles with [ color = red ]) links
    show map sort nw:get-context

Since all turtles are red, we get everything in our context:

    [[(turtle 0) (turtle 1) (turtle 2)] [(link 0 1) (link 0 2) (link 1 2)]]

But what if we ask one of them to turn blue?

    ask one-of turtles [ set color blue ]
    show map sort nw:get-context

No change. The agentset used in our context remains unaffected:

    [[(turtle 0) (turtle 1) (turtle 2)] [(link 0 1) (link 0 2) (link 1 2)]]

If we kill one of them, however...

    ask one-of turtles [ die ]
    show map sort nw:get-context

It gets removed from the set:

    [[(turtle 0) (turtle 2)] [(link 0 2)]]

What if we add a new red turtle?

    create-turtles 1 [ set color red ]
    show map sort nw:get-context

Nope:

    [[(turtle 0) (turtle 2)] [(link 0 2)]]

**A final note regarding the different types of agentsets**: because of how they are implemented in NetLogo and handled in the NW extension, it is likely that special agentsets will perform better for some of the extension's primitives. So, if you can, prefer a context referring to `turtles` and `links` or simple breeds, instead of agentsets built with `with`, `turtle-set`, etc.

## Primitives

### General

#### set-context

<tt>nw:set-context <i>turtleset linkset</i></tt>

Specifies the set of turtles and the set of links that the extension will consider to be the current graph. All the turtles from _turtleset_ and all the links from _linkset_ that connect two turtles from _turtleset_ will be included.

This context is used by all other primitives (unless specified otherwise) until a new context is specified. (At the moment, only the [generator primitives](#generators) and the file input primitives ([`nw:load-matrix`](#load-matrix) and [`nw:load-graphml`](#load-graphml)) are exceptions to this rule.)

See [the usage section](#usage) for a much more detailed explanation of `nw:set-context`.

#### get-context

`nw:get-context`

Reports the content of the current graph context as a list containing two agentsets: the agentset of turtles that are part of the context and the agentset of links that are part of the context.

Let's say with start with a blank slate and the default context consisting of `turtles` and `links`, `nw:get-context` will report a list the special `turtles` and `links` breed agentsets:

```
observer> clear-all
observer> show nw:get-context
observer: [turtles links]
```

If we add some turtles and links to our context, we'll still see the same thing, even though `turtles` and `links` have internally grown:

```
observer> crt 2 [ create-links-with other turtles ]
observer> show nw:get-context
observer: [turtles links]
```

If you had set your context to normal agentsets instead (built with `turtle-set`, `link-set` or `with`) here is what you would see:

```
observer> clear-all
observer> nw:set-context turtle-set turtles link-set links
observer> show nw:get-context
observer: [(agentset, 0 turtles) (agentset, 0 links)]
```

If you then create new turtles and links, they are not added to the context because normal agentsets don't grow (see [Special agentsets vs normal agentsets](#special-agentsets-vs-normal-agentsets)):

```
observer> crt 2 [ create-links-with other turtles ]
observer> show nw:get-context
observer: [(agentset, 0 turtles) (agentset, 0 links)]
```

But if you construct new agentsets and set the context to them, your new agents will be there:

```
observer> nw:set-context turtle-set turtles link-set links
observer> show nw:get-context
observer: [(agentset, 2 turtles) (agentset, 1 link)]
```

If you want to see the actual content of your context, it is easy to turn your agentsets into lists that can be nicely displayed. Just use a combination of [`map`](http://ccl.northwestern.edu/netlogo/docs/dictionary.html#map) and [`sort`](http://ccl.northwestern.edu/netlogo/docs/dictionary.html#sort):

```
observer> show map sort nw:get-context
observer: [[(turtle 0) (turtle 1)] [(link 0 1)]]
```

Finally, you can use `nw:get-context` to store a context that you eventually want to restore:

```
extensions [ nw ]
to store-and-restore-context
  clear-all
  crt 2 [
    set color red
    create-links-with other turtles with [ color = red ] [
      set color yellow
    ]
  ]
  crt 2 [
    set color blue
    create-links-with other turtles with [ color = blue ] [
      set color green
    ]
  ]
  nw:set-context turtles with [ color = red ] links with [ color = yellow ]
  show map sort nw:get-context
  let old-turtles item 0 nw:get-context
  let old-links item 1 nw:get-context
  nw:set-context turtles with [ color = blue ] links with [ color = green ]
  show map sort nw:get-context
  nw:set-context old-turtles old-links
  show map sort nw:get-context
end
```

Here is the result:

```
observer> store-and-restore-context
observer: [[(turtle 0) (turtle 1)] [(link 0 1)]]
observer: [[(turtle 2) (turtle 3)] [(link 2 3)]]
observer: [[(turtle 0) (turtle 1)] [(link 0 1)]]
```

#### with-context

<tt>nw:with-context <i>turtleset linkset command-block</i></tt>

Executes the _command-block_ with the context temporarily set to _turtleset_ and _linkset_.
After _command-block_ finishes running, the previous context will be restored.

For example:

```
observer> create-turtles 3 [ create-links-with other turtles ]
observer> nw:with-context (turtle-set turtle 0 turtle 1) (link-set link 0 1) [ show nw:get-context ]
observer: [(agentset, 2 turtles) (agentset, 1 link)
observer> show nw:get-context
observer: [turtles links]
```

If you have NW extension code running in two forever buttons or `loop` blocks that each need to use different contexts, you should use `nw:with-context` in each to make sure they are operating in the correct context.

### Path and Distance

#### turtles-in-radius, turtles-in-reverse-radius

![turtle][turtle] <tt>nw:turtles-in-radius <i>radius</i></tt>

![turtle][turtle] <tt>nw:turtles-in-reverse-radius <i>radius</i></tt>

Returns the set of turtles within the given distance (number of links followed) of the calling turtle in the current context. Both forms include the calling turtle, whom you can exclude with `other` if need be.

The `turtles-in-radius` form will follow both undirected links and directed **out** links. The `turtles-in-reverse-radius` form will follow both undirected links and directed **in** links. You can think of `turtles-in-radius` as "turtles **who I can get to** in _radius_ steps" and of `turtles-in-reverse-radius` as "turtles **who can get to me** in _radius_ steps".

If you want the primitive to follow only undirected links or only directed links, you can do it by setting the context appropriately. For example: `nw:set-context turtles undir-links` (assuming `undir-links` is an undirected link breed) or `nw:set-context turtles dir-links` (assuming `dir-links` is a directed link breed).

##### Example:

    clear-all
    create-turtles 5
    ask turtle 0 [ create-link-with turtle 1 ]
    ask turtle 0 [ create-link-with turtle 2 ]
    ask turtle 1 [ create-link-with turtle 3 ]
    ask turtle 2 [ create-link-with turtle 4 ]
    ask turtle 0 [
      show sort nw:turtles-in-radius 1
    ]

Will output:

    (turtle 0): [(turtle 0) (turtle 1) (turtle 2)]

As you may have noticed, the result includes the calling turtle. This mimics the behavior of the regular NetLogo [`in-radius`](#in-radius) primitive.

#### distance-to, weighted-distance-to

![turtle][turtle] <tt>nw:distance-to <i>target-turtle</i></tt>

![turtle][turtle] <tt>nw:weighted-distance-to <i>target-turtle weight-variable-name</i></tt>

Finds the shortest path to the target turtle and reports the total distance for this path, or false if no path exists in the current context.

The `nw:distance-to` version of the primitive assumes that each link counts for a distance of one. The `nw:weighted-distance-to` version accepts a _weight-variable-name_ parameter, which must be **a string** naming the link variable to use as the weight of each link in distance calculations. The weights cannot be negative numbers.

##### Example:

    links-own [ weight ]
    to go
      clear-all
      create-turtles 5
      ask turtle 0 [ create-link-with turtle 1 [ set weight 2.0 ] ]
      ask turtle 1 [ create-link-with turtle 2 [ set weight 2.0 ] ]
      ask turtle 0 [ create-link-with turtle 3 [ set weight 0.5 ] ]
      ask turtle 3 [ create-link-with turtle 4 [ set weight 0.5 ] ]
      ask turtle 4 [ create-link-with turtle 2 [ set weight 0.5 ] ]
      ask turtle 0 [ show nw:distance-to turtle 2 ]
      ask turtle 0 [ show nw:weighted-distance-to turtle 2 "weight" ]
    end

Will output:

    (turtle 0): 2
    (turtle 0): 1.5

#### path-to, turtles-on-path-to, weighted-path-to, turtles-on-weighted-path-to

![turtle][turtle] <tt>nw:path-to <i>target-turtle</i></tt>

![turtle][turtle] <tt>nw:turtles-on-path-to <i>target-turtle</i></tt>

![turtle][turtle] <tt>nw:weighted-path-to <i>target-turtle weight-variable-name</i></tt>

![turtle][turtle] <tt>nw:turtles-on-weighted-path-to <i>target-turtle weight-variable-name</i></tt>

Finds the shortest path to the target turtle and reports the actual path between the source and the target turtle. The `nw:path-to` and `nw:weighted-path-to` variants will report the list of links that constitute the path, while the `nw:turtles-on-path-to` and `nw:turtles-on-weighted-path-to` variants will report the list of turtles along the path, including the source and destination turtles.

As with the link distance primitives, the `nw:weighted-path-to` and `nw:turtles-on-weighted-path-to` accept a _weight-variable-name_ parameter, which must be **a string** naming the link variable to use as the weight of each link in distance calculations. The weights cannot be negative numbers.

If no path exist between the source and the target turtles, all primitives will report an empty list.

Note that the NW-Extension remembers paths that its calculated previously unless the network changes. Thus, you don't need to store paths to efficiently move across the network; you can just keep re-calling one of the path primitives. If the network changes, however, the stored answers are forgotten.

##### Example:

    links-own [ weight ]
    to go
      clear-all
      create-turtles 5
      ask turtle 0 [ create-link-with turtle 1 [ set weight 2.0 ] ]
      ask turtle 1 [ create-link-with turtle 2 [ set weight 2.0 ] ]
      ask turtle 0 [ create-link-with turtle 3 [ set weight 0.5 ] ]
      ask turtle 3 [ create-link-with turtle 4 [ set weight 0.5 ] ]
      ask turtle 4 [ create-link-with turtle 2 [ set weight 0.5 ] ]
      ask turtle 0 [ show nw:path-to turtle 2 ]
      ask turtle 0 [ show nw:turtles-on-path-to turtle 2 ]
      ask turtle 0 [ show nw:weighted-path-to turtle 2 "weight" ]
      ask turtle 0 [ show nw:turtles-on-weighted-path-to turtle 2 "weight" ]
    end

Will output:

    (turtle 0): [(link 0 1) (link 1 2)]
    (turtle 0): [(turtle 0) (turtle 1) (turtle 2)]
    (turtle 0): [(link 0 3) (link 3 4) (link 2 4)]
    (turtle 0): [(turtle 0) (turtle 3) (turtle 4) (turtle 2)]

#### mean-path-length, mean-weighted-path-length

<tt>nw:mean-path-length</tt>

<tt>nw:mean-weighted-path-length <i>weight-variable-name</i></tt>

Reports the average shortest-path length between all distinct pairs of nodes in the current context. If the `nw:mean-weighted-path-length` is used, the distances will be calculated using _weight-variable-name_. The weights cannot be negative numbers.

Reports false unless paths exist between all pairs.

##### Example:

    links-own [ weight ]
    to go
      clear-all
      create-turtles 3
      ask turtle 0 [ create-link-with turtle 1 [ set weight 2.0 ] ]
      ask turtle 1 [ create-link-with turtle 2 [ set weight 2.0 ] ]
      show nw:mean-path-length
      show nw:mean-weighted-path-length "weight"
      create-turtles 1 ; create a new, disconnected turtle
      show nw:mean-path-length
      show nw:mean-weighted-path-length "weight"
    end

Will ouput:

    observer: 1.3333333333333333
    observer: 2.6666666666666665
    observer: false
    observer: false

### Centrality

#### betweenness-centrality
![turtle][turtle] `nw:betweenness-centrality`

To calculate the [betweenness centrality](http://en.wikipedia.org/wiki/Betweenness_centrality) of a turtle, you take every other possible pairs of turtles and, for each pair, you calculate the proportion of shortest paths between members of the pair that passes through the current turtle. The betweenness centrality of a turtle is the sum of these.

As of now, link weights are not taken into account.

#### eigenvector-centrality
![turtle][turtle] `nw:eigenvector-centrality`

The [Eigenvector centrality](http://en.wikipedia.org/wiki/Centrality#Eigenvector_centrality) of a node can be thought of as the amount of influence a node has on a network. In practice, turtles that are connected to a lot of other turtles that are themselves well-connected (and so on) get a higher Eigenvector centrality score.

Eigenvector centrality is only defined for connected networks, and the primitive will report `false` for disconnected graphs. (Just like `distance-to` does when there is no path to the target turtle.)

In this implementation, the eigenvector centrality is normalized such that the highest eigenvector centrality a node can have is 1. This implementation is designed to agree with Gephi's implementation out to at least 3 decimal places. If you discover that it disagrees with Gephi on a particular network, please [report it](https://github.com/NetLogo/NW-Extension/issues/new).

The primitive respects link direction, even in mixed-directed networks. This is the one place where it should disagree with Gephi; Gephi refuses to treat directed links as directed in mixed-networks.

As of now, link weights are not taken into account.

#### page-rank
![turtle][turtle] `nw:page-rank`

The [page rank](https://en.wikipedia.org/wiki/PageRank) of a node can be thought of as the proportion of time that an agent walking forever at random on the network would spend at this node. The agent has an equal chance of taking any of a nodes edges, and will jump around the network completely randomly 15% of the time. In practice, like with eigenvector centrality, turtles that are connected to a lot of other turtles that are themselves well-connected (and so on) get a higher page rank.

Page rank is one of the several algorithms that search engines use to determine the importance of a website.

The sum of all page rank values should be approximately one. Unlike eigenvector centrality, page rank is defined for all networks, no matter the connectivity. Currently, it treats all links as undirected links.

As of now, link weights are not taken into account.

#### closeness-centrality
![turtle][turtle] `nw:closeness-centrality`

The [closeness centrality](http://en.wikipedia.org/wiki/Centrality#Closeness_centrality) of a turtle is defined as the inverse of the average of it's distances to all other turtles. (Some people use the sum of distances instead of the average, but the extension uses the average.)

Note that this primitive reports the _intra-component_ closeness of a turtle, that is, it takes into account only the distances to the turtles that are part of the same [component](http://en.wikipedia.org/wiki/Connected_component_%28graph_theory%29) as the current turtle, since distance to turtles in other components is undefined. The closeness centrality of an isolated turtle is defined to be zero.

#### weighted-closeness-centrality
![turtle][turtle] <tt>nw:weighted-closeness-centrality <i>weight-variable-name</i></tt>

This is identical to [closeness-centrality](#closeness-centrality), except that weights provided by the given variable are treated as the distances of links.

### Clustering Measures

#### clustering-coefficient
![turtle][turtle] `nw:clustering-coefficient`

Reports the [local clustering coefficient](http://en.wikipedia.org/wiki/Clustering_coefficient#Local_clustering_coefficient) of the turtle. The clustering coefficient of a node measures how connected its neighbors are. It is defined as the number of links between the node's neighbors divided by the total number of possible links between its neighbors.

`nw:clustering-coefficient` takes the directedness of links into account. A directed link counts as a single link whereas an undirected link counts as two links (one going one-way, one going the other).

The [global clustering coefficient](http://en.wikipedia.org/wiki/Clustering_coefficient#Global_clustering_coefficient) measures how much nodes tend to cluster together in the network in general. It is defined based on the types of triplets in the network. A triplet consists of a central node and two of its neighbors. If its neighbors are also connected, it's a closed triplet. If its neighbors are not connected, it's an open triplet. The global clustering coefficient is simply the number of closed triplets in a network divided by the total number of triplets. It can be calculated from the local clustering coefficient quite easily with the following code

    to-report global-clustering-coefficient
      let closed-triplets sum [ nw:clustering-coefficient * count my-links * (count my-links - 1) ] of turtles
      let triplets sum [ count my-links * (count my-links - 1) ] of turtles
      report closed-triplets / triplets
    end

Note that the above will only work with the default context, and may need to tweaked if you've set the turtles or links in the network to something other than `turtles` and `links`.

The average local clustering coefficient is another popular method for measuring the amount of clustering in the network as a whole. It may be calculated with

    mean [ nw:clustering-coefficient ] of turtles

### Clusterers

#### bicomponent-clusters
`nw:bicomponent-clusters`

Reports the list of [bicomponent clusters](http://en.wikipedia.org/wiki/Biconnected_component) in the current network context. A bicomponent (also known as a maximal biconnected subgraph) is a part of a network that cannot be disconnected by removing only one node (i.e. you need to remove at least two to disconnect it). The result is reported as a list of agentsets, in random order. Note that one turtle can be a member of more than one bicomponent at once.

#### weak-component-clusters
`nw:weak-component-clusters`

Reports the list of "weakly" [connected components](http://en.wikipedia.org/wiki/Connected_component_%28graph_theory%29) in the current network context. A weakly connected component is simply a group of nodes where there is a path from each node to every other node. A "strongly" connected component would be one where there is a _directed_ path from each node to every other. The extension does not support the identification of strongly connected components at the moment.

The result is reported as a list of agentsets, in random order. Note that one turtle _cannot_ be a member of more than one weakly connected component at once.

### Cliques

#### maximal-cliques
`nw:maximal-cliques`

A [clique](http://en.wikipedia.org/wiki/Clique_%28graph_theory%29) is a subset of a network in which every node has a direct link to every other node. A maximal clique is a clique that is not, itself, contained in a bigger clique.

The result is reported as a list of agentsets, in random order. Note that one turtle can be a member of more than one maximal clique at once.

The primitive uses the [Bron–Kerbosch algorithm](http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm) and only works with undirected links.

#### biggest-maximal-cliques
`nw:biggest-maximal-cliques`

The biggest maximal cliques are, as the name implies, the biggest [cliques](http://en.wikipedia.org/wiki/Clique_%28graph_theory%29) in the current context. Often, more than one clique are tied for the title of biggest clique, so the result is reported as a list of agentsets, in random order. If you want only one clique, use `one-of nw:biggest-maximal-cliques`.

The primitive uses the [Bron–Kerbosch algorithm](http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm) and only works with undirected links.

### Generators

The generators are amongst the only primitives that do not operate on the current network context. Instead, all of them take a turtle breed and a link breed as inputs and generate a new network using the given breeds.

#### generate-preferential-attachment

<tt>nw:generate-preferential-attachment <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

Generates a new network using the [Barabási–Albert](http://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model) algorithm. This network will have the property of being "scale free": the distribution of degrees (i.e. the number of links for each turtle) should follow a power law.

In this version of the primitive, turtles are added, one by one, each forming one link to a previously added turtle, until _nb-nodes_ is reached. The more links a turtle already has, the greater the probability that new turtles form links with it when they are added. Future versions of the primitive might provide more flexibility in the way the network is generated.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-preferential-attachment turtles links 100 [ set color red ]

#### generate-random
<tt>nw:generate-random <i>turtle-breed link-breed nb-nodes connection-probability optional-command-block</i></tt>

Generates a new random network of _nb-nodes_ turtles in which each one has a  _connection-probability_ (between 0 and 1) of being connected to each other turtles. The algorithm uses the _G(n, p)_ variant of the [Erdős–Rényi model](http://en.wikipedia.org/wiki/Erd%C5%91s%E2%80%93R%C3%A9nyi_model).

The algorithm is O(n²) for directed networks and O(n²/2) for undirected networks, so generating more than a couple thousand nodes will likely take a very long time.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-random turtles links 100 0.5 [ set color red ]

#### generate-small-world
<tt>nw:generate-small-world <i>turtle-breed link-breed row-count column-count clustering-exponent is-toroidal optional-command-block</i></tt>

Generates a new [small-world network](http://en.wikipedia.org/wiki/Small-world_network) using the [Kleinberg Model](http://en.wikipedia.org/wiki/Small_world_routing#The_Kleinberg_Model).

The algorithm proceeds by generating a lattice of the given number of rows and columns (the lattice will wrap around itself if _is-toroidal_ is `true`). The "small world effect" is created by adding additional links between the nodes in the lattice. The higher the _clustering-exponent_, the more the algorithm will favor already close-by nodes when adding new links. A clustering exponent of `2.0` is typically used.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-small-world turtles links 10 10 2.0 false [ set color red ]

#### generate-lattice-2d
<tt>nw:generate-lattice-2d <i>turtle-breed link-breed row-count column-count is-toroidal optional-command-block</i></tt>

Generates a new 2D [lattice network](http://en.wikipedia.org/wiki/Lattice_graph) (basically, a grid) of _row-count_ rows and _column-count_ columns. The grid will wrap around itself if _is-toroidal_ is `true`.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-lattice-2d turtles links 10 10 false [ set color red ]

#### generate-ring
<tt>nw:generate-ring <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

Generates a [ring network](http://en.wikipedia.org/wiki/Ring_network) of _nb-nodes_ turtles, in which each turtle is connected to exactly two other turtles.

The number of nodes must be at least three.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-ring turtles links 100 [ set color red ]

#### generate-star
<tt>nw:generate-star <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

Generates a [star network](http://en.wikipedia.org/wiki/Star_graph) in which there is one central turtle and every other turtle is connected only to this central node. The number of turtles can be as low as one, but it won't look much like a star.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-star turtles links 100 [ set color red ]

#### generate-wheel, generate-wheel-inward, generate-wheel-outward
<tt>nw:generate-wheel <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

<tt>nw:generate-wheel-inward <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

<tt>nw:generate-wheel-outward <i>turtle-breed link-breed nb-nodes optional-command-block</i></tt>

Generates a [wheel network](http://en.wikipedia.org/wiki/Wheel_graph), which is basically a [ring network](http://en.wikipedia.org/wiki/Ring_network) with an additional "central" turtle that is connected to every other turtle.

The number of nodes must be at least four.

The `nw:generate-wheel` only works with undirected link breeds. The `nw:generate-wheel-inward` and `nw:generate-wheel-outward` versions only work with directed _link-breed_. The `inward` and `outward` part of the primitive names refer to the direction that the "spokes" of the wheel point to relative to the central turtle.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-wheel turtles links 100 [ set color red ]

### Import and Export

#### save-matrix
<tt>nw:save-matrix <i>file-name</i></tt>

Saves the current network, as defined by `nw:set-context`, to _file-name_, as a text file, in the form of a simple connection matrix.

Here is, for example, a undirected ring network with four nodes:

    0.00 1.00 0.00 1.00
    1.00 0.00 1.00 0.00
    0.00 1.00 0.00 1.00
    1.00 0.00 1.00 0.00

And here is the directed version:

    0.00 1.00 0.00 0.00
    0.00 0.00 1.00 0.00
    0.00 0.00 0.00 1.00
    1.00 0.00 0.00 0.00

At the moment, `nw:save-matrix` does not support link weights. Every link is represented as a "1.00" in the connection matrix. This will change in a future version of the extension.

#### load-matrix
<tt>nw:load-matrix <i>file-name turtle-breed link-breed optional-command-block</i></tt>

Generates a new network according to the connection matrix saved in _file-name_, using _turtle-breed_ and _link-breed_ to create the new turtles and links.

At the moment, `nw:load-matrix` does not support link weights.

Please be aware that the breeds that use use to load the matrix may be different from those that you used when you saved it.

For example:

    extensions [ nw ]
    directed-link-breed [ dirlinks dirlink ]
    to go
      clear-all
      crt 5 [ create-dirlinks-to other turtles ]
      nw:set-context turtles dirlinks
      nw:save-matrix "matrix.txt"
      clear-all
      nw:load-matrix "matrix.txt" turtles links
      layout-circle turtles 10
    end

..._will_ give you back **undirected** links, even if you saved directed links into the matrix.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

      nw:load-matrix "matrix.txt" turtles links [ set color red ]

#### save-graphml
<tt>nw:save-graphml <i>file-name</i></tt>

You can save the current graph to GraphML. The following NetLogo code:

```
extensions [ nw ]

breed [ bankers banker ]
bankers-own [ bank-name ]
breed [ clients client ]
clients-own [ hometown ]

undirected-link-breed [ friendships friendship ]

directed-link-breed [ accounts account ]
accounts-own [ amount ]

to go
  clear-all
  create-bankers 1 [
    set bank-name "The Bank"
  ]
  create-clients 1 [
    set hometown "Turtle City"
    create-friendship-with banker 0
    create-account-to banker 0 [
      set amount 9999.99
    ]
  ]
  nw:set-context turtles links
  nw:save-graphml "example.graphml"
end
```

Will produce the following GraphML file:

```
<?xml version="1.0" encoding="UTF-8"?>
<graphml xmlns="http://graphml.graphdrawing.org/xmlns/graphml"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns/graphml">
<key id="PEN-MODE" for="node" attr.name="PEN-MODE" attr.type="string"/>
<key id="YCOR" for="node" attr.name="YCOR" attr.type="double"/>
<key id="PEN-SIZE" for="node" attr.name="PEN-SIZE" attr.type="double"/>
<key id="LABEL" for="node" attr.name="LABEL" attr.type="string"/>
<key id="SHAPE" for="node" attr.name="SHAPE" attr.type="string"/>
<key id="BREED" for="node" attr.name="BREED" attr.type="string"/>
<key id="WHO" for="node" attr.name="WHO" attr.type="double"/>
<key id="HIDDEN?" for="node" attr.name="HIDDEN?" attr.type="boolean"/>
<key id="LABEL-COLOR" for="node" attr.name="LABEL-COLOR" attr.type="double"/>
<key id="HEADING" for="node" attr.name="HEADING" attr.type="double"/>
<key id="BANK-NAME" for="node" attr.name="BANK-NAME" attr.type="string"/>
<key id="HOMETOWN" for="node" attr.name="HOMETOWN" attr.type="string"/>
<key id="COLOR" for="node" attr.name="COLOR" attr.type="double"/>
<key id="XCOR" for="node" attr.name="XCOR" attr.type="double"/>
<key id="SIZE" for="node" attr.name="SIZE" attr.type="double"/>
<key id="END1" for="edge" attr.name="END1" attr.type="string"/>
<key id="TIE-MODE" for="edge" attr.name="TIE-MODE" attr.type="string"/>
<key id="END2" for="edge" attr.name="END2" attr.type="string"/>
<key id="LABEL-COLOR" for="edge" attr.name="LABEL-COLOR" attr.type="double"/>
<key id="THICKNESS" for="edge" attr.name="THICKNESS" attr.type="double"/>
<key id="LABEL" for="edge" attr.name="LABEL" attr.type="string"/>
<key id="SHAPE" for="edge" attr.name="SHAPE" attr.type="string"/>
<key id="BREED" for="edge" attr.name="BREED" attr.type="string"/>
<key id="COLOR" for="edge" attr.name="COLOR" attr.type="double"/>
<key id="AMOUNT" for="edge" attr.name="AMOUNT" attr.type="double"/>
<key id="HIDDEN?" for="edge" attr.name="HIDDEN?" attr.type="boolean"/>
<graph edgedefault="undirected">
<node id="client 1">
<data key="PEN-MODE">up</data>
<data key="YCOR">0</data>
<data key="PEN-SIZE">1</data>
<data key="LABEL"></data>
<data key="SHAPE">default</data>
<data key="BREED">clients</data>
<data key="WHO">1</data>
<data key="HIDDEN?">false</data>
<data key="LABEL-COLOR">9.9</data>
<data key="HEADING">356</data>
<data key="HOMETOWN">Turtle City</data>
<data key="COLOR">115</data>
<data key="XCOR">0</data>
<data key="SIZE">1</data>
</node>
<node id="banker 0">
<data key="PEN-MODE">up</data>
<data key="YCOR">0</data>
<data key="PEN-SIZE">1</data>
<data key="LABEL"></data>
<data key="SHAPE">default</data>
<data key="BREED">bankers</data>
<data key="WHO">0</data>
<data key="HIDDEN?">false</data>
<data key="LABEL-COLOR">9.9</data>
<data key="HEADING">32</data>
<data key="BANK-NAME">The Bank</data>
<data key="COLOR">85</data>
<data key="XCOR">0</data>
<data key="SIZE">1</data>
</node>
<edge source="client 1" target="banker 0">
<data key="END1">(client 1)</data>
<data key="TIE-MODE">none</data>
<data key="END2">(banker 0)</data>
<data key="LABEL-COLOR">9.9</data>
<data key="THICKNESS">0</data>
<data key="LABEL"></data>
<data key="SHAPE">default</data>
<data key="BREED">accounts</data>
<data key="COLOR">5</data>
<data key="AMOUNT">9999.99</data>
<data key="HIDDEN?">false</data>
</edge>
<edge source="banker 0" target="client 1">
<data key="END1">(banker 0)</data>
<data key="TIE-MODE">none</data>
<data key="END2">(client 1)</data>
<data key="LABEL-COLOR">9.9</data>
<data key="THICKNESS">0</data>
<data key="LABEL"></data>
<data key="SHAPE">default</data>
<data key="BREED">friendships</data>
<data key="COLOR">5</data>
<data key="HIDDEN?">false</data>
</edge>
</graph>
</graphml>
```

A few things to notice:

- The breed is stored as data field, both for nodes and edges.
- The data includes both NetLogo's internal variables and the variables that were defined as either `breeds-own`, `turtles-own`, `linkbreeds-own` or `links-own`.
- Each key gets an `attr.type` based on the actual types of the values contained in the agent variables. The three possible types are `"string"`, `"double"` and `"boolean"`. To determine the attribute type of a particular agent variable, the extension will look at the first agent in the graph. To see which agent is first, you can look at the result of `nw:get-context`. Note that variables containing other types of values, such as turtles, patches, lists, etc., will be stored as strings.
- This example only has a directed link, and you will notice the `<graph edgedefault="directed">` element. If we had only undirected links, we would have `<graph edgedefault="undirected">`. What if we try to mix both kinds of link? At the moment, the extension will save such a "mixed" graph as if it were an undirected graph (see [this issue](https://github.com/NetLogo/NW-Extension/issues/58) for more details). The order of the `source` and `target` will be respected, however, so if you know which breeds represent directed links, you can figure it out _a posteriori_.

#### load-graphml

<tt>nw:load-graphml <i>file-name optional-command-block</i></tt>

Loading a GraphML file into NetLogo with the network extension should be as simple as calling `nw:load-graphml "example.graphml"`, but there is a bit of preparation involved.

The key idea is that `nw:load-graphml` will try to assign the attribute values defined in the GraphML file to NetLogo agent variables of the same names (this is *not* case sensitive). The first one it tries to set is `breed` if it is there, so the turtle or link will get the right breed and, hence, the right breed variables.

One special case is the `who` number, which is ignored by the importer if it is present as a GraphML attribute: NetLogo does not allow you to modify this number once a turtle is created and, besides, there could already be an existing turtle with that number.

The simplest case to handle is when the original GraphML file has been saved from NetLogo by using `nw:save-graphml`. In this case, all you should have to do is to make sure that you have the same breed and variables definition as when you saved the file and you should get back your original graph. For example, if you want to load the file from the `nw:save-graphml` example above, you should have the following definitions:

    breed [ bankers banker ]
    bankers-own [ bank-name ]
    breed [ clients client ]
    clients-own [ hometown ]

    undirected-link-breed [ friendships friendship ]

    directed-link-breed [ accounts account ]
    accounts-own [ amount ]

Loading a graph that was saved from a different program than NetLogo is quite possible as well, but it may take a bit of tinkering to get all the attribute-variable match up right. If you encounter major problems, please do not hesitate to [open an issue](https://github.com/NetLogo/NW-Extension/issues/new).

The extension will try to assign the type defined by `attr.type` to each variable that it loads. If it's unable to convert it to that type, it will load it as a string. If `attr.type` is not defined, or is set to an unknown value, the extension will first try to load the value as a double, then try it as a boolean, and finally fall back on a string.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:load-graphml "example.graphml" [ set color red ]

Note that this command block can be used to build a list or an agentset containing the newly created nodes:

    let node-list []
    nw:load-graphml "example.graphml" [
      set node-list lput self node-list
    ]
    let node-set turtle-set node-list

#### load

<tt>nw:load <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:load-dl <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:load-gdf <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:load-gexf <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:load-gml <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:load-vna <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

Import the given file into NetLogo. Like `nw:load-graphml`, the importer will do its best to match node and edge attributes in the file with turtle and link variables in NetLogo. If `breed` is specified for nodes and edges in the file and exists in NetLogo, it will be used. Otherwise, the default turtle and link breeds are used.

Limitations:

- Multigraphs are not supported in importing. Even if the file format supports it (and many don't), only the first link will be used on import. This is due to a limitation in the parsing libraries NW uses. `nw:load-graphml` does support multigraphs with the normal NetLogo limitation that two turtles can share more than one link only if all the links are of different breeds.

`nw:load` determines the file-type of given file based on the extension and calls the corresponding `load-*` primitive on it. Note that GraphML must be imported with `nw:load-graphml`.

#### save

<tt>nw:save <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:save-dl <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:save-gdf <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:save-gexf <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:save-gml <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

<tt>nw:save-vna <i>file-name default-turtle-breed default-link-breed optional-command-block</i></tt>

Export the network context in the given format to the given file. Turtle and link attributes will be exported to formats that support node and edge properties.

Limitations:

- `x` and `y` (not `xcor` and `ycor`) can only be numbers. `x` and `y` are commonly used in formats pertaining to position and behind the scenes NW uses Gephi's libraries for exporting. Furthermore, `x` and `y` will be added even if they didn't exist in the model. Again, this is because NW uses Gephi's libraries which assume that nodes have positions stored in `x` and `y`. If you wish to export to Gephi specifically, we recommend creating `x` and `y` turtles variables and setting them to `xcor` and `ycor` before export.
- Color will be exported in a standard RGB format. This should hopefully increase compatibility with other programs.
- Turtle and link variables that contain values of different types will be stored as strings. Unfortunately, most network formats require that node and attributes have a single type.
- Many programs use `label` to store the id of nodes. Thus, if you're having trouble importing data exported from NetLogo into another program, you might try setting turtles' labels to their `who` number.
- Multigraphs are not supported. Thus, two turtles can share at most one link. `nw:save-graphml` does support multigraphs, so use that if turtles can have more than one type of link connecting them.

`nw:save` determines the file-type of the given file based on the extension and calls the corresponding `save-*` primitive on it. Note that GraphML must be exported with `nw:save-graphml`.

## A note regarding floating point calculations

Neither [JGraphT](https://github.com/jgrapht) nor [Jung](http://jung.sourceforge.net/), the two network libraries that we use internally, use [`strictfp` floating point calculations](http://en.wikipedia.org/wiki/Strictfp). This does mean that exact reproducibility of results involving floating point calculations _between different hardware architectures_ is not fully guaranteed. (NetLogo itself [always uses strict math](http://ccl.northwestern.edu/netlogo/docs/faq.html#reproduce) so this only applies to some primitives of the NW extension.)

## Performance

In order to be fast in as many circumstances as possible, the NW extension tries hard to never calculate things twice. It remembers all paths, distances, and centralities that it calculates. So, while the first time you ask for the distance between `turtle 0` and `turtle 3782` may take some time, after that, it should be almost instantaneous. Furthermore, it keeps track of values it just happened to calculate along the way. For example, if `turtle 297` is closer to `turtle 0` than `turtle 3782` is, it may just happen to figure out the distance between `turtle 0` and `turtle 297` while it figures out the distance between `turtle 0` and `turtle 3782`. It will remember this value, so that if you ask it for the distance between `turtle 0` and `turtle 297`, it doesn't have to do all that work again.

There are a few circumstances where the NW extension has to forget things. If the network changes at all (you add turtles or links, or remove turtles or links), it has to forget everything. For weighted primitives, if the value of the weight variable changes for any of the links in the network, it will forget the values associated with that weight variable.

If you're working on a network that can change regularly, try to do all your network calculations at once, then all your network changes at once. The more your interweave network calculations and network changes, the more the NW extension will have to recalculate things. For example, if you have a traffic model, and cars need to figure out the shortest path to their destination based on the traffic each tick, have all the cars find their shortest paths, then change the network weights to account for how traffic has changed.

There may be rare occasions in which you don't want the NW extension to remember values. For example, if you're working on an extremely large network, remembering all those values may take more memory than you have. In that case, you can just call `nw:set-context (first nw:get-context) (last nw:get-context)` to force the NW extension to immediately forget everything.

## Using the extension with applets

If you want to use the extension with applets, you will find that the distributed `nw.zip` contains a folder named `alternate-netlogolite` with `NetLogoLite.jar` and `NetLogoLite.jar.pack.gz`. You should use these _instead_ of the `jar` files that come with the regular NetLogo distribution. (Reasons for this are explained [here](https://github.com/NetLogo/NW-Extension/issues/54).) Applet support is, however, brittle and experimental, and might not be supported in the final release of the extension. Nonetheless, please [report issues](https://github.com/NetLogo/NW-Extension/issues) if you find some.

General information on applets is available in the [NetLogo documentation](http://ccl.northwestern.edu/netlogo/docs/applet.html).

## Building

The extension is written in Scala (version 2.9.2).

Run `./sbt package` to build the extension.

Unless they are already present, sbt will download the needed Jung and JGraphT jar files from the Internet.

If the build succeeds, `nw.jar` will be created. To use the extension, this file and all the other jars will need to be in the `extensions/nw` folder under your NetLogo installation.

## Terms of Use

Copyright 1999-2013 by Uri Wilensky.

This program is free software; you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/copyleft/gpl.html) as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

[Jung](http://jung.sourceforge.net/) is licensed under the [BSD license](http://jung.sourceforge.net/license.txt) and [JGraphT](http://jgrapht.org/) is licensed under the [LGPL license](http://jgrapht.org/LGPL.html).

[turtle]: http://ccl.northwestern.edu/netlogo/docs/images/turtle.gif  "Turtle"
[link]: https://github.com/NetLogo/NW-Extension/raw/master/link.gif  "Link"
