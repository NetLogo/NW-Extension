# The "New" NetLogo Network Extension

This is a new, experimental, version of the Network Extension that is currently bundled with NetLogo (the current version of the extension is [here](https://github.com/NetLogo/Network-Extension).)

This version of the extension is **not** pre-installed in NetLogo 5.0.1. To use it, you will need to either build it yourself ([see below](https://github.com/nicolaspayette/netlogo-network/blob/master/README.md#building)) or **[download it from here](https://github.com/nicolaspayette/netlogo-network/downloads)**.

(For help with extensions in general, see the [NetLogo User Manual](http://ccl.northwestern.edu/netlogo/docs/).)

This extension is at a very early stage of development.  Users are invited to experiment with it and report any issues they might find [here on GitHub](https://github.com/nicolaspayette/netlogo-network/issues?state=open), but it should not be used for production code.

Also, be aware that the syntax of some primitives **will** change in future versions of the extension.  You will have to modify your code accordingly. 

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

The first thing that one needs to understand in order to work with the network extension is how to tell the extension _which_ network to work with. Consider the following example situation:

    breed [ bankers banker ]
    breed [ clients client ]
    
    undirected-link-breed [ friendships friendship ]
    directed-link-breed [ accounts account ]

Basically, you have bankers and clients. Clients can have accounts with bankers. Bankers can probably have account with other bankers, and anyone can be friends with anyone.

Now it is possible that you want to consider this whole thing as one big network, but it seems more likely that you will only be interested in a subset of it. Maybe you want to consider all friendships, but you might also want to consider only the friendship between bankers. After all, something having a very high centrality in the network of banker friendship is very different from having a high centrality in a network of client frienships.

To specify such networks, we need to tell the extension _both_ which turtles _and_ which links we are interested in. All the turtles from the specified set of turtles will be included in the network, and only the links from the specified set of links that are between turtles of the specified set will be included. For example, if you ask for `bankers` and `friendships`, even the lonely bankers with no friends will be included, but friendship links between bankers and clients will **not** be included. The current way to tell the extension about this is with the `nw:set-snapshot` primitive, which you must call _prior_ to doing any operations on a network.

Some examples:

- `nw:set-snapshot turtles links` will give you everything: bankers and clients, frienships and accounts, as one big network.
- `nw:set-snapshot turtles friendships` will give you all the bankers and clients and friendships between any of them.
- `nw:set-snapshot bankers friendships` will give you all the bankers, and only friendships between bankers.
- `nw:set-snapshot bankers links` will give you all the bankers, and any links between them, whether these links are friendships or accounts.
- `nw:set-snapshot clients accounts` will give all the clients and accounts between each other, but since in our fictionnal example clients can only have accounts with bankers, this will be a completely disconnected network.

Now one very important thing that you need to understand about `set-snapshot` is that, as its name suggests, it takes a static picture of the network at the time you call it. All subsequent network operations will use this static picture, _even if turtles or links have been created or died in the meantime_, until you call `set-snapshot` again.

In pratice, this means that you will write code like:

    nw:set-snapshot bankers friendships
    ask bankers [
      set size nw:closeness-centrality
    ]

This also means that you need to be careful:

    nw:set-snapshot bankers friendships
    create-bankers 1                    ; creates a new banker after taking the snapshot
    show nw:mean-path-length            ; this is OK, it just won't take the new banker into account
    ask bankers [
      set size nw:closeness-centrality  ; THIS WILL FAIL FOR THE NEWLY CREATED BANKER
    ]

In the example above, a banker is created _after_ the snapshot is taken. This is not a problem in itself: you can still run some measures on the network, such as `nw:mean-path-length` in the example above, but if you try to ask the newly created banker for, e.g., its closeness centrality, the extension will give you a runtime error.

One reason why things work the way they do is that it allows the extension to _cache_ the result of some computations. Many network algorithms are designed to operate on the whole network at once. In the example above, the closeness centrality is actually calculated for every banker the first time you ask for it and then stored in the snapshot so that other bankers just have to access the result.

This makes a big difference, in particular, for primitives like `nw:distance-to`, which uses [Dijkstra's algorithm](http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm). Without getting into the details of the algorithm, let's just say that a big part of the calculations that are made in finding the shortest path from `turtle 0` to `turtle 10` can be reused when finding the shortest path from `turtle 0` to `turtle 20`, and that these calculations are stored in the snapshot.

### Future Usage

Now wouldn't it be better if you _didn't_ have to call `nw:set-snapshot` everytime you want to do something with a network? Yes, indeed, it would. And eventually, it will be the case. What we have in mind for the moment is something like a `nw:set-context` primitive, which you would use to tell the extension that "in general, these are the turtles and links I want to work with." Once you set the context, the extension will be wise enough to decide by itself if it needs to take a new snapshot or not.

The reason we did not do it like this right away is that there currently is no efficient way to ask NetLogo if turtles and links have been created or deleted since a previous function call. If we can include this functionality in a future version of NetLogo, we will probably deprecate `nw:set-snapshot` and provide the much more convenient `nw:set-context` instead.

## Primitives

- [General](https://github.com/nicolaspayette/netlogo-network#general)
- [Path and Distance](https://github.com/nicolaspayette/netlogo-network#path-and-distance)
- [Centrality](https://github.com/nicolaspayette/netlogo-network#centrality)
- [Clusterers](https://github.com/nicolaspayette/netlogo-network#clusterers)
- [Cliques](https://github.com/nicolaspayette/netlogo-network#cliques)
- [Generators](https://github.com/nicolaspayette/netlogo-network#generators)
- [Import / Export](https://github.com/nicolaspayette/netlogo-network#import--export)

### General

#### set-snapshot

`nw:set-snapshot` _turtleset_ _linkset_

Builds a static internal representation of the network formed by all the turtles in _turtleset_ and all the links in _linkset_ that connect two turtles from _turtleset_. This network snapshot is the one that will be used by all other primitives (unless specified otherwise) until a new snapshot is created.

(At the moment, only the [generator primitives](https://github.com/nicolaspayette/netlogo-network#generators) and [`nw:load-matrix`](https://github.com/nicolaspayette/netlogo-network#load-matrix) are exceptions to this rule.)

Note that if turtles and links are created or die, changes will **not** be reflected in the snapshot until you call `nw:set-snapshot` again.

### Path and Distance

#### turtles-in-radius, turtles-in-out-radius, turtles-in-in-radius

![turtle][turtle] `nw:turtles-in-radius` _radius_ 

![turtle][turtle] `nw:turtles-in-out-radius` _radius_ 

![turtle][turtle] `nw:turtles-in-in-radius` _radius_ 

Returns the set of turtles within the given distance (number of links followed) of the calling turtle in the current snapshot.

The `turtles-in-radius` form works with undirected links.  The other two forms work with directed links; `out` or `in` specifies whether links are followed in the normal direction (`out`), or in reverse (`in`).

##### Example: 

    clear-all
    create-turtles 5
    ask turtle 0 [ create-link-with turtle 1 ]
    ask turtle 0 [ create-link-with turtle 2 ]
    ask turtle 1 [ create-link-with turtle 3 ]
    ask turtle 2 [ create-link-with turtle 4 ]
    nw:set-snapshot turtles links
    ask turtle 0 [
      show sort nw:turtles-in-radius 1
    ]

Will output:

    (turtle 0): [(turtle 1) (turtle 2)]

#### distance-to, weighted-distance-to

![turtle][turtle] `nw:distance-to` _target-turtle_

![turtle][turtle] `nw:weighted-distance-to` _target-turtle_ _weight-variable-name_

Finds the shortest path to the target turtle and reports the total distance for this path, or false if no path exists in the current snapshot.

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
      nw:set-snapshot turtles links
      ask turtle 0 [ show nw:distance-to turtle 2 ]
      ask turtle 0 [ show nw:weighted-distance-to turtle 2 "weight" ]
    end

Will ouput:

    (turtle 0): 2
    (turtle 0): 1.5

#### path-to, turtles-on-path-to, weighted-path-to, turtles-on-weighted-path-to

![turtle][turtle] `nw:path-to` _target-turtle_

![turtle][turtle] `nw:turtles-on-path-to` _target-turtle_

![turtle][turtle] `nw:weighted-path-to` _target-turtle_ _weight-variable-name_

![turtle][turtle] `nw:turtles-on-weighted-path-to` _target-turtle_ _weight-variable-name_

Finds the shortest path to the target turtle and reports the actual path between the source and the target turtle. The `nw:path-to` and `nw:weighted-path-to` variants will report the list of links that constitute the path, while the `nw:turtles-on-path-to` and `nw:turtles-on-weighted-path-to` variants will report the list of turtles along the path, including the source and destination turtles.

As with the link distance primitives, the `nw:weighted-path-to` and `nw:turtles-on-weighted-path-to` accept a _weight-variable-name_ parameter, which must be **a string** naming the link variable to use as the weight of each link in distance calculations. The weights cannot be negative numbers.

If no path exist between the source and the target turtles, all primitives will report an empty list.

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
      nw:set-snapshot turtles links
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

`nw:mean-path-length`

`nw:mean-weighted-path-length` _weight-variable-name_

Reports the average shortest-path length between all distinct pairs of nodes in the current snapshot. If the `nw:mean-weighted-path-length` is used, the distances will be calculated using _weight-variable-name_. The weights cannot be negative numbers.

Reports false unless paths exist between all pairs.

##### Example:

    links-own [ weight ]
    to go
      clear-all
      create-turtles 3
      ask turtle 0 [ create-link-with turtle 1 [ set weight 2.0 ] ]
      ask turtle 1 [ create-link-with turtle 2 [ set weight 2.0 ] ]
      nw:set-snapshot turtles links
      show nw:mean-path-length
      show nw:mean-weighted-path-length "weight"
      create-turtles 1 ; create a new, disconnected turtle
      nw:set-snapshot turtles links
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

To calculate the [betweenness centrality](http://en.wikipedia.org/wiki/Betweenness_centrality) of a turtle, you take every other possible pairs of turtles and, for each pair, you calculate the proportion of shortest paths between members of the pair that passes through the current turtle. The betweeness centrality of a turtle is the sum of these.

As of now, link weights are not taken into account.

#### eigenvector-centrality
![turtle][turtle] `nw:eigenvector-centrality`

The [Eigenvector centrality](http://en.wikipedia.org/wiki/Centrality#Eigenvector_centrality) of a node can be thought of as the proportion of its time that an agent forever "walking" at random on the network would spend on this node. In practice, turtles that are connected to a lot of other turtles that are themselves well-connected (and so) get a higher Eigenvector centrality score.

Eigenvector centrality is only defined for connected networks, and the primitive will report `false` for disconnected graphs. (Just like `distance-to` does when there is no path to the target turtle.)

As of now, link weights are not taken into account.

#### closeness-centrality
![turtle][turtle] `nw:closeness-centrality`

The [closeness centrality](http://en.wikipedia.org/wiki/Centrality#Closeness_centrality) of a turtle is defined as the inverse of the sum of it's distances to all other turtles.

Note that this primitive reports the _intra-component_ closeness of a turtle, that is, it takes into account only the distances to the turtles that are part of the same [component](http://en.wikipedia.org/wiki/Connected_component_%28graph_theory%29) as the current turtle, since distance to turtles in other components is undefined. The closeness centrality of an isolated turtle is defined to be zero.

Also note that, as of now, link weights are not taken into account.

### Clusterers

#### k-means-clusters
`nw:k-means-clusters` _nb-clusters_ _max-iterations_ _convergence-threshold_

Partitions the turtles in the current snapshot into _nb-clusters_ different groups. The [k-means](http://en.wikipedia.org/wiki/K-means_clustering#Standard_algorithm) algorithm is an iterative process that will produce groupings that get better and better until some _convergence-threshold_ or some maximum number of iterations (_max-iterations_) is reached.

Currently, `nw:k-means-clusters` uses the _x y coordinates_ of the turtles to group them together, ***not*** their distance in the network. This is coming in a future version of the extension.

The primitive reports a list of lists of turtles representing the different clusters. Each turtle can only be part of one cluster.

Note that k-means include a part of randomness, and may give different results everytime it runs.

##### Example:

    nw:set-snapshot turtles links
    let nb-clusters 10
    let clusters nw:k-means-clusters nb-clusters 500 0.01
    let colors n-of nb-clusters remove gray base-colors
    (foreach clusters colors [
      let c ?2
      foreach ?1 [ ask ? [ set color c ] ]
    ])

#### bicomponent-clusters
`nw:bicomponent-clusters`

Reports the list of [bicomponent clusters](http://en.wikipedia.org/wiki/Biconnected_component) in the current network snapshot. A bicomponent (also known as a maximal biconnected subgraph) is a part of a network that cannot be disconnected by removing only one node (i.e. you need to remove at least two to disconnect it). The result is reported as a list of lists of turtles. Note that one turtle can be a member of more than one bicomponent at once.

#### weak-component-clusters
`nw:weak-component-clusters`

Reports the list of "weakly" [connected components](http://en.wikipedia.org/wiki/Connected_component_%28graph_theory%29) in the current network snapshot. A weakly connected component is simply a group of nodes where there is a path from each node to every other node. A "strongly" connected component would be one where there is a _directed_ path from each node to every other. The extension does not support the identification of strongly connected components at the moment.

The result is reported as a list of lists of turtles. Note that one turtle _cannot_ be a member of more than one weakly connected component at once.

### Cliques

#### maximal-cliques
`nw:maximal-cliques`

A [clique](http://en.wikipedia.org/wiki/Clique_%28graph_theory%29) is a subset of a network in which every node has a direct link to every other node. A maximal clique is a clique that is not, itself, contained in a bigger clique.

The result is reported as a list of lists of turtles. Note that one turtle can be a member of more than one maximal clique at once.

#### biggest-maximal-clique
`nw:biggest-maximal-clique`

The biggest maximal clique is, as its name implies, the biggest [clique](http://en.wikipedia.org/wiki/Clique_%28graph_theory%29) in the current snapshot. The result is reported a list of turtles in the clique. If more than one cliques are tied for the title of biggest clique, only one of them is reported at random.

### Generators

The generators are amongst the only primitives that do not operate on the current network snapshot. Instead, all of them take a turtle breed and a link breed as inputs and generate a new network using the given breeds.

#### generate-preferential-attachment
`nw:generate-preferential-attachment` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a new network using the [Barabási–Albert](http://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model) algorithm. This network will have the property of being "scale free": the distribution of degrees (i.e. the number of links for each turtle) should follow a power law.

In this version of the primitive, turtles are added, one by one, each forming one link to a previously added turtle, until _nb-nodes_ is reached. The more links a turtle already has, the greater the probability that new turtles form links with it when they are added.

Future versions of the primitive might provide more flexibility in the way the network is generated.

#### generate-random
`nw:generate-random` _turtle-breed_ _link-breed_ _nb-nodes_ _connection_probability_

Generates a new random network of _nb-nodes_ turtles in which each one has a  _connection_probability_ (between 0 and 1) of being connected to each other turtles. The algorithm uses the [Erdős–Rényi model](http://en.wikipedia.org/wiki/Erd%C5%91s%E2%80%93R%C3%A9nyi_model).

#### generate-small-world
`nw:generate-small-world` _turtle-breed_ _link-breed_ _row-count_ _column_count_ _clustering-exponent_ _is-toroidal_

Generates a new [small-world network](http://en.wikipedia.org/wiki/Small-world_network) using the [Kleinberg Model](http://en.wikipedia.org/wiki/Small_world_routing#The_Kleinberg_Model). 

The algorithm proceeds by generating a lattice of the given number of rows and columns (the lattice will wrap around itself if _is_toroidal_ is `true`). The "small world effect" is created by adding additional links between the nodes in the lattice. The higher the _clustering_exponent_, the more the algorithm will favor already close-by nodes when adding new links. A clustering exponent of `2.0` is typically used.

#### generate-lattice-2d
`nw:generate-lattice-2d` _turtle-breed_ _link-breed_ _row-count_ _column_count_ _is-toroidal_

Generates a new 2D [lattice network](http://en.wikipedia.org/wiki/Lattice_graph) (basically, a grid) of _row-count_ rows and _column_count_ columns. The grid will wrap around itsef if _is_toroidal_ is `true`.

#### generate-ring
`nw:generate-ring` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a [ring network](http://en.wikipedia.org/wiki/Ring_network) of _nb-nodes_ turtles, in which each turtle is connected to exactly two other turtles.

The number of nodes must be at least three.

#### generate-star
`nw:generate-star` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a [star network](http://en.wikipedia.org/wiki/Star_graph) in which there is one central turtle and every other turtle is connected only to this central node. The number of turtles can be as low as one, but it won't look much like a star.

#### generate-wheel, generate-wheel-inward, generate-wheel-outward
`nw:generate-wheel` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a [wheel network](http://en.wikipedia.org/wiki/Wheel_graph), which is basically a [ring network](http://en.wikipedia.org/wiki/Ring_network) with an additional "central" turtle that is connected to every other turtle.

The number of nodes must be at least four.

The _link-breed_ must be undirected.

`nw:generate-wheel-inward` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a wheel network in which the "spokes" are pointing towards the central turtle.

The _link-breed_ must be directed.

`nw:generate-wheel-outward` _turtle-breed_ _link-breed_ _nb-nodes_

Generates a wheel network in which the "spokes" are pointing away from the central turtle.

The _link-breed_ must be directed.

### Import / Export

#### save-matrix
`nw:save-matrix` _file-name_

Saves the current network snapshot to _file-name_, as a text file, in the form of a simple connection matrix.

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
`nw:load-matrix` _file-name_ _turtle-breed_ _link-breed_

Generates a new network according to the connection matrix saved in _file-name_, using _turtle-breed_ and _link-breed_ to create the new turtles and links.

At the moment, `nw:load-matrix` does not support link weights.

Please be aware that the breeds that use use to load the matrix may be different from those that you used when you saved it.

For example:

    extensions [ nw ]
    directed-link-breed [ dirlinks dirlink ]
    to go
      clear-all
      crt 5 [ create-dirlinks-to other turtles ]
      nw:set-snapshot turtles dirlinks
      nw:save-matrix "matrix.txt"
      clear-all
      nw:load-matrix "matrix.txt" turtles links
      layout-circle turtles 10
    end

..._will_ give you back **undirected** links, even if you saved directed links into the matrix.

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

[Jung](http://jung.sourceforge.net/) is licensed under the [BSD license](http://jung.sourceforge.net/license.txt) and [JGraphT](http://jgrapht.org/) is licensed under the [LGPL license](http://jgrapht.org/LGPL.html).


[turtle]: https://github.com/nicolaspayette/netlogo-network/raw/master/turtle.gif  "Turtle"
[link]: https://github.com/nicolaspayette/netlogo-network/raw/master/link.gif  "Link"
