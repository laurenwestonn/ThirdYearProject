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

class ImageManipulation {

    private static boolean gShowEdgeOnly;

    private static List<Point> fineEdgeCoords;
    static int fineWidth;
    private static int fineWidthRadius;
    private static int fineHeightRadius;

    static Edge detectEdge(Bitmap bmp,
                           boolean sdDetail,
                           boolean useThinning, boolean showEdgeOnly)
    {
        if (bmp == null)
            Log.e(TAG, "Null bitmap was passed to detectEdge");

        // Save these variables globally for now Todo: This better
        gShowEdgeOnly = showEdgeOnly;
        Bitmap resultFineBMP;
        List<Point> coarseEdgeCoords = null;

        ////////////// COARSE MASK /////////////////
        CoarseMasking coarse = coarseMask(bmp);

        ///////////// Standard Deviation //////////////
        if (coarse.getCoords() == null || coarse.getCoords().size() > 0) {
            StandardDeviation coarseSD = findStandardDeviation(coarse.getBitmap(), coarse.getCoords(), sdDetail);
            coarseEdgeCoords = coarse.getCoords();
            Log.d(TAG, "findStandardDeviation: SD got");

            // Whether SD was drawn on or not, the coarse mask will get returned from the above

            ///////////////////// FINE MASK //////////////////
            Edge fine = fineMask(bmp, coarseSD);
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

            ///////// SHOW EDGES ONLY? /////////
            if (showEdgeOnly) {
                // Get a new copy of the photo to draw the edge on top of
                if (bmp != null)
                    resultFineBMP = bmp.copy(bmp.getConfig(), true);
                // Draw the edge on top of the photo from the edge coordinates we saved in fineEdgeCoords
                colourFineBitmap(resultFineBMP, fineEdgeCoords, fineWidthRadius, fineHeightRadius);
                }

        } else {
            Log.e(TAG, "detectEdge: Couldn't find edges with the coarse mask, so just return the original photo");
            resultFineBMP = coarse.getBitmap();
        }
        Log.d(TAG, "detectEdge: Found the photo edge coords " + fineEdgeCoords);
        return new Edge(fineEdgeCoords, coarseEdgeCoords, resultFineBMP);
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

    @NonNull
    private static Edge fineMask(Bitmap origBMP, StandardDeviation coarseSD)
    {
        //  1   0   2   0   1
        //  0   0   0   0   0
        //  -1  0  -2   0   -1

        // Get a copy of the original photo to use the fine mask on
        Bitmap resultBMP = origBMP.copy(origBMP.getConfig(), true);

        // Set the size of the mask. Have it be at least 5x5.
        fineWidthRadius = max(2,resultBMP.getWidth() / 250); // 1 would make a mask of width 3, 2 would give width 5
        fineWidth = fineWidthRadius * 2 + 1;    // Width of the fine mask
        fineHeightRadius = max(2, resultBMP.getHeight() / 110);
        int fineHeight = fineHeightRadius * 2 + 1;
        boolean relevantEdge;
        List<Point> edgeCoords = new ArrayList<>();
        // Thresholds
        int pointThreshold = resultBMP.getHeight() / 30; // The threshold to determine an edge for a point
        int neighbThreshold = (int) (pointThreshold * 0.9); // A point that is neighbouring an edge's threshold
        int loop = 0;

        Log.d(TAG, "fineMask: Fine Masking starting");
        // Use a fine mask on the area found to be the horizon by the coarse mask
        for (int x = fineWidthRadius;
             x < resultBMP.getWidth() - fineWidthRadius; x+= fineWidthRadius) {
            for (int y = coarseSD.getMinRange() + fineHeightRadius;
                 y < coarseSD.getMaxRange() - fineHeightRadius; y += fineHeightRadius) {

                // Is this an edge? Check neighbouring points too
                relevantEdge = colourFineMaskPoint(origBMP, resultBMP, x, y,
                        fineWidth, fineHeight, pointThreshold, neighbThreshold);
                if (relevantEdge)
                    // This should hold the location of every edge found with the fine mask
                    edgeCoords.add(new Point(x, y));
                loop++;
            }
        }
        Log.d(TAG, "fineMask: Fine Masking done. Looped " + loop + " times");

        return new Edge(edgeCoords, null, resultBMP);
    }

    private static StandardDeviation findStandardDeviation(Bitmap bmp, List<Point> coords, boolean sdDetail) {
        // Here we work out the standard deviation of the edges found using the coarse mask
        // We need this so we can narrow down the area to search using the fine mask
        StandardDeviation sd = new StandardDeviation(coords, bmp.getHeight() / 17);

        // Enable sdDetail if you want to print info and draw mean/sd lines on the image
        if (sdDetail) {
            Log.d("sd", "Coarse edge coords: " + coords.toString());
            Log.d("sd", "Standard Deviation is " + sd.getSd() + ". Mean is " + sd.getMean());
            Log.d("sd", "Range should be from " + sd.getMinRange()  + " to " + sd.getMaxRange());

            // Draw mean height of edges
            ImageManipulation.colourArea(bmp, bmp.getWidth()/2, (int)sd.getMean(), Color.YELLOW,
                    bmp.getWidth()-1, 10);

            // Draw SD of edges
            int drawnSDRadius = 15;
            ImageManipulation.colourArea(bmp, bmp.getWidth()/2,sd.getMinRange()+drawnSDRadius,
                    Color.RED,bmp.getWidth()-1, 30);
            ImageManipulation.colourArea(bmp, bmp.getWidth()/2,sd.getMaxRange()-drawnSDRadius,
                    Color.RED,bmp.getWidth()-1, 30);
        }
        sd.setBitmap(bmp);
        return sd;
    }

    // Run a large mask over the image to find roughly where the horizon is
    @NonNull
    private static CoarseMasking coarseMask(Bitmap bmp) {

        // Decide upon the size of the mask, based on the size of the image
        // The number of pixels to the left/right/above/below of the centre pixel
        int coarseRadius = bmp.getHeight() / 17;
        // The number of pixels for the width/height, the diameter
        int coarseDiam = coarseRadius * 2 + 1;

        // The coordinates detected as edges
        List<Point> edgeCoords = new ArrayList<>();
        Bitmap resultBMP = bmp.copy(bmp.getConfig(), true);

        // Search up to down, then left to right
        for (int x = coarseRadius+1; x < bmp.getWidth() - coarseRadius; x += coarseRadius) {
            for (int y = coarseRadius + 1; y < bmp.getHeight() - coarseRadius; y += coarseRadius) {
                // The threshold to determine if a point is an edge
                int pointThreshold = bmp.getHeight() / 23;
                // The looser threshold for a point that is neighbouring an edge
                int neighbThreshold = (int) (pointThreshold * 0.8);

                // Check if this point is determined an edge with the coarse mask
                boolean relevantEdge = colourCoarseMaskPoint(bmp, resultBMP, x, y, coarseRadius, pointThreshold, neighbThreshold);
                // If it is, remember it so we can narrow the area we use our fine mask in
                if (relevantEdge)
                    edgeCoords.add(new Point(x, y));
            }
        }
        Log.d(TAG, "CoarseMasking: Coarse Masking done: " + edgeCoords);
        return new CoarseMasking(edgeCoords, resultBMP);
    }



    /////////////////////// COARSE /////
    private static boolean colourCoarseMaskPoint(Bitmap origBMP, Bitmap resultBMP, int x, int y,
                                                 int distFromCentre, int loThresh, int hiThresh)
    {
        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = resultBMP.getPixel(x,y) != Color.BLUE ?
                getCoarseEdgeness(origBMP, x, y, distFromCentre) :
                Color.BLUE;
        //Log.d("Hi", "\tAnother coarse pixel. Edgeness of (" + x + ", " + y + ") is " + edgeness);

        int diameter = distFromCentre * 2 + 1;

        // If we coloured point at (x,y) a useful colour, return this fact
        return determineColour(resultBMP, edgeness, loThresh, hiThresh, x, y, diameter, diameter);
    }

    //       1     1
    //  1       3       1
    //
    //  1       -3      1
    //      -1     -1
    private static int getCoarseEdgeness(Bitmap bmp, int x, int y, int d)
    {
        if (x - d < 0 || y - d < 0 || x + d > bmp.getWidth() || y + d > bmp.getHeight())
            Log.e(TAG, "getCoarseEdgeness: Can't access " + d + "x" + d + " around point ("
                    + x + ", " + y + ") when the bmp is " + bmp.getWidth() + "x" + bmp.getHeight());
        int top, bottom;
        try {
            top =     Color.blue(bmp.getPixel(x - (d /2), y - d))
                    + Color.blue(bmp.getPixel(x + (d /2), y - d))

                    + Color.blue(bmp.getPixel(x - d, y - d / 2))
                    + Color.blue(bmp.getPixel(x       , y - d / 2)) * 3
                    + Color.blue(bmp.getPixel(x + d, y - d / 2));

            bottom = -Color.blue(bmp.getPixel(x - d , y + d / 2))
                    - Color.blue(bmp.getPixel(x        , y + d / 2)) * 3
                    - Color.blue(bmp.getPixel(x + d , y + d / 2))

                    - Color.blue(bmp.getPixel(x - (d /2), y + d))
                    - Color.blue(bmp.getPixel(x + (d /2), y + d));

            int edgeness = (top + bottom) / 7; // Max could be 13 * 255

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
    private static boolean colourFineMaskPoint(Bitmap origBMP, Bitmap resultBMP, int x, int y, int width, int height, int loThresh, int hiThresh)
    {
        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = resultBMP.getPixel(x,y) == Color.BLUE ? Color.BLUE :
                getFineEdgeness(origBMP, x, y, (width - 1) / 2, (height - 1) / 2);

        //Log.d("Hi", "\tAnother FINE pixel. Edgeness of (" + x + ", " + y + ") is " + edgeness);
        return determineColour(resultBMP, edgeness, hiThresh, loThresh, x, y, width, height);
    }

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

    // Colour in bitmap bmp at the locations in edgeCoords
    // edgeCoords is a 2D list:
    // x increases by 1 - but don't forget the bitmap increases by width
    // y is the actual y coordinate from the bitmap
    private static void colourFineBitmap(Bitmap bmp, List<Point> edgeCoords, int pWidth, int pHeight)
    {
        for (Point p : edgeCoords)
            colourArea(bmp, (int) p.getX(), (int) p.getY(), Color.YELLOW, pWidth, pHeight);
    }

    /////// COLOUR ///////
    // Colour in the point around pixel (x,y) based on the edgeness we got
    private static boolean determineColour(Bitmap bmp, int edgeness, int pThr, int nThr,
                                       int x, int y, int width, int height) {
        if (edgeness == Color.BLUE) {
            // If a neighbour set this as a semi edge, leave it be
            return true;
        } else if (edgeness < nThr) {
            //Log.d("Colour", "Black");
            // Not a strong edge, ignore it
            colourArea(bmp, x, y, Color.BLACK, width, height);
        } else if (edgeness < pThr ||
                (edgeness >= pThr && !checkAndSetNbour(bmp, x, y, width, height, nThr, pThr))) {
            //Log.d("Colour", "Medium blue");
            // Point is within the neighbouring threshold
            // or is a definite edge with no neighbours, therefore doesn't count
            colourArea(bmp, x, y, edgeness, width, height);
        } else {
            //Log.d("Colour", "White");
            // Point is an edge with neighbours
            colourArea(bmp, x, y, Color.WHITE, width, height);
            return true;
        }
        return false;
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

        if (i < 0 || j < 0 || i + width >= bmp.getWidth() || j + height >= bmp.getHeight()) {
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
    private static boolean checkAndSetNbour(Bitmap bmp, int x, int y, int pointWidth, int pointHeight, int minThreshold, int maxThreshold) {
        boolean anyEdges = false;
        // For the neighbours we've already seen before,
        // x.e. left three and immediately above
        // If any seen neighbours were edges, set anyEdges
        if (checkSeenNbours(bmp, x, y, pointWidth, pointHeight))
            anyEdges = true;

        // For new neighbours
        // x.e. immediately below and right three neighbours
        // If any unseen neighbours were edges, set anyEdges
        if (checkUnseenNbours(bmp, x, y, pointWidth, pointHeight, minThreshold, maxThreshold))
            anyEdges = true;

        return anyEdges;
    }

    // Check the neighbours we haven't yet seen - immediately below and right three neighbours
    // Returns true if any of these neighbours are edges
    private static boolean checkUnseenNbours(Bitmap bmp, int i, int j, int width, int height, int minThreshold, int maxThreshold) {
        boolean anyEdges = false;

        for (int x = i; x <= i + width; x += width) {
            for (int y = j - height; y <= j + height; y += height) {
                // Check if last four unchecked neighbours are within bounds of the bitmap
                if (!(x == i & y < j + height)  // Don't check the centre one or above that - these have already been seen
                        && x > ((width - 1) / 2) && x + ((width - 1) / 2) < bmp.getWidth()
                        && y > ((height - 1) / 2) && y + ((height - 1) / 2) < bmp.getHeight()) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    boolean thisNeighEdgy = checkUnseenNbour(bmp, x, y, width, height, minThreshold, maxThreshold);

                    // If this neighbour is the first found edge, mark that (i,j) has any coloured
                    // neighbours. If it's any edges found after, we've already set anyEdges
                    if (thisNeighEdgy && !anyEdges)
                        anyEdges = true;
                }
            }
        }
        return anyEdges;
    }

    private static boolean checkUnseenNbour(Bitmap bmp, int x, int y, int width, int height,
                                            int minThreshold, int maxThreshold)
    {
        // Find how strong of an edge this is
        double edgeness;
        if (width == bmp.getHeight() / 17 * 2 + 1) // Coarse masking
            edgeness = getCoarseEdgeness(bmp, x, y, (width-1)/2);
        else // Fine masking
            edgeness = getFineEdgeness(bmp, x, y, (width - 1) / 2, (height - 1) / 2);

        // Determine outcome of edge strength value
        if (edgeness > minThreshold) {
            if (edgeness < maxThreshold)
                colourArea(bmp, x, y, Color.BLUE, width, height);
            return true;
        } else {
            return false;
        }
    }

    // Checking neighbours we've seen - left three and immediately above
    // Returns true if any neighbours are edges
    private static boolean checkSeenNbours(Bitmap bmp, int x, int y, int width, int height) {
        boolean anyEdges = false;

        for (int i = x - width; i <= x; i += width)
            for (int j = y - height; j <= y + height; j += height) {
                // Check if last four unchecked neighbours are within bounds of the bitmap
                if (!(i == x & j > y - height) // Don't check the centre or the one below that
                        && i > ((width-1)/2) && i + ((width-1)/2) < bmp.getWidth()
                        && j > ((height-1)/2) && j + ((height-1)/2) < bmp.getHeight()) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    boolean thisNeighEdgy = checkSeenNbour(bmp, i, j, width, height);

                    // If this neighbour is the first found edge, mark that (x,y) has any coloured
                    // neighbours. If it's any edges found after, we've already set anyEdges
                    if (thisNeighEdgy && !anyEdges)
                        anyEdges = true;
                }
            }
        return anyEdges;
    }

    private static boolean checkSeenNbour(Bitmap bmp, int x, int y, int width, int height)
    {
        // Get the colour of this point we've already set
        int neighCol = bmp.getPixel(x, y);

        // See if we've marked any as possible edges i.e. just not black
        if (neighCol == Color.BLUE || neighCol == Color.WHITE) {
            //Log.d("Hi", "Seen neighbour (" + x + ", " + y + ") had a pure blue or white worthy edge.");
            return true; // Found an already found neighbouring edge
        } else if (neighCol != Color.BLACK){
            //Log.d("Hi", "Seen neighbour (" + x + ", " + y + ") must have been a weak edge, set it blue");

            // Found a new neighbouring edge
            colourArea(bmp, x, y, Color.BLUE, width, height);
            // New edge that we've already gone past so will not revisit
            // will have to add this to edgeCoords manually
            if (fineEdgeCoords != null)   // If it has been set (should be at this point)
                fineEdgeCoords.add(new Point(x, y));
            // Next time this point is checked it will be blue so we wouldn't enter
            // this area of code so the same coords can't be added twice
            return true;
        }

        // This neighbour is no edge
        return false;
    }

    /////// THINNING ///////
    private static List<Point> skeletonisePoints(List<Point> edgeCoords, int pointDiametre)
    {
        List<Point> thinnedCoords = new ArrayList<>();

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
