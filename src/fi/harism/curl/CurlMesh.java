package fi.harism.curl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

import android.graphics.PointF;
import android.graphics.RectF;

/**
 * Class implementing actual curl.
 * 
 * @author harism
 */
public class CurlMesh {

	// For testing purposes.
	private int mHelperLinesCount;
	private FloatBuffer mHelperLines;

	// Buffers for feeding rasterizer.
	private FloatBuffer mVertices;
	private FloatBuffer mTexCoords;
	private FloatBuffer mNormals;

	private int mLineIndicesCount;
	private ShortBuffer mLineIndices;

	private int mTriangleIndicesCount;
	private ShortBuffer mTriangleIndices;

	private int mMaxCurlSplits;

	private Vertex[] mRectangle = new Vertex[4];

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
	public CurlMesh(RectF rect, RectF texRect, int maxCurlSplits) {
		// We really must have at least one split line.
		mMaxCurlSplits = maxCurlSplits < 1 ? 1 : maxCurlSplits;

		mRectangle[0] = new Vertex(rect.left, rect.top, 0, texRect.left,
				texRect.top);
		mRectangle[1] = new Vertex(rect.left, rect.bottom, 0, texRect.left,
				texRect.bottom);
		mRectangle[2] = new Vertex(rect.right, rect.top, 0, texRect.right,
				texRect.top);
		mRectangle[3] = new Vertex(rect.right, rect.bottom, 0, texRect.right,
				texRect.bottom);

		mHelperLinesCount = 3;
		ByteBuffer hvbb = ByteBuffer
				.allocateDirect(mHelperLinesCount * 2 * 2 * 4);
		hvbb.order(ByteOrder.nativeOrder());
		mHelperLines = hvbb.asFloatBuffer();
		mHelperLines.position(0);

		int maxVerticesCount = 4 + 2 * mMaxCurlSplits;
		ByteBuffer vbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertices = vbb.asFloatBuffer();
		mVertices.position(0);

		ByteBuffer tbb = ByteBuffer.allocateDirect(maxVerticesCount * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTexCoords = tbb.asFloatBuffer();
		mTexCoords.position(0);

		ByteBuffer nbb = ByteBuffer.allocateDirect(maxVerticesCount * 3 * 4);
		nbb.order(ByteOrder.nativeOrder());
		mNormals = nbb.asFloatBuffer();
		mNormals.position(0);

		// TODO: Calculate element count more accurately.
		ByteBuffer libb = ByteBuffer
				.allocateDirect(maxVerticesCount * 2 * 2 * 2);
		libb.order(ByteOrder.nativeOrder());
		mLineIndices = libb.asShortBuffer();
		mLineIndices.position(0);
		mLineIndicesCount = 0;

		ByteBuffer tibb = ByteBuffer.allocateDirect(maxVerticesCount * 2);
		tibb.order(ByteOrder.nativeOrder());
		mTriangleIndices = tibb.asShortBuffer();
		mTriangleIndices.position(0);
		mTriangleIndicesCount = 0;
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

		// Actual 'curl' implementation starts here.
		mVertices.position(0);
		mTexCoords.position(0);
		mNormals.position(0);
		mLineIndices.position(0);
		mTriangleIndices.position(0);

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

		// Our rectangle lines/vertex indices.
		int lines[] = { 0, 1, 2, 0, 3, 1, 3, 2 };
		// Length of 'curl' curve.
		double curlLength = Math.PI * radius;
		// Calculate scan lines.
		Vector<Double> scanLines = new Vector<Double>();
		scanLines.add((double) 0);
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
			Vector<Vertex> temp = new Vector<Vertex>();
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
					temp.add(n);
				}
			}
			// This happens if scan line intersects a vertex.
			if (temp.size() == 1) {
				for (int j = 0; j < rotatedVertices.size(); ++j) {
					Vertex v = rotatedVertices.elementAt(j);
					if (v.mPosX == scanXmax) {
						Vertex n = new Vertex(v);
						temp.add(n);
					}
				}
			}
			out.addAll(temp);

