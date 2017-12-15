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

import static android.content.ContentValues.TAG;

public class ImageToDetect extends Activity {

    public static boolean showCoarse = false;   // Show results of the coarse or the fine?
    public static boolean sdDetail = false;     // Want to draw SD and log info under tag "sd"?
    public static boolean useThinning = true;   // Thin to have only one point per column?
    public static boolean showEdgeOnly = true;  // Colour in just the edge, or all searched area?

    public static List<List<Integer>> edgeCoords;
    public static int fineWidth;
    public static int fineHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_to_detect);
    }

    public void clickedImage(View view) {
        ImageButton imageButton = (ImageButton) view;
        Bitmap bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap();
        EdgeDetection edgeDetection = null;

        // Get the image off the button as a bitmap
        if (bmp != null)
            edgeDetection = detectEdge(bmp);

        if (edgeDetection != null)
            ((ImageButton) view).setImageBitmap(edgeDetection.bmp);
    }

    public static Bitmap getThreshold(Bitmap bmp) {

        // The number of pixels to the left/right/above/below of the centre pixel
        int pointRadius = bmp.getHeight() / 17;
        // The number of pixels for the width/height, the diameter
        int pointDiamm = pointRadius * 2 + 1;

        // Make a mutable copy of the bitmap to threshold
        Bitmap thresholdBMP = bmp.copy(bmp.getConfig(), true);

        for (int y = pointRadius + 1; y < thresholdBMP.getHeight(); y += pointDiamm)
            for (int x = pointRadius + 1; x < thresholdBMP.getWidth(); x += pointDiamm) {
                int threshold = 100;
                int brightness = Color.blue(thresholdBMP.getPixel(x, y));

                // Colour point in white if edge, black if not
                int colour = (brightness > threshold) ? Color.WHITE : Color.BLACK;

                ImageManipulation.colourArea(thresholdBMP, x, y, colour, pointDiamm, pointDiamm);
            }

        return thresholdBMP;
    }

    public static EdgeDetection detectEdge(Bitmap bmp) {
        // The number of pixels to the left/right/above/below of the centre pixel
        int coarseRadius = bmp.getHeight() / 17;
        // The number of pixels for the width/height, the diameter
        int coarseDiam = coarseRadius * 2 + 1;

        ////////////// Coarse Mask /////////////////
        List<Integer> ysOfEdges = new ArrayList<>();
        // Make a mutable copy of the bitmap to be used for the coarse mask
        Bitmap coarseBMP = bmp.copy(bmp.getConfig(), true);

        for (int y = coarseRadius+1;
             y < coarseBMP.getHeight();
             y += coarseDiam)
            for (int x = coarseRadius+1;
                 x < coarseBMP.getWidth();
                 x += coarseDiam) {

                // The threshold to determine if a point is an edge
                int pointThreshold = coarseBMP.getHeight() / 23;
                // The looser threshold for a point that is neighbouring an edge
                int neighbThreshold = (int) (pointThreshold * 0.8);

                // Check if this point is determined an edge with the coarse mask
                boolean relevantEdge = ImageManipulation.colourCoarseMaskPoint(coarseBMP, x, y, coarseRadius, pointThreshold, neighbThreshold);
                // If it is, remember it so we can narrow the area we use our fine mask in
                if (relevantEdge)
                    ysOfEdges.add(y);
            }
        Log.d(TAG, "detectEdge: Coarse Masking done");

        ///////////// Standard Deviation //////////////
        // Here we work out the standard deviation of the edges found using the coarse mask
        // We need this so we can narrow down the area to search using the fine mask
        StandardDeviation coarseSD = new StandardDeviation(ysOfEdges, coarseRadius);

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
        Log.d(TAG, "detectEdge: SD got");


        ///////////////////// FINE MASK //////////////////

        //  1   0   1   0   1
        //  0   0   0   0   0
        //  -1  0   -1  0   -1

        // Get a copy of the original photo to use the fine mask on
        Bitmap fineBMP = bmp.copy(bmp.getConfig(), true);

        int fineWidthRadius = fineBMP.getWidth() / 250; // 1 would make a mask of width 3, 2 would give width 5
        fineWidth = fineWidthRadius * 2 + 1;    // Width of the fine mask
        int fineHeightRadius = fineBMP.getHeight() / 120;
        fineHeight = fineHeightRadius * 2 + 1;  // Height of the fine mask
        int pointWidth = fineWidthRadius;       // The width of point to colour in
        int pointWidthRadius = pointWidth / 2;

        boolean relevantEdge;
        edgeCoords = new ArrayList<>();

        // Use a fine mask on the area found to be the horizon by the coarse mask
        for(int y = coarseSD.minRange + fineHeightRadius; y <= coarseSD.maxRange; y+= fineHeightRadius)
            for (int x = fineWidthRadius; x < fineBMP.getWidth(); x+= fineWidthRadius) {

                // Have a list for each column
                if (y == coarseSD.minRange + fineHeightRadius)
                    edgeCoords.add(new ArrayList<Integer>());

                /////// NEIGHBOURING THRESHOLD ///////

                // Thresholds
                int pointThreshold = bmp.getHeight() / 35; // The threshold to determine an edge for a point
                int neighbThreshold = (int) (pointThreshold * 0.9); // A point that is neighbouring an edge's threshold

                // Is this a edge?
                relevantEdge = ImageManipulation.colourFineMaskPoint(bmp, fineBMP, x, y, fineWidth, fineHeight, pointThreshold, neighbThreshold);
                if (relevantEdge)
                    // This should hold the location of every edge found with the fine mask
                    edgeCoords.get((x - pointWidthRadius) / pointWidth).add(y);
            }
        Log.d(TAG, "detectEdge: Fine Masking done");

        //// THINNING ////
        if (useThinning) {
            //Log.d("Hi", "Going to thin out edgeCoords: " + edgeCoords.toString());
            // Unsure if finebmp and edgecoords get updated here
            edgeCoords = ImageManipulation.thinBitmap(fineBMP, edgeCoords,
                                            fineWidth, fineHeight, fineWidthRadius);
            //Log.d("Hi", "Have thinned out edgeCoords:  " + edgeCoords.toString());
        }
        Log.d(TAG, "detectEdge: Thinning done");

        ///// SHOW EDGES ONLY? /////
        Bitmap edgeBMP = null;
        if (showEdgeOnly) {
            // Get a new copy of the photo to draw the edge on top of
            edgeBMP = bmp.copy(bmp.getConfig(), true);
            // Draw the edge on top of the photo from the edge coordinates we saved in edgeCoords
            ImageManipulation.colourFineBitmap(edgeBMP, edgeCoords,
                                            fineWidthRadius, fineHeightRadius, fineWidthRadius/2);
        }

        if (showCoarse)
            return new EdgeDetection(edgeCoords, coarseBMP);
        else if (edgeBMP != null && showEdgeOnly)
            return new EdgeDetection(edgeCoords, edgeBMP);
        else
            return new EdgeDetection(edgeCoords, fineBMP);
    }

}