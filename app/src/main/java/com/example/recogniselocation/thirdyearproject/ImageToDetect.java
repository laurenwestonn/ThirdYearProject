package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

/**
 * Created by LaUrE on 20/10/2017.
 */

public class ImageToDetect extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_to_detect);
    }

    public void clickedImage(View view) {
        Log.d("Hi", "You tapped the image");
        BitmapDrawable abmp = (BitmapDrawable)
                ResourcesCompat.getDrawable(getResources(), R.drawable.kinder_scout, null);
        Bitmap bmp = abmp.getBitmap();


        if (bmp != null) {
            // Make the bitmap mutable so we can mess with it
            bmp = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            for (int i = 0; i < bmp.getWidth(); i++)
                for (int j = 0; j < bmp.getHeight(); j++) {
                    testCount++;
                    bmp.setPixel(i, j, bmp.getPixel(i,j) * 3);
                }
            Log.d("Hi", "The image was " + bmp.getWidth() + " x " + bmp.getHeight());
            Log.d("Hi", "Looped " + testCount + " times.");

            ImageButton imageButton = (ImageButton) findViewById(R.id.imageButton);
            imageButton.setImageBitmap(bmp);

            Log.d("Hi", "Put the bitmap on the buttonImage");
        } else {
            Log.d("Hi", "No bitmap!");
        }
    }
}
