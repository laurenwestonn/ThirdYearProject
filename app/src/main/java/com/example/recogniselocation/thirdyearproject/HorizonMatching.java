package com.example.recogniselocation.thirdyearproject;

import android.util.Log;

import com.jjoe64.graphview.series.OnDataPointTapListener;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by LaUrE on 21/11/2017.
 */

public class HorizonMatching {

    static void matchUpHorizons(List<Integer> photoCoords, List<Integer> elevationCoords)
    {
        // Find all minimas and maximas of both horizons
        List<Point> photoMMs = findMaximaMinima(photoCoords,0, 2);
        List<Point> elevationMMs = findMaximaMinima(elevationCoords, 0, 2);

        Log.d(TAG, "matchUpHorizons: photo max mins" + photoMMs);
        Log.d(TAG, "matchUpHorizons: eleva max mins" + elevationMMs);

        // Find best minima maxima pair for the photo - i.e. the biggest difference in height
        // If the first in photo is a maxima, the first two in photoMM will hold max then min
        // If first in photo is minima, first index will be null, next two will be min then max
        List<Point> photoMM = findBestMaximaMinima(photoMMs);

        Log.d("matching", "Best maxima minima of " + photoMMs + "is ");
        Log.d("matching",  "" + photoMM);

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

            // Transform the photo coords to match each
            howWellMatched(photoMM, elevationMM, photoCoords, elevationCoords);

            // Find difference in y between the same x coords of both horizons
        }

