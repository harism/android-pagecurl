Intro
=====
Project for trying to implement 'page curl' effect on Android + OpenGL ES 1.0 (possibly 1.1/2.0 too if there's clear advantage).

ToDo
====
* Fix shadows as now there's overlapping in some situations.
* Adjust fake shadow calculation.
* Add some details to make it more book like.

ReadMe
======
Here are a few links describing this page curl implementation somewhat well. Only difference is that instead of using a static grid I implemented an algorithm which 'splits' rectangle dynamically regarding curl position and angle. This is done in order to get better render quality and to reduce polygon count. We really do not need to draw polygons separately if they lie next to each other on same plane.<br/>
<br/>
(http://nomtek.com/tips-for-developers/page-flip-2d/)<br/>
(http://nomtek.com/tips-for-developers/page-flip-3d/)<br/>
<br/>
It isn't very difficult to see what happens here once you take a paper and simply curl it to some direction. If you fold paper completely cylinder, curling happens around, radius becomes zero, making it more of a 2D effect. And otherwise press the curl so that curl radius is constant. Keeping this in mind makes calculations a lot easier compared to using a cone as solid curling is done around.<br/>
<br/>
Also, 'paper' is first rotated and translated so that curl position is translated to origo and rotated so that curl opens to right (1,0). This transformation makes it a bit easier to calculate split vertices as all vertices which have x -coordinate at least 0 are not affected. Vertices which have x -coordinate between (-PI*radius, 0) are within curl, and if x -coordinate is less than -PI*radius they are completely rotated. And scan line algorithm for splitting curled area is somewhat more straightforward as scan lines are vertical. Not to forget curling happens always around y -axis (0, radius) as curl is positioned to x=0.