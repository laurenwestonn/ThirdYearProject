package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

class HorizonMatching {

    private static int width = ImageToDetect.fineWidth;
    private static int height = ImageToDetect.fineHeight;

    static void matchUpHorizons(List<Point> photoCoords, List<Point> elevationCoords,
                                                                        Bitmap bmp, Activity a)
    {
        // Find all minimas and maximas of both horizons
        List<Point> photoMMs = findMaximaMinima(photoCoords,15, 5);
        List<Point> elevationMMs = findMaximaMinima(elevationCoords, 15, 3);

        Log.d(TAG, "matchUpHorizons: photo max mins" + photoMMs);
        Log.d(TAG, "matchUpHorizons: eleva max mins" + elevationMMs);

        // Find best minima maxima pair for the photo - i.e. the biggest difference in height
        // If the first in photo is a maxima, the first two in photoMM will hold max then min
        // If first in photo is minima, first index will be null, next two will be min then max
        List<Point> photoMM = findBestMaximaMinima(photoMMs);

        Log.d("matching", "Best maxima minima of " + photoMMs + "is ");
        Log.d("matching",  "\t\t\t\t\t" + photoMM);

        // Mark these best ones on the image
        markMaximaMinimaOnPhoto(photoMM, bmp, a);

        // Go through each maxima minima pair from the elevations
        for (int i = 0; i < elevationMMs.size() - 1; i += 2) {
            // Store this elevation maxima minima pair into elevationMM
            // As with the photoMM, this could hold 2 or three values
            List<Point> elevationMM = new ArrayList<>();

            if (i == 0 && photoMM.size() == 2) { // From the photo we found a maxima then minima
                // Elevation's first max is from index 0 unless the elevations started with
                // a minima
                if (elevationMMs.get(0) == null)
                    i = 2;

            } else if (i == 0 && photoMM.size() == 3) { // Photo had a minima then maxima
                i = 1;                  // Start at the first min
                elevationMM.add(null);  // 'null' the first even index as it represents maxima
            }

            elevationMM.add(elevationMMs.get(i));       // add the first max/min
            elevationMM.add(elevationMMs.get(i + 1));   // add the first min/max after the max/min

            Log.d("matching", "Checking elevation max min " + elevationMM);

            double diff = howWellMatched(photoMM, elevationMM, photoCoords, elevationCoords);
            Log.d("matching", "howWellMatched: Each point had an average diff of "
                    + diff);
        }

        // Find the best matched up set, mark on the map


    }

    // Colour maxima in red, minima in blue
    private static void markMaximaMinimaOnPhoto(List<Point> photoMM, Bitmap bmp, Activity a)
    {
        // If the first is a maxima, start from the first index
        int maxInd = photoMM.size() == 2 ? 0 : 2;

        ImageManipulation.colourArea(bmp,   (int) photoMM.get(maxInd).getX(),
                                            (int) photoMM.get(maxInd).getY(),
                                            Color.RED, width * 2, height * 2);
        ImageManipulation.colourArea(bmp,   (int) photoMM.get(1).getX(),
                                            (int) photoMM.get(1).getY(),
                                            Color.BLUE, width * 2, height * 2);

        // Put this on the image button
        ImageButton imageButton = (ImageButton) a.findViewById(R.id.edgeDetection);
        BitmapDrawable drawable = new BitmapDrawable(a.getResources(), bmp);
        imageButton.setBackground(drawable);
    }

