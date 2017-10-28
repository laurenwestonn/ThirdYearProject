package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class ImageManipulation {

    public static int masking(Bitmap bmp, int i, int j, int distFromCentre) {
        int pThr = 40; // Threshold which means this point is an edge
        int nThr = 35; // Threshold acceptable for neighbouring points

        int widthToColourAtOnce = distFromCentre * 2 + 1;

        // Get the likelihood that this is an edge,
        // unless it has already been marked blue
        int edgeness = bmp.getPixel(i,j) != Color.BLUE ?
                getEdgeness(bmp, i, j, distFromCentre) :
                Color.BLUE;

        Log.d("Colour", "\tAnother pixel. Edgeness of (" + i + ", " + j + ") is " + edgeness);
        if (edgeness == Color.BLUE) {
            Log.d("Colour", "Blue");
            // If a neighbour set this as a semi edge, leave it be
            return Color.BLUE;
        }
        else if (edgeness < nThr) {
            Log.d("Colour", "Black");
            // Not a strong edge, ignore it
            return Color.BLACK;
        }
        else if (edgeness < pThr ||
                (edgeness >= 40 &&
                        !checkAndSetNbour(bmp, i, j, widthToColourAtOnce, nThr))) {
            Log.d("Hi", "Edgeness of " + edgeness + ". If this is above " + pThr + ", it must'nt have coloured any neighbours");
            Log.d("Colour", "Medium blue");
            // Point is within the neighbouring threshold
            // or is a definite edge with no neighbours, therefore doesn't count
            return edgeness;
        }
        else {
            Log.d("Colour", i + ", " + j + " is White");
            // Point is an edge with neighbours
            return Color.WHITE;
        }

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
        boolean anyColoured = false;
        // For the neighbours we've already seen before,
        // i.e. top three neighbours or left neighbour
        for (int y = j - pointWidth; y <= j; y += pointWidth) {
            for (int x = i - pointWidth; x <= i + pointWidth; x += pointWidth) {
                // Check first four already checked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j - pointWidth) || x == (i - pointWidth))
                        && (x >= 0 && x + ((pointWidth-1)/2) < bmp.getWidth() && y >= 0 && y + ((pointWidth-1)/2) < bmp.getHeight())) {

                    // Get the colour of this point we've already set
                    int neighCol = bmp.getPixel(x, y);
                    // See if there's any edgy neighbours 8-)
                    // Black cannot be an edge, ignore it
                    if (neighCol == Color.BLUE || neighCol == Color.WHITE) {
                        Log.d("Hi", "Seen neighbour (" + x + ", " + y + ") had a pure blue or white worthy edge.");
                        anyColoured = true; // Found an already found neighbouring edge
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
                        anyColoured = true;
                    }
                }
            }
        }

        // For new neighbours
        for (int y = j; y <= j + pointWidth; y += pointWidth) {
            for (int x = i - pointWidth; x <= i + pointWidth; x += pointWidth) {
                // Check last four unchecked neighbours
                // and if the coordinates are within the bitmap
                if ((y == (j + pointWidth) || x == (i + pointWidth))
                        && (x >= 0 && x + ((pointWidth-1)/2) < bmp.getWidth() && y >= 0 && y + ((pointWidth-1)/2) < bmp.getHeight())) {

                    // If this neighbour meets the minimum threshold, the centre has
                    // a neighbouring edge
                    if (getEdgeness(bmp, x, y, (pointWidth-1)/2) > minThreshold) {
                        Log.d("Hi", "Neighbour (" + x + ", " + y + ") had a worthy edge of " + getEdgeness(bmp, x, y, (pointWidth-1)/2));
                        if (getEdgeness(bmp, x, y, (pointWidth-1)/2) < minThreshold + 20) {
                            Log.d("Hi", "Set pixel blue");
                            bmp.setPixel(x, y, 255);
                        }
                        anyColoured = true;
                        continue;
                    } else {
                        Log.d("Hi", "Neighbour (" + x + ", " + y + ") isn't edgy enough.. " + getEdgeness(bmp, x, y, (pointWidth-1)/2));
                    }

                }
            }
        }

        return anyColoured;
    }
}
