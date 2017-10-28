package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.Arrays;

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

        ImageButton imageButton = (ImageButton) findViewById(R.id.imageButton);
        Bitmap bmp;

        if ((bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap()) != null) {

            // Make the bitmap mutable so we can mess with it
            bmp = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            int distFromCentre = 25;     //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY
            int widthToColourAtOnce = distFromCentre * 2 + 1;

            for (int j = distFromCentre+1;
                 j <= bmp.getHeight()-distFromCentre;
                 j += widthToColourAtOnce)
                for (int i = distFromCentre+1;
                     i <= bmp.getWidth()-distFromCentre;
                     i += widthToColourAtOnce) {

                    testCount++;
                    int colour = 0;

                    // 1: Threshold
                    // 2: Masking
                    int method = 2;

                    if (method == 1) {
                        int threshold = 100;
                        int brightness = Color.blue(bmp.getPixel(i,j));
                        colour = (brightness > threshold) ? Color.WHITE : Color.BLACK;

                        // Colour in the point around this pixel
                        ImageManipulation.colourPoint(bmp, i, j, colour, distFromCentre);
                    } else if (method == 2) {
                        ImageManipulation.colourMaskPoint(bmp, i, j, distFromCentre);
                    }
                }

            Log.d("Hi", "The image was " + bmp.getWidth() + " x " + bmp.getHeight());
            Log.d("Hi", "Looped " + testCount + " times.");

            imageButton.setImageBitmap(bmp);

            Log.d("Hi", "Put the bitmap on the buttonImage");
        } else {
            Log.d("Hi", "Couldn't find a bitmap! :'(");
        }
    }
}
