package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class ImageManipulation {

    /////////////////////// COARSE /////
    static boolean colourCoarseMaskPoint(Bitmap bmp, int i, int j, int distFromCentre, int loThresh, int hiThresh) {

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getCoarseEdgeness(bmp, i, j, distFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother COARSE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);

        int widthToColourAtOnce = distFromCentre * 2 + 1;
        int heightToColourAtOnce = widthToColourAtOnce; // For the coarse detector, we're using a square

        // If we coloured point at (i,j) a useful colour, return this fact
        return determineColour(bmp, edgeness, loThresh, hiThresh, i, j, widthToColourAtOnce, heightToColourAtOnce);

    }

    private static int getCoarseEdgeness(Bitmap bmp, int i, int j, int d) {
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
            Log.e("Hi", e.toString());
        }

        return -1;
    }

    /////////////////////// FINE /////
    static boolean colourFineMaskPoint(Bitmap bmp, int i, int j, int fineWidth, int fineHeight, int loThresh, int hiThresh) {

        int widthFromCentre = (fineWidth - 1) / 2;
        int heightFromCentre = (fineHeight - 1) / 2;

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getFineEdgeness(bmp, i, j, widthFromCentre, heightFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother FINE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);
        return determineColour(bmp, edgeness, hiThresh, loThresh, i, j, fineWidth, fineHeight);
    }

    private static int getFineEdgeness(Bitmap bmp, int i, int j, int widthRadius, int heightRadius) {
        int edgeness = 0;

        for (int y = j - heightRadius; y <= j + heightRadius; y += heightRadius + heightRadius)
            for (int x = i - widthRadius; x <= i + widthRadius; x+= widthRadius)
                if (x < bmp.getWidth() && y < bmp.getHeight()) {
                    if (x == i) // If this is a centre point, weigh it twice as heavily
                        edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);
                    edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightRadius) ? -1 : 1);

            }
        edgeness /= 4; // Max could be 3 * 255

        return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these
    }

    // Colour in bitmap bmp at the locations in edgeCoords
    // edgeCoords is a 2D list:
    // x increases by 1 - but don't forget the bitmap increases by width
    // y is the actual y coordinate from the bitmap
    static void colourFineBitmap(Bitmap bmp, List<List<Integer>> edgeCoords,
                                 int width, int height, int widthFromCentre) {

        for (int i = 0; i < edgeCoords.size(); i++)
            for (int j = 0; j < edgeCoords.get(i).size(); j++)
                // from x = widthFromCentre, then x = widthFrCe + width, until x = bmpwidth-
                colourArea(bmp, i * width + widthFromCentre, edgeCoords.get(i).get(j),
                        Color.YELLOW, width, height);
    }

    /////// COLOUR ///////
    // Colour in the point around pixel (i,j) based on the edgeness we got
    private static boolean determineColour(Bitmap bmp, int edgeness, int pThr, int nThr,
                                       int i, int j, int width, int height) {
        if (edgeness == Color.BLUE) {
            // If a neighbour set this as a semi edge, leave it be
            return true;
        }
        else if (edgeness < nThr) {
            //Log.d("Colour", "Black");
            // Not a strong edge, ignore it
            colourArea(bmp, i, j, Color.BLACK, width, height);
        }
        else if (edgeness < pThr ||
                (edgeness >= 40 &&
                        !checkAndSetNbour(bmp, i, j, width, height, nThr, pThr))) {
            //Log.d("Colour", "Medium blue");
            // Point is within the neighbouring threshold
            // or is a definite edge with no neighbours, therefore doesn't count
            colourArea(bmp, i, j, edgeness, width, height);
        }
        else {
            //Log.d("Colour", "White");
            // Point is an edge with neighbours
            colourArea(bmp, i, j, Color.WHITE, width, height);
            return true;
        }
        return false;
    }

    // Colour a wxh block of pixels around (i,j) in the requested colour
    static void colourArea(Bitmap bmp, int i, int j, int colour, int width, int height) {

        // setPixels needs an int array of colours
        int[] colours = new int[width * height];
        Arrays.fill(colours, colour);

        // The top left coordinate of the area to colour
        int x = i - (width-1) / 2;
        int y = j - (height-1) / 2;

        // Don't try colour in areas outside of the image
        if (x < 0) {
            width += x;
            x = 0;
        }
        else if (x + width >= bmp.getWidth())
            width = bmp.getWidth() - x - 1;

        if (y < 0) {
            height += y;
            y = 0;
        } else if (y + height >= bmp.getHeight())
            height = bmp.getHeight() - y - 1;

        //Log.d("Hi", "Trying to colour from " + x + ", " + y + ". Width x height: "
          //      + width + "x" + height + " BMP: " + bmp.getWidth() + ", " + bmp.getHeight());
        bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                width,      // stride, width of what you wanna colour in
                x,          // x co-ord of first pixel to colour
                y,          // y co-ord of first pixel to colour
                width,      // width of area to colour
                height);    // height of area to colour
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
        if (ImageToDetect.showCoarse) {
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
            if (ImageToDetect.edgeCoords != null)   // If it has been set (should be at this point)
                ImageToDetect.edgeCoords.get((x-widthFromCentre) / width).add(y);
            // Next time this point is checked it will be blue so we wouldn't enter
            // this area of code so the same coords can't be added twice
            return true;
        }

        // This neighbour is no edge
        return false;
    }

    /////// THINNING ///////
    // Reduce the number of edges in this column to one. Pick the middle one.
    private static int thinColumn(Bitmap fineBMP, List col, int colIndex, int fineWidth, int fineHeight) {
        // Skip any columns that don't have edges
        int noOfEdgesInCol = col.size();
        if (noOfEdgesInCol > 0) {
            // The middle edge in the column is most likely to be accurate so use this.
            Collections.sort(col);
            int mostAccurateEdgeInCol = 0;//noOfEdgesInCol / 2; Changed this to check if I should be getting the top value, as the horizon is at the top
            int yToUse = (int)col.get(mostAccurateEdgeInCol);
            // Have col hold only the edges we wont consider
            col.remove(mostAccurateEdgeInCol);

            //Log.d("Hi", "In column " + colIndex + " there are edges at " + col + ". Keep edge (" + colIndex + ", " + yToUse + ")");

            if (!ImageToDetect.showEdgeOnly) {
                // In this column, colour in the only edge we want as white, the rest as black
                // ~Change to yellow to show the result of thinning more clearly~

                colourArea(fineBMP, colIndex, yToUse,
                        Color.WHITE, fineWidth, fineHeight);

                for (Object y : col) {
                    // ~Change to red to see which edges were removed from thinning~
                    //Log.d("Hi", "Thin out " + y + " from column " + colIndex);
                    colourArea(fineBMP, colIndex, (int) y, Color.BLACK, fineWidth, fineHeight);
                }
            } // No need to colour in fineBMP if we're only showing the horizon on the bitmap

            return yToUse;

        } else {
            //Log.d("Hi", "No edges in column " + colIndex);
            return -1;
        }
    }

    static List<List<Integer>> thinBitmap(Bitmap fineBMP, List<List<Integer>> edgeCoords,
                                          int fineWidth, int fineHeight, int fineWidthFromCentre)
    {
        // Start at the centre of the first point
        int colIndex = fineWidthFromCentre;

        // Go through each of the edge coords
        for (int i = 0; i < edgeCoords.size(); i++) {
            int yToUse = thinColumn(fineBMP, edgeCoords.get(i), colIndex, fineWidth, fineHeight);

            if(ImageToDetect.showEdgeOnly) {
                // Have each column hold the 1 edge (if exists) found through thinning
                if (yToUse != -1) {
                    edgeCoords.get(i).clear();
                    edgeCoords.get(i).add(yToUse);
                    //Log.d("Hi", "Col " + colIndex + " now only holds: " + edgeCoords.get(i));
                } else {
                    //Log.d("Hi", "Col " + colIndex + " didn't have any edges, make it null" );
                    // No edges in column colIndex, make it null
                    edgeCoords.get(i).clear();
                }
            }
            colIndex += fineWidth;
        }

        return edgeCoords;
    }

}
