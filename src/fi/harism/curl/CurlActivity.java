package fi.harism.curl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CurlActivity extends Activity {
	
	private CurlView mCurlView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mCurlView = (CurlView) findViewById(R.id.curl);
        
        Button renderButton = (Button) findViewById(R.id.render_button);
        renderButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				mCurlView.requestRender();
			}
		});
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