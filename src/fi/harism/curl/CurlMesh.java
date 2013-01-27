/*
   Copyright 2013 Harri Smatt

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package fi.harism.curl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Class implementing actual curl/page rendering.
 * 
 * @author harism
 */
public class CurlMesh {

	// Let's avoid using 'new' as much as possible. Meaning we introduce arrays
	// once here and reuse them on runtime. Doesn't really have very much effect
	// but avoids some garbage collections from happening.
	private Array<Vertex> mArrIntersections;
	private Array<Vertex> mArrOutputVertices;
	private Array<Vertex> mArrRotatedVertices;
	private Array<Double> mArrScanLines;
	private Array<ShadowVertex> mArrShadowDropVertices;
	private Array<ShadowVertex> mArrShadowSelfVertices;
	private Array<ShadowVertex> mArrShadowTempVertices;
	private Array<Vertex> mArrTempVertices;

	// Buffers for feeding rasterizer.
	private FloatBuffer mBufNormals;
	private FloatBuffer mBufShadowPenumbra;
	private FloatBuffer mBufShadowVertices;
	private FloatBuffer mBufTexCoords;
	private FloatBuffer mBufVertices;

	// Counter values.
	private int mCountShadowDrop;
	private int mCountShadowSelf;
	private int mCountVertices;

	// Boolean for 'flipping' texture sideways.
	private boolean mFlipTexture = false;
	// Maximum number of split lines used for creating a curl.
	private int mMaxCurlSplits;

	// Page instance.
	private final CurlPage mPage = new CurlPage();

	// Bounding rectangle for this mesh. mRectagle[0] = top-left corner,
	// mRectangle[1] = bottom-left, mRectangle[2] = top-right and mRectangle[3]
	// bottom-right.
	private final Vertex[] mRectangle = new Vertex[4];
	// Texture ids and other variables.
	private int[] mTextureIds = null;

