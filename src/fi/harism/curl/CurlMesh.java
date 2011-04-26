package fi.harism.curl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.opengl.GLUtils;

/**
 * Class implementing actual curl.
 * 
 * @author harism
 */
public class CurlMesh {

	private static final boolean DRAW_HELPERS = false;
	private static final boolean DRAW_POLYGON_OUTLINES = false;

	private static final float[] SHADOW_INNER_COLOR = { 0f, 0f, 0f, .7f };
	private static final float[] SHADOW_OUTER_COLOR = { 0f, 0f, 0f, 0f };

	private static final double BACKFACE_ALPHA = .4f;
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

	/**
	 * Constructor for mesh object.
	 * 
	 * @param rect
	 *            Rect for the surface.
	 * @param texRect
	 *            Texture coordinates.
	 * @param maxCurlSplits
	 *            Maximum number curl can be divided into.
	 */
	public CurlMesh(int maxCurlSplits) {
		mMaxCurlSplits = maxCurlSplits;

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

		int maxVerticesCount = 4 + 2 * mMaxCurlSplits;
		ByteBuffer vbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertices = vbb.asFloatBuffer();
		mVertices.position(0);

		ByteBuffer tbb = ByteBuffer.allocateDirect(maxVerticesCount * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTexCoords = tbb.asFloatBuffer();
		mTexCoords.position(0);

		ByteBuffer cbb = ByteBuffer.allocateDirect(maxVerticesCount * 4 * 4);
		cbb.order(ByteOrder.nativeOrder());
		mColors = cbb.asFloatBuffer();
		mColors.position(0);

		int maxShadowVerticesCount = (mMaxCurlSplits + 1) * 2 * 2;
		ByteBuffer scbb = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 4 * 4);
		scbb.order(ByteOrder.nativeOrder());
		mShadowColors = scbb.asFloatBuffer();
		mShadowColors.position(0);

		ByteBuffer sibb = ByteBuffer
				.allocateDirect(maxShadowVerticesCount * 2 * 4);
		sibb.order(ByteOrder.nativeOrder());
		mShadowVertices = sibb.asFloatBuffer();
		mShadowVertices.position(0);

		mDropShadowCount = mSelfShadowCount = 0;
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
		mTexCoords.position(0);
		mColors.position(0);

		// Calculate curl direction.
		double curlAngle = Math.acos(directionVec.x);
		curlAngle = directionVec.y > 0 ? -curlAngle : curlAngle;

		// Initiate rotated 'rectangle' which's is translated to curlPos and
		// rotated so that curl direction heads to (1,0).
		Vector<Vertex> rotatedVertices = new Vector<Vertex>();
		for (int i = 0; i < 4; ++i) {
			Vertex v = new Vertex(mRectangle[i]);
			v.translate(-curlPos.x, -curlPos.y);
			v.rotateZ(-curlAngle);

			int j = 0;
			for (; j < rotatedVertices.size(); ++j) {
				Vertex v2 = rotatedVertices.elementAt(j);
				// We want to test for equality but rotating messes this unless
				// we check against certain error margin instead.
				double errormargin = 0.001f;
				if (Math.abs(v.mPosX - v2.mPosX) <= errormargin
						&& v.mPosY < v2.mPosY) {
					break;
				}
				if (Math.abs(v.mPosX - v2.mPosX) > errormargin
						&& v.mPosX > v2.mPosX) {
					break;
				}
			}
			rotatedVertices.add(j, v);
		}

		mVerticesCountFront = mVerticesCountBack = 0;
		Vector<ShadowVertex> dropShadowVertices = new Vector<ShadowVertex>();
		Vector<ShadowVertex> selfShadowVertices = new Vector<ShadowVertex>();

		// Our rectangle lines/vertex indices.
		int lines[] = { 0, 1, 2, 0, 3, 1, 3, 2 };
		// Length of 'curl' curve.
		double curlLength = Math.PI * radius;
		// Calculate scan lines.
		Vector<Double> scanLines = new Vector<Double>();
		if (mMaxCurlSplits > 0) {
			scanLines.add((double) 0);
		}
		for (int i = 1; i < mMaxCurlSplits; ++i) {
			scanLines.add((-curlLength * i) / (mMaxCurlSplits - 1));
		}
		scanLines.add(rotatedVertices.elementAt(3).mPosX - 1);

		// Start from right most vertex.
		double scanXmax = rotatedVertices.elementAt(0).mPosX + 1;
		Vector<Vertex> out = new Vector<Vertex>();
		for (int i = 0; i < scanLines.size(); ++i) {
			double scanXmin = scanLines.elementAt(i);
			// First iterate vertices within scan area.
			for (int j = 0; j < rotatedVertices.size(); ++j) {
				Vertex v = rotatedVertices.elementAt(j);
				if (v.mPosX >= scanXmin && v.mPosX < scanXmax) {
					Vertex n = new Vertex(v);
					out.add(n);
				}
			}
			// Search for line intersections.
			for (int j = 0; j < lines.length; j += 2) {
				Vertex v1 = rotatedVertices.elementAt(lines[j]);
				Vertex v2 = rotatedVertices.elementAt(lines[j + 1]);
				if ((v1.mPosX > scanXmin && v2.mPosX < scanXmin)
						|| (v2.mPosX > scanXmin && v1.mPosX < scanXmin)) {
					Vertex n = new Vertex(v2);
					n.mPosX = scanXmin;
					double c = (scanXmin - v2.mPosX) / (v1.mPosX - v2.mPosX);
					n.mPosY += (v1.mPosY - v2.mPosY) * c;
					n.mTexX += (v1.mTexX - v2.mTexX) * c;
					n.mTexY += (v1.mTexY - v2.mTexY) * c;
					out.add(n);
				}
			}

			// Add vertices to out buffers.
			while (out.size() > 0) {
				Vertex v = out.remove(0);

				// Untouched vertices.
				if (i == 0) {
					v.mAlpha = mSwapAlpha ? BACKFACE_ALPHA : FRONTFACE_ALPHA;
					mVerticesCountFront++;
				}
				// 'Completely' rotated vertices.
				else if (i == scanLines.size() - 1 || radius == 0) {
					v.mPosX = -(curlLength + v.mPosX);
					v.mPosZ = 2 * radius;

					v.mAlpha = mSwapAlpha ? FRONTFACE_ALPHA : BACKFACE_ALPHA;
					mVerticesCountBack++;
				}
				// Vertex lies within 'curl'.
				else {
					double rotY = Math.PI / 2;
					rotY -= Math.PI * (v.mPosX / curlLength);
					// rotY = -rotY;
					v.mPosX = radius * Math.cos(rotY);
					v.mPosZ = radius + (radius * -Math.sin(rotY));
					v.mColor = Math.sqrt(Math.cos(rotY) + 1);

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
				if (v.mPosZ > 0 && v.mPosZ <= radius) {
					// TODO: There is some overlapping in some cases, not all
					// vertices should be added to shadow.
					ShadowVertex sv = new ShadowVertex();
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPenumbraX = (v.mPosZ / 4) * -directionVec.x;
					sv.mPenumbraY = (v.mPosZ / 4) * -directionVec.y;
					int idx = (dropShadowVertices.size() + 1) / 2;
					dropShadowVertices.add(idx, sv);
				}
				// Self shadow is cast partly over mesh.
				if (v.mPosZ > radius) {
					// TODO: Shadow penumbra direction is not good, shouldn't be
					// calculated using only directionVec.
					ShadowVertex sv = new ShadowVertex();
					sv.mPosX = v.mPosX;
					sv.mPosY = v.mPosY;
					sv.mPenumbraX = ((v.mPosZ - radius) / 4) * directionVec.x;
					sv.mPenumbraY = ((v.mPosZ - radius) / 4) * directionVec.y;
					int idx = (selfShadowVertices.size() + 1) / 2;
					selfShadowVertices.add(idx, sv);
				}
			}

			scanXmax = scanXmin;
		}

		mVertices.position(0);
		mTexCoords.position(0);
		mColors.position(0);

		// Add shadow Vertices.
		mShadowColors.position(0);
		mShadowVertices.position(0);
		mDropShadowCount = 0;
		for (int i = 0; i < dropShadowVertices.size(); ++i) {
			ShadowVertex sv = dropShadowVertices.get(i);
			mShadowVertices.put((float) sv.mPosX);
			mShadowVertices.put((float) sv.mPosY);
			mShadowVertices.put((float) (sv.mPosX + sv.mPenumbraX));
			mShadowVertices.put((float) (sv.mPosY + sv.mPenumbraY));
			mShadowColors.put(SHADOW_INNER_COLOR);
			mShadowColors.put(SHADOW_OUTER_COLOR);
			mDropShadowCount += 2;
		}
		mSelfShadowCount = 0;
		for (int i = 0; i < selfShadowVertices.size(); ++i) {
			ShadowVertex sv = selfShadowVertices.get(i);
			mShadowVertices.put((float) sv.mPosX);
			mShadowVertices.put((float) sv.mPosY);
			mShadowVertices.put((float) (sv.mPosX + sv.mPenumbraX));
			mShadowVertices.put((float) (sv.mPosY + sv.mPenumbraY));
			mShadowColors.put(SHADOW_INNER_COLOR);
			mShadowColors.put(SHADOW_OUTER_COLOR);
			mSelfShadowCount += 2;
		}
		mShadowColors.position(0);
		mShadowVertices.position(0);
	}

	/**
	 * Draw our mesh.
	 */
	public synchronized void draw(GL10 gl) {
		// First allocate texture if there is not one yet.
		if (mTextureIds == null) {
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
		if (mBitmap != null) {
			gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);
			GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, mBitmap, 0);
			mBitmap = null;
		}

		gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureIds[0]);

