package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.ContentValues.TAG;
import static java.lang.StrictMath.max;

class FunctionsImageManipulation {

    private static List<Point> fineEdgeCoords;

    static Edge detectEdge(Bitmap bmp, boolean useThinning)
    {
        if (bmp == null) {
            Log.e(TAG, "Null bitmap was passed to detectEdge");
            return null;
        }

        // Save these variables globally for now Todo: This better
        Bitmap resultFineBMP;
        StandardDeviation coarseSD = null;

        ////////////// COARSE MASK /////////////////
        List<Point> coarseEdgeCoords = coarseMask(bmp);

        ///////////// Standard Deviation //////////////
        if (coarseEdgeCoords != null && coarseEdgeCoords.size() > 0) {

            coarseSD = new StandardDeviation(coarseEdgeCoords, bmp.getHeight() / 17);
            Log.d(TAG, "detectEdge: SD got");

            // Whether SD was drawn on or not, the coarse mask will get returned from the above

            ///////////////////// FINE MASK //////////////////
            Edge fine = fineMask(bmp, coarseSD);
            if (fine == null)
                return null;

            resultFineBMP = fine.getBitmap();
            fineEdgeCoords = fine.getCoords();

            ////////// THINNING //////////
            if (useThinning) {
                //Log.d(TAG, "detectEdge: Going to skeletonise fineEdgeCoords: " + fineEdgeCoords.toString());
                //fineEdgeCoords = skeletonisePoints(fineEdgeCoords, fineWidth);
                Log.d(TAG, "detectEdge: fineEdgeCoords: " + fineEdgeCoords.toString());
                fineEdgeCoords = thinColumns(fineEdgeCoords);
                Log.d(TAG, "Result of 1 per column: " + fineEdgeCoords.toString());
            }

        } else {
            Log.e(TAG, "detectEdge: Couldn't find edges with the coarse mask, so just return the original photo");
            resultFineBMP = bmp;
        }
        return new Edge(fineEdgeCoords, coarseEdgeCoords, coarseSD, resultFineBMP);
    }

    static Point bestPointInCol(List<Point> col, Point prevP)
    {
        if (col == null || col.size() <= 0)
            return null;

        int midIndex = (int) Math.ceil(col.size() / 2.0) - 1;   // Favouring the higher ones

        // We have nothing to judge this point with, assume the middle one is okay
        if (prevP == null)
            return col.get(midIndex);

        // Find a relevant middle point
        while (!similarPoints(col.get(midIndex), prevP)) {
            col.remove(midIndex);   // This point must be noise, remove it
            midIndex = (int) Math.ceil(col.size() / 2.0) - 1;   // Find new middle of the column

            if (col.size() == 0)
                return null;
        }
        return col.get(midIndex);
    }

    // Lets you know if two point are nearby
    // Used to avoid picking noise as points - assuming the first point is correct
    private static boolean similarPoints(Point p1, Point p2)
    {
        if (p1.getY() == p2.getY())
            return true;

        double ratio = 0.5;
        double difference = Math.abs((p2.getX() - p1.getX()) / (p2.getY() - p1.getY()));
        return difference >= ratio;
    }

    // Returns every point that is also a part of column 'colIndex'
    private static List<Point> findPointsInCol(List<Point> coords, int colIndex) {
        List<Point> pointsInCol = new ArrayList<>();

        for (Point p : coords)
            if (p.getX() == colIndex)
                pointsInCol.add(p);

        return pointsInCol;
    }