        // Find the best matched up set, mark on the map


    }

    // Transforms coordinate system so that transformMM matches with baseMM
    private static double howWellMatched(List<Point> transformMM, List<Point> baseMM, List<Integer> transformCoords, List<Integer> baseCoords)
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

        double diffSum = 0;

        for (i = 0; i < transformCoords.size(); i++) {
            double tX = i * scaleX + translateX;
            double tY = transformCoords.get(i) * scaleY + translateY;

            // Assuming this value is within the elevation coordinates

            // Find the difference between this y and the y of the relevant base coordinates
            diffSum += Math.abs(baseCoords.get((int)tX) - tY);
            Log.d(TAG, "howWellMatched: " + i + ": Diff between " + baseCoords.get((int)tX) + " and " + tY +  " is " + Math.abs(baseCoords.get((int)tX) - tY));
        }

        Log.d("matching", "howWellMatched: Difference of " + diffSum);

        return diffSum;
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
    private static int gradientAhead(List<Integer> coords, int startingIndex, int width)
    {
        if (startingIndex + width - 1 > coords.size())
            Log.e("Hi", "Wont be able to access " + width + " spaces from index " +
                    startingIndex + " when there are " + coords.size() + " coords");

        int sum = 0;
        for (int count = 0; count < width - 1; count++) {
            int thisY = coords.get(startingIndex + count);
            int nextY = coords.get(startingIndex + count + 1);

            Log.d("gradient", "Diff in height between "
                    + nextY + " and " + thisY + " is "
                    + (nextY - thisY));

            sum +=  nextY - thisY;
        }
        return sum;
    }

    // Maxima in even numbers, Minima in odd
    public static List<Point> findMaximaMinima(List<Integer> coords, int noiseThreshold,
                                               int searchWidth) {
        int x = 0;
        int nextGradient = 99999;
        List<Point> maxMin = new ArrayList<>();
        boolean wereGoingUp = true;    //  Whether the hill is heading up or down. Updated.

        while ((x + searchWidth - 1) < coords.size()) {
            Log.d("gradient", " ");
            Log.d("gradient", "Finding coords from " + x);

            // Skip over initial flat areas
            if (x == 0) {
                Log.d("gradient", "First time going through coords, skip initially flat areas");
                x = skipOverInitialFlatAreas(coords, noiseThreshold, searchWidth);

                // Now there's an up or down direction, remember it
                if (x + searchWidth - 1 < coords.size())
                    wereGoingUp = gradientAhead(coords, x, searchWidth) < 0;
                else
                    break;

                Log.d("gradient", "The first direction is " + (wereGoingUp ? "up" : "down"));
            }

            // Keep climbing/descending horizon until you get a straight path
            while (x + searchWidth - 1 < coords.size()
                    && Math.abs(nextGradient = gradientAhead(coords, x, searchWidth)) > noiseThreshold) {

                addAnyPointyPeaks(coords, x, wereGoingUp, nextGradient, maxMin);

                x += searchWidth - 1;
                Log.d("gradient", "Gradient ahead of index " + (x - searchWidth + 1)
                        + " is " + nextGradient + " so check what it is from " + x + " if possible.");
                wereGoingUp = (nextGradient < 0);   // y coords are +ve downwards for bitmaps
            }

            // If this is too close to the edge that we can't see the next gradient, exit
            if (outOfBounds(x, searchWidth, coords))
                break;

            // Now you've got a straight area ahead
            Log.d("gradient", "The gradient ahead of " + x + " is quite flat (" + nextGradient + "), could be maxima/minima");
            int xAtStartOfFlat = x++;  // Increment X so it is at the second flat point

            // Keep going along the flat area until you reach a strong gradient again
            x = reachEndOfFlat(coords, x, searchWidth, noiseThreshold);

            // If that while was exited due to reaching the end of the horizon, exit
            if (outOfBounds(x, searchWidth, coords))
                break;

            // Got a gradient ahead again, see which direction it is to
            // determine if the flat was a maxima or minima

            // Get the middle point of the possible maxima/minima
            //   \________/
            //       ^
            int xAtEndOfFlat = x;
            int centreOfFlat = (int) Math.ceil((double) (xAtEndOfFlat - xAtStartOfFlat) / 2);
            int xOfMaxOrMin = xAtStartOfFlat + centreOfFlat;

            nextGradient = gradientAhead(coords, x, searchWidth);
            wereGoingUp = addAnyMaximaMinima(coords, xOfMaxOrMin, maxMin, wereGoingUp, nextGradient);
        }
        return maxMin;
    }

    private static boolean outOfBounds(int x, int searchWidth, List<Integer> coords) {
        return x + searchWidth - 1 >= coords.size();
    }

    // Keep going along the horizon until you reach a strong gradient again
    private static int reachEndOfFlat(List<Integer> coords, int x, int searchWidth, int noiseThreshold) {
        while ((x + searchWidth - 1) < coords.size() && Math.abs(gradientAhead(coords, x, searchWidth)) <= noiseThreshold)
            x++;
        return x;
    }

    private static boolean addAnyMaximaMinima(List<Integer> coords, int xOfMaxOrMin, List<Point> maxMin, boolean wereGoingUp, int nextGradient)
    {
        //  MAXIMA   _______
        if (wereGoingUp && nextGradient > 0) {          //          /       \
            Log.d("gradient", "Maxima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
            wereGoingUp = !wereGoingUp; // Bug fix to avoid adding duplicate pointy points

            // Ensure that maximas are stored at even indexes
            if (maxMin.size() % 2 == 1)
                maxMin.add(null);

            maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));

            //  MINIMA
        } else if (!wereGoingUp && nextGradient < 0) {      //          \_______/
            Log.d("gradient", "Minima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
            wereGoingUp = !wereGoingUp; // Bug fix to avoid adding duplicate pointy points

            // Ensure that minimas are stored at odd indexes
            if (maxMin.size() % 2 == 0)
                maxMin.add(null);

            maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));

            // \_____   or    _____/
        } else              //       \       /
            Log.d("gradient", "The gradient after is " + nextGradient
                    +  " which doesn't make a max or min considering that before,"
                    + " we were going " + (wereGoingUp ? "up" : "down"));
        return wereGoingUp;
    }

    private static void addAnyPointyPeaks(List<Integer> coords, int x, boolean wereGoingUp, int nextGradient, List<Point> maxMin)
    {
        // Check if the direction has flipped, if so, this is a maxima or minima
        if (wereGoingUp & nextGradient > 0) {
            Log.d("gradient", "Adding Pointy maxima");
            if (maxMin.size() % 2 == 1) // Done this so that maximas are even
                maxMin.add(null);
            maxMin.add(new Point(x, coords.get(x)));
        }
        else if (!wereGoingUp & nextGradient < 0){
            Log.d("gradient", "Adding Pointy minima");
            if (maxMin.size() % 2 == 0) // Done this so that minima are odd
                maxMin.add(null);
            maxMin.add(new Point(x, coords.get(x)));
        }
    }

    private static int skipOverInitialFlatAreas(List<Integer> coords, int noiseThreshold, int searchWidth)
    {
        int x = 0;
        while (x + searchWidth - 1 < coords.size()
                && Math.abs(gradientAhead(coords, x, searchWidth)) < noiseThreshold)
            // This one is flat too, carry on
            x += searchWidth;
        return x;
    }
}
