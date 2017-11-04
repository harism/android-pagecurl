package fi.harism.curl.sample;

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import fi.harism.curl.CurlView;
import fi.harism.curl.sample.databinding.ActivitySampleBinding;

public class SampleActivity extends Activity{

    private ActivitySampleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample);

        int index = 0;
        if (getLastNonConfigurationInstance() != null) {
            index = (int) getLastNonConfigurationInstance();
        }

        binding.curl.setPageProvider(new SamplePageProvider(this, 5));
        binding.curl.setSizeChangedObserver(new CurlView.SizeChangedObserver() {
            @Override
            public void onSizeChanged(int width, int height) {
                if (width > height) {
                    binding.curl.setViewMode(CurlView.SHOW_TWO_PAGES);
                    binding.curl.setMargins(.1f, .05f, .1f, .05f);
                } else {
                    binding.curl.setViewMode(CurlView.SHOW_ONE_PAGE);
                    binding.curl.setMargins(.1f, .1f, .1f, .1f);
                }
            }
        });
        binding.curl.setCurrentIndex(index);
        binding.curl.setBackgroundColor(0xFF202830);
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.curl.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.curl.onResume();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return binding.curl.getCurrentIndex();
    }
}
