extensions [ nw ]

turtles-own
[
  node-clustering-coefficient
  node-average-path-length
]

links-own
[
  rewired?                    ;; keeps track of whether the link has been rewired or not
]

globals
[
  clustering-coefficient               ;; the clustering coefficient of the network; this is the
                                       ;; average of clustering coefficients of all turtles
  average-path-length                  ;; average path length of the network
  clustering-coefficient-of-lattice    ;; the clustering coefficient of the initial lattice
  average-path-length-of-lattice       ;; average path length of the initial lattice
  number-rewired                       ;; number of edges that have been rewired. used for plots.
  rewire-one?                          ;; these two variables record which button was last pushed
  rewire-all?
  
  ;; Colors:
  default-node-color
  default-link-color
  highlighted-node-color
  highlighted-neighbor-color
  highlighted-link-color
  rewired-link-color
  between-neighbors-link-color
  
  currently-highlighted-node  
  
]

;;;;;;;;;;;;;;;;;;;;;;;;
;;; Setup Procedures ;;;
;;;;;;;;;;;;;;;;;;;;;;;;

to setup
  clear-all
  
  ;; Colors:
  set default-node-color gray
  set default-link-color gray - 1
  set highlighted-node-color pink
  set highlighted-neighbor-color blue + 1
  set highlighted-link-color blue - 1
  set rewired-link-color green - 2
  set between-neighbors-link-color yellow
  
  set currently-highlighted-node nobody  
  
  set-default-shape turtles "circle"
  make-turtles
  
  ;; set up a variable to determine if we still have a connected network
  ;; (in most cases we will since it starts out fully connected)
  let success? false
  while [ not success? ] [
    ;; we need to find initial values for lattice
    wire-them
    ;;calculate average path length and clustering coefficient for the lattice
    set success? do-calculations
  ]
  
  ;; setting the values for the initial lattice
  set clustering-coefficient-of-lattice clustering-coefficient
  set average-path-length-of-lattice average-path-length
  set number-rewired 0
end

to make-turtles
  crt num-nodes [ set color default-node-color ]
  ;; arrange them in a circle in order by who number
  layout-circle (sort turtles) max-pxcor - 1
end

;;;;;;;;;;;;;;;;;;;;;;;
;;; Main Procedure ;;;
;;;;;;;;;;;;;;;;;;;;;;;

to rewire-one
  
  ;; make sure num-turtles is setup correctly else run setup first
  if count turtles != num-nodes [ setup ]
  
  ;; record which button was pushed
  set rewire-one? true
  set rewire-all? false
  
  let potential-edges links with [ not rewired? ]
  ifelse any? potential-edges [
    ask one-of potential-edges [
      ;; "a" remains the same
      let node1 end1
      ;; if "a" is not connected to everybody
      if [ count link-neighbors ] of end1 < (count turtles - 1) [
        ;; find a node distinct from node1 and not already a neighbor of node1
        let node2 one-of turtles with [ (self != node1) and (not link-neighbor? node1) ]
        ;; wire the new edge
        ask node1 [ 
          create-link-with node2 [ 
            set color rewired-link-color  
            set rewired? true 
          ] 
        ]
        set number-rewired number-rewired + 1  ;; counter for number of rewirings
        die                                    ;; remove the old edge
      ]
    ]
    ;; plot the results
    let connected? do-calculations
    update-plots
    display
  ]
  [ 
    user-message "All edges have already been rewired once."
    stop
  ]
end

