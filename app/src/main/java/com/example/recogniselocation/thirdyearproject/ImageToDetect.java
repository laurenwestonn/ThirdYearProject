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

    public static boolean showCoarse = false;    // Show results of the coarse or the fine?
    public static boolean useThinning = true;  // Thin to have only one point per column?
    public static boolean showEdgeOnly = true;  // Colour in just the edge, or all searched area?
    public static boolean sdDetail = false;      // Want to draw SD and log info about standard deviation under "sd"?

    public static List<List<Integer>> edgeCoords;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_to_detect);
    }

    public void clickedImage(View view) {
        ImageButton imageButton = (ImageButton) view;
        Bitmap bmp;

        // Get the image off the button as a bitmap
        if ((bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap()) != null)
            bmp = detectEdge(bmp);

        ((ImageButton) view).setImageBitmap(bmp);
    }

    public static Bitmap detectEdge(Bitmap bmp) {
        Log.d("Hi", "Going to detect the edge");

        // Copy it (mutable) into new bitmap, which will be used for the coarse mask
        Bitmap coarseBMP = bmp.copy(bmp.getConfig(), true);

        // Check number of times we loop around for efficiency
        int testCount = 0;

        List<Integer> ysOfEdges = new ArrayList<>();

        // The number of pixels to the left/right/above/below of the centre pixel
        int distFromCentre = bmp.getHeight() / 17;
        // The number of pixels for the width/height
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
                    // The threshold to determine if a point is an edge
                    int pointThreshold = coarseBMP.getHeight() / 23;
                    // The looser threshold for a point that is neighbouring an edge
                    int neighbThreshold = (int) (pointThreshold * 0.8);

                    // Check if this point is determined an edge with the coarse mask
                    boolean relevantEdge = ImageManipulation.colourCoarseMaskPoint(coarseBMP, i, j, distFromCentre, pointThreshold, neighbThreshold);
                    // If it is, remember it so we can narrow the area we use our fine mask in
                    if (relevantEdge)
                        ysOfEdges.add(j);
                }
            }

        Log.d("Hi", "The image was " + coarseBMP.getWidth() + " x " + coarseBMP.getHeight());
        Log.d("Hi", "Looped " + testCount + " times.");

        ///////////// Standard Deviation //////////////
        // Here we work out the standard deviation of the edges found using the coarse mask
        // We need this so we can narrow down the area to search using the fine mask
        StandardDeviation coarseSD = new StandardDeviation(ysOfEdges, distFromCentre);

        // Enable sdDetail if you want to print info and draw mean/sd lines on the image
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

        // Get a copy of the original photo to use the fine mask on
        Bitmap fineBMP = bmp.copy(bmp.getConfig(), true);

        int fineWidthFromCentre = fineBMP.getWidth() / 250; // 1 would make a mask of width 3, 2 would give width 5
        int fineWidth = fineWidthFromCentre * 2 + 1; // Total width of the fine mask
        int fineHeightFromCentre = fineBMP.getHeight() / 200;
        int fineHeight = fineHeightFromCentre * 2 + 1; // Total height of the fine mask

        boolean relevantEdge;
        edgeCoords = new ArrayList<>();

        // Use a fine mask on the area found to be the horizon by the coarse mask
        for(int y = coarseSD.minRange + fineHeightFromCentre; y <= coarseSD.maxRange - fineHeightFromCentre; y+= fineHeight)
            for (int x = fineWidthFromCentre; x < fineBMP.getWidth() - fineWidthFromCentre; x+= fineWidth) {

                if (y == coarseSD.minRange + fineHeightFromCentre) // Want to add a new list for every column
                    edgeCoords.add(new ArrayList<Integer>());

                /////// NEIGHBOURING THRESHOLD ///////

                // Thresholds
                int pointThreshold = bmp.getHeight() / 25; // The threshold to determine an edge for a point
                int neighbThreshold = (int) (pointThreshold * 0.9); // A point that is neighbouring an edge's threshold

                // Is this a edge?
                relevantEdge = ImageManipulation.colourFineMaskPoint(fineBMP, x, y, fineWidth, fineHeight, pointThreshold, neighbThreshold);
                if (relevantEdge) {
                    // This should hold the location of every edge found with the fine mask
                    edgeCoords.get((x-fineWidthFromCentre)/fineWidth).add(y);
                }
            }

        //// THINNING ////
        if (useThinning) {
            //Log.d("Hi", "Going to thin out edgeCoords: " + edgeCoords.toString());
            // Unsure if finebmp and edgecoords get updated here
            edgeCoords = ImageManipulation.thinBitmap(fineBMP, edgeCoords, fineWidth, fineHeight, fineWidthFromCentre);
            //Log.d("Hi", "Have thinned out edgeCoords:  " + edgeCoords.toString());
        }

        ///// SHOW EDGES ONLY? /////
        Bitmap edgeBMP = null;
        if (showEdgeOnly) {
            // Get a new copy of the photo to draw the edge on top of
            edgeBMP = bmp.copy(bmp.getConfig(), true);
            // Draw the edge on top of the photo from the edge coordinates we saved in edgeCoords
            ImageManipulation.colourFineBitmap(edgeBMP, edgeCoords,
                    fineWidth, fineHeight, fineWidthFromCentre);
        }



        if (showCoarse)
            return coarseBMP;
        else if (edgeBMP != null && showEdgeOnly)
            return edgeBMP;
        else
            return fineBMP;

    }

}
