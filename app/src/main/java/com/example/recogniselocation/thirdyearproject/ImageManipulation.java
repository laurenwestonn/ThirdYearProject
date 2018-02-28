package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.content.ContentValues.TAG;

class ImageManipulation {

    private static boolean gShowCoarse;
    private static boolean gShowEdgeOnly;

    private static List<Point> edgeCoords;
    static int fineWidth;
    private static int fineHeight;
    private static int fineWidthRadius;
    private static int fineHeightRadius;

    static Edge detectEdge(Bitmap bmp,
                           boolean showCoarse, boolean sdDetail,
                           boolean useThinning, boolean showEdgeOnly)
    {
        if (bmp == null)
            Log.e(TAG, "Null bitmap was passed to detectEdge");

        // Save these variables globally for now Todo: This better
        gShowCoarse = showCoarse;
        gShowEdgeOnly = showEdgeOnly;
        Bitmap resultBMP;

        ////////////// COARSE MASK /////////////////
        CoarseMasking coarse = coarseMask(bmp);

        ///////////// Standard Deviation //////////////
        if (coarse.getCoords().size() > 0) {
            StandardDeviation coarseSD;

            coarseSD = findStandardDeviation(coarse.getBitmap(), coarse.getCoords(), sdDetail);
            edgeCoords = coarse.getCoords();
            Log.d(TAG, "findStandardDeviation: SD got");

            // Whether SD was drawn on or not, the coarse mask will get returned from the above
            if (showCoarse) // Todo: Set the edgeCoords to be the coarse mask coords (so, get them) if we're asking to 'showCoarse'
                resultBMP = coarseSD.getBitmap();
            else {
                ///////////////////// FINE MASK //////////////////
                Edge fine = fineMask(bmp, coarseSD);
                resultBMP = fine.getBitmap();
                edgeCoords = fine.getCoords();

                ////////// THINNING //////////
                if (useThinning) {
                    Log.d(TAG, "detectEdge: Going to skeletonise edgeCoords: " + edgeCoords.toString());
                    edgeCoords = ImageManipulation.thinBitmap(edgeCoords, fineWidth);
                    Log.d(TAG, "detectEdge: Result of skeletonising coords:  " + edgeCoords.toString());
                    edgeCoords = ImageManipulation.thinColumns(edgeCoords);
                    Log.d(TAG, "detectEdge: Result of one coord per column:  " + edgeCoords.toString());
                }

                ///////// SHOW EDGES ONLY? /////////
                if (showEdgeOnly) {
                    // Get a new copy of the photo to draw the edge on top of
                    resultBMP = bmp.copy(bmp.getConfig(), true);
                    // Draw the edge on top of the photo from the edge coordinates we saved in edgeCoords
                    colourFineBitmap(resultBMP, edgeCoords, fineWidthRadius, fineHeightRadius);
                }
            }

        } else {
            Log.e(TAG, "detectEdge: Couldn't find edges with the coarse mask, so just return the original photo");
            resultBMP = coarse.getBitmap();
        }
        Log.d(TAG, "detectEdge: Found the photo edge coords " + edgeCoords);
        return new Edge(edgeCoords, resultBMP);
    }

    private static List<Point> thinColumns(List<Point> edgeCoords)
    {
        List<Point> onePerColumn = new ArrayList<>();
        for (Point p : edgeCoords) {
            // Find all points within the same column
            int colIndex = (int) p.getX();
            List<Point> pointsInCol = findPointsInCol(edgeCoords, colIndex);

            // Pick best point in column (middle, more towards top due to noise below horizon)
            Point bestPoint = bestPointInCol(pointsInCol);
            if (!onePerColumn.contains(bestPoint))  // Add it if you haven't already
                onePerColumn.add(bestPoint);
        }
        return onePerColumn;
    }

