Introduction
============
Project for implementing 'page curl' effect on Android + OpenGL ES 1.0 (possibly 1.1/2.0 too if there's clear advantage in using them).
Feel free to use everything found here on what ever purpose you can imagine of. Let it be as-is implementation
or - maybe more preferably - as an example for implementing your own effect.

ToDo
====
* Fix shadows as now there's overlapping with surface in some situations.
* Adjust fake shadow calculation.
* Add some details to make it more book like.
* Split implementation into two separate Eclipse projects. A library and an example application using it.

Some details
============
Here are a few links describing this page curl implementation somewhat well.
Only difference is that instead of using a static grid I implemented an algorithm
which 'splits' rectangle dynamically regarding curl position and direction.
This is done in order to get better render quality and to reduce polygon count.
We really do not want to draw polygons separately if they lie next to each other on same plane.
It's more appropriate to have more vertices used for drawing curled area instead.
On negative side lots of code complexity comes from the need for creating a triangle strip for rendering.
Using a solid grid such problems do not occur at all.<br/>
<br/>
[Page Flip 2D](http://nomtek.com/tips-for-developers/page-flip-2d/)<br/>
[Page Flip 3D](http://nomtek.com/tips-for-developers/page-flip-3d/)<br/>
<br/>
It isn't very difficult to see what happens here once you take a paper and simply
curl it to some direction. If you fold paper completely, cylinder, curling happens around,
radius becomes zero, making it more of a 2D effect. And likewise folding the paper so
that curl radius is constant most of the characteristics remain - most importantly there
is a line - at center of this 'cylinder' - which has constant slope not dependent on radius
its distance varies only. Using such approach makes handling curl a lot easier compared to
using a cone as solid curling is done around.<br/>
<br/>
Curl/cylinder is defined with three parameters, position, which is any point on a line collinear to
curl. Direction vector which tells direction curl 'opens to'. And curl/cylinder
radius. 'Paper' is first translated and rotated, curl position is translated
to origin and rotated so that curl opens to right (1,0). This transformation makes
it a bit easier to calculate curled vertices as all vertices which have x -coordinate
at least 0 are not affected. Vertices which have x -coordinate between (-PI*radius, 0)
are within curl, and if x -coordinate is less than -PI*radius they are completely rotated.
And scan line algorithm for splitting lines within curled area is more simple as
scan lines are always vertical. Not to forget curling happens around y -axis (0, radius) as
cylinder center is positioned at x=0. Once again, using a piece of paper works much more as a proper
explanation than words, mine at least, can tell.<br/>
<br/>
Happy curling :)
