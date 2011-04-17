package fi.harism.curl;

import android.app.Activity;
import android.os.Bundle;

/**
 * Basic holder for Activity for curl testing.
 * 
 * @author harism
 */
public class CurlActivity extends Activity {

	private CurlView mCurlView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mCurlView = (CurlView) findViewById(R.id.curl);
	}

	@Override
	public void onPause() {
		super.onPause();
		mCurlView.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		mCurlView.onResume();
	}

}