to rewire-all
  
  ;; make sure num-turtles is setup correctly; if not run setup first
  if count turtles != num-nodes [
    setup
  ]
  
  ;; record which button was pushed
  set rewire-one? false
  set rewire-all? true
  
  ;; set up a variable to see if the network is connected
  let success? false
  
  ;; if we end up with a disconnected network, we keep trying, because the APL distance
  ;; isn't meaningful for a disconnected network.
  while [ not success? ] [
    ;; kill the old lattice, reset neighbors, and create new lattice
    ask links [ die ]
    wire-them
    set number-rewired 0
    
    ask links [
      
      ;; whether to rewire it or not?
      if (random-float 1) < rewiring-probability
      [
        ;; "a" remains the same
        let node1 end1
        ;; if "a" is not connected to everybody
        if [ count link-neighbors ] of end1 < (count turtles - 1)
        [
          ;; find a node distinct from node1 and not already a neighbor of node1
          let node2 one-of turtles with [ (self != node1) and (not link-neighbor? node1) ]
          ;; wire the new edge
          ask node1 [ 
            create-link-with node2 [ 
              set color rewired-link-color  
              set rewired? true 
            ]
          ]
          
          set number-rewired number-rewired + 1  ;; counter for number of rewirings
          set rewired? true
        ]
      ]
      if (rewired?) [ die ] ;; remove the old edge
    ]
    
    ;; check to see if the new network is connected and calculate path length and clustering
    ;; coefficient at the same time
    set success? do-calculations
  ]
  
  ;; do the plotting
  update-plots
end

to sweep
  ; runs the "rewire all" procedure with incremental values of rewiring-probability
  set rewiring-probability precision (rewiring-probability + 0.01) 2
  if rewiring-probability > 1 [ set rewiring-probability 0 ]
  rewire-all
  display
end

;; do-calculations reports true if the network is connected,
;;   and reports false if the network is disconnected.
;; (In the disconnected case, the average path length does not make sense)
to-report do-calculations
  
  ;; find the path lengths in the network
  ask turtles [
    let distances remove false [ nw:distance-to myself ] of other turtles
    ifelse empty? distances [
      set node-average-path-length false ;; undefined
    ]
    [
      set node-average-path-length sum distances / length distances
    ]

    let hood link-neighbors
    ifelse count hood <= 1 [
      set node-clustering-coefficient "undefined"
    ]
    [
      let links-in-hood links with [ (member? end1 hood and member? end2 hood) ]
      set node-clustering-coefficient (2 * count links-in-hood) / (count hood * (count hood - 1))
    ]
  ]
  
  set average-path-length nw:mean-path-length ;; will be false if the network is disconnected

  let coefficients remove "undefined" [ node-clustering-coefficient ] of turtles
  set clustering-coefficient sum coefficients / length coefficients
  
  ;; report whether the network is connected or not
  report (average-path-length != false)
end

to-report in-neighborhood? [ hood ]
  report (member? end1 hood and member? end2 hood)
end

;;;;;;;;;;;;;;;;;;;;;;;
;;; Edge Operations ;;;
;;;;;;;;;;;;;;;;;;;;;;;

;; creates a new lattice
to wire-them
  ;; iterate over the turtles
  let n 0
  while [ n < count turtles ] [
    ;; make edges with the next two neighbors
    ;; this makes a lattice with average degree of 4
    make-edge turtle n turtle ((n + 1) mod count turtles)
    make-edge turtle n turtle ((n + 2) mod count turtles)
    set n n + 1
  ]
end

;; connects the two turtles
to make-edge [node1 node2]
  ask node1 [ 
    create-link-with node2  [
      set rewired? false
    ] 
  ]
end

;;;;;;;;;;;;;;;;
;;; Graphics ;;;
;;;;;;;;;;;;;;;;

to highlight
  if mouse-inside? [ do-highlight ]
  display
end

to do-highlight
  ;; get the node with neighbors that is closest to the mouse
  let node min-one-of turtles with [ count link-neighbors > 0 ] [ distancexy mouse-xcor mouse-ycor ]
  if node = nobody [ stop ]
  if node = currently-highlighted-node [ stop ]
  set currently-highlighted-node node
  
  ;; remove previous highlights
  ask turtles [ set color default-node-color ]
  ask links [ 
    set color ifelse-value rewired? [ rewired-link-color ] [ default-link-color ] 
    set thickness 0
  ]
  
  ;; highlight the chosen node
  ask node [ set color highlighted-node-color ]

  let neighbor-nodes [ link-neighbors ] of node
  ;; highlight neighbors
  ask neighbor-nodes [
    set color highlighted-neighbor-color
    ;; highlight edges connecting the chosen node to its neighbors
    ask my-links [
      ifelse (end1 = node or end2 = node) [
        set thickness 0.2
        set color highlighted-link-color
      ]
      [
        if (member? end1 neighbor-nodes and member? end2 neighbor-nodes) [ 
          set thickness 0.2
          set color between-neighbors-link-color 
        ]
      ]
    ]
  ]