	/**
	 * Constructor for mesh object.
	 * 
	 * @param maxCurlSplits
	 *            Maximum number curl can be divided into. The bigger the value
	 *            the smoother curl will be. With the cost of having more
	 *            polygons for drawing.
	 */
	public CurlMesh(int maxCurlSplits) {
		// There really is no use for 0 splits.
		mMaxCurlSplits = maxCurlSplits < 1 ? 1 : maxCurlSplits;

		mArrScanLines = new Array<Double>(maxCurlSplits + 2);
		mArrOutputVertices = new Array<Vertex>(7);
		mArrRotatedVertices = new Array<Vertex>(4);
		mArrIntersections = new Array<Vertex>(2);
		mArrTempVertices = new Array<Vertex>(7 + 4);
		for (int i = 0; i < 7 + 4; ++i) {
			mArrTempVertices.add(new Vertex());
		}

		mArrShadowSelfVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		mArrShadowDropVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		mArrShadowTempVertices = new Array<ShadowVertex>(
				(mMaxCurlSplits + 2) * 2);
		for (int i = 0; i < (mMaxCurlSplits + 2) * 2; ++i) {
			mArrShadowTempVertices.add(new ShadowVertex());
		}

		// Rectangle consists of 4 vertices. Index 0 = top-left, index 1 =
		// bottom-left, index 2 = top-right and index 3 = bottom-right.
		for (int i = 0; i < 4; ++i) {
			mRectangle[i] = new Vertex();
		}
		// Set up shadow penumbra direction to each vertex. We do fake 'self
		// shadow' calculations based on this information.
		mRectangle[0].mPenumbraX = mRectangle[1].mPenumbraX = mRectangle[1].mPenumbraY = mRectangle[3].mPenumbraY = -1;
		mRectangle[0].mPenumbraY = mRectangle[2].mPenumbraX = mRectangle[2].mPenumbraY = mRectangle[3].mPenumbraX = 1;

		// There are 4 vertices from bounding rect, max 2 from adding split line
		// to two corners and curl consists of max mMaxCurlSplits lines each
		// outputting 2 vertices.
		int maxVerticesCount = 4 + 2 + (2 * mMaxCurlSplits);
		mBufVertices = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufVertices.position(0);

		mBufTexCoords = ByteBuffer.allocateDirect(maxVerticesCount * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufTexCoords.position(0);

		mBufNormals = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufNormals.position(0);

		int maxShadowVerticesCount = (mMaxCurlSplits + 2) * 2 * 2;
		mBufShadowVertices = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 3 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufShadowVertices.position(0);

		mBufShadowPenumbra = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 2 * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mBufShadowPenumbra.position(0);
	}

	/**
	 * Adds vertex to buffers.
	 */
	private void addVertex(Vertex vertex) {
		mBufVertices.put((float) vertex.mPosX);
		mBufVertices.put((float) vertex.mPosY);
		mBufVertices.put((float) vertex.mPosZ);
		mBufTexCoords.put((float) vertex.mTexX);
		mBufTexCoords.put((float) vertex.mTexY);
		mBufNormals.put((float) vertex.mNormalX);
		mBufNormals.put((float) vertex.mNormalY);
		mBufNormals.put((float) vertex.mNormalZ);
	}

	/**
	 * Sets curl for this mesh.
	 * 
	 * @param curlPos
	 *            Position for curl 'center'. Can be any point on line collinear
	 *            to curl.
	 * @param curlDir
	 *            Curl direction, should be normalized.
	 * @param radius
	 *            Radius of curl.
	 */
	public void curl(PointF curlPos, PointF curlDir, double radius) {

		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);

		// Calculate curl angle from direction.
		double curlAngle = Math.acos(curlDir.x);
		curlAngle = curlDir.y > 0 ? -curlAngle : curlAngle;

		// Initiate rotated rectangle which's is translated to curlPos and
		// rotated so that curl direction heads to right (1,0). Vertices are
		// ordered in ascending order based on x -coordinate at the same time.
		// And using y -coordinate in very rare case in which two vertices have
		// same x -coordinate.
		mArrTempVertices.addAll(mArrRotatedVertices);
		mArrRotatedVertices.clear();
		for (int i = 0; i < 4; ++i) {
			Vertex v = mArrTempVertices.remove(0);
			v.set(mRectangle[i]);
			v.translate(-curlPos.x, -curlPos.y);
			v.rotateZ(-curlAngle);
			int j = 0;
			for (; j < mArrRotatedVertices.size(); ++j) {
				Vertex v2 = mArrRotatedVertices.get(j);
				if (v.mPosX > v2.mPosX) {
					break;
				}
				if (v.mPosX == v2.mPosX && v.mPosY > v2.mPosY) {
					break;
				}
			}
			mArrRotatedVertices.add(j, v);
		}

		// Rotated rectangle lines/vertex indices. We need to find bounding
		// lines for rotated rectangle. After sorting vertices according to
		// their x -coordinate we don't have to worry about vertices at indices
		// 0 and 1. But due to inaccuracy it's possible vertex 3 is not the
		// opposing corner from vertex 0. So we are calculating distance from
		// vertex 0 to vertices 2 and 3 - and altering line indices if needed.
		// Also vertices/lines are given in an order first one has x -coordinate
		// at least the latter one. This property is used in getIntersections to
		// see if there is an intersection.
		int lines[][] = { { 0, 1 }, { 0, 2 }, { 1, 3 }, { 2, 3 } };
		{
			// TODO: There really has to be more 'easier' way of doing this -
			// not including extensive use of sqrt.
			Vertex v0 = mArrRotatedVertices.get(0);
			Vertex v2 = mArrRotatedVertices.get(2);
			Vertex v3 = mArrRotatedVertices.get(3);
			double dist2 = Math.sqrt((v0.mPosX - v2.mPosX)
					* (v0.mPosX - v2.mPosX) + (v0.mPosY - v2.mPosY)
					* (v0.mPosY - v2.mPosY));
			double dist3 = Math.sqrt((v0.mPosX - v3.mPosX)
					* (v0.mPosX - v3.mPosX) + (v0.mPosY - v3.mPosY)
					* (v0.mPosY - v3.mPosY));
			if (dist2 > dist3) {
				lines[1][1] = 3;
				lines[2][1] = 2;
			}
		}

		mCountVertices = 0;

		mArrShadowTempVertices.addAll(mArrShadowDropVertices);
		mArrShadowTempVertices.addAll(mArrShadowSelfVertices);
		mArrShadowDropVertices.clear();
		mArrShadowSelfVertices.clear();

		// Length of 'curl' curve.
		double curlLength = Math.PI * radius;
		// Calculate scan lines.
		// TODO: Revisit this code one day. There is room for optimization here.
		mArrScanLines.clear();
		if (mMaxCurlSplits > 0) {
			mArrScanLines.add((double) 0);
		}
		for (int i = 1; i < mMaxCurlSplits; ++i) {
			mArrScanLines.add((-curlLength * i) / (mMaxCurlSplits - 1));
		}
		// As mRotatedVertices is ordered regarding x -coordinate, adding
		// this scan line produces scan area picking up vertices which are
		// rotated completely. One could say 'until infinity'.
		mArrScanLines.add(mArrRotatedVertices.get(3).mPosX - 1);

		// Start from right most vertex. Pretty much the same as first scan area
		// is starting from 'infinity'.
		double scanXmax = mArrRotatedVertices.get(0).mPosX + 1;

		for (int i = 0; i < mArrScanLines.size(); ++i) {
			// Once we have scanXmin and scanXmax we have a scan area to start
			// working with.
			double scanXmin = mArrScanLines.get(i);
			// First iterate 'original' rectangle vertices within scan area.
			for (int j = 0; j < mArrRotatedVertices.size(); ++j) {
				Vertex v = mArrRotatedVertices.get(j);
				// Test if vertex lies within this scan area.
				// TODO: Frankly speaking, can't remember why equality check was
				// added to both ends. Guessing it was somehow related to case
				// where radius=0f, which, given current implementation, could
				// be handled much more effectively anyway.
				if (v.mPosX >= scanXmin && v.mPosX <= scanXmax) {
					// Pop out a vertex from temp vertices.
					Vertex n = mArrTempVertices.remove(0);
					n.set(v);
					// This is done solely for triangulation reasons. Given a
					// rotated rectangle it has max 2 vertices having
					// intersection.
					Array<Vertex> intersections = getIntersections(
							mArrRotatedVertices, lines, n.mPosX);
					// In a sense one could say we're adding vertices always in
					// two, positioned at the ends of intersecting line. And for
					// triangulation to work properly they are added based on y
					// -coordinate. And this if-else is doing it for us.
					if (intersections.size() == 1
							&& intersections.get(0).mPosY > v.mPosY) {
						// In case intersecting vertex is higher add it first.
						mArrOutputVertices.addAll(intersections);
						mArrOutputVertices.add(n);
					} else if (intersections.size() <= 1) {
						// Otherwise add original vertex first.
						mArrOutputVertices.add(n);
						mArrOutputVertices.addAll(intersections);
					} else {
						// There should never be more than 1 intersecting
						// vertex. But if it happens as a fallback simply skip
						// everything.
						mArrTempVertices.add(n);
						mArrTempVertices.addAll(intersections);
					}
				}
			}

			// Search for scan line intersections.
			Array<Vertex> intersections = getIntersections(mArrRotatedVertices,
					lines, scanXmin);

			// We expect to get 0 or 2 vertices. In rare cases there's only one
			// but in general given a scan line intersecting rectangle there
			// should be 2 intersecting vertices.
			if (intersections.size() == 2) {
				// There were two intersections, add them based on y
				// -coordinate, higher first, lower last.

				Vertex v1 = intersections.get(0);
				Vertex v2 = intersections.get(1);
				if (v1.mPosY < v2.mPosY) {
					mArrOutputVertices.add(v2);
					mArrOutputVertices.add(v1);
				} else {
					mArrOutputVertices.addAll(intersections);
				}
			} else if (intersections.size() != 0) {
				// This happens in a case in which there is a original vertex
				// exactly at scan line or something went very much wrong if
				// there are 3+ vertices. What ever the reason just return the
				// vertices to temp vertices for later use. In former case it
				// was handled already earlier once iterating through
				// mRotatedVertices, in latter case it's better to avoid doing
				// anything with them.
				mArrTempVertices.addAll(intersections);
			}

			// Add vertices found during this iteration to vertex etc buffers.
			while (mArrOutputVertices.size() > 0) {
				Vertex v = mArrOutputVertices.remove(0);
				mArrTempVertices.add(v);

				// Local texture front-facing flag.
				// boolean textureFront;

				// Untouched vertices.
				if (i == 0) {
					v.mNormalX = 0;
					v.mNormalY = 0;
					v.mNormalZ = 1;
				}
				// 'Completely' rotated vertices.
				else if (i == mArrScanLines.size() - 1 || curlLength == 0) {
					v.mPosX = -(curlLength + v.mPosX);
					v.mPosZ = 2 * radius;
					v.mNormalX = 0;
					v.mNormalY = 0;
					v.mNormalZ = -1;
					v.mPenumbraX = -v.mPenumbraX;
				}
				// Vertex lies within 'curl'.
				else {
					// Even though it's not obvious from the if-else clause,
					// here v.mPosX is between [-curlLength, 0]. And we can do
					// calculations around a half cylinder.
					double rotY = Math.PI * (v.mPosX / curlLength);
					v.mPosX = radius * Math.sin(rotY);
					v.mPosZ = radius - (radius * Math.cos(rotY));
					v.mNormalX = Math.sin(rotY);
					v.mNormalY = 0;
					v.mNormalZ = Math.cos(rotY);
					v.mPenumbraX *= Math.cos(rotY);
				}

				// Move vertex back to 'world' coordinates.
				v.rotateZ(curlAngle);
				v.translate(curlPos.x, curlPos.y);
				addVertex(v);
				++mCountVertices;

				// Drop shadow is cast 'behind' the curl.
				if (v.mPosZ > 0 && v.mPosZ <= radius) {
					ShadowVertex sv = mArrShadowTempVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = (v.mPosZ * 0.8) * -curlDir.x;
					sv.mPenumbraY = (v.mPosZ * 0.8) * -curlDir.y;
					// sv.mPenumbraX += (v.mPosZ * 0.8) * v.mPenumbraX;
					// sv.mPenumbraY += (v.mPosZ * 0.8) * v.mPenumbraY;
					int idx = (mArrShadowDropVertices.size() + 1) / 2;
					mArrShadowDropVertices.add(idx, sv);
				}
				// Self shadow is cast partly over mesh.
				if (v.mPosZ > radius) {
					ShadowVertex sv = mArrShadowTempVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = ((v.mPosZ - radius) * 0.2) * v.mPenumbraX;
					sv.mPenumbraY = ((v.mPosZ - radius) * 0.2) * v.mPenumbraY;
					int idx = (mArrShadowSelfVertices.size() + 1) / 2;
					mArrShadowSelfVertices.add(idx, sv);
				}
			}

			// Switch scanXmin as scanXmax for next iteration.
			scanXmax = scanXmin;
		}

		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);