    // Transforms coordinate system so that transformMM matches with baseMM
    private static double howWellMatched(List<Point> transformMM, List<Point> baseMM, List<Point> transformCoords, List<Point> baseCoords)
    {
        double scaleX, scaleY, translateX, translateY;

        // Start from index 1 if this is a minima - maxima pair
        int i = (baseMM.get(0) == null) ? 1 : 0;

        /////////////SCALE//////////////
        scaleX = (baseMM.get(i).getX() - baseMM.get(i+1).getX())
                / (transformMM.get(i).getX() - transformMM.get(i+1).getX());
        scaleY = (baseMM.get(i).getY() - baseMM.get(i+1).getY())
                / (transformMM.get(i).getY() - transformMM.get(i+1).getY());

        ///////////TRANSLATE///////////
        translateX = baseMM.get(i).getX() - transformMM.get(i).getX() * scaleX;
        translateY = baseMM.get(i).getY() - transformMM.get(i).getY() * scaleY;


        Log.d("matching", "Scale x by " + scaleX + " and translate by " + translateX);
        Log.d("matching", "Scale y by " + scaleY + " and translate by " + translateY);


        int numMatched = 0;
        double diffSum = 0;

        for (Point coord : transformCoords)
            if (coord.getY() != -1) { // Only check where an edge was detected

                int tX = (int)(coord.getX() * scaleX + translateX);
                double tY = coord.getY() * scaleY + translateY;
                Log.d(TAG, "howWellMatched: Translated " + coord + " to (" + tX + ", " + tY + ")");

                Point matchingBasePoint;

                // If this transformed coords can be found in the other coordinate system
                // Todo: Photo coords should all be within the elevation coords, check this
                if (tX >= 0 && tX < baseCoords.get(baseCoords.size()-1).getX() && (matchingBasePoint = findPointWithX(baseCoords, tX)) != null) {
                    // Find the difference between the heights of both points with common x values
                    diffSum += Math.abs(matchingBasePoint.getY() - tY);
                    Log.d(TAG, "howWellMatched: Diff between " + matchingBasePoint.getY() + " and " + tY + " is " + Math.abs(matchingBasePoint.getY() - tY));

                    numMatched++;
                }
            }

        return diffSum / numMatched;
    }

    // Return the coordinate that has this x value
    private static Point findPointWithX(List<Point> coords, int x) {
        for (Point coord : coords)
            if ((int)coord.getX() >= x - 10 && (int)coord.getX() <= x + 10)
                return coord;

        Log.e(TAG, "findPointWithX: Can't find a coordinate with an x of " + x + " in " + coords.toString());
        return null;
    }

    // maximasMinimas is a list of the positions of maximas and minimas
    // Even indexes represent maximum points, odd; minimas
    // The 'best' is the greatest difference in height
    private static List<Point> findBestMaximaMinima(List<Point> maximasMinimas)
    {
        List<Point> bestMaximaMinima = new ArrayList<>();

        double maxYDiff, thisYDiff;
        maxYDiff = -1;  // Low number so it gets updated by any height
        int bestIndex = -1;
        int i = (maximasMinimas.get(0) == null) ? 1 : 0; // skip null index

        // For each pair of max-min / min-max find the greatest difference in height
        for (; i < maximasMinimas.size() - 1; i++) {
            Log.d(TAG, "findBestMaximaMinima: Checking pair " + maximasMinimas.get(i)
                                                            + " and " + maximasMinimas.get(i+1));
            if ((thisYDiff = Math.abs(maximasMinimas.get(i).getY() - maximasMinimas.get(i+1).getY())) > maxYDiff) {
                Log.d(TAG, "findBestMaximaMinima: Yes this difference (" + thisYDiff + ") is bigger than our current max difference " + maxYDiff);
                maxYDiff = thisYDiff;
                bestIndex = i;
            }
        }

        // If the best index is of a minima, pad out the first even index of the result
        if (bestIndex % 2 == 1)
            bestMaximaMinima.add(null);

        bestMaximaMinima.add(maximasMinimas.get(bestIndex));
        bestMaximaMinima.add(maximasMinimas.get(bestIndex+1));

        return bestMaximaMinima;
    }

