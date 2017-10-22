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

            // Make the bitmap mutable so we can mess with it
            bmp = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            int distFromCentre = 4;     //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY
            int widthToColourAtOnce = distFromCentre * 2 + 1;

            for (int i = distFromCentre+1;
                 i <= bmp.getWidth()-distFromCentre;
                 i += widthToColourAtOnce)
                for (int j = distFromCentre+1;
                     j <= bmp.getHeight()-distFromCentre;
                     j += widthToColourAtOnce) {
                    testCount++;
                    int colour = bmp.getPixel(i, j) * 3;

                    // setPixels needs an int array of colours
                    // only need to make the array hold the one colour
                    //int[] colours = new int[]{colour};
                    int[] colours = new int[widthToColourAtOnce * widthToColourAtOnce];
                    Arrays.fill(colours, colour);

                    bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                            widthToColourAtOnce,    // stride, width of what you wanna colour in
                            i - distFromCentre - 1, // x coord of first pixel to colour
                            j - distFromCentre - 1, // y coord of first pixel to colour
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