		// Add shadow Vertices.
		mBufShadowVertices.position(0);
		mBufShadowPenumbra.position(0);
		mCountShadowDrop = mCountShadowSelf = 0;

		for (int i = 0; i < mArrShadowDropVertices.size(); ++i) {
			ShadowVertex sv = mArrShadowDropVertices.get(i);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put(0);
			mBufShadowPenumbra.put(0);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put((float) sv.mPenumbraX);
			mBufShadowPenumbra.put((float) sv.mPenumbraY);
			mCountShadowDrop += 2;
		}
		for (int i = 0; i < mArrShadowSelfVertices.size(); ++i) {
			ShadowVertex sv = mArrShadowSelfVertices.get(i);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put(0);
			mBufShadowPenumbra.put(0);
			mBufShadowVertices.put((float) sv.mPosX);
			mBufShadowVertices.put((float) sv.mPosY);
			mBufShadowVertices.put((float) sv.mPosZ);
			mBufShadowPenumbra.put((float) sv.mPenumbraX);
			mBufShadowPenumbra.put((float) sv.mPenumbraY);
			mCountShadowSelf += 2;
		}
		mBufShadowVertices.position(0);
		mBufShadowPenumbra.position(0);
	}

	/**
	 * Getter for drop shadow vertices count.
	 */
	public int getDropShadowCount() {
		return mCountShadowDrop;
	}

	/**
	 * Getter for whether page should be flipped.
	 */
	public boolean getFlipTexture() {
		return mFlipTexture;
	}

	/**
	 * Calculates intersections for given scan line.
	 */
	private Array<Vertex> getIntersections(Array<Vertex> vertices,
			int[][] lineIndices, double scanX) {
		mArrIntersections.clear();
		// Iterate through rectangle lines each re-presented as a pair of
		// vertices.
		for (int j = 0; j < lineIndices.length; j++) {
			Vertex v1 = vertices.get(lineIndices[j][0]);
			Vertex v2 = vertices.get(lineIndices[j][1]);
			// Here we expect that v1.mPosX >= v2.mPosX and wont do intersection
			// test the opposite way.
			if (v1.mPosX > scanX && v2.mPosX < scanX) {
				// There is an intersection, calculate coefficient telling 'how
				// far' scanX is from v2.
				double c = (scanX - v2.mPosX) / (v1.mPosX - v2.mPosX);
				Vertex n = mArrTempVertices.remove(0);
				n.set(v2);
				n.mPosX = scanX;
				n.mPosY += (v1.mPosY - v2.mPosY) * c;
				n.mTexX += (v1.mTexX - v2.mTexX) * c;
				n.mTexY += (v1.mTexY - v2.mTexY) * c;
				n.mPenumbraX += (v1.mPenumbraX - v2.mPenumbraX) * c;
				n.mPenumbraY += (v1.mPenumbraY - v2.mPenumbraY) * c;
				mArrIntersections.add(n);
			}
		}
		return mArrIntersections;
	}

	/**
	 * Getter for normal Buffer.
	 */
	public FloatBuffer getNormals() {
		return mBufNormals;
	}

	/**
	 * Getter for page for this mesh.
	 */
	public CurlPage getPage() {
		return mPage;
	}

	/**
	 * Getter for self shadow vertices count.
	 */
	public int getSelfShadowCount() {
		return mCountShadowSelf;
	}

	/**
	 * Getter for shadow penumbra buffer.
	 */
	public FloatBuffer getShadowPenumbra() {
		return mBufShadowPenumbra;
	}

	/**
	 * Getter for shadow vertices buffer.
	 */
	public FloatBuffer getShadowVertices() {
		return mBufShadowVertices;
	}

	/**
	 * Getter for texture coordinate Buffer.
	 */
	public FloatBuffer getTexCoords() {
		return mBufTexCoords;
	}

	/**
	 * Getter for texture ids. Must be called from GL thread.
	 */
	public int[] getTextures() {

		// First allocate texture if there is not one yet.
		if (mTextureIds == null) {
			// Generate texture.
			mTextureIds = new int[2];
			GLES20.glGenTextures(2, mTextureIds, 0);
			for (int textureId : mTextureIds) {
				// Set texture attributes.
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
						GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			}
		}

		if (mPage.getBitmapsChanged()) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[0]);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0,
					mPage.getBitmap(CurlPage.SIDE_FRONT), 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureIds[1]);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0,
					mPage.getBitmap(CurlPage.SIDE_BACK), 0);
			mPage.recycle();
		}

		return mTextureIds;
	}

	/**
	 * Getter for vertex count.
	 */
	public int getVertexCount() {
		return mCountVertices;
	}

	/**
	 * Getter for vertex Buffer.
	 */
	public FloatBuffer getVertices() {
		return mBufVertices;
	}

	/**
	 * Resets mesh to 'initial' state. Meaning this mesh will draw a plain
	 * textured rectangle after call to this method.
	 */
	public void reset() {
		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);
		for (int i = 0; i < 4; ++i) {
			Vertex tmp = mArrTempVertices.get(0);
			tmp.set(mRectangle[i]);
			addVertex(tmp);
		}
		mCountVertices = 4;
		mBufVertices.position(0);
		mBufTexCoords.position(0);
		mBufNormals.position(0);
		mCountShadowDrop = mCountShadowSelf = 0;
	}

	/**
	 * Resets allocated texture id forcing creation of new one. After calling
	 * this method you most likely want to set bitmap too as it's lost. This
	 * method should be called only once e.g GL context is re-created as this
	 * method does not release previous texture id, only makes sure new one is
	 * requested on next render.
	 */
	public void resetTextures() {
		mTextureIds = null;
	}

	/**
	 * If true, flips texture sideways.
	 */
	public void setFlipTexture(boolean flipTexture) {
		mFlipTexture = flipTexture;
		if (flipTexture) {
			setTexCoords(1f, 0f, 0f, 1f);
		} else {
			setTexCoords(0f, 0f, 1f, 1f);
		}
	}

	/**
	 * Update mesh bounds.
	 */
	public void setRect(RectF r) {
		mRectangle[0].mPosX = r.left;
		mRectangle[0].mPosY = r.top;
		mRectangle[1].mPosX = r.left;
		mRectangle[1].mPosY = r.bottom;
		mRectangle[2].mPosX = r.right;
		mRectangle[2].mPosY = r.top;
		mRectangle[3].mPosX = r.right;
		mRectangle[3].mPosY = r.bottom;
	}

	/**
	 * Sets texture coordinates to mRectangle vertices.
	 */
	private void setTexCoords(float left, float top, float right, float bottom) {
		mRectangle[0].mTexX = left;
		mRectangle[0].mTexY = top;
		mRectangle[1].mTexX = left;
		mRectangle[1].mTexY = bottom;
		mRectangle[2].mTexX = right;
		mRectangle[2].mTexY = top;
		mRectangle[3].mTexX = right;
		mRectangle[3].mTexY = bottom;
	}

	/**
	 * Simple fixed size array implementation.
	 */
	private class Array<T> {
		private Object[] mArray;
		private int mCapacity;
		private int mSize;

		public Array(int capacity) {
			mCapacity = capacity;
			mArray = new Object[capacity];
		}

		public void add(int index, T item) {
			if (index < 0 || index > mSize || mSize >= mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			for (int i = mSize; i > index; --i) {
				mArray[i] = mArray[i - 1];
			}
			mArray[index] = item;
			++mSize;
		}

		public void add(T item) {
			if (mSize >= mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			mArray[mSize++] = item;
		}

		public void addAll(Array<T> array) {
			if (mSize + array.size() > mCapacity) {
				throw new IndexOutOfBoundsException();
			}
			for (int i = 0; i < array.size(); ++i) {
				mArray[mSize++] = array.get(i);
			}
		}

		public void clear() {
			mSize = 0;
		}

		@SuppressWarnings("unchecked")
		public T get(int index) {
			if (index < 0 || index >= mSize) {
				throw new IndexOutOfBoundsException();
			}
			return (T) mArray[index];
		}

		@SuppressWarnings("unchecked")
		public T remove(int index) {
			if (index < 0 || index >= mSize) {
				throw new IndexOutOfBoundsException();
			}
			T item = (T) mArray[index];
			for (int i = index; i < mSize - 1; ++i) {
				mArray[i] = mArray[i + 1];
			}
			--mSize;
			return item;
		}

		public int size() {
			return mSize;
		}

	}

	/**
	 * Holder for shadow vertex information.
	 */
	private class ShadowVertex {
		public double mPenumbraX;
		public double mPenumbraY;
		public double mPosX;
		public double mPosY;
		public double mPosZ;
	}

	/**
	 * Holder for vertex information.
	 */
	private class Vertex {
		public double mNormalX;
		public double mNormalY;
		public double mNormalZ;
		public double mPenumbraX;
		public double mPenumbraY;
		public double mPosX;
		public double mPosY;
		public double mPosZ;
		public double mTexX;
		public double mTexY;

		public Vertex() {
			mNormalX = mNormalY = 0;
			mNormalZ = 1;
			mPosX = mPosY = mPosZ = mTexX = mTexY = 0;
		}

		public void rotateZ(double theta) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			double x = mPosX * cos + mPosY * sin;
			double y = mPosX * -sin + mPosY * cos;
			mPosX = x;
			mPosY = y;
			double nx = mNormalX * cos + mNormalY * sin;
			double ny = mNormalX * -sin + mNormalY * cos;
			mNormalX = nx;
			mNormalY = ny;
			double px = mPenumbraX * cos + mPenumbraY * sin;
			double py = mPenumbraX * -sin + mPenumbraY * cos;
			mPenumbraX = px;
			mPenumbraY = py;
		}

		public void set(Vertex vertex) {
			mPosX = vertex.mPosX;
			mPosY = vertex.mPosY;
			mPosZ = vertex.mPosZ;
			mTexX = vertex.mTexX;
			mTexY = vertex.mTexY;
			mNormalX = vertex.mNormalX;
			mNormalY = vertex.mNormalY;
			mNormalZ = vertex.mNormalZ;
			mPenumbraX = vertex.mPenumbraX;
			mPenumbraY = vertex.mPenumbraY;
		}

		public void translate(double dx, double dy) {
			mPosX += dx;
			mPosY += dy;
		}
	}
}
