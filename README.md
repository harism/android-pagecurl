NOTE
====

As of this commit all "pages" will be 2-sided. There's a new class CurlPage, using which
you can assign a separate texture on both sides, one for front side only, or same
texture can be shared on both sides. CurlPage contains also color values for blending
which allows you to e.g share texture on both sides but do alpha blending
only on back side of the page, leaving you with exactly same effect what earlier
version of this example application did. This time with the difference there's
some more freedom included.

Here's an example video from [cags12](https://github.com/cags12) showing 2-sided page support in landscape mode;

[http://www.youtube.com/watch?v=iwu7P5PCpsw]

Introduction
============
Project for implementing 'page curl' effect on Android + OpenGL ES 1.0 (possibly 1.1/2.0 too if there's clear advantage in using them).

The source code is released under Apache 2.0 and can be used in commercial or personal projects. See LICENSE for more information. See NOTICE for any exceptions, these include namely the application icon and images used in the demo. Besides these exceptions, let it be as-is implementation or - maybe more preferably - as an example for implementing your own effect.

For the ones without Android development environment, and/or people willing to take only
a brief look on what's happening here, there are a few video captures. While person responsible for capturing
them is not a happy owner of Android device yet It's truly hoped performance is not that poor on actual device compared
to what you see here;

* [With textures](http://www.youtube.com/watch?v=WbNyapB9jvI)
* [Without textures](http://www.youtube.com/watch?v=AFmJ-ON-ulI)

So what you saw there;

* There are approximately 26 + 26 + 4 + 4 = 60 vertices at most.
* 8 vertices for underlying pages, 4 for each.
* ~26 vertices for curled page + ~26 vertices for fake soft shadow. These numbers are maximum
values and vary depending on curl position and angle.
* Rendering them as triangle strips end up producing approximately 50 polygons at most. To give
some perspective rendering a cube without back face culling requires 8 vertices and 12 polygons.

What you didn't see;

* There is an experimental flag unable to show on emulator. With the kind help of
[Andrew Winter](https://github.com/drewjw81) it was possible to do some experimenting
on using touch pressure information. If you go and call CurlView.setEnableTouchPressure(true),
curl radius will be adjusted based on the touch pressure. The more you press, the smaller it gets.
Currently it's a vast one line 'hack', simply to show off it's there, mostly for experimenting,
but who knows. If someone takes the work and calibrates it properly it might turn out to something.
* Z-buffering, really, we don't need to do depth check giving us a minor performance boost.
* Lightning, it's a more of a trick we use here, leaving us with something very close to flat shading actually.
* Perspective projection. Since it's very much easier to map render target to screen size using
orthogonal projection it's used by default. For quick prototyping you can enable it though,
check USE_PERSPECTIVE_PROJECTION flag in CurlRenderer.

ToDo
====
* Adjust fake soft shadow calculation. Current 'drop shadow', cast behind curl, implementation
is not exactly what one wants necessarily. It might be better strategy to leave some 'softness'
for situations in which radius is close to zero. Currently it means shadow is decreased to
non-existent. Well, that's how shadows work, but considering from usability side..
* Add some details to make it a bit more book alike.

Some details
============
Implementation can be divided roughly in two parts.

* CurlMesh - which handles actual curl calculations and everything related to its rendering.
* CurlView - which is responsible for providing rendering environment - but more importantly -
handles curl manipulation. Meaning it receives touch events and repositions curl based on them.
While this sounds something utterly trivial, it really isn't, and becomes more complex if curl didn't happen
around a cylinder. Depending on what you're trying to achieve of course. For me it was
most important from the beginning that 'paper edge' follows pointer at all times.

Anyway, here are a few links describing this page curl implementation somewhat well.
Only difference is that instead of using a static grid an algorithm which 'splits'
rectangle dynamically regarding curl position and direction was implemented.
This is done in order to get better render quality and to reduce polygon count.
It's an absolute win-win situation if these things can be combined with limited amount
of extra calculation to ease the work of renderer. In this particular case, we really
do not want to draw polygons separately if they lie next to each other on same plane.
It's more appropriate to have more vertices used for drawing rotating part instead.
On negative side lots of code complexity comes from the need for creating a triangle strip for rendering.
Using a solid grid such problems do not occur at all.

* Page Flip 2D [http://nomtek.com/tips-for-developers/page-flip-2d/]
* Page Flip 3D [http://nomtek.com/tips-for-developers/page-flip-3d/]

It isn't very difficult to see what happens here once you take a paper and simply
curl it to some direction. If you fold paper completely, cylinder, curl happens around,
radius becomes zero, making it more of a 2D effect. And likewise folding the paper so
that curl radius is constant most of the characteristics remain - most importantly there
is a line - at the center of this 'cylinder' - which has constant slope not dependent on radius.
Its distance from the point you're holding the paper varies only. Keeping this in mind makes
curl position handling based on touch events a lot easier compared to using a cone
as solid curling is done around. For information on using a cone, it's highly recommended to take a look on W. Dana Nuon's [blog
post](http://wdnuon.blogspot.com/2010/05/implementing-ibooks-page-curling-using.html)
on the topic. Chris Luke's [article](http://blog.flirble.org/2010/10/08/the-anatomy-of-a-page-curl/)
is a good read too, even though he came to pretty much the same conclusion as me,
better to go with cylinder instead.

Curl/cylinder is defined with three parameters, position, which is any point on a line collinear to
curl. Direction vector which tells direction curl 'opens to'. And curl/cylinder
radius. 'Paper' is first translated and rotated; curl position translates
to origin and then rotated so that curl opens to right (1, 0). This transformation makes
it a bit easier to calculate rotating vertices as all vertices which have x -coordinate
at least 0 are not affected. Vertices which have x -coordinate between (-PI*radius, 0)
are within 'curl', and if x -coordinate is less than equal to -PI*radius they are completely rotated.
And scan line algorithm for splitting lines within rotating area is more simple as
scan lines are always vertical. Not to forget rotating happens around y -axis as
cylinder center is positioned at x = 0. And after we translate these vertices back to
original position we have a curl which direction heads to direction vector and it's center
is located at given curl position.

1. At first there is a piece of paper represented by 4 vertices at its corners.<br/>
![Paper 1](https://github.com/harism/android_page_curl/blob/master/paper1.jpg?raw=true)<br/>
2. And let's say we want to curl it approximately like this.<br/>
![Paper 2](https://github.com/harism/android_page_curl/blob/master/paper2.jpg?raw=true)<br/>
3. Which could results in something like these vertices. Adding more scan lines within the rotating
area increases its quality. But the idea remains, bounding lines of original rectangle are split on
more/less dense basis.<br/>
![Paper 3](https://github.com/harism/android_page_curl/blob/master/paper3.jpg?raw=true)<br/>

But, once again, using a piece of paper and doing some experiments by yourself works
much more as a proper explanation than words, mine at least, can tell.
And maybe gives some idea how to make better/more realistic implementation.
Happy page flipping  :)

Sources of inspiration
======================
Some YouTube links to page curl/flip implementations which were found interesting for a reason or another.

* Tiffany [http://www.youtube.com/watch?v=Yg04wfnDpiQ]
* Huone Inc [http://www.youtube.com/watch?v=EVHksX0GdIQ]
* CodeFlakes [http://www.youtube.com/watch?v=ynu4Ov-29Po]

Not to forget many of the true real-time rendering heros. One day We're about to beat them all. One day. Right?

* Rgba&Tbc - Elevated [http://www.youtube.com/watch?v=_YWMGuh15nE]
* Andromeda&Orb - Stargazer [http://www.youtube.com/watch?v=5u1cqYLNbJI]
* Cncd&Flt - Numb Res [http://www.youtube.com/watch?v=LTOC_ajkRkU]