			// Add vertices to out buffers.
			while (out.size() > 0) {
				Vertex v = out.remove(0);

				// 'Completely' rotated vertices.
				if (v.mPosX <= -curlLength) {
					v.mPosX = -(curlLength + v.mPosX);
					v.mPosZ = 2 * radius;
				}
				// Vertex lies within 'curl'.
				else if (v.mPosX < 0) {
					double rotY = Math.PI / 2;
					rotY -= Math.PI * (v.mPosX / curlLength);
					// rotY = -rotY;
					v.mPosX = radius * Math.cos(rotY);
					v.mPosZ = radius + (radius * -Math.sin(rotY));
					v.mNormalX = Math.cos(rotY);
					v.mNormalZ = Math.abs(Math.sin(rotY));
				}
				
				// Rotate vertex back to 'world' coordinates.
				v.rotateZ(curlAngle);
				v.translate(curlPos.x, curlPos.y);
				addVertex(v);
			}

			scanXmax = scanXmin;
		}

		mLineIndicesCount = mLineIndices.position();
		mTriangleIndicesCount = mTriangleIndices.position();
		mVertices.position(0);
		mTexCoords.position(0);
		mNormals.position(0);
		mLineIndices.position(0);
		mTriangleIndices.position(0);
	}

	/**
	 * Adds vertex to buffers.
	 */
	private void addVertex(Vertex vertex) {
		int pos = mVertices.position() / 3;
		mVertices.put((float) vertex.mPosX);
		mVertices.put((float) vertex.mPosY);
		mVertices.put((float) vertex.mPosZ);
		mTexCoords.put((float) vertex.mTexX);
		mTexCoords.put((float) vertex.mTexY);
		mNormals.put((float) vertex.mNormalX);
		mNormals.put((float) vertex.mNormalY);
		mNormals.put((float) vertex.mNormalZ);

		mTriangleIndices.put((short) pos);

		if (pos > 0) {
			mLineIndices.put((short) (pos - 1));
			mLineIndices.put((short) pos);
		}
		if (pos > 1) {
			mLineIndices.put((short) (pos - 2));
			mLineIndices.put((short) pos);
		}

	}

	/**
	 * Draw our mesh.
	 */
	public synchronized void draw(GL10 gl) {
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);

		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoords);

		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		float light0Ambient[] = { 0.7f, 0.7f, 0.7f, 1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, light0Ambient, 0);
		float light0Diffuse[] = { 1.0f, 1.0f, 1.0f, 1.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, light0Diffuse, 0);
		float light0Specular[] = { 0.0f, 0.0f, 0.0f, 0.0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, light0Specular, 0);
		float light0Position[] = { 0f, 0f, 10f, 0f };
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light0Position, 0);

		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glNormalPointer(GL10.GL_FLOAT, 0, mNormals);

		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glDrawElements(GL10.GL_TRIANGLE_STRIP, mTriangleIndicesCount,
				GL10.GL_UNSIGNED_SHORT, mTriangleIndices);
		gl.glDisable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_LIGHT0);
		gl.glDisableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnable(GL10.GL_LINE_SMOOTH);
		gl.glLineWidth(1.0f);

		gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertices);
		gl.glDrawElements(GL10.GL_LINES, mLineIndicesCount,
				GL10.GL_UNSIGNED_SHORT, mLineIndices);

		gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mHelperLines);
		gl.glDrawArrays(GL10.GL_LINES, 0, mHelperLinesCount * 2);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
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
		public double mNormalX;
		public double mNormalY;
		public double mNormalZ;

		public Vertex(double posX, double posY, double posZ, double texX,
				double texY) {
			mPosX = posX;
			mPosY = posY;
			mPosZ = posZ;
			mTexX = texX;
			mTexY = texY;
			mNormalX = mNormalY = 0;
			mNormalZ = 1.0f;
		}

		public Vertex(Vertex vertex) {
			mPosX = vertex.mPosX;
			mPosY = vertex.mPosY;
			mPosZ = vertex.mPosZ;
			mTexX = vertex.mTexX;
			mTexY = vertex.mTexY;
			mNormalX = vertex.mNormalX;
			mNormalY = vertex.mNormalY;
			mNormalZ = vertex.mNormalZ;
		}

		public void translate(double dx, double dy) {
			mPosX += dx;
			mPosY += dy;
		}

		public void rotateZ(double theta) {
			double cos = Math.cos(theta);
			double sin = Math.sin(theta);
			double x = mPosX * cos + mPosY * sin;
			double y = mPosX * -sin + mPosY * cos;
			mPosX = x;
			mPosY = y;

			x = mNormalX * cos + mNormalY * sin;
			y = mNormalX * -sin + mPosY * cos;
			mNormalX = x;
			mNormalY = y;
		}
	}
}