    public static Point bestPointInCol(List<Point> col) {
        switch (col.size()) {
            case 0: return null;
            case 1: return col.get(0);
            default: return col.get((int)Math.ceil(col.size() / 2.0) - 1);
        }
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
        //  1   0   1   0   1
        //  0   0   0   0   0
        //  -1  0   -1  0   -1

        // Get a copy of the original photo to use the fine mask on
        Bitmap resultBMP = origBMP.copy(origBMP.getConfig(), true);

        fineWidthRadius = resultBMP.getWidth() / 250; // 1 would make a mask of width 3, 2 would give width 5
        fineWidth = fineWidthRadius * 2 + 1;    // Width of the fine mask
        fineHeightRadius = resultBMP.getHeight() / 120;
        fineHeight = fineHeightRadius * 2 + 1;
        int pointWidth = fineWidthRadius;       // The width of point to colour in
        int pointWidthRadius = pointWidth / 2;

        boolean relevantEdge;
        List<Point> edgeCoords = new ArrayList<>();

        // Use a fine mask on the area found to be the horizon by the coarse mask
        for (int x = fineWidthRadius; x < resultBMP.getWidth() - fineWidthRadius; x+= fineWidthRadius)
            for(int y = coarseSD.getMinRange() + fineHeightRadius; y < coarseSD.getMaxRange() - fineHeightRadius; y+= fineHeightRadius) {

                /////// NEIGHBOURING THRESHOLD ///////

                // Thresholds
                int pointThreshold = origBMP.getHeight() / 35; // The threshold to determine an edge for a point
                int neighbThreshold = (int) (pointThreshold * 0.9); // A point that is neighbouring an edge's threshold

                // Is this a edge?
                relevantEdge = ImageManipulation.colourFineMaskPoint(origBMP, resultBMP, x, y, fineWidth, fineHeight, pointThreshold, neighbThreshold);
                if (relevantEdge)
                    // This should hold the location of every edge found with the fine mask
                    edgeCoords.add(new Point(x , y));
            }
        Log.d(TAG, "fineMask: Fine Masking done");

        return new Edge(edgeCoords, resultBMP);
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

        for (int x = coarseRadius+1; x < bmp.getWidth(); x += coarseDiam)
            for (int y = coarseRadius+1; y < bmp.getHeight(); y += coarseDiam) {

                // The threshold to determine if a point is an edge
                int pointThreshold = bmp.getHeight() / 23;
                // The looser threshold for a point that is neighbouring an edge
                int neighbThreshold = (int) (pointThreshold * 0.8);

                // Check if this point is determined an edge with the coarse mask
                boolean relevantEdge = ImageManipulation.colourCoarseMaskPoint(bmp, x, y, coarseRadius, pointThreshold, neighbThreshold);
                // If it is, remember it so we can narrow the area we use our fine mask in
                if (relevantEdge)
                    edgeCoords.add(new Point(x,y));
            }
        Log.d(TAG, "CoarseMasking: Coarse Masking done: " + edgeCoords);
        return new CoarseMasking(edgeCoords, bmp);
    }




    /////////////////////// COARSE /////
    static boolean colourCoarseMaskPoint(Bitmap bmp, int i, int j, int distFromCentre, int loThresh, int hiThresh) {

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getCoarseEdgeness(bmp, i, j, distFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother COARSE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);

        int pointWidth = distFromCentre * 2 + 1;
        int pointHeight = pointWidth; // For the coarse detector, we're using a square


        // If we coloured point at (i,j) a useful colour, return this fact
        return determineColour(bmp, edgeness, loThresh, hiThresh, i, j, pointWidth, pointHeight);

    }

