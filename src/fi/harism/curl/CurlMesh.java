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
		
		ByteBuffer hvbb = ByteBuffer.allocateDirect(5 * 2 * 2 * 4);
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
		
		float theta = (y2-y1) / (x1-x2);
		float mx = ((x1 - x2) / 2.0f) + x2;
		float my = ((y1 - y2) / 2.0f) + y2;
		
		float limX = mx + (0.5f - my) * theta;
		if (limX < 0) {
			mx -= limX;
		}
		limX = mx + (-0.5f - my) * theta;
		if (limX < 0) {
			mx -= limX;
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
		
		mHelperVertices.put(x2 + ((1.0f - y2) * theta));
		mHelperVertices.put(1.0f);
		mHelperVertices.put(x2 + ((-1.0f - y2) * theta));
		mHelperVertices.put(-1.0f);
		
		mHelperVertices.put(x1 + ((1.0f - y1)* theta));
		mHelperVertices.put(1.0f);
		mHelperVertices.put(x1 + ((-1.0f - y1) * theta));
		mHelperVertices.put(-1.0f);
				
		mHelperVertices.put(mx + ((1.0f - my)* theta));
		mHelperVertices.put(1.0f);
		mHelperVertices.put(mx + ((-1.0f - my) * theta));
		mHelperVertices.put(-1.0f);
		
		mHelperVertices.position(0);
		
		mVertices.position(0);
		mCurledVertices.position(0);
		while (mVertices.remaining() >= 2) {
			double x = mVertices.get();
			double y = mVertices.get();
			double z = 0.0f;
			
			double xx0 = mx + (y - my) * theta;
			double xx1 = x - xx0;
			
			double ny = x2 - x1;
			double nx = y1 - y2;
			
			double len = Math.sqrt(nx*nx + ny*ny);
			nx *= 1 / len;
			ny *= 1 / len;
			
			double dotp = (xx1*nx);
			if (x > xx0) {
				x = xx0 - xx1 + 2*nx*dotp;
				y = y + 2*ny*dotp;
				z = 0.1f;
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
		gl.glDrawElements(GL10.GL_TRIANGLES, mTriangleIndicesCount, GL10.GL_UNSIGNED_SHORT, mTriangleIndices);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		
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