end

; Copyright 2012 Uri Wilensky.
; See Info tab for full copyright and license.
@#$#@#$#@
GRAPHICS-WINDOW
330
10
798
499
17
17
13.1
1
10
1
1
1
0
0
0
1
-17
17
-17
17
0
0
0
ticks
30.0

SLIDER
90
10
320
43
num-nodes
num-nodes
10
125
30
1
1
NIL
HORIZONTAL

PLOT
10
95
320
274
Network Properties Rewire-One
fraction of edges rewired
NIL
0.0
1.0
0.0
1.0
true
true
"" "if not rewire-one? [ stop ]"
PENS
"apl" 1.0 2 -65485 true "" "plotxy number-rewired / count links\n       (ifelse-value (average-path-length = false) [ 99999 ] [ average-path-length ])\n         / average-path-length-of-lattice"
"cc" 1.0 2 -10899396 true "" ";; note: dividing by initial value to normalize the plot\nplotxy number-rewired / count links\n       clustering-coefficient / clustering-coefficient-of-lattice"

BUTTON
10
50
156
81
rewire one
rewire-one
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

SLIDER
84
281
261
314
rewiring-probability
rewiring-probability
0
1
0.3
0.01
1
NIL
HORIZONTAL

BUTTON
10
280
80
313
rewire all
rewire-all
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
810
450
935
495
highlight node
highlight
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

MONITOR
810
10
990
55
Network clustering coefficient
clustering-coefficient
3
1
11

MONITOR
995
10
1175
55
Network avg path length
average-path-length
3
1
11

PLOT
10
320
320
499
Network Properties Rewire-All
rewiring probability
NIL
0.0
1.0
0.0
1.0
false
true
"" "if not rewire-all? [ stop ]"
PENS
"apl" 1.0 2 -2674135 true "" ";; note: dividing by value at initial value to normalize the plot\nplotxy rewiring-probability\n       average-path-length / average-path-length-of-lattice"
"cc" 1.0 2 -10899396 true "" ";; note: dividing by initial value to normalize the plot\nplotxy rewiring-probability\n       clustering-coefficient / clustering-coefficient-of-lattice"

BUTTON
10
10
85
41
NIL
setup
NIL
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

PLOT
810
60
1175
180
Betweenness Centrality Distribution
Betweenness Centrality
Nodes
0.0
10.0
0.0
15.0
false
false
"" ""
PENS
"default" 1.0 1 -16777216 true "" "set-plot-y-range 0 num-nodes / 2\nlet ys sort [ nw:betweenness-centrality ] of turtles\nlet y-min precision first ys 3\nlet y-max precision last ys 3\nif y-max > y-min [\n  set-plot-pen-interval (y-max - y-min) / 10\n  set-plot-x-range y-min y-max\n  histogram ys\n]"

PLOT
810
185
1175
305
Closeness Centrality Distribution
Closeness Centrality
Nodes
0.0
5.0
0.0
10.0
true
false
"" ""
PENS
"default" 0.05 1 -16777216 true "" "set-plot-y-range 0 num-nodes / 2\nlet ys sort [ nw:closeness-centrality ] of turtles\nlet y-min precision first ys 3\nlet y-max precision last ys 3\nif y-max > y-min [\n  set-plot-pen-interval (y-max - y-min) / 10\n  set-plot-x-range y-min y-max\n  histogram ys\n]"

