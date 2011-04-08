package fi.harism.curl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CurlView extends GLSurfaceView {

	private CurlRenderer mCurlRenderer;
	private static final String LOG = "CurlView";
	
	public CurlView(Context ctx) {
		super(ctx);
		init(ctx);
		Log.d(LOG, "CurlView(ctx)");
	}
	
	public CurlView(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init(ctx);
		Log.d(LOG, "CurlView(ctx, attrs)");
	}
	
	public CurlView(Context ctx, AttributeSet attrs, int defStyle) {
		this(ctx, attrs);
		Log.d(LOG, "CurlView(ctx, attrs, defStyle)");
	}
	
	private void init(Context ctx) {
		Log.d(LOG, "init(ctx)");
		mCurlRenderer = new CurlRenderer();
		setRenderer(mCurlRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		
		this.setOnTouchListener(new TouchListener());
	}
	
	@Override
	public void onSizeChanged(int w, int h, int ow, int oh) {
		super.onSizeChanged(w, h, ow, oh);
		
		Drawable d = getResources().getDrawable(R.drawable.world);
		d.setBounds(0, 0, w, h);
		Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		b.eraseColor(0x00000000);
		Canvas c = new Canvas(b);
		d.draw(c);
		
		mCurlRenderer.setBitmap(b);
		requestRender();
	}
	
	private class TouchListener implements View.OnTouchListener {
		
		private float x1 = 0;
		private float y1 = 0;

		@Override
		public boolean onTouch(View v, MotionEvent me) {
			Log.d("MOTION", "x=" + me.getX() + " y=" + me.getY());
			if (me.getAction() == MotionEvent.ACTION_DOWN) {
				x1 = me.getX();
				y1 = me.getY();
				
				x1 = 1.0f; //(x1 - (getWidth() / 2.0f)) / (getWidth() / 2.0f);
				y1 = ((getHeight() / 2.0f) - y1) / (getHeight());
			}
			if (me.getAction() == MotionEvent.ACTION_MOVE) {
				float x2 = me.getX();
				float y2 = me.getY();
				
				x2 = (x2 - (getWidth() / 2.0f)) / (getWidth() / 2.0f);
				y2 = ((getHeight() / 2.0f) - y2) / (getHeight());
				
				mCurlRenderer.curl(x1, y1, x2, y2);
				requestRender();
			}
			return true;
		}
		
	}
	
}