    // Gets the average difference in y between the next 'width' coords
    private static int gradientAhead(List<Point> coords, int startingIndex, int width)
    {
        // If there are no coordinates ahead, say that there is no gradient
        if (!aheadExists(coords, startingIndex, width))
            return 0;

        if (startingIndex + width - 1 > coords.size())
            Log.e("Hi", "Wont be able to access " + width + " spaces from index " +
                    startingIndex + " when there are " + coords.size() + " coords");

        int sum = 0;
        for (int count = 0; count < width - 1; count++) {
            int thisY = (int) coords.get(startingIndex + count).getY();
            int nextY = (int) coords.get(startingIndex + count + 1).getY();

            //Log.d("gradient", "Diff in height between "  + nextY + " and " + thisY
            // + " is " + (nextY - thisY));
            sum +=  nextY - thisY;
        }
        //Log.d(TAG, "gradientAhead: The gradient ahead of " + coords.get(startingIndex)
        //     + " is " + sum);
        return sum;
    }

    // Maxima in even numbers, Minima in odd
    private static List<Point> findMaximaMinima(List<Point> coords, int noiseThreshold,
                                                int searchWidth) {
        int arrayIndex = 0;
        int nextGradient = Integer.MAX_VALUE;
        List<Point> maxMin = new ArrayList<>();
        boolean wereGoingUp = true;    //  Whether the hill is heading up or down. Updated.

        while ((arrayIndex + searchWidth - 1) < coords.size()) { // While there is a horizon ahead
            Log.d("gradient", " ");
            Log.d("gradient", "Searching coords from " + coords.get(arrayIndex));

            // Skip over any flat areas at the start
            if (arrayIndex == 0) {
                Log.d("gradient", "First time going through coords, skip initially flat areas");
                arrayIndex = skipOverInitialFlatAreas(coords, noiseThreshold, searchWidth);

                // Now there's an up or down direction, remember it
                if (arrayIndex + searchWidth - 1 < coords.size())
                    wereGoingUp = gradientAhead(coords, arrayIndex, searchWidth) < 0;
                else
                    break;

                Log.d("gradient", "The first direction is " + (wereGoingUp ? "up" : "down"));
            }

            // Keep climbing/descending horizon until you get a straight path
            while (arrayIndex + searchWidth - 1 < coords.size()
                    && Math.abs(nextGradient = gradientAhead(coords, arrayIndex, searchWidth)) > noiseThreshold) {

                addAnyPointyPeaks(coords, arrayIndex, wereGoingUp, nextGradient, maxMin);

                Log.d("gradient", "Gradient ahead of " + coords.get(arrayIndex)
                        + " is significant at " + nextGradient + " so check what is from "
                        + coords.get(arrayIndex + searchWidth - 1) + " if possible.");
                arrayIndex += searchWidth - 1;
                wereGoingUp = (nextGradient < 0);   // y coords are +ve downwards for bitmaps
                Log.d("gradient", "We've just gone " + (wereGoingUp ? "up" : "down"));
            }

            // If this is too close to the edge that we can't see the next gradient, exit
            if (outOfBounds(arrayIndex, searchWidth, coords))
                break;

            // Now you've got a straight area ahead
            Log.d("gradient", "The gradient ahead of " + coords.get(arrayIndex)
                    + " is quite flat at " + nextGradient + ". Could be maxima/minima");
            int iAtStartOfFlat = arrayIndex++;  // Increment index so it is at the second flat point

            // Keep going along the flat area until you reach a strong gradient again
            arrayIndex = reachEndOfFlat(coords, arrayIndex, searchWidth, noiseThreshold);

            // If that while was exited due to reaching the end of the horizon, exit
            if (outOfBounds(arrayIndex, searchWidth, coords))
                break;

            // Got a gradient ahead again, see which direction it is to
            // determine if the flat was a maxima or minima

            // Get the middle point of the possible maxima/minima
            //   \________/
            //       ^
            int iEndOfFlat = arrayIndex;
            int centreOfFlat = (int) Math.ceil((double) (iEndOfFlat - iAtStartOfFlat) / 2);
            int iOfMaxOrMin = iAtStartOfFlat + centreOfFlat;

            nextGradient = gradientAhead(coords, arrayIndex, searchWidth);
            wereGoingUp = addAnyMaximaMinima(coords, iOfMaxOrMin, maxMin, wereGoingUp, nextGradient);
        }
        return maxMin;
    }

