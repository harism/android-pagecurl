package fi.harism.curl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * Class implementing actual curl.
 * 
 * @author harism
 */
public class CurlMesh {

	// Flag for additional lines.
	private static final boolean DRAW_HELPERS = false;
	// Flag for drawing polygon outlines.
	private static final boolean DRAW_POLYGON_OUTLINES = false;
	// Flag for enabling texture rendering.
	private static final boolean DRAW_TEXTURE = true;
	// Flag for enabling shadow rendering.
	private static final boolean DRAW_SHADOW = true;

	// Colors for shadow.
	private static final float[] SHADOW_INNER_COLOR = { 0f, 0f, 0f, .5f };
	private static final float[] SHADOW_OUTER_COLOR = { 0f, 0f, 0f, .0f };

	// Alpha values for front and back facing texture.
	private static final double BACKFACE_ALPHA = .2f;
	private static final double FRONTFACE_ALPHA = 1f;
	private boolean mSwapAlpha = false;

	// For testing purposes.
	private int mHelperLinesCount;
	private FloatBuffer mHelperLines;

	// Buffers for feeding rasterizer.
	private FloatBuffer mVertices;
	private FloatBuffer mTexCoords;
	private FloatBuffer mColors;
	private int mVerticesCountFront;
	private int mVerticesCountBack;

	private FloatBuffer mShadowColors;
	private FloatBuffer mShadowVertices;
	private int mDropShadowCount;
	private int mSelfShadowCount;

	private int mMaxCurlSplits;

	private Vertex[] mRectangle = new Vertex[4];

	private int[] mTextureIds;
	private Bitmap mBitmap;

	// Let's avoid using 'new' as much as possible.
	private Array<Vertex> mTempVertices;
	private Array<Vertex> mIntersections;
	private Array<Vertex> mOutputVertices;
	private Array<Vertex> mRotatedVertices;
	private Array<Double> mScanLines;
	private Array<ShadowVertex> mTempShadowVertices;
	private Array<ShadowVertex> mSelfShadowVertices;
	private Array<ShadowVertex> mDropShadowVertices;

	/**
	 * Constructor for mesh object.
	 * 
	 * @param maxCurlSplits
	 *            Maximum number curl can be divided into.
	 */
	public CurlMesh(int maxCurlSplits) {
		// There really is no use for 0 splits.
		mMaxCurlSplits = maxCurlSplits < 1 ? 1 : maxCurlSplits;

		mScanLines = new Array<Double>(maxCurlSplits + 2);
		mOutputVertices = new Array<Vertex>(7);
		mRotatedVertices = new Array<Vertex>(4);
		mIntersections = new Array<Vertex>(2);
		mTempVertices = new Array<Vertex>(7 + 4);
		for (int i = 0; i < 7 + 4; ++i) {
			mTempVertices.add(new Vertex());
		}

		if (DRAW_SHADOW) {
			mSelfShadowVertices = new Array<ShadowVertex>(
					(mMaxCurlSplits + 2) * 2);
			mDropShadowVertices = new Array<ShadowVertex>(
					(mMaxCurlSplits + 2) * 2);
			mTempShadowVertices = new Array<ShadowVertex>(
					(mMaxCurlSplits + 2) * 2);
			for (int i = 0; i < (mMaxCurlSplits + 2) * 2; ++i) {
				mTempShadowVertices.add(new ShadowVertex());
			}
		}

		for (int i = 0; i < 4; ++i) {
			mRectangle[i] = new Vertex();
		}

		if (DRAW_HELPERS) {
			mHelperLinesCount = 3;
			ByteBuffer hvbb = ByteBuffer
					.allocateDirect(mHelperLinesCount * 2 * 2 * 4);
			hvbb.order(ByteOrder.nativeOrder());
			mHelperLines = hvbb.asFloatBuffer();
			mHelperLines.position(0);
		}

		int maxVerticesCount = 4 + 2 + 2 * mMaxCurlSplits;
		ByteBuffer vbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertices = vbb.asFloatBuffer();
		mVertices.position(0);

		if (DRAW_TEXTURE) {
			ByteBuffer tbb = ByteBuffer
					.allocateDirect(maxVerticesCount * 2 * 4);
			tbb.order(ByteOrder.nativeOrder());
			mTexCoords = tbb.asFloatBuffer();
			mTexCoords.position(0);
		}

		ByteBuffer cbb = ByteBuffer.allocateDirect(maxVerticesCount * 4 * 4);
		cbb.order(ByteOrder.nativeOrder());
		mColors = cbb.asFloatBuffer();
		mColors.position(0);

		if (DRAW_SHADOW) {
			int maxShadowVerticesCount = (mMaxCurlSplits + 2) * 2 * 2;
			ByteBuffer scbb = ByteBuffer
					.allocateDirect(maxShadowVerticesCount * 4 * 4);
			scbb.order(ByteOrder.nativeOrder());
			mShadowColors = scbb.asFloatBuffer();
			mShadowColors.position(0);

			ByteBuffer sibb = ByteBuffer
					.allocateDirect(maxShadowVerticesCount * 3 * 4);
			sibb.order(ByteOrder.nativeOrder());
			mShadowVertices = sibb.asFloatBuffer();
			mShadowVertices.position(0);

			mDropShadowCount = mSelfShadowCount = 0;
		}
	}

