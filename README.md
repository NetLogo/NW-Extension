# The "New" NetLogo Network Extension

This is a new, experimental, version of the Network Extension that is currently bundled with NetLogo (the current version of the extension is [here](https://github.com/NetLogo/Network-Extension).)

This version of the extension is **not** pre-installed in NetLogo 5.0.2. To use it, you will need to either build it yourself ([see below](https://github.com/nicolaspayette/netlogo-network#building)) or **[download it from here](https://github.com/downloads/NetLogo/NW-Extension/nw-ext-beta-0.01.zip)**.

(For help with extensions in general, see the [NetLogo User Manual](http://ccl.northwestern.edu/netlogo/docs/).)

This extension is at a very early stage of development.  Users are invited to experiment with it and report any issues they might find [here on GitHub](https://github.com/NetLogo/NW-Extension/issues?state=open), but it should not be used for production code. As matter of fact, a look at [the list of open issues](https://github.com/NetLogo/NW-Extension/issues?state=open) will give you a good idea of the current state of developement.

Also, be aware that the syntax of some primitives **will** change in future versions of the extension.  You will have to modify your code accordingly. 

The source code for the extension is currently hosted online at
https://github.com/NetLogo/NW-Extension.

## Changes

Compared to the current extension, this new version offers:

- **Improved performance and functionality of existing features**: pathfinding primitives are now faster and allow taking edge weights into account.
- **Centrality measures**: calculate the betweenness centrality, closeness centrality and eigenvector centrality of the nodes in your network.
- **Clusterers**: find bicomponent and weak component clusters in your network.
- **Clique finder**: find all maximal cliques or the biggest maximal clique in your network.
- **Generators**: generate many different kinds of networks, namely, preferential attachment, random, small world, 2D lattice, ring, star, and wheel networks.
- **Import/Export**: save and load your networks using plain text matrix files, or export them to [GraphML](http://graphml.graphdrawing.org/).

There is also more to come in the future. This is just a very preliminary version of what we have in mind. Future versions will include import from [GraphML](http://graphml.graphdrawing.org/), some new network layouts, and more algorithms and measures.

To provide all this functionality, the Network Extension is relying on two external, popular and well-tested network libraries: [Jung](http://jung.sourceforge.net/) and [JGraphT](https://github.com/jgrapht/jgrapht).

## Usage

The first thing that one needs to understand in order to work with the network extension is how to tell the extension _which_ network to work with. Consider the following example situation:

    breed [ bankers banker ]
    breed [ clients client ]
    
    undirected-link-breed [ friendships friendship ]
    directed-link-breed [ accounts account ]

Basically, you have bankers and clients. Clients can have accounts with bankers. Bankers can probably have account with other bankers, and anyone can be friends with anyone.

Now it is possible that you want to consider this whole thing as one big network, but it seems more likely that you will only be interested in a subset of it. Maybe you want to consider all friendships, but you might also want to consider only the friendships between bankers. After all, something having a very high centrality in the network of banker friendships is very different from having a high centrality in a network of client frienships.

To specify such networks, we need to tell the extension _both_ which turtles _and_ which links we are interested in. All the turtles from the specified set of turtles will be included in the network, and only the links from the specified set of links that are between turtles of the specified set will be included. For example, if you ask for `bankers` and `friendships`, even the lonely bankers with no friends will be included, but friendship links between bankers and clients will **not** be included. The current way to tell the extension about this is with the `nw:set-snapshot` primitive, which you must call _prior_ to doing any operations on a network.

Some examples:

- `nw:set-snapshot turtles links` will give you everything: bankers and clients, frienships and accounts, as one big network.
- `nw:set-snapshot turtles friendships` will give you all the bankers and clients and friendships between any of them.
- `nw:set-snapshot bankers friendships` will give you all the bankers, and only friendships between bankers.
- `nw:set-snapshot bankers links` will give you all the bankers, and any links between them, whether these links are friendships or accounts.
- `nw:set-snapshot clients accounts` will give you all the clients, and accounts between each other, but since in our fictional example clients can only have accounts with bankers, this will be a completely disconnected network.

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

[General](https://github.com/NetLogo/NW-Extension#general)

- [set-snapshot](https://github.com/NetLogo/NW-Extension#set-snapshot)

[Path and Distance](https://github.com/NetLogo/NW-Extension#path-and-distance)

- [turtles-in-radius, turtles-in-out-radius, turtles-in-in-radius](https://github.com/NetLogo/NW-Extension#turtles-in-radius-turtles-in-out-radius-turtles-in-in-radius), [distance-to, weighted-distance-to](https://github.com/NetLogo/NW-Extension/#distance-to-weighted-distance-to), [path-to, turtles-on-path-to, weighted-path-to, turtles-on-weighted-path-to](https://github.com/NetLogo/NW-Extension#path-to-turtles-on-path-to-weighted-path-to-turtles-on-weighted-path-to), [mean-path-length, mean-weighted-path-length](https://github.com/NetLogo/NW-Extension#mean-path-length-mean-weighted-path-length)

[Centrality](https://github.com/NetLogo/NW-Extension#centrality)

- [betweenness-centrality](https://github.com/NetLogo/NW-Extension#betweenness-centrality), [eigenvector-centrality](https://github.com/NetLogo/NW-Extension#eigenvector-centrality), [closeness-centrality](https://github.com/NetLogo/NW-Extension#closeness-centrality)

[Clusterers](https://github.com/NetLogo/NW-Extension#clusterers)

- [k-means-clusters](https://github.com/NetLogo/NW-Extension#k-means-clusters), [bicomponent-clusters](https://github.com/NetLogo/NW-Extension#bicomponent-clusters), [weak-component-clusters](https://github.com/NetLogo/NW-Extension#weak-component-clusters)

[Cliques](https://github.com/NetLogo/NW-Extension#cliques)

- [maximal-cliques](https://github.com/NetLogo/NW-Extension#maximal-cliques), [biggest-maximal-clique](https://github.com/NetLogo/NW-Extension#biggest-maximal-clique)

[Generators](https://github.com/NetLogo/NW-Extension#generators)

- [generate-preferential-attachment](https://github.com/NetLogo/NW-Extension#generate-preferential-attachment), [generate-random](https://github.com/NetLogo/NW-Extension#generate-random), [generate-small-world](https://github.com/NetLogo/NW-Extension#generate-small-world), [generate-lattice-2d](https://github.com/NetLogo/NW-Extension#generate-lattice-2d), [generate-ring](https://github.com/NetLogo/NW-Extension#generate-ring), [generate-star](https://github.com/NetLogo/NW-Extension#generate-star), [generate-wheel, generate-wheel-inward, generate-wheel-outward](https://github.com/NetLogo/NW-Extension#generate-wheel-generate-wheel-inward-generate-wheel-outward)

[Import / Export](https://github.com/NetLogo/NW-Extension#import--export)

- [save-matrix](https://github.com/NetLogo/NW-Extension#save-matrix), [load-matrix](https://github.com/NetLogo/NW-Extension#load-matrix), [save-graphml](https://github.com/NetLogo/NW-Extension#save-graphml)

### General

#### set-snapshot

`nw:set-snapshot` _turtleset_ _linkset_

Builds a static internal representation of the network formed by all the turtles in _turtleset_ and all the links in _linkset_ that connect two turtles from _turtleset_. This network snapshot is the one that will be used by all other primitives (unless specified otherwise) until a new snapshot is created.

(At the moment, only the [generator primitives](https://github.com/NetLogo/NW-Extension#generators) and [`nw:load-matrix`](https://github.com/NetLogo/NW-Extension#load-matrix) are exceptions to this rule.)

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

The [Eigenvector centrality](http://en.wikipedia.org/wiki/Centrality#Eigenvector_centrality) of a node can be thought of as the proportion of its time that an agent forever "walking" at random on the network would spend on this node. In practice, turtles that are connected to a lot of other turtles that are themselves well-connected (and so on) get a higher Eigenvector centrality score.

In this implementation, the sum of the eigenvector centralities for a network should be approximately equal to one ([floating point calculations](http://en.wikipedia.org/wiki/Floating_point#Accuracy_problems) can cause slight deviations).

Eigenvector centrality is only defined for connected networks, and the primitive will report `false` for disconnected graphs. (Just like `distance-to` does when there is no path to the target turtle.)

As of now, the primitive treats every network as if it were an undirected network (even if the links are directed). Future versions will take the direction of links into account.

As of now, link weights are not taken into account.

#### closeness-centrality
![turtle][turtle] `nw:closeness-centrality`

The [closeness centrality](http://en.wikipedia.org/wiki/Centrality#Closeness_centrality) of a turtle is defined as the inverse of the average of it's distances to all other turtles. (Some people use the sum of distances instead of the average, but the extension uses the average.)

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

The primitive uses the [Bron–Kerbosch algorithm](http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm) and only works with undirected links.

#### biggest-maximal-clique
`nw:biggest-maximal-clique`

The biggest maximal clique is, as its name implies, the biggest [clique](http://en.wikipedia.org/wiki/Clique_%28graph_theory%29) in the current snapshot. The result is reported a list of turtles in the clique. If more than one cliques are tied for the title of biggest clique, only one of them is reported at random.

The primitive uses the [Bron–Kerbosch algorithm](http://en.wikipedia.org/wiki/Bron%E2%80%93Kerbosch_algorithm) and only works with undirected links.

### Generators

The generators are amongst the only primitives that do not operate on the current network snapshot. Instead, all of them take a turtle breed and a link breed as inputs and generate a new network using the given breeds.

#### generate-preferential-attachment

`nw:generate-preferential-attachment` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

Generates a new network using the [Barabási–Albert](http://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model) algorithm. This network will have the property of being "scale free": the distribution of degrees (i.e. the number of links for each turtle) should follow a power law.

In this version of the primitive, turtles are added, one by one, each forming one link to a previously added turtle, until _nb-nodes_ is reached. The more links a turtle already has, the greater the probability that new turtles form links with it when they are added. Future versions of the primitive might provide more flexibility in the way the network is generated.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-preferential-attachment turtles links 100 [ set color red ]

#### generate-random
`nw:generate-random` _turtle-breed_ _link-breed_ _nb-nodes_ _connection_probability_ _optional-command-block_

Generates a new random network of _nb-nodes_ turtles in which each one has a  _connection_probability_ (between 0 and 1) of being connected to each other turtles. The algorithm uses the [Erdős–Rényi model](http://en.wikipedia.org/wiki/Erd%C5%91s%E2%80%93R%C3%A9nyi_model).

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-random turtles links 100 0.5 [ set color red ]

#### generate-small-world
`nw:generate-small-world` _turtle-breed_ _link-breed_ _row-count_ _column_count_ _clustering-exponent_ _is-toroidal_ _optional-command-block_

Generates a new [small-world network](http://en.wikipedia.org/wiki/Small-world_network) using the [Kleinberg Model](http://en.wikipedia.org/wiki/Small_world_routing#The_Kleinberg_Model). 

The algorithm proceeds by generating a lattice of the given number of rows and columns (the lattice will wrap around itself if _is_toroidal_ is `true`). The "small world effect" is created by adding additional links between the nodes in the lattice. The higher the _clustering_exponent_, the more the algorithm will favor already close-by nodes when adding new links. A clustering exponent of `2.0` is typically used.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-small-world turtles links 10 10 2.0 false [ set color red ]

#### generate-lattice-2d
`nw:generate-lattice-2d` _turtle-breed_ _link-breed_ _row-count_ _column_count_ _is-toroidal_ _optional-command-block_

Generates a new 2D [lattice network](http://en.wikipedia.org/wiki/Lattice_graph) (basically, a grid) of _row-count_ rows and _column_count_ columns. The grid will wrap around itsef if _is_toroidal_ is `true`.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-lattice-2d turtles links 10 10 false [ set color red ]

#### generate-ring
`nw:generate-ring` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

Generates a [ring network](http://en.wikipedia.org/wiki/Ring_network) of _nb-nodes_ turtles, in which each turtle is connected to exactly two other turtles.

The number of nodes must be at least three.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-ring turtles links 100 [ set color red ]

#### generate-star
`nw:generate-star` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

Generates a [star network](http://en.wikipedia.org/wiki/Star_graph) in which there is one central turtle and every other turtle is connected only to this central node. The number of turtles can be as low as one, but it won't look much like a star.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-star turtles links 100 [ set color red ]

#### generate-wheel, generate-wheel-inward, generate-wheel-outward
`nw:generate-wheel` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

`nw:generate-wheel-inward` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

`nw:generate-wheel-outward` _turtle-breed_ _link-breed_ _nb-nodes_ _optional-command-block_

Generates a [wheel network](http://en.wikipedia.org/wiki/Wheel_graph), which is basically a [ring network](http://en.wikipedia.org/wiki/Ring_network) with an additional "central" turtle that is connected to every other turtle.

The number of nodes must be at least four.

The `nw:generate-wheel` only works with undirected link breeds. The `nw:generate-wheel-inward` and `nw:generate-wheel-outward` versions only work with directed _link-breed_. The `inward` and `outward` part of the primitive names refer to the direction that the "spokes" of the wheel point to relative to the central turtle.

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

    nw:generate-wheel turtles links 100 [ set color red ]

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
`nw:load-matrix` _file-name_ _turtle-breed_ _link-breed_ _optional-command-block_

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

If you specify an _optional-command-block_, it is executed for each turtle in the newly created network. For example:

      nw:load-matrix "matrix.txt" turtles links [ set color red ]

#### save-graphml
`save-graphml` _file-name_

You can save the current snapshot to GraphML. The following NetLogo code:

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
  nw:set-snapshot turtles links
  nw:save-graphml "graph.xml"
end
```

Will produce the following GraphML file:

```
<?xml version="1.0" encoding="UTF-8"?>
<graphml xmlns="http://graphml.graphdrawing.org/xmlns/graphml"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"  
xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns/graphml">
<key id="PEN-MODE" for="node"/>
<key id="YCOR" for="node"/>
<key id="PEN-SIZE" for="node"/>
<key id="LABEL" for="node"/>
<key id="SHAPE" for="node"/>
<key id="BREED" for="node"/>
<key id="WHO" for="node"/>
<key id="HIDDEN?" for="node"/>
<key id="LABEL-COLOR" for="node"/>
<key id="HEADING" for="node"/>
<key id="BANK-NAME" for="node"/>
<key id="HOMETOWN" for="node"/>
<key id="COLOR" for="node"/>
<key id="XCOR" for="node"/>
<key id="SIZE" for="node"/>
<key id="END1" for="edge"/>
<key id="TIE-MODE" for="edge"/>
<key id="END2" for="edge"/>
<key id="LABEL-COLOR" for="edge"/>
<key id="THICKNESS" for="edge"/>
<key id="LABEL" for="edge"/>
<key id="SHAPE" for="edge"/>
<key id="BREED" for="edge"/>
<key id="COLOR" for="edge"/>
<key id="AMOUNT" for="edge"/>
<key id="HIDDEN?" for="edge"/>
<graph edgedefault="directed">
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
<data key="HEADING">346</data>
<data key="BANK-NAME">The Bank</data>
<data key="COLOR">85</data>
<data key="XCOR">0</data>
<data key="SIZE">1</data>
</node>
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
<data key="HEADING">244</data>
<data key="HOMETOWN">Turtle City</data>
<data key="COLOR">105</data>
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
</graph>
</graphml>

```

A few things to notice:

- The breed is stored as data field, both for nodes and edges.
- The data includes both NetLogo's internal variables and the variables that were defined as either `breeds-own`, `turtles-own`, `linkbreeds-own` or `links-own`.
- This example only has a directed link, and you will notice the `<graph edgedefault="directed">` element. If we had only undirected links, we would have `<graph edgedefault="undirected">`. What if we try to mix both kinds of link? At the moment, the extension will save such a "mixed" graph as if it were an undirected graph (see [this issue](https://github.com/NetLogo/NW-Extension/issues/58) for more details). The order of the `source` and `target` will be respected, however, so if you know which breeds represent directed links, you can figure it out _a posteriori_.
- At the moment, all data is written as if it was the output of a NetLogo `print` command and the GraphML `attr.type` is not set for the keys. It will be [added eventually](https://github.com/NetLogo/NW-Extension/issues/60).

## A note regarding floating point calculations

Neither [JGraphT](https://github.com/jgrapht) nor [Jung](http://jung.sourceforge.net/), the two network librairies that we use internally, use [`strictfp` floating point calculations](http://en.wikipedia.org/wiki/Strictfp). This does mean that exact reproducibility of results involving floating point calculations _between different hardware architectures_ is not fully guaranteed. (NetLogo itself [always uses strict math](http://ccl.northwestern.edu/netlogo/docs/faq.html#reproduce) so this only applies to some primitives of the NW extension.)

## Using the extension with applets

If you want to use the extension with applets, you will find that the distributed `nw-ext-beta-0.0x.zip` contains a folder named `alternate-netlogolite` with `NetLogoLite.jar` and `NetLogoLite.jar.pack.gz`. You should use these _instead_ of the `jar` files that come with the regular NetLogo 5.0.2 distribution. (Reasons for this are explained [here](https://github.com/NetLogo/NW-Extension/issues/54).) Applet support is, however, brittle and experimental, and might not be supported in the final release of the extension. Nonetheless, please [report issues](https://github.com/NetLogo/NW-Extension/issues) if you find some.

General information on applets is available in the [NetLogo documentation](http://ccl.northwestern.edu/netlogo/docs/applet.html).

## Building

The extension is written in Scala (version 2.9.2).

Run the `bin/sbt` script to build the extension.

Unless they are already present, sbt will download the needed Jung and JGraphT jar files from the Internet. Note that the extension requires a modified version of `jung-algorithms-2.0.2` (namely, `jung-algorithms-2.0.2-nlfork-0.1.jar`) which will be downloaded from the CCL server.

If the build succeeds, `nw.jar` will be created. To use the extension, this file and all the other jars will need to be in the `extensions/nw` folder under your NetLogo installation.

## Terms of Use

Copyright 1999-2012 by Uri Wilensky.

This program is free software; you can redistribute it and/or modify it under the terms of the [GNU General Public License](http://www.gnu.org/copyleft/gpl.html) as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

[Jung](http://jung.sourceforge.net/) is licensed under the [BSD license](http://jung.sourceforge.net/license.txt) and [JGraphT](http://jgrapht.org/) is licensed under the [LGPL license](http://jgrapht.org/LGPL.html).

[turtle]: https://github.com/NetLogo/NW-Extension/raw/master/turtle.gif  "Turtle"
[link]: https://github.com/NetLogo/NW-Extension/raw/master/link.gif  "Link"
