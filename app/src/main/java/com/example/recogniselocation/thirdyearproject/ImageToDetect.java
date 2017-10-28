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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LaUrE on 20/10/2017.
 */

public class ImageToDetect extends Activity {

    boolean sdDetail = true;    // Want to draw SD and log info about standard deviation under "sd"?
    int distFromCentre = 25;    //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY

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

            List ysOfEdges = new ArrayList();

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            int widthToColourAtOnce = distFromCentre * 2 + 1;

            for (int j = distFromCentre+1;
                 j < bmp.getHeight()-distFromCentre;
                 j += widthToColourAtOnce)
                for (int i = distFromCentre+1;
                     i < bmp.getWidth()-distFromCentre;
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
                        ImageManipulation.colourArea(bmp, i, j, colour, widthToColourAtOnce, widthToColourAtOnce);
                    } else if (method == 2) {
                        boolean relevantEdge = ImageManipulation.colourMaskPoint(bmp, i, j, distFromCentre);
                        if (relevantEdge)
                            ysOfEdges.add(j);
                    }
                }

            Log.d("Hi", "The image was " + bmp.getWidth() + " x " + bmp.getHeight());
            Log.d("Hi", "Looped " + testCount + " times.");

            imageButton.setImageBitmap(bmp);

            Log.d("Hi", "Put the bitmap on the buttonImage");

            // Standard Deviation
            double mean = StandardDeviation.calcMean(ysOfEdges);
            double sd = StandardDeviation.calcSD(ysOfEdges, mean);
            int minRange = (int) (mean - 3*sd) - distFromCentre;    // Get more above the horizon
            int maxRange = (int) (mean + sd) + distFromCentre;      // as there's more likely to be more noise below

            if (sdDetail) {
                Log.d("sd", "ysOfEdges: " + ysOfEdges.toString());
                Log.d("sd", "Standard Deviation is " + sd + ". Mean is " + mean);
                Log.d("sd", "Range should be from " + minRange  + " to " + maxRange);
                // Draw mean height of edges
                ImageManipulation.colourArea(bmp, bmp.getWidth()/2, (int)mean, Color.YELLOW, bmp.getWidth(), 10);
                // Draw SD of edges
                ImageManipulation.colourArea(bmp, bmp.getWidth()/2, minRange, Color.RED, bmp.getWidth(), 30);
                ImageManipulation.colourArea(bmp, bmp.getWidth()/2, maxRange, Color.RED, bmp.getWidth(), 30);
            }


        } else {
            Log.d("Hi", "Couldn't find a bitmap! :'(");
        }
    }
}
