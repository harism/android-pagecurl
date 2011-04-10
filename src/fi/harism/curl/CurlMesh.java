package fi.harism.curl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class CurlMesh {

	private FloatBuffer mHelperVertices;
	
	private int mLineIndicesCount;
	private ShortBuffer mLineIndices;
	
	private int mTriangleIndicesCount;
	private ShortBuffer mTriangleIndices;

	private FloatBuffer mVertices;
	private FloatBuffer mCurledVertices;
	private FloatBuffer mTexCoords;	

	public CurlMesh(float x1, float y1, float x2, float y2, int c) {
		
		ByteBuffer hvbb = ByteBuffer.allocateDirect(6 * 2 * 2 * 4);
		hvbb.order(ByteOrder.nativeOrder());
		mHelperVertices = hvbb.asFloatBuffer();

		int w = c + 1;

		ByteBuffer vbb = ByteBuffer.allocateDirect(w * w * 2 * 4);
		vbb.order(ByteOrder.nativeOrder());
		mVertices = vbb.asFloatBuffer();
		ByteBuffer cvbb = ByteBuffer.allocateDirect(w * w * 3 * 4);
		cvbb.order(ByteOrder.nativeOrder());
		mCurledVertices = cvbb.asFloatBuffer();
		ByteBuffer tbb = ByteBuffer.allocateDirect(w * w * 2 * 4);
		tbb.order(ByteOrder.nativeOrder());
		mTexCoords = tbb.asFloatBuffer();
		
		mLineIndicesCount = c * c * 5 * 2;		
		ByteBuffer libb = ByteBuffer.allocateDirect(mLineIndicesCount * 2);
		libb.order(ByteOrder.nativeOrder());
		mLineIndices = libb.asShortBuffer();
		
		mTriangleIndicesCount = c * c * 3 * 2;
		ByteBuffer tibb = ByteBuffer.allocateDirect(mTriangleIndicesCount * 2);
		tibb.order(ByteOrder.nativeOrder());
		mTriangleIndices = tibb.asShortBuffer();		

		split(mTexCoords, 0, 0, 1, 1, c);
		split(mVertices, x1, y1, x2, y2, c);

		for (int y = 0; y < c; ++y) {
			for (int x = 0; x < c; ++x) {
				int ind = x + (y * w);
				
				// Lines.
				mLineIndices.put((short) (ind));
				mLineIndices.put((short) (ind + w));

				mLineIndices.put((short) (ind + w));
				mLineIndices.put((short) (ind + 1));

				mLineIndices.put((short) (ind + 1));
				mLineIndices.put((short) (ind));

				mLineIndices.put((short) (ind + w));
				mLineIndices.put((short) (ind + w + 1));

				mLineIndices.put((short) (ind + w + 1));
				mLineIndices.put((short) (ind + 1));
				
				// Triangles.
				mTriangleIndices.put((short)(ind));
				mTriangleIndices.put((short)(ind+w));
				mTriangleIndices.put((short)(ind+1));
				
				mTriangleIndices.put((short)(ind+w));
				mTriangleIndices.put((short)(ind+w+1));
				mTriangleIndices.put((short)(ind+1));				
			}
		}

		mHelperVertices.position(0);
		mVertices.position(0);
		mTexCoords.position(0);
		mLineIndices.position(0);
		mTriangleIndices.position(0);
	}

	public synchronized void curl(float x1, float y1, float x2, float y2) {
		
		double theta = (y2-y1) / (x1-x2);
		
		double radius = 1.0f;
		radius *= Math.min(1.0f, x2 - -1.0f);
		double curlLen = radius * Math.PI;
		double cx = (x1 - x2);
		double cy = (y1 - y2);
		double cLen = Math.sqrt(cx * cx + cy * cy);
		if (cLen < curlLen) {
			// TODO: This does not work.
			double r = (Math.PI / 2) - ((cLen / curlLen) * Math.PI);
			double x = radius * Math.cos(r);
			double cLen2 = -x;
			cLen2 = (cLen2 / 2) / cLen;
			cx = x2 + (cx * cLen2);
			cy = y2 + (cy * cLen2);
		} else {
			double cLen2 = cLen - (Math.PI * radius);
			cLen2 = (cLen2 / 2) / cLen;
			cx = x2 + (cx * cLen2);
			cy = y2 + (cy * cLen2);
		}
		
		mHelperVertices.position(0);
		
		mHelperVertices.put(x2);
		mHelperVertices.put(1.0f);
		mHelperVertices.put(x2);
		mHelperVertices.put(-1.0f);

		mHelperVertices.put(-1.0f);
		mHelperVertices.put(y2);
		mHelperVertices.put(1.0f);
		mHelperVertices.put(y2);
		
		mHelperVertices.put((float)(x2 + ((1.0f - y2) * theta)));
		mHelperVertices.put(1.0f);
		mHelperVertices.put((float)(x2 + ((-1.0f - y2) * theta)));
		mHelperVertices.put(-1.0f);
		
		mHelperVertices.put((float)(x1 + ((1.0f - y1)* theta)));
		mHelperVertices.put(1.0f);
		mHelperVertices.put((float)(x1 + ((-1.0f - y1) * theta)));
		mHelperVertices.put(-1.0f);
				
		mHelperVertices.put((float)(cx + ((1.0f - cy)* theta)));
		mHelperVertices.put(1.0f);
		mHelperVertices.put((float)(cx + ((-1.0f - cy) * theta)));
		mHelperVertices.put(-1.0f);
		
		mHelperVertices.position(0);
		
		mVertices.position(0);
		mCurledVertices.position(0);
		while (mVertices.remaining() >= 2) {
			double x = mVertices.get();
			double y = mVertices.get();
			double z = 0.0f;
			
			double curlX = cx + (y - cy) * theta;
			
			if (x > curlX) {
				double rotZ = -Math.atan(theta);
				double pX = (x - curlX) * Math.cos(rotZ);
				double pY = (x - curlX) * Math.sin(rotZ);
								
				if (pX < curlLen) {
					double rotY = Math.PI / 2;
					rotY -= Math.PI * (pX / curlLen);
					double rX = radius * Math.cos(rotY);
					double rZ = radius * (-Math.sin(rotY));				
					pX = rX;
					z = radius + rZ;
				} else {
					pX -= curlLen;
					pX = -pX;
					z = radius * 2;
				}
				
				double fX = (pX * Math.cos(-rotZ)) - (pY * Math.sin(-rotZ));
				double fY = (pX * Math.sin(-rotZ)) + (pY * Math.cos(-rotZ));
				
				x = curlX + fX;
				y -= fY;
			}
			
			mCurledVertices.put((float) x);
			mCurledVertices.put((float) y);
			mCurledVertices.put((float) z);
		}
		mVertices.position(0);
		mCurledVertices.position(0);
	}

	private void split(FloatBuffer buf, float x1, float y1, float x2, float y2,	int c) {
		float xArray[] = new float[c + 1];
		float yArray[] = new float[c + 1];

		for (int i = 0; i <= c; ++i) {
			xArray[i] = x1 + ((x2 - x1) * ((float) i / c));
			yArray[i] = y1 + ((y2 - y1) * ((float) i / c));
		}

		for (int iy = 0; iy <= c; ++iy) {
			for (int ix = 0; ix <= c; ++ix) {
				buf.put(xArray[ix]);
				buf.put(yArray[iy]);
			}
		}
	}

	public synchronized void draw(GL10 gl) {
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mCurledVertices);
		
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexCoords);
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		
		// TODO: Calculate normals.
		//gl.glEnable(GL10.GL_LIGHTING);
		//gl.glEnable(GL10.GL_LIGHT0);
		//float light0Ambient[] = { 0.0f, 0.0f, 0.0f, 1.0f };
		//gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, light0Ambient, 0);
		//float light0Diffuse[] = { 0.5f, 0.5f, 0.5f, 1.0f };
		//gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, light0Diffuse, 0);
		//float light0Specular[] = { 1.0f, 1.0f, 1.0f, 1.0f };
		//gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, light0Specular, 0);
		//float light0Position[] = { 0f, 0f, 100f, 0f };
		//gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, light0Position, 0);
		
		gl.glDrawElements(GL10.GL_TRIANGLES, mTriangleIndicesCount, GL10.GL_UNSIGNED_SHORT, mTriangleIndices);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		//gl.glDisable(GL10.GL_LIGHTING);
		//gl.glDisable(GL10.GL_LIGHT0);
		
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnable(GL10.GL_LINE_SMOOTH);

		gl.glColor4f(0.5f, 0.5f, 1.0f, 1.0f);
		gl.glDrawElements(GL10.GL_LINES, mLineIndicesCount, GL10.GL_UNSIGNED_SHORT,
				mLineIndices);

		gl.glColor4f(1.0f, 0.5f, 0.5f, 1.0f);
		gl.glVertexPointer(2, GL10.GL_FLOAT, 0, mHelperVertices);
		gl.glDrawArrays(GL10.GL_LINES, 0, 10);

		gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
	}

}