PLOT
810
310
1175
430
Egeinvector Centrality Distribution
Egeinvector Centrality
Nodes
0.0
10.0
0.0
10.0
true
false
"" ""
PENS
"default" 1.0 1 -16777216 true "" "set-plot-y-range 0 num-nodes / 2\nlet ys sort [ nw:eigenvector-centrality ] of turtles\nifelse not empty? ys [\n  let y-min first ys\n  let y-max last ys\n  if y-max > y-min [\n    set-plot-pen-interval (y-max - y-min) / 10\n    set-plot-x-range y-min y-max\n    histogram ys\n  ]\n]\n[\n  clear-plot\n]"

MONITOR
940
450
1070
495
Clustering coefficient
[ node-clustering-coefficient ] of currently-highlighted-node
3
1
11

MONITOR
1075
450
1175
495
Avg path length
[ node-average-path-length ] of currently-highlighted-node
3
1
11

BUTTON
169
50
320
81
rewire one
rewire-one
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

BUTTON
264
281
319
314
NIL
sweep
T
1
T
OBSERVER
NIL
NIL
NIL
NIL
1

@#$#@#$#@
## WHAT IS IT?

This model explores the formation of networks that result in the "small world" phenomenon -- the idea that a person is only a couple of connections away any other person in the world.

A popular example of the small world phenomenon is the network formed by actors appearing in the same movie (e.g. the "six degrees of Kevin Bacon" game), but small worlds are not limited to people-only networks.  Other examples range from power grids to the neural networks of worms.  This model illustrates some general, theoretical conditions under which small world networks between people or things might occur.

## HOW IT WORKS

This model is an adaptation of a model proposed by Duncan Watts and Steve Strogatz (1998). It begins with a network where each person (or "node") is connected to his or her two neighbors on either side.  The REWIRE-ONE button picks a random connection (or "edge") and rewires it. By rewiring, we mean changing one end of a connected pair of nodes, and keeping the other end the same.

The REWIRE-ALL button creates the network and then visits all edges and tries to rewire them.  The REWIRING-PROBABILITY slider determines the probability that an edge will get rewired.  Running REWIRE-ALL at multiple probabilities produces a range of possible networks with varying average path lengths and clustering coefficients.

To identify small worlds, the "average path length" (abbreviated "apl") and "clustering coefficient" (abbreviated "cc") of the network are calculated and plotted after the REWIRE-ONE or REWIRE-ALL buttons are pressed. These two plots are separated because the x-axis is slightly different.  The REWIRE-ONE x-axis is the fraction of edges rewired so far, whereas the REWIRE-ALL x-axis is the probability of rewiring.  Networks with short average path lengths and high clustering coefficients are considered small world networks. (Note: The plots for both the clustering coefficient and average path length are normalized by dividing by the values of the initial network. The monitors give the actual values.)

Average Path Length: Average path length is calculated by finding the shortest path between all pairs of nodes, adding them up, and then dividing by the total number of pairs. This shows us, on average, the number of steps it takes to get from one member of the network to another.

Clustering Coefficient:  Another property of small world networks is that from one person's perspective it seems unlikely that they could be only a few steps away from anybody else in the world.  This is because their friends more or less know all the same people they do. The clustering coefficient is a measure of this "all-my-friends-know-each-other" property.  This is sometimes described as the friends of my friends are my friends.  More precisely, the clustering coefficient of a node is the ratio of existing links connecting a node's neighbors to each other to the maximum possible number of such links.  You can see this is if you press the HIGHLIGHT button and click a node, that will display all of the neighbors in blue and the edges connecting those neighbors in yellow.  The more yellow links, the higher the clustering coefficient for the node you are examining (the one in pink) will be.  The clustering coefficient for the entire network is the average of the clustering coefficients of all the nodes. A high clustering coefficient for a network is another indication of a small world.

## HOW TO USE IT

The NUM-NODES slider controls the size of the network.  Choose a size and press SETUP.

Pressing the REWIRE-ONE button picks one edge at random, rewires it, and then plots the resulting network properties. The REWIRE-ONE button always rewires at least one edge (i.e., it ignores the REWIRING-PROBABILITY).

