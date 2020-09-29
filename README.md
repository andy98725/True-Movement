# About

This is a demo app centered around raycasting for sight and true movement.

"True Movement" is a rather intuitive concept; given a point and a distance, find the space reachable in a geometry from that point.

## Significance

This differs from traditional algorithms in a few minor (but important) ways.

First, the algorithm is gridless. gridded movement already has many tried and tested algorithms.

The geometry includes curves; similar algorithms exist for geometry composed entirely of lines. With such cases, one can simply form a graph of vertices and apply a Bellman Ford algorithm to solve.

And lastly, rather than solving for the distance between 2 points, the algorithm recieves a distance and provides a set of points. Practically, this makes very little difference, actually; it is mostly done for ease of display.

## Data Structures

The algorithm makes heavy use of the java.awt.Area class, which is a closed space defined by a set of lines and Bezier cCurves. Both the sight and movement algorithms iterate on these sets.

# Building

Compile and run src/Main.java

# Contact Me

This was written by Andy Hudson on 9/29/2020.
Any inquiries or comments may be directed to andyhudson725@gmail.com.