    private static int getCoarseEdgeness(Bitmap bmp, int i, int j, int d) {
        // TODO: CHECK WHAT IS CONSTANTLY THROWING AN ERROR, CAN WE AVOID THIS?
        int top, bottom;
        try {
            top = Color.blue(bmp.getPixel(i - d / 3, j - d))
                    + Color.blue(bmp.getPixel(i + 0, j - d)) * 2
                    + Color.blue(bmp.getPixel(i + d / 3, j - d))

                    + Color.blue(bmp.getPixel(i - d, j - d / 2))
                    + Color.blue(bmp.getPixel(i - d / 3, j - d / 2)) * 2
                    + Color.blue(bmp.getPixel(i + 0, j - d / 2)) * 3
                    + Color.blue(bmp.getPixel(i + d / 3, j - d / 2)) * 2
                    + Color.blue(bmp.getPixel(i + d - 1, j - d / 2));

            bottom = -Color.blue(bmp.getPixel(i - d, j + d / 2))
                    - Color.blue(bmp.getPixel(i - d / 3, j + d / 2)) * 2
                    - Color.blue(bmp.getPixel(i + 0, j + d / 2)) * 3
                    - Color.blue(bmp.getPixel(i + d / 3, j + d / 2)) * 2
                    - Color.blue(bmp.getPixel(i + d - 1, j + d / 2))

                    - Color.blue(bmp.getPixel(i - d / 3, j + d - 1))
                    - Color.blue(bmp.getPixel(i + 0, j + d - 1)) * 2
                    - Color.blue(bmp.getPixel(i + d / 3, j + d - 1));

            int edgeness = (top + bottom) / 13; // Max could be 13 * 255

            return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these

        } catch (ArrayIndexOutOfBoundsException boundsException) {
            Log.e("Hi", "You can't access (" + i + ", " + j + ") in a bitmap "
                    + bmp.getWidth() + " x " + bmp.getHeight()
                    + "\n" + boundsException.toString());
        } catch (Exception e){
            Log.e("getCoarseEdgeness", e.toString());
        }

        return -1;
    }

    /////////////////////// FINE /////
    static boolean colourFineMaskPoint(Bitmap origBmp, Bitmap bmp, int i, int j, int maskWidth, int maskHeight, int loThresh, int hiThresh) {
        int maskRadiusWidth = (maskWidth - 1) / 2;
        int maskRadiusHeight = (maskHeight - 1) / 2;

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) == Color.BLUE ? Color.BLUE :
                getFineEdgeness(origBmp, i, j, maskRadiusWidth, maskRadiusHeight);