Pressing the REWIRE-ALL button re-creates the initial network (each node connected to its two neighbors on each side for a total of four neighbors) and rewires all the edges with the current rewiring probability, then plots the resulting network properties on the rewire-all plot. Changing the REWIRING-PROBABILITY slider changes the fraction of links rewired after each run. The SWEEP button will automatically try the REWIRE-ALL procedure for all the range of possible rewiring probabilities.

When you press HIGHLIGHT and then point to node in the view it color-codes the nodes and edges.  The node itself turns pink. Its neighbors and the edges connecting the node to those neighbors turn blue. Edges connecting the neighbors of the node to each other turn yellow. The amount of yellow between neighbors can gives you an indication of the clustering coefficient for that node.  The NODE-PROPERTIES monitor displays the average path length and clustering coefficient of the highlighted node only.  The AVERAGE-PATH-LENGTH and CLUSTERING-COEFFICIENT monitors display the values for the entire network.

## THINGS TO NOTICE

Note that for certain ranges of the fraction of nodes, the average path length decreases faster than the clustering coefficient.  In fact, there is a range of values for which the average path length is much smaller than clustering coefficient.  (Note that the values for average path length and clustering coefficient have been normalized, so that they are more directly comparable.)  Networks in that range are considered small worlds.

## THINGS TO TRY

Try plotting the values for different rewiring probabilities and observe the trends of the values for average path length and clustering coefficient.  What is the relationship between rewiring probability and fraction of nodes?  In other words, what is the relationship between the rewire-one plot and the rewire-all plot?

Do the trends depend on the number of nodes in the network?

Can you get a small world by repeatedly pressing REWIRE-ONE?

Set NUM-NODES to 80 and then press SETUP. Go to BehaviorSpace and run the VARY-REWIRING-PROBABILITY experiment. Try running the experiment multiple times without clearing the plot (i.e., do not run SETUP again).  What range of rewiring probabilities result in small world networks?

## EXTENDING THE MODEL

Try to see if you can produce the same results if you start with a different initial network.  Create new BehaviorSpace experiments to compare results.

In a precursor to this model, Watts and Strogatz created an "alpha" model where the rewiring was not based on a global rewiring probability.  Instead, the probability that a node got connected to another node depended on how many mutual connections the two nodes had. The extent to which mutual connections mattered was determined by the parameter "alpha."  Create the "alpha" model and see if it also can result in small world formation.

## NETLOGO FEATURES

