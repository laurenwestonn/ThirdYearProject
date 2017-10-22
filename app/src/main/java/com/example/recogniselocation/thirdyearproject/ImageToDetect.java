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

            // 1: Resize image, work on that. Faster but unclear
            // 2: Keep same sized image, skip pixels in the loops. Clear but slow
            // 3: 2, but see if I can set many pixels in one method call
            int method = 1;

            if (method == 1)
                // Make image 4 times smaller so we have less pixels to deal with
                bmp = bmp.createScaledBitmap(bmp,bmp.getWidth() / 5, bmp.getHeight() / 5, true);

            // Make the bitmap mutable so we can mess with it
            bmp = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            if (method == 1)
                for (int i = 0; i < bmp.getWidth(); i++)
                    for (int j = 0; j < bmp.getHeight(); j++) {
                        testCount++;
                        bmp.setPixel(i, j, bmp.getPixel(i,j) * 3);
                    }
            else if (method == 2)
                for (int i = 2; i < bmp.getWidth()-2; i += 5)
                    for (int j = 2; j < bmp.getHeight()-2; j += 5) {
                        testCount++;
                        int colour = bmp.getPixel(i,j) * 3;
                        //Top left
                        bmp.setPixel(i-2, j+2, colour);
                        bmp.setPixel(i-2, j+1, colour);
                        bmp.setPixel(i-1, j+2, colour);
                        bmp.setPixel(i-1, j+1, colour);

                        // Top
                        bmp.setPixel(i, j+1, colour);
                        bmp.setPixel(i, j+2, colour);

                        //Top right
                        bmp.setPixel(i+2, j+2, colour);
                        bmp.setPixel(i+2, j+1, colour);
                        bmp.setPixel(i+1, j+2, colour);
                        bmp.setPixel(i+1, j+1, colour);

                        //Right
                        bmp.setPixel(i+1, j, colour);
                        bmp.setPixel(i+2, j, colour);

                        //Bottom right
                        bmp.setPixel(i+2, j-2, colour);
                        bmp.setPixel(i+2, j-1, colour);
                        bmp.setPixel(i+1, j-2, colour);
                        bmp.setPixel(i+1, j-1, colour);

                        //Bottom
                        bmp.setPixel(i, j-1, colour);
                        bmp.setPixel(i, j-2, colour);

                        //Bottom left
                        bmp.setPixel(i-2, j-2, colour);
                        bmp.setPixel(i-2, j-1, colour);
                        bmp.setPixel(i-1, j-2, colour);
                        bmp.setPixel(i-1, j-1, colour);

                        //Left
                        bmp.setPixel(i-1, j, colour);
                        bmp.setPixel(i-2, j, colour);

                        //Centre
                        bmp.setPixel(i, j, colour);
                    }
            else if (method == 3)
                for (int i = 2; i < bmp.getWidth()-2; i += 5)
                    for (int j = 2; j < bmp.getHeight()-2; j += 5) {
                        testCount++;
                        int colour = bmp.getPixel(i, j) * 3;

                        // setPixels needs an int array of colours
                        // only need to make the array hold the one colour
                        int[] colours = new int[]{colour};

                        bmp.setPixels(colours, 0, 0, i - 2, j - 2, 5, 5);
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
