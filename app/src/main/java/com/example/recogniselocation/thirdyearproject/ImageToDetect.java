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
import java.util.Collections;
import java.util.List;

/**
 * Created by LaUrE on 20/10/2017.
 */

public class ImageToDetect extends Activity {

    public static boolean useCoarse = false;
    boolean sdDetail = true;    // Want to draw SD and log info about standard deviation under "sd"?
    int distFromCentre = 25;    //TODO: CHANGE THIS TO CHANGE THE SPEED/CLARITY
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
            Bitmap coarseBMP = bmp.copy(bmp.getConfig(), true);;

            //Check no of times we loop around
            int testCount = 0;

            List ysOfEdges = new ArrayList();

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
                        boolean relevantEdge = ImageManipulation.colourCoarseMaskPoint(coarseBMP, i, j, distFromCentre);
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

            //  1   1   1
            //  -1  -1  -1

            // Get the original photo again, as we've coloured in the other bitmap we were using
            Bitmap fineBMP = bmp.copy(bmp.getConfig(), true);

            int fineWidthFromCentre = 2; // 1 would make a mask of width 3, 2 would give width 5
            int fineWidth = fineWidthFromCentre * 2 + 1; // Total width of the fine mask
            int fineHeightFromCentre = 1;
            int fineHeight = fineHeightFromCentre * 2 + 1; // Total height of the fine mask

            boolean relevantEdge;
            edgeCoords = new ArrayList<List<Integer>>();

            // Use a fine mask on the area found to be the horizon by the useCoarse mask
            for(int y = coarseSD.minRange + 1; y <= coarseSD.maxRange - fineHeightFromCentre; y+= fineHeight)
                for (int x = fineWidthFromCentre; x < fineBMP.getWidth() - fineWidthFromCentre; x+= fineWidth) {

                    // Want to add a new list to store each column, only need to add for the first row
                    if (y == coarseSD.minRange + 1)
                        edgeCoords.add(new ArrayList<Integer>());
                    // Is this a edge?
                    relevantEdge = ImageManipulation.colourFineMaskPoint(fineBMP, x, y, fineWidth, fineHeight);
                    if (relevantEdge) {
                        //ysOfEdges.add(y);

                        // This should hold the location of every edge found with the fine mask
                        edgeCoords.get(x/fineWidth).add(y);
                    }
                }

            int colIndex = fineWidthFromCentre;
            for (List col : edgeCoords) {

                // Skip any columns that don't have edges
                int noOfEdgesInCol = col.size();
                if (noOfEdgesInCol > 0) {
                    // The middle edge in the column is most likely to be accurate, keep it
                    int mostAccurateEdgeInCol = noOfEdgesInCol / 2;

                    Collections.sort(col);
                    Log.d("Hi", "In column " + colIndex + " there are edges at " + col + ". Keep edge (" + colIndex + ", " + col.get(mostAccurateEdgeInCol) + ")");

                    // Change to yellow to show the result of thinning more clearly
                    ImageManipulation.colourArea(fineBMP, colIndex, (int) col.get(mostAccurateEdgeInCol), Color.WHITE, fineWidth, fineHeight);

                    // Only hold the edges we don't want in col, we want to show the most accurate
                    col.remove(mostAccurateEdgeInCol);

                    // Clear the unnecessary edges
                    for (Object y : col) {
                        Log.d("Hi", "Thin out " + y + " from column " + colIndex);
                        // Change to red to see which edges were removed from thinning
                        ImageManipulation.colourArea(fineBMP, colIndex, (int) y, Color.BLACK, fineWidth, fineHeight);
                    }
                } else {
                    Log.d("Hi", "No edges in column " + colIndex);
                }

                // Keep track of the column number
                colIndex+= fineWidth;
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