	/**
	 * Curl calculation.
	 * 
	 * @param curlPos
	 *            Position for curl center.
	 * @param directionVec
	 *            Curl direction.
	 * @param radius
	 *            Radius of curl.
	 */
	public synchronized void curl(PointF curlPos, PointF directionVec,
			double radius) {

		// First add some 'helper' lines used for development.
		if (DRAW_HELPERS) {
			mHelperLines.position(0);

			mHelperLines.put(curlPos.x);
			mHelperLines.put(curlPos.y - 1.0f);
			mHelperLines.put(curlPos.x);
			mHelperLines.put(curlPos.y + 1.0f);
			mHelperLines.put(curlPos.x - 1.0f);
			mHelperLines.put(curlPos.y);
			mHelperLines.put(curlPos.x + 1.0f);
			mHelperLines.put(curlPos.y);

			mHelperLines.put(curlPos.x);
			mHelperLines.put(curlPos.y);
			mHelperLines.put(curlPos.x + directionVec.x * 2);
			mHelperLines.put(curlPos.y + directionVec.y * 2);

			mHelperLines.position(0);
		}

		// Actual 'curl' implementation starts here.
		mVertices.position(0);
		mColors.position(0);
		if (DRAW_TEXTURE) {
			mTexCoords.position(0);
		}

		// Calculate curl direction.
		double curlAngle = Math.acos(directionVec.x);
		curlAngle = directionVec.y > 0 ? -curlAngle : curlAngle;

		// Initiate rotated 'rectangle' which's is translated to curlPos and
		// rotated so that curl direction heads to (1,0). Vertices are ordered
		// in ascending order based on x -coordinate at the same time. And using
		// y -coordinate in very rare case where two vertices have same x
		// -coordinate.
		mTempVertices.addAll(mRotatedVertices);
		mRotatedVertices.clear();
		for (int i = 0; i < 4; ++i) {
			Vertex v = mTempVertices.remove(0);
			v.set(mRectangle[i]);
			v.translate(-curlPos.x, -curlPos.y);
			v.rotateZ(-curlAngle);
			int j = 0;
			for (; j < mRotatedVertices.size(); ++j) {
				Vertex v2 = mRotatedVertices.get(j);
				if (v.mPosX > v2.mPosX) {
					break;
				}
				if (v.mPosX == v2.mPosX && v.mPosY > v2.mPosY) {
					break;
				}
			}
			mRotatedVertices.add(j, v);
		}

		// Rotated rectangle lines/vertex indices. We need to find bounding
		// lines for rotated rectangle. After sorting vertices according to
		// their x -coordinate we don't have to worry about vertices at indices
		// 0 and 1. But due to inaccuracy it's possible vertex 3 is not the
		// opposing corner from vertex 0. So we are calculating distance from
		// vertex 0 to vertices 2 and 3 - and altering line indices if needed.
		int lines[][] = { {0, 1}, {0, 2}, {1, 3}, {2, 3} };
		{
			Vertex v0 = mRotatedVertices.get(0);
			Vertex v2 = mRotatedVertices.get(2);
			Vertex v3 = mRotatedVertices.get(3);
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

		mVerticesCountFront = mVerticesCountBack = 0;

		if (DRAW_SHADOW) {
			mTempShadowVertices.addAll(mDropShadowVertices);
			mTempShadowVertices.addAll(mSelfShadowVertices);
			mDropShadowVertices.clear();
			mSelfShadowVertices.clear();
		}

		// Length of 'curl' curve.
		double curlLength = Math.PI * radius;
		// Calculate scan lines.
		mScanLines.clear();
		if (mMaxCurlSplits > 0) {
			mScanLines.add((double) 0);
		}
		for (int i = 1; i < mMaxCurlSplits; ++i) {
			mScanLines.add((-curlLength * i) / (mMaxCurlSplits - 1));
		}
		mScanLines.add(mRotatedVertices.get(3).mPosX - 1);

		// Start from right most vertex.
		double scanXmax = mRotatedVertices.get(0).mPosX + 1;
		for (int i = 0; i < mScanLines.size(); ++i) {
			double scanXmin = mScanLines.get(i);
			// First iterate vertices within scan area.
			for (int j = 0; j < mRotatedVertices.size(); ++j) {
				Vertex v = mRotatedVertices.get(j);
				if (v.mPosX >= scanXmin && v.mPosX <= scanXmax) {
					Vertex n = mTempVertices.remove(0);
					n.set(v);
					Array<Vertex> intersections = getIntersections(
							mRotatedVertices, lines, n.mPosX);
					if (intersections.size() == 1
							&& intersections.get(0).mPosY > v.mPosY) {
						mOutputVertices.addAll(intersections);
						mOutputVertices.add(n);
					} else if (intersections.size() <= 1) {
						mOutputVertices.add(n);
						mOutputVertices.addAll(intersections);
					} else {
						Log.d("CurlMesh", "Intersections size > 1");
						mTempVertices.add(n);
						mTempVertices.addAll(intersections);
					}
				}
			}
			// Search for line intersections.
			Array<Vertex> intersections = getIntersections(mRotatedVertices,
					lines, scanXmin);
			if (intersections.size() == 2) {
				Vertex v1 = intersections.get(0);
				Vertex v2 = intersections.get(1);
				if (v1.mPosY < v2.mPosY) {
					mOutputVertices.add(v2);
					mOutputVertices.add(v1);
				} else {
					mOutputVertices.addAll(intersections);
				}
			} else if (intersections.size() != 0) {
				Log.d("CurlMesh", "Intersections size != 0 or 2");
				mTempVertices.addAll(intersections);
			}

			// Add vertices to out buffers.
			while (mOutputVertices.size() > 0) {
				Vertex v = mOutputVertices.remove(0);
				mTempVertices.add(v);

				// Untouched vertices.
				if (i == 0) {
					v.mAlpha = mSwapAlpha ? BACKFACE_ALPHA : FRONTFACE_ALPHA;
					mVerticesCountFront++;
				}
				// 'Completely' rotated vertices.
				else if (i == mScanLines.size() - 1 || curlLength == 0) {
					v.mPosX = -(curlLength + v.mPosX);
					v.mPosZ = 2 * radius;

					v.mAlpha = mSwapAlpha ? FRONTFACE_ALPHA : BACKFACE_ALPHA;
					mVerticesCountBack++;
				}
				// Vertex lies within 'curl'.
				else {
					// Even though it's not obvious from the if-else clause,
					// here
					// v.mPosX is between [-curlLength, 0]. And we can do
					// calculations around a half cylinder.
					double rotY = Math.PI * (v.mPosX / curlLength);
					v.mPosX = radius * Math.sin(rotY);
					v.mPosZ = radius - (radius * Math.cos(rotY));
					// Map color multiplier to [.1f, 1f] range.
					v.mColor = .1f + .9f * Math.sqrt(Math.sin(rotY) + 1);

					if (v.mPosZ >= radius) {
						v.mAlpha = mSwapAlpha ? FRONTFACE_ALPHA
								: BACKFACE_ALPHA;
						mVerticesCountBack++;
					} else {
						v.mAlpha = mSwapAlpha ? BACKFACE_ALPHA
								: FRONTFACE_ALPHA;
						mVerticesCountFront++;
					}
				}

				// Rotate vertex back to 'world' coordinates.
				v.rotateZ(curlAngle);
				v.translate(curlPos.x, curlPos.y);
				addVertex(v);

				// Drop shadow is cast 'behind' the curl.
				if (DRAW_SHADOW && v.mPosZ > 0 && v.mPosZ <= radius) {
					ShadowVertex sv = mTempShadowVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = (v.mPosZ / 2) * -directionVec.x;
					sv.mPenumbraY = (v.mPosZ / 2) * -directionVec.y;
					sv.mPenumbraInnerColor = v.mPosZ / radius;
					int idx = (mDropShadowVertices.size() + 1) / 2;
					mDropShadowVertices.add(idx, sv);
				}
				// Self shadow is cast partly over mesh.
				if (DRAW_SHADOW && v.mPosZ > radius) {
					// TODO: Shadow penumbra direction is not good, shouldn't be
					// calculated using only directionVec.
					ShadowVertex sv = mTempShadowVertices.remove(0);
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPosZ = v.mPosZ;
					sv.mPenumbraX = ((v.mPosZ - radius) / 2) * directionVec.x;
					sv.mPenumbraY = ((v.mPosZ - radius) / 2) * directionVec.y;
					sv.mPenumbraInnerColor = (v.mPosZ - radius) / radius;
					int idx = (mSelfShadowVertices.size() + 1) / 2;
					mSelfShadowVertices.add(idx, sv);
				}
			}

			scanXmax = scanXmin;
		}

		mVertices.position(0);
		mColors.position(0);
		if (DRAW_TEXTURE) {
			mTexCoords.position(0);
		}

		// Add shadow Vertices.
		if (DRAW_SHADOW) {
			mShadowColors.position(0);
			mShadowVertices.position(0);
			mDropShadowCount = 0;

			for (int i = 0; i < mDropShadowVertices.size(); ++i) {
				ShadowVertex sv = mDropShadowVertices.get(i);
				mShadowVertices.put((float) sv.mPosX);
				mShadowVertices.put((float) sv.mPosY);
				mShadowVertices.put((float) sv.mPosZ);
				mShadowVertices.put((float) (sv.mPosX + sv.mPenumbraX));
				mShadowVertices.put((float) (sv.mPosY + sv.mPenumbraY));
				mShadowVertices.put((float) sv.mPosZ);
				for (int j = 0; j < 4; ++j) {
					double color = SHADOW_OUTER_COLOR[j]
							+ (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j])
							* sv.mPenumbraInnerColor;
					mShadowColors.put((float) color);
				}
				mShadowColors.put(SHADOW_OUTER_COLOR);
				mDropShadowCount += 2;
			}
			mSelfShadowCount = 0;
			for (int i = 0; i < mSelfShadowVertices.size(); ++i) {
				ShadowVertex sv = mSelfShadowVertices.get(i);
				mShadowVertices.put((float) sv.mPosX);
				mShadowVertices.put((float) sv.mPosY);
				mShadowVertices.put((float) sv.mPosZ);
				mShadowVertices.put((float) (sv.mPosX + sv.mPenumbraX));
				mShadowVertices.put((float) (sv.mPosY + sv.mPenumbraY));
				mShadowVertices.put((float) sv.mPosZ);
				for (int j = 0; j < 4; ++j) {
					double color = SHADOW_OUTER_COLOR[j]
							+ (SHADOW_INNER_COLOR[j] - SHADOW_OUTER_COLOR[j])
							* sv.mPenumbraInnerColor;
					mShadowColors.put((float) color);
				}
				mShadowColors.put(SHADOW_OUTER_COLOR);
				mSelfShadowCount += 2;
			}
			mShadowColors.position(0);
			mShadowVertices.position(0);
		}
	}