    private static Edge fineMask(Bitmap bmp, StandardDeviation coarseSD)
    {
        //  1   0   2   0   1
        //  0   0   0   0   0
        //  -1  0  -2   0   -1

        if (bmp == null) {
            Log.e(TAG, "fineMask got a null bitmap");
            return null;
        }
        // Set the size of the mask. Have it be at least 5x5.
        int widthRadius = max(2, bmp.getWidth() / 250);
        int heightRadius = max(2, bmp.getHeight() / 110);
        int width = widthRadius * 2 + 1;
        int height = heightRadius * 2 + 1;

        // Thresholds
        int pointThr = bmp.getHeight() / 30; // The threshold to determine an edge for a point
        int neighbThr = (int) (pointThr * 0.9); // A point that is neighbouring an edge's threshold

        boolean relevantEdge;
        List<Point> edgeCoords = new ArrayList<>();
        int loop = 0;

        Log.d(TAG, "fineMask: Fine Masking starting");
        // Use a fine mask on the area found to be the horizon by the coarse mask
        for (int x = widthRadius;
             x < bmp.getWidth() - widthRadius; x+= widthRadius) {
            for (int y = coarseSD.getMinRange() + heightRadius;
                 y < coarseSD.getMaxRange() - heightRadius; y += heightRadius) {

                // Is this an edge? Check neighbouring points too
                relevantEdge = determineEdge(bmp, x, y, width, height, pointThr, neighbThr, false);
                if (relevantEdge)
                    // This should hold the location of every edge found with the fine mask
                    edgeCoords.add(new Point(x, y));
                loop++;
            }
        }
        Log.d(TAG, "fineMask: Fine Masking done. Looped " + loop + " times");

        return new Edge(edgeCoords, null, coarseSD, bmp);
    }

    // Run a large mask over the image to find roughly where the horizon is
    private static List<Point> coarseMask(Bitmap bmp)
    {
        if (bmp == null) {
            Log.e(TAG, "coarseMask got a null bitmap");
            return null;
        }
        // Decide upon the size of the mask, based on the size of the image
        // The number of pixels to the left/right/above/below of the centre pixel
        int coarseRadius = bmp.getHeight() / 17;

        // The coordinates detected as edges
        List<Point> edgeCoords = new ArrayList<>();

        // Search up to down, then left to right
        for (int x = coarseRadius+1; x < bmp.getWidth() - coarseRadius; x += coarseRadius) {
            for (int y = coarseRadius + 1; y < bmp.getHeight() - coarseRadius; y += coarseRadius) {
                // The threshold to determine if a point is an edge
                int pointThreshold = bmp.getHeight() / 23;
                // The looser threshold for a point that is neighbouring an edge
                int neighbThreshold = (int) (pointThreshold * 0.8);

                // Check if this point is determined an edge with the coarse mask
                boolean relevantEdge = determineEdge(bmp, x, y, coarseRadius*2+1,
                        coarseRadius*2+1, pointThreshold, neighbThreshold, true);
                // If it is, remember it so we can narrow the area we use our fine mask in
                if (relevantEdge)
                    edgeCoords.add(new Point(x, y));
            }
        }
        Log.d(TAG, "CoarseMasking: Coarse Masking done: " + edgeCoords);
        return edgeCoords;
    }



    /////////////////////// COARSE ///////////////////////
    private static boolean determineEdge(Bitmap bmp, int x, int y, int width, int height,
                                         int loThresh, int hiThresh, boolean coarse)
    {
        int edgeness = coarse ?
                getCoarseEdgeness(bmp, x, y, (width-1)/2) : getFineEdgeness(bmp, x, y, width, height);

        // Get the likelihood that (x,y) is an edge
        return determineIfEdge(bmp, edgeness, loThresh, hiThresh, x, y, width, height);
    }

