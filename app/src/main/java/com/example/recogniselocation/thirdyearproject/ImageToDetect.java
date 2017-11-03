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

    public static boolean useCoarse = true;
    public static boolean useThinning = false;

    boolean sdDetail = true;    // Want to draw SD and log info about standard deviation under "sd"?
    int distFromCentre;    //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY
    public static List<List<Integer>> edgeCoords;

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
            Bitmap coarseBMP = bmp.copy(bmp.getConfig(), true);

            //Check no of times we loop around
            int testCount = 0;

            List<Integer> ysOfEdges = new ArrayList<>();

            distFromCentre = bmp.getHeight() / 17;

            // No of pixels around the centre to do at once
            // i.e 5 will be a 11 * 11 sized block. 5 + center + 5
            int widthToColourAtOnce = distFromCentre * 2 + 1;

            ////////////// Coarse Mask /////////////////
            for (int j = distFromCentre+1;
                 j < coarseBMP.getHeight()-distFromCentre;
                 j += widthToColourAtOnce)
                for (int i = distFromCentre+1;
                     i < coarseBMP.getWidth()-distFromCentre;
                     i += widthToColourAtOnce) {

                    testCount++;
                    int colour;

                    // 1: Threshold
                    // 2: Masking
                    int method = 2;

                    if (method == 1) {
                        int threshold = 100;
                        int brightness = Color.blue(coarseBMP.getPixel(i,j));
                        colour = (brightness > threshold) ? Color.WHITE : Color.BLACK;
                        // Colour in the square around this pixel
                        ImageManipulation.colourArea(coarseBMP, i, j, colour, widthToColourAtOnce, widthToColourAtOnce);
                    } else if (method == 2) {

                        // Thresholds
                        int pointThreshold = coarseBMP.getHeight() / 23; // The threshold to determine an edge for a point
                        int neighbThreshold = (int) (pointThreshold * 0.8); // A point that is neighbouring an edge's threshold
                        Log.d("Hi", "COARSE threshold of " + pointThreshold + ". Neighbouring threshold at " + neighbThreshold);

                        boolean relevantEdge = ImageManipulation.colourCoarseMaskPoint(coarseBMP, i, j, distFromCentre, pointThreshold, neighbThreshold);
                        if (relevantEdge)
                            ysOfEdges.add(j);
                    }
                }

            Log.d("Hi", "The image was " + coarseBMP.getWidth() + " x " + coarseBMP.getHeight());
            Log.d("Hi", "Looped " + testCount + " times.");

            ///////////// Standard Deviation //////////////
            StandardDeviation coarseSD = new StandardDeviation(ysOfEdges, distFromCentre);

            if (sdDetail) {
                Log.d("sd", "ysOfEdges: " + ysOfEdges.toString());
                Log.d("sd", "Standard Deviation is " + coarseSD.sd + ". Mean is " + coarseSD.mean);
                Log.d("sd", "Range should be from " + coarseSD.minRange  + " to " + coarseSD.maxRange);
                // Draw mean height of edges
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, (int)coarseSD.mean, Color.YELLOW, coarseBMP.getWidth()-1, 10);
                // Draw SD of edges
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, coarseSD.minRange+15, Color.RED, coarseBMP.getWidth()-1, 30);
                ImageManipulation.colourArea(coarseBMP, coarseBMP.getWidth()/2, coarseSD.maxRange-15, Color.RED, coarseBMP.getWidth()-1, 30);
            }


            ///////////////////// FINE MASK //////////////////

            //  1   0   1   0   1
            //  0   0   0   0   0
            //  -1  0   -1  0   -1

            // Get the original photo again, as we've coloured in the other bitmap we were using
            Bitmap fineBMP = bmp.copy(bmp.getConfig(), true);

            int fineWidthFromCentre = fineBMP.getWidth() / 250; // 1 would make a mask of width 3, 2 would give width 5
            int fineWidth = fineWidthFromCentre * 2 + 1; // Total width of the fine mask
            int fineHeightFromCentre = fineBMP.getHeight() / 200;
            int fineHeight = fineHeightFromCentre * 2 + 1; // Total height of the fine mask

            boolean relevantEdge;
            edgeCoords = new ArrayList<>();

            // Use a fine mask on the area found to be the horizon by the useCoarse mask
            for(int y = coarseSD.minRange + fineHeightFromCentre; y <= coarseSD.maxRange - fineHeightFromCentre; y+= fineHeight)
                for (int x = fineWidthFromCentre; x < fineBMP.getWidth() - fineWidthFromCentre; x+= fineWidth) {

                    if (y == coarseSD.minRange + fineHeightFromCentre) // Want to add a new list for every column
                        edgeCoords.add(new ArrayList<Integer>());

                    /////// NEIGHBOURING THRESHOLD ///////

                    // Thresholds
                    int pointThreshold = bmp.getHeight() / 25; // The threshold to determine an edge for a point
                    int neighbThreshold = (int) (pointThreshold * 0.9); // A point that is neighbouring an edge's threshold
                    Log.d("Hi", "Fine threshold of " + pointThreshold + ". Neighbouring threshold at " + neighbThreshold);

                    // Is this a edge?
                    relevantEdge = ImageManipulation.colourFineMaskPoint(fineBMP, x, y, fineWidth, fineHeight, pointThreshold, neighbThreshold);
                    if (relevantEdge) {
                        // This should hold the location of every edge found with the fine mask
                        edgeCoords.get(x/fineWidth).add(y);
                    }
                }

            //// THINNING ////
            if (useThinning) {
                int colIndex = fineWidthFromCentre;
                for (List<Integer> col : edgeCoords) {
                    colIndex = ImageManipulation.thinColumn(fineBMP, col, colIndex, fineWidth, fineHeight);
                }
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
