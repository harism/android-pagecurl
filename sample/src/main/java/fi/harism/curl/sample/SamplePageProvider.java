package fi.harism.curl.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import fi.harism.curl.CurlPage;
import fi.harism.curl.CurlView;

/**
 * Created by namgyu.park on 2017. 11. 4..
 */

public class SamplePageProvider implements CurlView.PageProvider {

    // Bitmap resources.
    private int[] mBitmapIds = { fi.harism.curl.R.drawable.obama, fi.harism.curl.R.drawable.road_rage,
            fi.harism.curl.R.drawable.taipei_101, fi.harism.curl.R.drawable.world };

    private final Context context;
    private final int pageCount;

    public SamplePageProvider(Context context, int pageCount) {
        this.context = context;
        this.pageCount = pageCount;
    }

    @Override
    public int getPageCount() {
        return pageCount;
    }

    @Override
    public void updatePage(CurlPage page, int width, int height, int index) {

        switch (index) {
            // First case is image on front side, solid colored back.
            case 0: {
                Bitmap front = loadBitmap(width, height, index);
                page.setTexture(front, CurlPage.SIDE_FRONT);
                page.setColor(Color.rgb(180, 180, 180), CurlPage.SIDE_BACK);
                break;
            }
            // Second case is image on back side, solid colored front.
            case 1: {
                Bitmap back = loadBitmap(width, height, 2);
                page.setTexture(back, CurlPage.SIDE_BACK);
                page.setColor(Color.rgb(127, 140, 180), CurlPage.SIDE_FRONT);
                break;
            }
            // Third case is images on both sides.
            case 2: {
                Bitmap front = loadBitmap(width, height, 1);
                Bitmap back = loadBitmap(width, height, 3);
                page.setTexture(front, CurlPage.SIDE_FRONT);
                page.setTexture(back, CurlPage.SIDE_BACK);
                break;
            }
            // Fourth case is images on both sides - plus they are blend against
            // separate colors.
            case 3: {
                Bitmap front = loadBitmap(width, height, 2);
                Bitmap back = loadBitmap(width, height, 1);
                page.setTexture(front, CurlPage.SIDE_FRONT);
                page.setTexture(back, CurlPage.SIDE_BACK);
                page.setColor(Color.argb(127, 170, 130, 255),
                        CurlPage.SIDE_FRONT);
                page.setColor(Color.rgb(255, 190, 150), CurlPage.SIDE_BACK);
                break;
            }
            // Fifth case is same image is assigned to front and back. In this
            // scenario only one texture is used and shared for both sides.
            case 4:
                Bitmap front = loadBitmap(width, height, 0);
                page.setTexture(front, CurlPage.SIDE_BOTH);
                page.setColor(Color.argb(127, 255, 255, 255),
                        CurlPage.SIDE_BACK);
                break;
        }

    }

    private Bitmap loadBitmap(int width, int height, int index) {
        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        b.eraseColor(0xFFFFFFFF);
        Canvas c = new Canvas(b);
        Drawable d = context.getResources().getDrawable(mBitmapIds[index]);

        int margin = 7;
        int border = 3;
        Rect r = new Rect(margin, margin, width - margin, height - margin);

        int imageWidth = r.width() - (border * 2);
        int imageHeight = imageWidth * d.getIntrinsicHeight() / d.getIntrinsicWidth();

        if (imageHeight > r.height() - (border * 2)) {
            imageHeight = r.height() - (border * 2);
            imageWidth = imageHeight * d.getIntrinsicWidth() / d.getIntrinsicHeight();
        }

        r.left += ((r.width() - imageWidth) / 2) - border;
        r.right = r.left + imageWidth + border + border;
        r.top += ((r.height() - imageHeight) / 2) - border;
        r.bottom = r.top + imageHeight + border + border;

        Paint p = new Paint();
        p.setColor(0xFFC0C0C0);
        c.drawRect(r, p);
        r.left += border;
        r.right -= border;
        r.top += border;
        r.bottom -= border;

        d.setBounds(r);
        d.draw(c);

        return b;
    }
}
