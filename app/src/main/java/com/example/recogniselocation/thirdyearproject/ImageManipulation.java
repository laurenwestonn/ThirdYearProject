package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class ImageManipulation {

    public static boolean colourMaskPoint(Bitmap bmp, int i, int j, int distFromCentre) {
        
        // Thresholds
        int pThr = 40; // The threshold to determine an edge for a point
        int nThr = 35; // A point that is neighbouring an edge's threshold
        
        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getEdgeness(bmp, i, j, distFromCentre) :
                Color.BLUE;

        //Log.d("Hi", "\tAnother pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);

        // If we coloured point at (i,j) a useful colour, return this fact
        return determineColour(bmp, edgeness, pThr, nThr, i, j, distFromCentre);

    }

    // Colour in the point around pixel (i,j) based on the edgeness we got
    private static boolean determineColour(Bitmap bmp, int edgeness, int pThr, int nThr,
                                       int i, int j, int distFromCentre) {
        // Width of the whole area we're checking for this pixel (i,j)
        int widthToColourAtOnce = distFromCentre * 2 + 1;
        
        if (edgeness == Color.BLUE) {
            //Log.d("Colour", "Blue");
            // If a neighbour set this as a semi edge, leave it be
            colourArea(bmp, i, j, Color.BLUE, widthToColourAtOnce, widthToColourAtOnce);
            return true;
        }
        else if (edgeness < nThr) {
            //Log.d("Colour", "Black");
            // Not a strong edge, ignore it
            colourArea(bmp, i, j, Color.BLACK, widthToColourAtOnce, widthToColourAtOnce);
        }
        else if (edgeness < pThr ||
                (edgeness >= 40 &&
                        !checkAndSetNbour(bmp, i, j, widthToColourAtOnce, nThr))) {
            //Log.d("Colour", "Medium blue");
            // Point is within the neighbouring threshold
            // or is a definite edge with no neighbours, therefore doesn't count
            colourArea(bmp, i, j, edgeness, widthToColourAtOnce, widthToColourAtOnce);
        }
        else {
            //Log.d("Colour", "White");
            // Point is an edge with neighbours
            colourArea(bmp, i, j, Color.WHITE, widthToColourAtOnce, widthToColourAtOnce);
            return true;
        }
        return false;
    }


    private static int getEdgeness(Bitmap bmp, int i, int j, int d) {
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


    private static boolean checkAndSetNbour(Bitmap bmp, int i, int j, int pointWidth, int minThreshold) {
        boolean anyEdges = false;
        // For the neighbours we've already seen before,
        // i.e. top three and immediate left neighbour
        // If any seen neighbours were edges, set anyEdges
        if (checkSeenNbours(bmp, i, j, pointWidth, minThreshold))
            anyEdges = true;
        
        // For new neighbours
        // i.e. immediate right and the bottom three neighbours
        // If any unseen neighbours were edges, set anyEdges
        if (checkUnseenNbours(bmp, i, j, pointWidth, minThreshold))
            anyEdges = true;

        return anyEdges;
    }

    private static boolean checkUnseenNbours(Bitmap bmp, int i, int j, int pointWidth, int minThreshold) {
        boolean anyEdges = false;
        
        for (int y = j; y <= j + pointWidth; y += pointWidth) {
            for (int x = i - pointWidth; x <= i + pointWidth; x += pointWidth) {
                // Check last four unchecked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j + pointWidth) || x == (i + pointWidth))
                        && (x >= 0 && x + ((pointWidth-1)/2) < bmp.getWidth() && y >= 0 && y + ((pointWidth-1)/2) < bmp.getHeight())) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    boolean thisNeighEdgy = checkUnseenNbour(bmp, x, y, pointWidth, minThreshold);
                    
                    // If this neighbour is the first found edge, mark that (i,j) has any coloured
                    // neighbours. If it's any edges found after, we've already set anyEdges
                    if (thisNeighEdgy && !anyEdges)
                        anyEdges = true;
                }
            }
        }
        return anyEdges;
    }

    private static boolean checkUnseenNbour(Bitmap bmp, int x, int y, int pointWidth, int minThreshold) {
        if (getEdgeness(bmp, x, y, (pointWidth-1)/2) > minThreshold) {
            //Log.d("Hi", "Neighbour (" + x + ", " + y + ") had a worthy edge of " + getEdgeness(bmp, x, y, (pointWidth-1)/2));
            if (getEdgeness(bmp, x, y, (pointWidth-1)/2) < minThreshold + 20) {
                Log.d("Hi", "Set pixel blue");
                bmp.setPixel(x, y, 255);
            }
            return true;
        } else {
            //Log.d("Hi", "Neighbour (" + x + ", " + y + ") isn't edgy enough.. " + getEdgeness(bmp, x, y, (pointWidth-1)/2));
            return false;
        }
    }

    private static boolean checkSeenNbours(Bitmap bmp, int i, int j, int pointWidth, int minThreshold) {
        boolean anyEdges = false;
        
        for (int y = j - pointWidth; y <= j; y += pointWidth) {
            for (int x = i - pointWidth; x <= i + pointWidth; x += pointWidth) {
                // Check first four already checked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j - pointWidth) || x == (i - pointWidth))
                        && (x >= 0 && x + ((pointWidth-1)/2) < bmp.getWidth() && y >= 0 && y + ((pointWidth-1)/2) < bmp.getHeight())) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    boolean thisNeighEdgy = checkSeenNbour(bmp, x, y, pointWidth, minThreshold);

                    // If this neighbour is the first found edge, mark that (i,j) has any coloured
                    // neighbours. If it's any edges found after, we've already set anyEdges
                    if (thisNeighEdgy && !anyEdges)
                        anyEdges = true;
                }
            }
        }
        return anyEdges;
    }

    private static boolean checkSeenNbour(Bitmap bmp, int x, int y, int pointWidth, int minThreshold) {
        // Get the colour of this point we've already set
        int neighCol = bmp.getPixel(x, y);
        // See if there's any edgy neighbours 8-)
        // Black cannot be an edge, ignore it
        if (neighCol == Color.BLUE || neighCol == Color.WHITE) {
            Log.d("Hi", "Seen neighbour (" + x + ", " + y + ") had a pure blue or white worthy edge.");
            return true; // Found an already found neighbouring edge
        } else if (neighCol != Color.BLACK){
            Log.d("Hi", "Seen neighbour (" + x + ", " + y + ") must have been a weak edge, set it blue");

            // Found a new neighbouring edge
            int[] colours = new int[pointWidth * pointWidth];
            Arrays.fill(colours, Color.BLUE);

            bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                    pointWidth,    // stride, width of what you wanna colour in
                    x - ((pointWidth-1)/2) - 1, // x co-ord of first pixel to colour
                    y - ((pointWidth-1)/2) - 1, // y co-ord of first pixel to colour
                    pointWidth,    // width of area to colour
                    pointWidth);   // height of area to colour

            Log.d("Colour", "*Actually setting it to yellow to test, doesn't seem to stay yellow");
            return true;
        }

        // This neighbour is no edge
        return false;
    }


    // Colour a square block of pixels around (i,j) the requested colour 
    public static void colourArea(Bitmap bmp, int i, int j, int colour, int width, int height) {

        // setPixels needs an int array of colours
        int[] colours = new int[width * height];
        Arrays.fill(colours, colour);

        //Log.d("Hi", "Trying to colour from " + (i - (width-1) / 2) + ", " + (j - (height-1) / 2) + ". Width x height: " + width + "x" + height);
        bmp.setPixels(colours, 0,       // array to colour in this area, no offset
                width,    // stride, width of what you wanna colour in
                i - (width-1) / 2, // x co-ord of first pixel to colour
                j - (height-1) / 2, // y co-ord of first pixel to colour
                width,    // width of area to colour
                height);   // height of area to colour
    }
}
