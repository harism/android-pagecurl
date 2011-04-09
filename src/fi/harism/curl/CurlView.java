package fi.harism.curl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * EGL View.
 * 
 * @author harism
 */
public class CurlView extends GLSurfaceView {

	// Actual renderer.
	private CurlRenderer mCurlRenderer;
	
	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx) {
		super(ctx);
		init(ctx);
	}
	
	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init(ctx);
	}
	
	/**
	 * Default constructor.
	 */
	public CurlView(Context ctx, AttributeSet attrs, int defStyle) {
		this(ctx, attrs);
	}
	
	/**
	 * Initialize method.
	 */
	private void init(Context ctx) {
		mCurlRenderer = new CurlRenderer();
		setRenderer(mCurlRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		this.setOnTouchListener(new TouchListener());
	}
	
	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		
		// Create Bitmap for renderer.
		Drawable d = getResources().getDrawable(R.drawable.world);
		d.setBounds(0, 0, w, h);
		Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		b.eraseColor(0x00000000);
		Canvas c = new Canvas(b);
		d.draw(c);
		
		// Update Bitmap.
		mCurlRenderer.setBitmap(b);
		requestRender();
	}
	
	/**
	 * Touch listener.
	 */
	private class TouchListener implements View.OnTouchListener {
		
		// ACTION_DOWN coodinates.
		private float x1 = 0;
		private float y1 = 0;

		@Override
		public boolean onTouch(View v, MotionEvent me) {
			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				x1 = 1.0f;
				y1 = ((getHeight() / 2.0f) - me.getY()) / (getHeight());
			}
			if (me.getAction() == MotionEvent.ACTION_MOVE) {
				float x2 = (me.getX() - (getWidth() / 2.0f)) / (getWidth() / 2.0f);
				float y2 = ((getHeight() / 2.0f) - me.getY()) / (getHeight());
				mCurlRenderer.curl(x1, y1, x2, y2);
				requestRender();
			}
			return true;
		}
		
	}
	
}