		// Some 'global' settings.
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);

		// Enable texture coordinates.
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoords);

		// Enable color array.
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColors);

		// Draw blank / 'white' front facing vertices.
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ZERO);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVerticesCountFront);
		// Draw front facing texture.
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mVerticesCountFront);
		// Draw blank / 'white' back facing vertices.
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glBlendFunc(GL10.GL_ONE, GL10.GL_ZERO);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, mVerticesCountFront,
				mVerticesCountBack);
		// Draw back facing texture.
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, mVerticesCountFront,
				mVerticesCountBack);
		gl.glDisable(GL10.GL_TEXTURE_2D);

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

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, mShadowColors);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mShadowVertices);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, mDropShadowCount);
		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, mDropShadowCount,
				mSelfShadowCount);
		gl.glDisableClientState(GL10.GL_COLOR_ARRAY);
		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

	/**
	 * Resets mesh to 'initial' state.
	 */
	public synchronized void reset() {
		mVertices.position(0);
		mTexCoords.position(0);
		mColors.position(0);
		for (int i = 0; i < 4; ++i) {
			addVertex(mRectangle[i]);
		}
		mVerticesCountFront = 4;
		mVerticesCountBack = 0;
		mVertices.position(0);
		mTexCoords.position(0);
		mColors.position(0);

		mDropShadowCount = mSelfShadowCount = 0;
	}

	/**
	 * Sets new texture for this mesh.
	 */
	public void setBitmap(Bitmap bitmap) {
		mBitmap = bitmap;
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
		mTexCoords.put((float) vertex.mTexX);
		mTexCoords.put((float) vertex.mTexY);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mColor);
		mColors.put((float) vertex.mAlpha);
	}

	/**
	 * Holder for shadow vertex information.
	 */
	private class ShadowVertex {
		public double mPosX;
		public double mPosY;
		public double mPenumbraX;
		public double mPenumbraY;
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

		public Vertex(Vertex vertex) {
			mPosX = vertex.mPosX;
			mPosY = vertex.mPosY;
			mPosZ = vertex.mPosZ;
			mTexX = vertex.mTexX;
			mTexY = vertex.mTexY;
			mColor = vertex.mColor;
			mAlpha = vertex.mAlpha;
		}

		public void rotateZ(double theta) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			double x = mPosX * cos + mPosY * sin;
			double y = mPosX * -sin + mPosY * cos;
			mPosX = x;
			mPosY = y;
		}

		public void translate(double dx, double dy) {
			mPosX += dx;
			mPosY += dy;
		}
	}
}
