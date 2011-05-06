Introduction
============
Project for implementing 'page curl' effect on Android + OpenGL ES 1.0 (possibly 1.1/2.0 too if there's clear advantage in using them).
Feel free to use everything found here on what ever purpose you can imagine of. With the exception of
images I'm using as example as they are randomly selected from Google Images. And application icon is borrowed
from [deviantART](http://browse.deviantart.com/customization/icons/dock/#/dz0w8n). Besides these
exceptions, let it be as-is implementation or - maybe more preferably - as an example for implementing your own effect.<br>
<br>
Ps. If you still feel I'm breaking your intellectual rights it's absolutely unintentional.
I did some research using Google and checked a bunch of videos found on YouTube but never visited www.upsto.gov.
In such case please do drop me an email - instead of suing without a blink - ok?

ToDo
====
* Adjust fake soft shadow calculation. It's an endless road..
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
Only difference is that instead of using a static grid I implemented an algorithm
which 'splits' rectangle dynamically regarding curl position and direction.
This is done in order to get better render quality and to reduce polygon count.
We really do not want to draw polygons separately if they lie next to each other on same plane.
It's more appropriate to have more vertices used for drawing rotating part instead.
On negative side lots of code complexity comes from the need for creating a triangle strip for rendering.
Using a solid grid such problems do not occur at all.<br/>

* Page Flip 2D [http://nomtek.com/tips-for-developers/page-flip-2d/]
* Page Flip 3D [http://nomtek.com/tips-for-developers/page-flip-3d/]

It isn't very difficult to see what happens here once you take a paper and simply
curl it to some direction. If you fold paper completely, cylinder, curl happens around,
radius becomes zero, making it more of a 2D effect. And likewise folding the paper so
that curl radius is constant most of the characteristics remain - most importantly there
is a line - at center of this 'cylinder' - which has constant slope not dependent on radius.
Its distance from the point you're holding the paper varies only. Using such approach makes
handling curl position based on touch events a lot easier compared to using a cone
as solid curling is done around.<br/>
<br/>
Curl/cylinder is defined with three parameters, position, which is any point on a line collinear to
curl. Direction vector which tells direction curl 'opens to'. And curl/cylinder
radius. 'Paper' is first translated and rotated; curl position translates
to origin and then rotated so that curl opens to right (1,0). This transformation makes
it a bit easier to calculate rotating vertices as all vertices which have x -coordinate
at least 0 are not affected. Vertices which have x -coordinate between (-PI*radius, 0)
are within 'curl', and if x -coordinate is less than equal to -PI*radius they are completely rotated.
And scan line algorithm for splitting lines within rotating area is more simple as
scan lines are always vertical. Not to forget rotating happens around y -axis (0, radius) as
cylinder center is positioned at x=0. And after we translate these vertices back to
original position we have a curl which direction heads to direction vector and it's center
is located at given curl position.<br/>
<br/>
1. At first there is a piece of paper represented by 4 vertices at its corners.<br/>
![Paper 1](https://github.com/harism/android_page_curl/blob/master/paper1.jpg?raw=true)<br/>
2. And let's say we want to curl it approximately like this.<br/>
![Paper 2](https://github.com/harism/android_page_curl/blob/master/paper2.jpg?raw=true)<br/>
3. Which could results in something like these vertices. Adding more scan lines within the rotating
area increases its quality. But the idea remains, bounding lines of original rectangle are split on
more/less dense basis.<br/>
![Paper 3](https://github.com/harism/android_page_curl/blob/master/paper3.jpg?raw=true)<br/>
<br/>
But, once again, using a piece of paper and doing some experiments by yourself works
much more as a proper explanation than words, mine at least, can tell.
And maybe gives some idea how to make better/more realistic implementation.
Happy page flipping  :)<br/>

Sources of inspiration
======================
Some YouTube links to page curl/flip implementations I've found interesting for a reason or another.

* Tiffany [http://www.youtube.com/watch?v=Yg04wfnDpiQ]
* Huone Inc [http://www.youtube.com/watch?v=EVHksX0GdIQ]
* CodeFlakes [http://www.youtube.com/watch?v=ynu4Ov-29Po]

Not to forget some of my real time rendering heros. One day I'm beating them all. One day.

* Rgba&Tbc - Elevated [http://www.youtube.com/watch?v=_YWMGuh15nE]
* Andromeda&Orb - Stargazer [http://www.youtube.com/watch?v=5u1cqYLNbJI]
* Cncd&Flt - Numb Res [http://www.youtube.com/watch?v=LTOC_ajkRkU]