    //  1               1
    //          3
    //
    //         -3
    //  -1             -1
    // Radius r around (x,y)
    private static int getCoarseEdgeness(Bitmap bmp, int x, int y, int r)
    {
        if (x - r < 0 || y - r < 0 || x + r > bmp.getWidth() || y + r > bmp.getHeight())
            Log.e(TAG, "getCoarseEdgeness: Can't access " + r + "x" + r + " around point ("
                    + x + ", " + y + ") when the bmp is " + bmp.getWidth() + "x" + bmp.getHeight());
        int top, bottom;
        try {
            top =     Color.blue(bmp.getPixel(x - r, y - r))
                    + Color.blue(bmp.getPixel(x + r, y - r))

                    + Color.blue(bmp.getPixel(x, y - r / 2)) * 3;

            bottom = - Color.blue(bmp.getPixel(x, y + r / 2)) * 3

                    - Color.blue(bmp.getPixel(x - r, y + r))
                    - Color.blue(bmp.getPixel(x + r, y + r));

            int edgeness = (top + bottom) / 5; // Max could be 5 * 255

            return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these

        } catch (ArrayIndexOutOfBoundsException boundsException) {
            Log.e("Hi", "You can't access (" + x + ", " + y + ") in a bitmap "
                    + bmp.getWidth() + " x " + bmp.getHeight()
                    + "\n" + boundsException.toString());
        } catch (Exception e){
            Log.e("getCoarseEdgeness", e.toString());
        }

        return -1;
    }

    /////////////////////// FINE /////

