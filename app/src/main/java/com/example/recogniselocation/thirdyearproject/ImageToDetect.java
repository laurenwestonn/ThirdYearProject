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

    boolean useCoarse = false;
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

            // Save orig bitmap and make it mutable so we can mess with it
            Bitmap coarseBMP = bmp.copy(bmp.getConfig(), true);;

            //Check no of times we loop around
            int testCount = 0;

            List ysOfEdges = new ArrayList();

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            int widthToColourAtOnce = distFromCentre * 2 + 1;

            ////////////// useCoarse Mask /////////////////
            for (int j = distFromCentre+1;
                 j < coarseBMP.getHeight()-distFromCentre;
                 j += widthToColourAtOnce)
                for (int i = distFromCentre+1;
                     i < coarseBMP.getWidth()-distFromCentre;
                     i += widthToColourAtOnce) {

                    testCount++;
                    int colour = 0;

                    // 1: Threshold
                    // 2: Masking
                    int method = 2;

                    if (method == 1) {
                        int threshold = 100;
                        int brightness = Color.blue(coarseBMP.getPixel(i,j));
                        colour = (brightness > threshold) ? Color.WHITE : Color.BLACK;

                        // Colour in the point around this pixel
                        ImageManipulation.colourArea(coarseBMP, i, j, colour, widthToColourAtOnce, widthToColourAtOnce);
                    } else if (method == 2) {
                        boolean relevantEdge = ImageManipulation.colourMaskPoint(coarseBMP, i, j, distFromCentre);
                        if (relevantEdge)
                            ysOfEdges.add(j);
                    }
                }

            Log.d("Hi", "The image was " + coarseBMP.getWidth() + " x " + coarseBMP.getHeight());
            Log.d("Hi", "Looped " + testCount + " times.");

            ///////////// Standard Deviation //////////////
            double mean = StandardDeviation.calcMean(ysOfEdges);
            double sd = StandardDeviation.calcSD(ysOfEdges, mean);
            int minRange = (int) (mean - 3*sd) - distFromCentre;    // Get more above the horizon
            int maxRange = (int) (mean + sd) + distFromCentre;      // as there's more likely to be more noise below

            if (sdDetail) {
                Log.d("sd", "ysOfEdges: " + ysOfEdges.toString());
                Log.d("sd", "Standard Deviation is " + sd + ". Mean is " + mean);
                Log.d("sd", "Range should be from " + minRange  + " to " + maxRange);
                // Draw mean height of edges
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, (int)mean, Color.YELLOW, coarseBMP.getWidth(), 10);
                // Draw SD of edges
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, minRange+15, Color.RED, coarseBMP.getWidth(), 30);
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, maxRange-15, Color.RED, coarseBMP.getWidth(), 30);
            }


            ///////////////////// FINE MASK //////////////////

            //  1   1   1
            //  -1  -1  -1

            // Get the original photo again, as we've coloured in the other bitmap we were using
            Bitmap fineBMP = bmp.copy(bmp.getConfig(), true);

            int fineWidthFromCentre = 2; // 1 would make a mask of width 3, 2 would give width 5
            int fineWidth = fineWidthFromCentre * 2 + 1; // Total width of the fine mask
            int fineHeightFromCentre = 1;
            int fineHeight = fineHeightFromCentre * 2 + 1; // Total height of the fine mask
            int colour;

            // Use a fine mask on the area found to be the horizon by the useCoarse mask
            for(int y = minRange+1; y <= maxRange - fineHeightFromCentre; y+= fineHeight)
                for (int x = fineWidthFromCentre; x <= fineBMP.getWidth() - fineWidthFromCentre; x+= fineWidth) {
                    colour = (Color.blue(fineBMP.getPixel(x-2, y-1))
                            + Color.blue(fineBMP.getPixel(x,   y-1))
                            + Color.blue(fineBMP.getPixel(x+2, y-1))
                            - Color.blue(fineBMP.getPixel(x-2, y+1))
                            - Color.blue(fineBMP.getPixel(x,   y+1))
                            - Color.blue(fineBMP.getPixel(x+2, y+1))) / 3;

                    // Determine which colour we should have this group of pixels appearing as
                    if (colour < 0)     // We don't care about edges going from dark to light
                        colour = 0;
                    else if (colour > 30) // Classify this as an edge
                        colour = Color.WHITE;
                    else                // Classify this as not an edge
                        colour = Color.BLACK;

                    ImageManipulation.colourArea(fineBMP, x, y, colour, fineWidth, fineHeight);
                }


            if (useCoarse)
                imageButton.setImageBitmap(coarseBMP);
            else
                imageButton.setImageBitmap(fineBMP);
            Log.d("Hi", "Put the bitmap on the buttonImage");

        } else {
            Log.d("Hi", "Couldn't find a bitmap! :'(");
        }
    }
}
