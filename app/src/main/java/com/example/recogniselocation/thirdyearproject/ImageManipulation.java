package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class ImageManipulation {

    public static boolean colourCoarseMaskPoint(Bitmap bmp, int i, int j, int distFromCentre) {

        // Thresholds
        int pThr = 40; // The threshold to determine an edge for a point
        int nThr = 35; // A point that is neighbouring an edge's threshold

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getCoarseEdgeness(bmp, i, j, distFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother COARSE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);

        int widthToColourAtOnce = distFromCentre * 2 + 1;
        int heightToColourAtOnce = widthToColourAtOnce; // For the coarse detector, we're using a square

        // If we coloured point at (i,j) a useful colour, return this fact
        return determineColour(bmp, edgeness, pThr, nThr, i, j, widthToColourAtOnce, heightToColourAtOnce);

    }

    public static boolean colourFineMaskPoint(Bitmap bmp, int i, int j, int fineWidth, int fineHeight) {

        int widthFromCentre = (fineWidth - 1) / 2;
        int heightFromCentre = (fineHeight - 1) / 2;

        // Thresholds
        int pThr = 30; // The threshold to determine an edge for a point
        int nThr = 27; // A point that is neighbouring an edge's threshold

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getFineEdgeness(bmp, i, j, widthFromCentre, heightFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother FINE pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);
        return determineColour(bmp, edgeness, pThr, nThr, i, j, fineWidth, fineHeight);
    }

    private static int getFineEdgeness(Bitmap bmp, int i, int j, int widthFromCentre, int heightFromCentre) {
        int edgeness = 0;

        for (int y = j - heightFromCentre; y <= j + heightFromCentre; y = y + heightFromCentre + heightFromCentre)
            for (int x = i - widthFromCentre; x <= i + widthFromCentre; x+= widthFromCentre)
                edgeness += Color.blue(bmp.getPixel(x, y)) * ((y == j + heightFromCentre) ? -1 : 1);
        edgeness /= 3; // Max could be 3 * 255

        return edgeness > 0 ? edgeness : 0; // Edges with dark on top are -ve, ignore these
    }

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


    private static int getCoarseEdgeness(Bitmap bmp, int i, int j, int d) {
        int top, bottom;
        try {
            top = Color.blue(bmp.getPixel(i - d / 3, j - d)) //ToDo: get rid of these darn plus ones
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
        if (ImageToDetect.useCoarse == true) {
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
            return true;
        }

        // This neighbour is no edge
        return false;
    }

    // Colour a wxh block of pixels around (i,j) in the requested colour
    public static void colourArea(Bitmap bmp, int i, int j, int colour, int width, int height) {

        // setPixels needs an int array of colours
        int[] colours = new int[width * height];
        Arrays.fill(colours, colour);

        //Log.d("Hi", "Trying to colour from " + (i - (width-1) / 2) + ", " + (j - (height-1) / 2) + ". Width x height: " + width + "x" + height + " BMP: " + bmp.getWidth() + ", " + bmp.getHeight());
        bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                width,    // stride, width of what you wanna colour in
                i - (width-1) / 2, // x co-ord of first pixel to colour
                j - (height-1) / 2, // y co-ord of first pixel to colour
                width,    // width of area to colour
                height);   // height of area to colour
    }
}