    // Looking at spread out pixels inside this point, see how likely it is that this is an edge
    private static int getFineEdgeness(Bitmap bmp, int i, int j, int widthRadius, int heightRadius)
    {
        if (widthRadius == 0 | heightRadius == 0) {
            Log.e(TAG, "getFineEdgeness: Can't colour in " + i + ", " + j + " with radius of 0");
            return 0;
        }

        int edgeness = 0;

        for (int y = j - heightRadius; y <= j + heightRadius; y += heightRadius + heightRadius) {
            for (int x = i - widthRadius; x <= i + widthRadius; x += widthRadius) {
                if (x >= 0 && y >= 0 && x < bmp.getWidth() && y < bmp.getHeight()) {
                    if (x == i)// If this is a centre point, weigh it twice as heavily
                        edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);
                    edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);
                }
            }
        }
        edgeness /= 4; // Max could be 4 * 255 due to the 6 neighbours and weighting
        return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these
    }

    private static boolean determineIfEdge(Bitmap bmp, int edgeness, int nThr, int pThr,
                                           int x, int y, int width, int height)
    {
        // Is marked as a definite edge, and has neighbours at least with weak edges
        return edgeness >= pThr && checkIfNboursEdges(bmp, x, y, width, height, nThr);
    }

    // Colour a wxh block of pixels around (x,y) in the requested colour
    static Bitmap colourArea(Bitmap bmp, int x, int y, int colour, int width, int height) {
        // setPixels needs an int array of colours
        int[] colours = new int[width * height];
        Arrays.fill(colours, colour);

        // The top left coordinate of the area to colour
        int i = x - (width-1) / 2;
        int j = y - (height-1) / 2;

        // Don't try colour in areas outside of the image
        if (i < 0) {
            width += i;
            if (height < 0) {
                Log.e(TAG, "colourArea: This pixel (" + x + ", " + y + ") and surrounding area is completely before the image (i is negative) So can't be coloured");
                return bmp;
            }
            i = 0;
        }
        else if (i + width >= bmp.getWidth())
            width = bmp.getWidth() - i - 1;

        if (j < 0) {
            height += j;    // Just colour in the difference from the edge
            if (height < 0) {
                Log.e(TAG, "colourArea: This pixel (" + x + ", " + y + ") and surrounding area is completely above the image (j is negative) So can't be coloured");
                return bmp;
            }
            j = 0;
        } else if (j + height >= bmp.getHeight())
            height = bmp.getHeight() - j - 1;

        // Ensure the Bitmap is mutable so that it can be coloured
        if (!bmp.isMutable()) {
            bmp = bmp.copy(bmp.getConfig(), true);
        }

        if (i < 0 || j < 0 || i + width >= bmp.getWidth() || j + height >= bmp.getHeight()
                || width < 0 || height < 0) {
            Log.e(TAG, "colourArea: Can't colour in " + width + " x " + height
                    + " around the area " + x + ", " + y
                    + " when the bmp is " + bmp.getWidth() + " x " + bmp.getHeight());
            return bmp;
        }

        //Log.d("Hi", "Trying to colour from " + i + ", " + j + ". Width x height: "
        //        + width + "i" + height + " BMP: " + bmp.getWidth() + ", " + bmp.getHeight());
        bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                width,      // stride, width of what you want to colour in
                i,          // i co-ord of first pixel to colour
                j,          // j co-ord of first pixel to colour
                width,      // width of area to colour
                height);    // height of area to colour

        return bmp;
    }


    /////// NEIGHBOURS ///////
    // Returns true if any of the neighbours classify as a weak edge
    private static boolean checkIfNboursEdges(Bitmap bmp, int x, int y, int width, int height,
                                               int minThreshold)
    {
        for (int i = x - width; i <= x + width; i += width) {
            for (int j = y - height; j <= y + height; j += height) {
                if (checkIfNbourEdge(bmp, i, j, width, height, minThreshold))
                    return true;
            }
        }
        return false;
    }

    // Returns true if this neighbour classifies as a weak edge
    private static boolean checkIfNbourEdge(Bitmap bmp, int x, int y, int width, int height, int minThreshold)
    {
        // Find how strong of an edge this is
        if (width == bmp.getHeight() / 34 + 1) // Coarse masking
            return getCoarseEdgeness(bmp, x, y, (width-1)/2) > minThreshold;
        else // Fine masking
            return getFineEdgeness(bmp, x, y, (width - 1) / 2, (height - 1) / 2)
                    > minThreshold;

    }

    /////// THINNING ///////
    private static List<Point> skeletonisePoints(List<Point> edgeCoords, int pointDiametre)
    {
        List<Point> thinnedCoords = new ArrayList<>();
        boolean gShowEdgeOnly = true;

        // Go through each of the edge coords
        for (int i = 0; i < edgeCoords.size(); i++) {
            boolean removeThisPoint = skeletonisePoint(edgeCoords, edgeCoords.get(i), pointDiametre);

            if(gShowEdgeOnly)
                if (!removeThisPoint)
                    thinnedCoords.add(edgeCoords.get(i));
        }
        if (gShowEdgeOnly)
            return thinnedCoords;
        else
            return edgeCoords;
    }

    // Used for the skeletonisation
    static boolean skeletonisePoint(List<Point> coords, Point point, int pointDiameter)
    {
        boolean removeThisPoint = false;
        double x,y;
        x = point.getX();
        y = point.getY();

        // If point above this one is an edge
        if (coords.indexOf(new Point(x, y - pointDiameter)) != -1
                // and if point below is an edge
                && coords.indexOf(new Point(x, y + pointDiameter)) != -1
                // and point on EITHER the right or left is an edge
                && (coords.indexOf(new Point(x + pointDiameter, y)) != -1 && coords.indexOf(new Point(x - pointDiameter, y)) == -1
                    || coords.indexOf(new Point(x - pointDiameter, y)) != -1 && coords.indexOf(new Point(x + pointDiameter, y)) == -1) ) {
            // Don't include this point
            removeThisPoint = true;
            Log.d(TAG, "skeletonisePoint: As all three neighbouring points exist, we can thin point " + point);
        }

        return removeThisPoint;
    }

    private static List<Point> thinColumns(List<Point> coords)
    {
        List<Point> onePerColumn = new ArrayList<>();
        Point prevBestPoint = null;
        for (int i = 0; i < coords.size(); i++) {
            // Find all points within the same column
            int colIndex = (int) coords.get(i).getX();
            List<Point> pointsInCol = findPointsInCol(coords, colIndex);
            i += pointsInCol.size() - 1;

            // Pick best point in column (middle, more towards top due to noise below horizon)
            Point bestPoint = bestPointInCol(pointsInCol, prevBestPoint);
            if (bestPoint != null && !onePerColumn.contains(bestPoint)) { // Add it if you haven't already
                onePerColumn.add(bestPoint);
                prevBestPoint = bestPoint;  // Update the last best point if we found a new one now
            }
        }
        return onePerColumn;
    }
}