    private static boolean outOfBounds(int index, int searchWidth, List<Point> coords) {
        return index + searchWidth - 1 >= coords.size();
    }

    // Keep going along the horizon until you reach a strong gradient again
    private static int reachEndOfFlat(List<Point> coords, int index, int searchWidth, int noiseThreshold) {
        while ((index + searchWidth - 1) < coords.size() && Math.abs(gradientAhead(coords, index, searchWidth)) <= noiseThreshold)
            index++;
        return index;
    }

    private static boolean addAnyMaximaMinima(List<Point> coords, int maxOrMinIndex, List<Point> maxMin, boolean wereGoingUp, int nextGradient)
    {
                                                        //  MAXIMA   _______
        if (wereGoingUp && nextGradient > 0) {          //          /       \
            Log.d("gradient", "Maxima found at " + coords.get(maxOrMinIndex).toString());
            wereGoingUp = false; // Bug fix to avoid adding duplicate pointy points

            // Ensure that maximas are stored at even indexes
            if (maxMin.size() % 2 == 1)
                maxMin.add(null);

            maxMin.add(coords.get(maxOrMinIndex));

                                                            //  MINIMA
        } else if (!wereGoingUp && nextGradient < 0) {      //          \_______/
            Log.d("gradient", "Minima found at " + coords.get(maxOrMinIndex).toString());
            wereGoingUp = true; // Bug fix to avoid adding duplicate pointy points

            // Ensure that minimas are stored at odd indexes
            if (maxMin.size() % 2 == 0)
                maxMin.add(null);

            maxMin.add(coords.get(maxOrMinIndex));

                            // \_____   or    _____/
        } else              //       \       /
            Log.d("gradient", "The gradient after is " + nextGradient
                    +  " which doesn't make a max or min considering that before,"
                    + " we were going " + (wereGoingUp ? "up" : "down"));
        return wereGoingUp;
    }

    private static void addAnyPointyPeaks(List<Point> coords, int arrayIndex, boolean wereGoingUp, int nextGradient, List<Point> maxMin)
    {
        // Check if the direction has flipped, if so, this is a maxima or minima
        if (wereGoingUp & nextGradient > 0) {
            Log.d("gradient", "Adding Pointy maxima");
            if (maxMin.size() % 2 == 1) // Done this so that maximas are even
                maxMin.add(null);

            maxMin.add(coords.get(arrayIndex));

        } else if (!wereGoingUp & nextGradient < 0) {
            Log.d("gradient", "Adding Pointy minima");
            if (maxMin.size() % 2 == 0) // Done this so that minima are odd
                maxMin.add(null);

            maxMin.add(coords.get(arrayIndex));
        }
    }

    private static int skipOverInitialFlatAreas(List<Point> coords, int noiseThreshold, int searchWidth)
    {
        int index = 0;
        while (index + searchWidth - 1 < coords.size()
                && Math.abs(gradientAhead(coords, index, searchWidth)) < noiseThreshold)
            // This one is flat too, carry on
            index += searchWidth;
        return index;
    }

    // True if the next 'searchWidth' coords after the index are all there (i.e. not set to -1)
    private static boolean aheadExists(List<Point> coords, int index, int searchWidth)
    {
        for (int i = index; i < index + searchWidth; i++)
            if (coords.get(i).getY() == -1)
                return false;
        return true;
    }

    static List<Integer> removeDimensionFromCoords(List<List<Integer>> coords2D)
    {
        List<Integer> coords1D = new ArrayList<>();

        for (List<Integer> col : coords2D)
            if (col.size() > 0)
                coords1D.add(col.get(0));
            else
                coords1D.add(-1);
        return coords1D;
    }

    static List<Point> convertToPoints(List<Integer> intList, int width)
    {
        List<Point> pointList = new ArrayList<>();
        for (int i = 0; i < intList.size(); i++)
            pointList.add(new Point(i * width + (width-1)/2, intList.get(i)));

        return pointList;
    }
}