	/**
	 * Draw our mesh.
	 */
	public synchronized void draw(GL10 gl) {
		// First allocate texture if there is not one yet.
		if (DRAW_TEXTURE && mTextureIds == null) {
			// Generate texture.
			mTextureIds = new int[1];
			gl.glGenTextures(1, mTextureIds, 0);
			// Set texture attributes.
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
					GL10.GL_LINEAR);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
					GL10.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
					GL10.GL_CLAMP_TO_EDGE);
		}
		// If mBitmap != null we have new texture.
		if (DRAW_TEXTURE && mBitmap != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
			mBitmap = null;
		}

		if (DRAW_TEXTURE) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
		}

		// Some 'global' settings.
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

		// TODO: Drop shadow drawing is done temporarily here to hide some
		// problems with its calculation.
		if (DRAW_SHADOW) {
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, mShadowColors);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mShadowVertices);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mDropShadowCount);
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}

		// Enable texture coordinates.
		if (DRAW_TEXTURE) {
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoords);
		}
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);

		// Enable color array.
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColors);

		// Draw blank / 'white' front facing vertices.
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ZERO);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVerticesCountFront);
		// Draw front facing texture.
		if (DRAW_TEXTURE) {
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVerticesCountFront);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}
		int backStartIdx = Math.max(0, mVerticesCountFront - 2);
		int backCount = mVerticesCountFront + mVerticesCountBack - backStartIdx;
		// Draw blank / 'white' back facing vertices.
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ZERO);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount);
		// Draw back facing texture.
		if (DRAW_TEXTURE) {
			gl.glEnable(GL10.GL_TEXTURE_2D);
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, backStartIdx, backCount);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		}

		// Disable textures and color array.
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

		if (DRAW_POLYGON_OUTLINES || DRAW_HELPERS) {
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnable(GL10.GL_LINE_SMOOTH);
			gl.glLineWidth(1.0f);
		}

		if (DRAW_POLYGON_OUTLINES) {
			gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);
			gl.glDrawArrays(GL10.GL_LINE_STRIP, 0, mVerticesCountFront);
		}

		if (DRAW_HELPERS) {
			gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f);
			gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mHelperLines);
			gl.glDrawArrays(GL10.GL_LINES, 0, mHelperLinesCount * 2);
		}

		if (DRAW_SHADOW) {
			gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
			gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, mShadowColors);
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mShadowVertices);
			gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, mDropShadowCount,
					mSelfShadowCount);
			gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		}

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	/**
	 * Resets mesh to 'initial' state.
	 */
	public synchronized void reset() {
		mVertices.position(0);
		mColors.position(0);
		if (DRAW_TEXTURE) {
			mTexCoords.position(0);
		}
		for (int i = 0; i < 4; ++i) {
			addVertex(mRectangle[i]);
		}
		mVerticesCountFront = 4;
		mVerticesCountBack = 0;
		mVertices.position(0);
		mColors.position(0);
		if (DRAW_TEXTURE) {
			mTexCoords.position(0);
		}

		mDropShadowCount = mSelfShadowCount = 0;
	}

	/**
	 * Sets new texture for this mesh.
	 */
	public void setBitmap(Bitmap bitmap) {
		if (DRAW_TEXTURE) {
			mBitmap = Bitmap.createScaledBitmap(bitmap,
					getNextHighestPO2(bitmap.getWidth()),
					getNextHighestPO2(bitmap.getHeight()), true);
		}
	}

	/**
	 * Update mesh bounds.
	 */
	public synchronized void setRect(RectF r) {
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
	 * Update texture bounds.
	 */
	public synchronized void setTexRect(RectF r) {
		mRectangle[0].mTexX = r.left;
		mRectangle[0].mTexY = r.top;
		mRectangle[1].mTexX = r.left;
		mRectangle[1].mTexY = r.bottom;
		mRectangle[2].mTexX = r.right;
		mRectangle[2].mTexY = r.top;
		mRectangle[3].mTexX = r.right;
		mRectangle[3].mTexY = r.bottom;

		mSwapAlpha = r.left > r.right;
		for (int i = 0; i < 4; ++i) {
			mRectangle[i].mAlpha = mSwapAlpha ? BACKFACE_ALPHA
					: FRONTFACE_ALPHA;
		}
	}

	/**
	 * Adds vertex to buffers.
	 */
	private void addVertex(Vertex vertex) {
		mVertices.put((float) vertex.mPosX);
		mVertices.put((float) vertex.mPosY);
		mVertices.put((float) vertex.mPosZ);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mAlpha);
		if (DRAW_TEXTURE) {
			mTexCoords.put((float) vertex.mTexX);
			mTexCoords.put((float) vertex.mTexY);
		}
	}

	/**
	 * Calculates intersections for given scan line.
	 */
	private Array<Vertex> getIntersections(Array<Vertex> vertices,
			int[][] lineIndices, double scanX) {
		mIntersections.clear();
		for (int j = 0; j < lineIndices.length; j++) {
			Vertex v1 = vertices.get(lineIndices[j][0]);
			Vertex v2 = vertices.get(lineIndices[j][1]);
			if (v1.mPosX > scanX && v2.mPosX < scanX) {
				double c = (scanX - v2.mPosX) / (v1.mPosX - v2.mPosX);
				Vertex n = mTempVertices.remove(0);
				n.set(v2);
				n.mPosX = scanX;
				n.mPosY += (v1.mPosY - v2.mPosY) * c;
				if (DRAW_TEXTURE) {
					n.mTexX += (v1.mTexX - v2.mTexX) * c;
					n.mTexY += (v1.mTexY - v2.mTexY) * c;
				}
				mIntersections.add(n);
			}
		}
		return mIntersections;
	}

	/**
	 * Calculates the next highest power of two for a given integer.
	 */
	private int getNextHighestPO2(int n) {
		n -= 1;
		n = n | (n >> 1);
		n = n | (n >> 2);
		n = n | (n >> 4);
		n = n | (n >> 8);
		n = n | (n >> 16);
		n = n | (n >> 32);
		return n + 1;
	}

	/**
	 * Simple fixed size array implementation.
	 */
	private class Array<T> {
		private Object[] mArray;
		private int mSize;
		private int mCapacity;

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

		public void set(int index, T item) {
			if (index < 0 || index >= mSize) {
				throw new IndexOutOfBoundsException();
			}
			mArray[index] = item;
		}

		public int size() {
			return mSize;
		}

	}

	/**
	 * Holder for shadow vertex information.
	 */
	private class ShadowVertex {
		public double mPosX;
		public double mPosY;
		public double mPosZ;
		public double mPenumbraX;
		public double mPenumbraY;
		public double mPenumbraInnerColor;
	}

	/**
	 * Holder for vertex information.
	 */
	private class Vertex {
		public double mPosX;
		public double mPosY;
		public double mPosZ;
		public double mTexX;
		public double mTexY;
		public double mColor;
		public double mAlpha;

		public Vertex() {
			mPosX = mPosY = mPosZ = mTexX = mTexY = 0;
			mColor = mAlpha = 1;
		}

		public void rotateZ(double theta) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			double x = mPosX * cos + mPosY * sin;
			double y = mPosX * -sin + mPosY * cos;
			mPosX = x;
			mPosY = y;
		}

		public void set(Vertex vertex) {
			mPosX = vertex.mPosX;
			mPosY = vertex.mPosY;
			mPosZ = vertex.mPosZ;
			mTexX = vertex.mTexX;
			mTexY = vertex.mTexY;
			mColor = vertex.mColor;
			mAlpha = vertex.mAlpha;
		}

		public void translate(double dx, double dy) {
			mPosX += dx;
			mPosY += dy;
		}
	}
}