In this model we need to find the shortest paths between all pairs of nodes. The version of this model that is in the NetLogo model library uses the [Floyd Warshall algorithm](http://en.wikipedia.org/wiki/Floyd-Warshall_algorithm). This version directly use the network extension's `mean-path-length` primitive instead. (`nw:mean-path-length` internally uses either [Breadth-First Search](http://en.wikipedia.org/wiki/Breath_first_search) or [Dijkstra's algorithm](http://en.wikipedia.org/wiki/Dijkstra%27s_algorithm) depending on whether the graph is weighted or not. In this case, the graph is not weighted.)

This version of the model also shows plots of the distribution of the different centrality measures that the network extension makes available to you, namely: `nw:betweenness-centrality`, `nw:closeness-centrality` and `nw:eigenvector-centrality`.

## RELATED MODELS

See other models in the Networks section of the Models Library, such as Giant Component and Preferential Attachment.

## CREDITS AND REFERENCES

This model is adapted from:
Duncan J. Watts, Six Degrees: The Science of a Connected Age (W.W. Norton & Company, New York, 2003), pages 83-100.

The work described here was originally published in:
DJ Watts and SH Strogatz. Collective dynamics of 'small-world' networks, Nature,
393:440-442 (1998)

For more information please see Watts' website:  http://smallworld.columbia.edu/index.html

The small worlds idea was first made popular by Stanley Milgram's famous experiment (1967) which found that two random US citizens where on average connected by six acquaintances (giving rise to the popular "six degrees of separation" expression):
Stanley Milgram.  The Small World Problem,  Psychology Today,  2: 60-67 (1967).

This experiment was popularized into a game called "six degrees of Kevin Bacon" which you can find more information about here:  http://www.cs.virginia.edu/oracle/


## HOW TO CITE

If you mention this model in a publication, we ask that you include these citations for the model itself and for the NetLogo software:  
- Wilensky, U. (2005).  NetLogo Small Worlds model.  http://ccl.northwestern.edu/netlogo/models/SmallWorlds.  Center for Connected Learning and Computer-Based Modeling, Northwestern University, Evanston, IL.  
- Wilensky, U. (1999). NetLogo. http://ccl.northwestern.edu/netlogo/. Center for Connected Learning and Computer-Based Modeling, Northwestern University, Evanston, IL.  

## COPYRIGHT AND LICENSE

Copyright 2005 Uri Wilensky.

![CC BY-NC-SA 3.0](http://i.creativecommons.org/l/by-nc-sa/3.0/88x31.png)

This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 License.  To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ or send a letter to Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA.

Commercial licenses are also available. To inquire about commercial licenses, please contact Uri Wilensky at uri@northwestern.edu.
@#$#@#$#@
default
true
0
Polygon -7500403 true true 150 5 40 250 150 205 260 250

airplane
true
0
Polygon -7500403 true true 150 0 135 15 120 60 120 105 15 165 15 195 120 180 135 240 105 270 120 285 150 270 180 285 210 270 165 240 180 180 285 195 285 165 180 105 180 60 165 15

arrow
true
0
Polygon -7500403 true true 150 0 0 150 105 150 105 293 195 293 195 150 300 150

box
false
0
Polygon -7500403 true true 150 285 285 225 285 75 150 135
Polygon -7500403 true true 150 135 15 75 150 15 285 75
Polygon -7500403 true true 15 75 15 225 150 285 150 135
Line -16777216 false 150 285 150 135
Line -16777216 false 150 135 15 75
Line -16777216 false 150 135 285 75

bug
true
0
Circle -7500403 true true 96 182 108
Circle -7500403 true true 110 127 80
Circle -7500403 true true 110 75 80
Line -7500403 true 150 100 80 30
Line -7500403 true 150 100 220 30

butterfly
true
0
Polygon -7500403 true true 150 165 209 199 225 225 225 255 195 270 165 255 150 240
Polygon -7500403 true true 150 165 89 198 75 225 75 255 105 270 135 255 150 240
Polygon -7500403 true true 139 148 100 105 55 90 25 90 10 105 10 135 25 180 40 195 85 194 139 163
Polygon -7500403 true true 162 150 200 105 245 90 275 90 290 105 290 135 275 180 260 195 215 195 162 165
Polygon -16777216 true false 150 255 135 225 120 150 135 120 150 105 165 120 180 150 165 225
Circle -16777216 true false 135 90 30
Line -16777216 false 150 105 195 60
Line -16777216 false 150 105 105 60

car
false
0
Polygon -7500403 true true 300 180 279 164 261 144 240 135 226 132 213 106 203 84 185 63 159 50 135 50 75 60 0 150 0 165 0 225 300 225 300 180
Circle -16777216 true false 180 180 90
Circle -16777216 true false 30 180 90
Polygon -16777216 true false 162 80 132 78 134 135 209 135 194 105 189 96 180 89
Circle -7500403 true true 47 195 58
Circle -7500403 true true 195 195 58

circle
false
0
Circle -7500403 true true 0 0 300

circle 2
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240

cow
false
0
Polygon -7500403 true true 200 193 197 249 179 249 177 196 166 187 140 189 93 191 78 179 72 211 49 209 48 181 37 149 25 120 25 89 45 72 103 84 179 75 198 76 252 64 272 81 293 103 285 121 255 121 242 118 224 167
Polygon -7500403 true true 73 210 86 251 62 249 48 208
Polygon -7500403 true true 25 114 16 195 9 204 23 213 25 200 39 123

cylinder
false
0
Circle -7500403 true true 0 0 300

dot
false
0
Circle -7500403 true true 90 90 120

face happy
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 255 90 239 62 213 47 191 67 179 90 203 109 218 150 225 192 218 210 203 227 181 251 194 236 217 212 240

face neutral
false
0
Circle -7500403 true true 8 7 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Rectangle -16777216 true false 60 195 240 225

face sad
false
0
Circle -7500403 true true 8 8 285
Circle -16777216 true false 60 75 60
Circle -16777216 true false 180 75 60
Polygon -16777216 true false 150 168 90 184 62 210 47 232 67 244 90 220 109 205 150 198 192 205 210 220 227 242 251 229 236 206 212 183

fish
false
0
Polygon -1 true false 44 131 21 87 15 86 0 120 15 150 0 180 13 214 20 212 45 166
Polygon -1 true false 135 195 119 235 95 218 76 210 46 204 60 165
Polygon -1 true false 75 45 83 77 71 103 86 114 166 78 135 60
Polygon -7500403 true true 30 136 151 77 226 81 280 119 292 146 292 160 287 170 270 195 195 210 151 212 30 166
Circle -16777216 true false 215 106 30

flag
false
0
Rectangle -7500403 true true 60 15 75 300
Polygon -7500403 true true 90 150 270 90 90 30
Line -7500403 true 75 135 90 135
Line -7500403 true 75 45 90 45

flower
false
0
Polygon -10899396 true false 135 120 165 165 180 210 180 240 150 300 165 300 195 240 195 195 165 135
Circle -7500403 true true 85 132 38
Circle -7500403 true true 130 147 38
Circle -7500403 true true 192 85 38
Circle -7500403 true true 85 40 38
Circle -7500403 true true 177 40 38
Circle -7500403 true true 177 132 38
Circle -7500403 true true 70 85 38
Circle -7500403 true true 130 25 38
Circle -7500403 true true 96 51 108
Circle -16777216 true false 113 68 74
Polygon -10899396 true false 189 233 219 188 249 173 279 188 234 218
Polygon -10899396 true false 180 255 150 210 105 210 75 240 135 240

house
false
0
Rectangle -7500403 true true 45 120 255 285
Rectangle -16777216 true false 120 210 180 285
Polygon -7500403 true true 15 120 150 15 285 120
Line -16777216 false 30 120 270 120

leaf
false
0
Polygon -7500403 true true 150 210 135 195 120 210 60 210 30 195 60 180 60 165 15 135 30 120 15 105 40 104 45 90 60 90 90 105 105 120 120 120 105 60 120 60 135 30 150 15 165 30 180 60 195 60 180 120 195 120 210 105 240 90 255 90 263 104 285 105 270 120 285 135 240 165 240 180 270 195 240 210 180 210 165 195
Polygon -7500403 true true 135 195 135 240 120 255 105 255 105 285 135 285 165 240 165 195

line
true
0
Line -7500403 true 150 0 150 300

line half
true
0
Line -7500403 true 150 0 150 150

pentagon
false
0
Polygon -7500403 true true 150 15 15 120 60 285 240 285 285 120

person
false
0
Circle -7500403 true true 110 5 80
Polygon -7500403 true true 105 90 120 195 90 285 105 300 135 300 150 225 165 300 195 300 210 285 180 195 195 90
Rectangle -7500403 true true 127 79 172 94
Polygon -7500403 true true 195 90 240 150 225 180 165 105
Polygon -7500403 true true 105 90 60 150 75 180 135 105

plant
false
0
Rectangle -7500403 true true 135 90 165 300
Polygon -7500403 true true 135 255 90 210 45 195 75 255 135 285
Polygon -7500403 true true 165 255 210 210 255 195 225 255 165 285
Polygon -7500403 true true 135 180 90 135 45 120 75 180 135 210
Polygon -7500403 true true 165 180 165 210 225 180 255 120 210 135
Polygon -7500403 true true 135 105 90 60 45 45 75 105 135 135
Polygon -7500403 true true 165 105 165 135 225 105 255 45 210 60
Polygon -7500403 true true 135 90 120 45 150 15 180 45 165 90

square
false
0
Rectangle -7500403 true true 30 30 270 270

square 2
false
0
Rectangle -7500403 true true 30 30 270 270
Rectangle -16777216 true false 60 60 240 240

star
false
0
Polygon -7500403 true true 151 1 185 108 298 108 207 175 242 282 151 216 59 282 94 175 3 108 116 108

target
false
0
Circle -7500403 true true 0 0 300
Circle -16777216 true false 30 30 240
Circle -7500403 true true 60 60 180
Circle -16777216 true false 90 90 120
Circle -7500403 true true 120 120 60

tree
false
0
Circle -7500403 true true 118 3 94
Rectangle -6459832 true false 120 195 180 300
Circle -7500403 true true 65 21 108
Circle -7500403 true true 116 41 127
Circle -7500403 true true 45 90 120
Circle -7500403 true true 104 74 152

triangle
false
0
Polygon -7500403 true true 150 30 15 255 285 255

triangle 2
false
0
Polygon -7500403 true true 150 30 15 255 285 255
Polygon -16777216 true false 151 99 225 223 75 224

truck
false
0
Rectangle -7500403 true true 4 45 195 187
Polygon -7500403 true true 296 193 296 150 259 134 244 104 208 104 207 194
Rectangle -1 true false 195 60 195 105
Polygon -16777216 true false 238 112 252 141 219 141 218 112
Circle -16777216 true false 234 174 42
Rectangle -7500403 true true 181 185 214 194
Circle -16777216 true false 144 174 42
Circle -16777216 true false 24 174 42
Circle -7500403 false true 24 174 42
Circle -7500403 false true 144 174 42
Circle -7500403 false true 234 174 42

turtle
true
0
Polygon -10899396 true false 215 204 240 233 246 254 228 266 215 252 193 210
Polygon -10899396 true false 195 90 225 75 245 75 260 89 269 108 261 124 240 105 225 105 210 105
Polygon -10899396 true false 105 90 75 75 55 75 40 89 31 108 39 124 60 105 75 105 90 105
Polygon -10899396 true false 132 85 134 64 107 51 108 17 150 2 192 18 192 52 169 65 172 87
Polygon -10899396 true false 85 204 60 233 54 254 72 266 85 252 107 210
Polygon -7500403 true true 119 75 179 75 209 101 224 135 220 225 175 261 128 261 81 224 74 135 88 99

wheel
false
0
Circle -7500403 true true 3 3 294
Circle -16777216 true false 30 30 240
Line -7500403 true 150 285 150 15
Line -7500403 true 15 150 285 150
Circle -7500403 true true 120 120 60
Line -7500403 true 216 40 79 269
Line -7500403 true 40 84 269 221
Line -7500403 true 40 216 269 79
Line -7500403 true 84 40 221 269

x
false
0
Polygon -7500403 true true 270 75 225 30 30 225 75 270
Polygon -7500403 true true 30 75 75 30 270 225 225 270

@#$#@#$#@
NetLogo 5.0.5-RC1
@#$#@#$#@
setup
repeat 5 [rewire-one]
@#$#@#$#@
@#$#@#$#@
<experiments>
  <experiment name="vary-rewiring-probability" repetitions="5" runMetricsEveryStep="false">
    <go>rewire-all</go>
    <timeLimit steps="1"/>
    <exitCondition>rewiring-probability &gt; 1</exitCondition>
    <metric>average-path-length</metric>
    <metric>clustering-coefficient</metric>
    <steppedValueSet variable="rewiring-probability" first="0" step="0.025" last="1"/>
  </experiment>
</experiments>
@#$#@#$#@
@#$#@#$#@
default
0.0
-0.2 0 1.0 0.0
0.0 1 1.0 0.0
0.2 0 1.0 0.0
link direction
true
0
Line -7500403 true 150 150 90 180
Line -7500403 true 150 150 210 180

@#$#@#$#@
1
@#$#@#$#@