        //Log.d("Hi", "\tAnother FINE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);
        return determineColour(bmp, edgeness, hiThresh, loThresh, i, j, maskRadiusWidth, maskRadiusHeight);
    }

    private static int getFineEdgeness(Bitmap bmp, int i, int j, int widthRadius, int heightRadius) {
        int edgeness = 0;

        for (int y = j - heightRadius; y <= j + heightRadius; y += heightRadius + heightRadius)
            for (int x = i - widthRadius; x <= i + widthRadius; x += widthRadius)
                if (x >= 0 && y >= 0 && x < bmp.getWidth() && y < bmp.getHeight()) {
                    if (x == i)// If this is a centre point, weigh it twice as heavily
                        edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);
                    edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);
                }
        edgeness /= 4; // Max could be 4 * 255
        return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these
    }

    // Colour in bitmap bmp at the locations in edgeCoords
    // edgeCoords is a 2D list:
    // x increases by 1 - but don't forget the bitmap increases by width
    // y is the actual y coordinate from the bitmap
    static void colourFineBitmap(Bitmap bmp, List<Point> edgeCoords, int pWidth, int pHeight)
    {
        for (Point p : edgeCoords)
            colourArea(bmp, (int) p.getX(), (int) p.getY(), Color.YELLOW, pWidth, pHeight);
    }

    /////// COLOUR ///////
    // Colour in the point around pixel (i,j) based on the edgeness we got
    private static boolean determineColour(Bitmap bmp, int edgeness, int pThr, int nThr,
                                       int i, int j, int width, int height) {
        if (edgeness == Color.BLUE) {
            // If a neighbour set this as a semi edge, leave it be
            return true;
        } else if (edgeness < nThr) {
            //Log.d("Colour", "Black");
            // Not a strong edge, ignore it
            colourArea(bmp, i, j, Color.BLACK, width, height);
        } else if (edgeness < pThr ||
                (edgeness >= 40 && !checkAndSetNbour(bmp, i, j, width, height, nThr, pThr))) {
            //Log.d("Colour", "Medium blue");
            // Point is within the neighbouring threshold
            // or is a definite edge with no neighbours, therefore doesn't count
            colourArea(bmp, i, j, edgeness, width, height);
        } else {
            //Log.d("Colour", "White");
            // Point is an edge with neighbours
            colourArea(bmp, i, j, Color.WHITE, width, height);
            return true;
        }

        return false;
    }

    // Colour a wxh block of pixels around (i,j) in the requested colour
    static Bitmap colourArea(Bitmap bmp, int i, int j, int colour, int width, int height) {

        // setPixels needs an int array of colours
        int[] colours = new int[width * height];
        Arrays.fill(colours, colour);

        // The top left coordinate of the area to colour
        int x = i - (width-1) / 2;
        int y = j - (height-1) / 2;

        // Don't try colour in areas outside of the image
        if (x < 0) {
            width += x;
            if (height < 0) {
                Log.e(TAG, "colourArea: This pixel (" + i + ", " + j + ") and surrounding area is completely before the image (x is negative) So can't be coloured");
                return bmp;
            }
            x = 0;
        }
        else if (x + width >= bmp.getWidth())
            width = bmp.getWidth() - x - 1;

        if (y < 0) {
            height += y;    // Just colour in the difference from the edge
            if (height < 0) {
                Log.e(TAG, "colourArea: This pixel (" + i + ", " + j + ") and surrounding area is completely above the image (y is negative) So can't be coloured");
                return bmp;
            }
            y = 0;
        } else if (y + height >= bmp.getHeight())
            height = bmp.getHeight() - y - 1;

        // Ensure the Bitmap is mutable so that it can be coloured
        if (!bmp.isMutable()) {
            bmp = bmp.copy(bmp.getConfig(), true);
        }

        //Log.d("Hi", "Trying to colour from " + x + ", " + y + ". Width x height: "
        //        + width + "x" + height + " BMP: " + bmp.getWidth() + ", " + bmp.getHeight());
        bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                width,      // stride, width of what you want to colour in
                x,          // x co-ord of first pixel to colour
                y,          // y co-ord of first pixel to colour
                width,      // width of area to colour
                height);    // height of area to colour

        return bmp;
    }


    /////// NEIGHBOURS ///////
    private static boolean checkAndSetNbour(Bitmap bmp, int i, int j, int pointWidth, int pointHeight, int minThreshold, int maxThreshold) {
        boolean anyEdges = false;
        // For the neighbours we've already seen before,
        // i.e. top three and immediate left neighbour
        // If any seen neighbours were edges, set anyEdges
        if (checkSeenNbours(bmp, i, j, pointWidth, pointHeight))
            anyEdges = true;

        // For new neighbours
        // i.e. immediate right and the bottom three neighbours
        // If any unseen neighbours were edges, set anyEdges
        if (checkUnseenNbours(bmp, i, j, pointWidth, pointHeight, minThreshold, maxThreshold))
            anyEdges = true;

        return anyEdges;
    }

    private static boolean checkUnseenNbours(Bitmap bmp, int i, int j, int width, int height, int minThreshold, int maxThreshold) {
        boolean anyEdges = false;

        for (int y = j; y <= j + height; y += height) {
            for (int x = i - width; x <= i + width; x += width) {
                // Check last four unchecked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j + height) || x == (i + width))
                        && (x >= 0 && x + ((width-1)/2) < bmp.getWidth() && y >= 0 && y + ((height-1)/2) < bmp.getHeight())) {

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

    private static boolean checkUnseenNbour(Bitmap bmp, int x, int y, int width, int height, int minThreshold, int maxThreshold) {
        if (gShowCoarse) {
            if (getCoarseEdgeness(bmp, x, y, (width-1)/2) > minThreshold) {
                //Log.d("Hi", "Neighbour (" + x + ", " + y + ") had a worthy edge of " + getCoarseEdgeness(bmp, x, y, (width-1)/2));
                if (getCoarseEdgeness(bmp, x, y, (width-1)/2) < maxThreshold) {
                    //Log.d("Hi", "Set pixel blue");
                    colourArea(bmp, x, y, Color.BLUE, width, height);
                }
                return true;
            } else {
                //Log.d("Hi", "Neighbour (" + x + ", " + y + ") isn't edgy enough.. " + getCoarseEdgeness(bmp, x, y, (width-1)/2));
                return false;
            }
        } else {    //TODO: Choose which edgeness technique to use, more neatly
            if (getFineEdgeness(bmp, x, y, (width-1)/2, (height-1)/2) > minThreshold) {
                //Log.d("Hi", "Neighbour (" + x + ", " + y + ") had a worthy edge of " + getFineEdgeness(bmp, x, y, (width-1)/2));
                if (getFineEdgeness(bmp, x, y, (width-1)/2, (height-1)/2) < maxThreshold) {
                    //Log.d("Hi", "Set pixel blue");
                    colourArea(bmp, x, y, Color.BLUE, width, height);
                    // This blue will be added to edgeCoords when we go on to check it later
                }
                return true;
            } else {
                //Log.d("Hi", "Neighbour (" + x + ", " + y + ") isn't edgy enough.. " + getFineEdgeness(bmp, x, y, (width-1)/2));
                return false;
            }
        }
    }

    private static boolean checkSeenNbours(Bitmap bmp, int i, int j, int width, int height) {
        boolean anyEdges = false;

        for (int y = j - height; y <= j; y += height) {
            for (int x = i - width; x <= i + width; x += width) {
                // Check first four already checked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j - height) || x == (i - width))
                        && (x >= 0 && x + ((width-1)/2) < bmp.getWidth() && y >= 0 && y + ((height-1)/2) < bmp.getHeight())) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    boolean thisNeighEdgy = checkSeenNbour(bmp, x, y, width, height);

                    // If this neighbour is the first found edge, mark that (i,j) has any coloured
                    // neighbours. If it's any edges found after, we've already set anyEdges
                    if (thisNeighEdgy && !anyEdges)
                        anyEdges = true;
                }
            }
        }
        return anyEdges;
    }

    private static boolean checkSeenNbour(Bitmap bmp, int x, int y, int width, int height) {
        int widthFromCentre = (width-1) / 2;

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
            if (edgeCoords != null)   // If it has been set (should be at this point)
                edgeCoords.add(new Point(x, y));
            // Next time this point is checked it will be blue so we wouldn't enter
            // this area of code so the same coords can't be added twice
            return true;
        }

        // This neighbour is no edge
        return false;
    }

    /////// THINNING ///////
    // Reduce the number of edges in this column to one. Pick the first one that isn't noise.
    // Returns the most recent valid point you've seen
    private static Point thinColumn(Bitmap bmp, List<Integer> col, int colX, Point prevPoint, int width, int height) {
        int bestColIndex = Integer.MIN_VALUE;
        Point pointToUse = null;

        for (int i = 0; i < col.size(); i++)
            // Avoid noise by checking that the previous point isn't too different from this one
            if (prevPoint == null)      // The first point
                if (col.size() > 0)
                    bestColIndex = 0;   // There's no points to compare against, just assume this is from the horizon
                else
                    return null;
            else if (Math.abs((colX - prevPoint.getX())
                    / (col.get(i) - prevPoint.getY())) >= 0.4) {
                bestColIndex = i;   // Pick the highest in this column which isn't noise
                //Log.d(TAG, "thinColumn: We've found a point in this column to keep which is nearby the last. " + new Point(colX, col.get(bestColIndex)).toString());
                break;
            }

        // Have col hold only the points we want to remove
        // This body is entered if you found a point to keep
        if (bestColIndex != Integer.MIN_VALUE) {
            pointToUse = new Point(colX, col.get(bestColIndex));
            col.remove(bestColIndex);
            //Log.d("Hi", "In column " + colX + " there are edges at " + col + ". Keep edge (" + colX + ", " + yToUse + ")");
        }

        if (!gShowEdgeOnly) {
            // Colour in the only point we declare as an edge in this column
            if (pointToUse != null)
                colourArea(bmp, (int)pointToUse.getX(), (int)pointToUse.getY(), Color.WHITE, width, height);

            // Get rid of the points we'd found in col that we now don't want
            for (Integer y : col)
                // ~Change to red to see which edges were removed from thinning~
                colourArea(bmp, colX, y, Color.BLACK, width, height);
        }

        // Return the most recent valid point you've seen
        return pointToUse == null ? prevPoint : pointToUse;
    }

    private static List<Point> thinBitmap(List<Point> edgeCoords, int pointDiametre)
    {
        List<Point> thinnedCoords = new ArrayList<>();

        // Go through each of the edge coords
        for (int i = 0; i < edgeCoords.size(); i++) {
            boolean removeThisPoint = thinPoint(edgeCoords, edgeCoords.get(i), pointDiametre);

            if(gShowEdgeOnly)
                if (!removeThisPoint)
                    thinnedCoords.add(edgeCoords.get(i));
        }
        if (gShowEdgeOnly)
            return thinnedCoords;
        else
            return edgeCoords;
    }

    // Using techniques from skeletonisation
    public static boolean thinPoint(List<Point> coords, Point point, int pointDiametre)
    {
        boolean removeThisPoint = false;
        double x,y;
        x = point.getX();
        y = point.getY();

        /*
        if (coords.indexOf(new Point(x, y - pointDiametre)) != -1)
            Log.d(TAG, "thinPoint: There's a point above " + point + "; " + x + ", " + (y - pointDiametre));
        else
            Log.d(TAG, "thinPoint: There isn't a point above " + point + "; " + x + ", " + (y - pointDiametre));

        if (coords.indexOf(new Point(x, y - pointDiametre)) != -1)
            Log.d(TAG, "thinPoint: There's a point below " + point + "; " + x + ", " + (y + pointDiametre));
        else
            Log.d(TAG, "thinPoint: There isn't a point below " + point + "; " + x + ", " + (y + pointDiametre));

        if (coords.indexOf(new Point(x + pointDiametre, y)) != -1 && coords.indexOf(new Point(x - pointDiametre, y)) == -1
                || coords.indexOf(new Point(x - pointDiametre, y)) != -1 && coords.indexOf(new Point(x + pointDiametre, y)) == -1)
            Log.d(TAG, "thinPoint: There's a point either on the right or left of " + point
                    + "; " + (x + pointDiametre) + ", " + y + " or " + (x - pointDiametre) + ", " + y);
        else
            Log.d(TAG, "thinPoint: There isn't a point only on the right or left of " + point
                    + "; " + (x + pointDiametre) + ", " + y + " or " + (x - pointDiametre) + ", " + y);
*/

        // If point above this one is an edge
        if (coords.indexOf(new Point(x, y - pointDiametre)) != -1
                // and if point below is an edge
                && coords.indexOf(new Point(x, y + pointDiametre)) != -1
                // and point on EITHER the right or left is an edge
                && (coords.indexOf(new Point(x + pointDiametre, y)) != -1 && coords.indexOf(new Point(x - pointDiametre, y)) == -1
                    || coords.indexOf(new Point(x - pointDiametre, y)) != -1 && coords.indexOf(new Point(x + pointDiametre, y)) == -1) ) {
            // Don't include this point
            removeThisPoint = true;
            //Log.d(TAG, "thinPoint: As all three neighbouring points exist, we can thin point " + point);
        }

        return removeThisPoint;
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

                colourArea(thresholdBMP, x, y, colour, pointDiamm, pointDiamm);
            }

        return thresholdBMP;
    }

    public static Bitmap colourBitmapCoords(Bitmap bmp, List<Point> coords, int colour, int size) {
        for (Point p : coords)
            bmp = colourArea(bmp, (int) p.getX() , (int) p.getY(), colour, size, size);
        return bmp;
    }

    public static Bitmap markMatchedCoords(Bitmap bmp, List<Point> coords) {
        int colour = Color.BLACK;

        for (Point p : coords) {
            bmp = colourArea(bmp, (int) p.getX(), (int) p.getY(), colour, 10, 10);
            colour += 1000;
        }
        return bmp;
    }
}
