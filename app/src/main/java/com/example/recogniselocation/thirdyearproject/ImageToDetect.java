package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.IntentCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.ColorUtils;
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
        BitmapDrawable abmp = (BitmapDrawable)
                ResourcesCompat.getDrawable(getResources(), R.drawable.kinder_scout, null);
        Bitmap bmp = abmp.getBitmap();

        if (bmp != null) {

            // Make the bitmap mutable so we can mess with it
            bmp = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            // distFromCentre
            int d = 25;     //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY
            int widthToColourAtOnce = d * 2 + 1;

            for (int j = d+1;
                 j <= bmp.getHeight()-d;
                 j += widthToColourAtOnce)
                for (int i = d+1;
                     i <= bmp.getWidth()-d;
                     i += widthToColourAtOnce) {
                    testCount++;

                    // 1: Threshold
                    // 2: Masking
                    // 3: Thresh then mask
                    int method = 2;

                    int colour = 0;

                    if (method == 1) {
                        int threshold = 100;
                        int brightness = Color.blue(bmp.getPixel(i,j));
                        colour = (brightness > threshold) ? Color.WHITE : Color.BLACK;
                    } else if (method == 2) {
                        int top = Color.blue(bmp.getPixel(i - d / 3,    j - d))
                                + Color.blue(bmp.getPixel(i + 0,        j - d)) * 2
                                + Color.blue(bmp.getPixel(i + d / 3,    j - d))

                                + Color.blue(bmp.getPixel(i - d,        j - d / 2))
                                + Color.blue(bmp.getPixel(i - d / 3,    j - d / 2)) * 2
                                + Color.blue(bmp.getPixel(i + 0 ,       j - d / 2)) * 3
                                + Color.blue(bmp.getPixel(i + d / 3,    j - d / 2)) * 2
                                + Color.blue(bmp.getPixel(i + d - 1,    j - d / 2));

                        int bottom = - Color.blue(bmp.getPixel(i - d,   j + d / 2))
                                - Color.blue(bmp.getPixel(i - d / 3,    j + d / 2)) * 2
                                - Color.blue(bmp.getPixel(i + 0,        j + d / 2)) * 3
                                - Color.blue(bmp.getPixel(i + d / 3,    j + d / 2)) * 2
                                - Color.blue(bmp.getPixel(i + d - 1,    j + d / 2))

                                - Color.blue(bmp.getPixel(i - d / 3,    j + d - 1))
                                - Color.blue(bmp.getPixel(i + 0,        j + d - 1)) * 2
                                - Color.blue(bmp.getPixel(i + d / 3,    j + d - 1));

                        int edgeness = (top + bottom) / 5; // 5 got from trial and error Todo: Figure this out

                        //Log.d("Hi", "Top: " + top + " \tBottom: " + bottom + " \t= " + Math.abs(edgeness));
                        colour = (Math.abs(edgeness) > 180) ? Color.WHITE : Color.BLACK;
                        //colour = Math.abs(edgeness) > 255 ? 255 : Math.abs(edgeness);
                    } else if (method == 3) {

                        // We are looking at the first pixel (area) here, we can't
                        // threshold AND mask here. We'll have to complete this i,j loop
                        // and then mask
                    }

                    // setPixels needs an int array of colours
                    int[] colours = new int[widthToColourAtOnce * widthToColourAtOnce];
                    Arrays.fill(colours, colour);

                    bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                            widthToColourAtOnce,    // stride, width of what you wanna colour in
                            i - d - 1, // x co-ord of first pixel to colour
                            j - d - 1, // y co-ord of first pixel to colour
                            widthToColourAtOnce,    // width of area to colour
                            widthToColourAtOnce);   // height of area to colour
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
