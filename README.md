# NetLogo network extension

This extension is pre-installed in NetLogo 5.0. (For help with extensions in general, see the NetLogo User Manual.)

This extension is experimental.  Although it is presently bundled with NetLogo, it should not be considered a standard, fully-supported part of the application.

The source code for the extension is hosted online at
https://github.com/NetLogo/Network-Extension

## Usage

Anywhere a link breed is required, `links` is also accepted.

Path lengths are computed based solely on the number of hops.  There
isn't currently any way to specify a "weight" or "distance" variable
for links.

Ideally, instead of taking a link breed as input, the breed could take
the place of `link` in the primitive name.  Currently the extensions
API doesn't allow primitives that change name like this, but it should
in some future NetLogo version.  Anyway, this is why all the names
have `link` in them.

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

### Renamed primitives

The primitives in this extension were present in NetLogo 4.1, but with different names.
They were renamed as follows:

* `__network-distance` to `network:link-distance`
* `__in-network-radius` to `network:in-link-radius`, `network:in-out-link-radius`, `network:in-in-link-radius`
* `__average-path-length` to `network:mean-link-path-length`
* `__network-shortest-path-links` to `network:link-path`
* `__network-shortest-path-turtles` to `network:link-path-turtles`

### Omitted primitives

The following primitives, present in NetLogo 4.1 but not NetLogo 5.0, are not included in this extension either:

* `__create-network-preferential`
* `__layout-magspring`
* `__layout-quick`
* `__layout-sphere`

For the source code for these primitives, see [this commit](https://github.com/NetLogo/Network-Extension/commit/eea275e20b5c2a76fc76b8b7642d2a5e7df0a1e4).  But note they are written in the style used by built-in NetLogo primitives. To be brought back to life, they'd need to be changed to use the extensions API instead.

## Building

Use the NETLOGO environment variable to tell the Makefile which NetLogo.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0 make

If compilation succeeds, `network.jar` will be created.

## Credits

The first versions of these primitives were written by Forrest Stonedahl.

## Terms of Use

[![CC0](http://i.creativecommons.org/p/zero/1.0/88x31.png)](http://creativecommons.org/publicdomain/zero/1.0/)

The NetLogo network extension is in the public domain.  To the extent possible under law, Uri Wilensky has waived all copyright and related or neighboring rights.
