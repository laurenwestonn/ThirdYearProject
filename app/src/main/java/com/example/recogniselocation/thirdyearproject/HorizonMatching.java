package com.example.recogniselocation.thirdyearproject;

import android.graphics.Color;
import android.util.Log;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

class HorizonMatching {
    static double graphHeight;
    private static boolean debug = false;   // Can't log when testing

    // Returns the horizon you manage to match up from the photo as a series so can plot on graph
    static Horizon matchUpHorizons(List<Point> photoCoords, List<Point> elevationCoords) {
        // Find all minimas and maximas of both horizons
        MaximasMinimas photoMMsObj = findMaximasMinimas(photoCoords, true);
        List<Point> photoMMs = photoMMsObj.getMaximasMinimas();
        if (debug)
            Log.d(TAG, "matchUpHorizons: Now check elevationMMs from coords: " + elevationCoords);
        MaximasMinimas elevMMsObj = findMaximasMinimas(elevationCoords, false);// Todo: This better. Using a looser(...is it?) threshold here because my edge detection is too thick to notice subtle dips
        List<Point> elevationMMs = elevMMsObj.getMaximasMinimas();
        List<Integer> elevationMMsIndexes = elevMMsObj.getIndexes();

        if (photoMMs == null || elevationMMs == null                        // None found
                ||photoMMs.size() < 2 || elevationMMs.size() < 2            // Just a maxima found
                || photoMMs.get(0) == null && photoMMs.size() <= 2          // Just a minima found
                || elevationMMs.get(0) == null && elevationMMs.size() == 2) // Just a minima found
            Log.e(TAG, "matchUpHorizons: Didn't find enough maximas and minimas; "
                    + "Elevation ones: " + elevationMMs + "\tPhoto ones: " + photoMMs);

        if (debug) {
            Log.d(TAG, "matchUpHorizons: photo max mins" + photoMMs);
            Log.d(TAG, "matchUpHorizons: eleva max mins" + elevationMMs);
        }

        // Find best minima maxima pair for the photo - i.e. the biggest difference in height
        // If the first in photo is a maxima, the first two in photoMM will hold max then min
        // If first in photo is minima, first index will be null, next two will be min then max
        List<Point> photoMM = findBestMaximaMinima(photoMMs);

        if (debug) {
            Log.d("matching", "Best maxima minima of " + photoMMs + "is ");
            Log.d("matching", "\t\t\t\t\t" + photoMM);
        }

        // Store how accurate each min max pairing is
        List<Matching> allMatchings = new ArrayList<>();

        // Go through each maxima minima pair from the elevations

        if (debug)
            Log.d(TAG, "matchUpHorizons: Going to check each elevation min max pair from " + elevationMMs);

        // Start at the odd index if this starts with a minimum
        int i = getFirstElevationIndex( photoMM.size() == 2,
                elevationMMs.get(0) != null);
        for (; i < elevationMMs.size() - 1; i += 2) {
            // Store this elevation maxima minima pair into elevationMM
            // As with the photoMM, this could hold 2 or three values
            List<Point> elevationMM = getTheNextElevationMM(photoMM, elevationMMs, i);

            if (elevationMM != null){
                // Only look at this pair if they're fairly far apart - we'll want mountains not dips
                double signifWidth = elevationCoords.get(elevationCoords.size()-1).getX() / 8;
                if (elevationMM.get(0) == null) {   // Starts with a minima
                    if ((elevationMM.get(2).getX() - elevationMM.get(1).getX()) < signifWidth) {
                        if (debug)
                            Log.d(TAG, "matchUpHorizons: the width difference between this pair of elevations max/min \n"
                                    + elevationMM + " and the chosen photo max/min \n" + photoMM + " is only \n"
                                    + (elevationMM.get(2).getX() - elevationMM.get(1).getX())
                                    + " which isn't *significant* -> " + signifWidth);
                        continue;
                    }
                } else if ((elevationMM.get(1).getX() - elevationMM.get(0).getX()) < signifWidth) { //maxima
                    if (debug)
                        Log.d(TAG, "matchUpHorizons: the width difference between this pair of elevations max/min \n"
                                + elevationMM + " and the chosen photo max/min \n" + photoMM + " is only \n"
                                + (elevationMM.get(1).getX() - elevationMM.get(0).getX())
                                + " which isn't significant - " + signifWidth);
                    continue;
                }
            } else
                Log.e(TAG, "matchUpHorizons: Didn't find a elevation MM pair. Try a broader search");


            if (debug)
                Log.d("matching", "Checking elevation max min " + elevationMM);
            allMatchings.add(howWellMatched(photoMM, elevationMM, photoCoords, elevationCoords));
        }

        Log.d(TAG, "matchUpHorizons: All matchings: " + allMatchings);

        // Find the best matching
        Matching bestMatching = allMatchings.get(0);

        for (i = 1; i < allMatchings.size(); i++)
            if (allMatchings.get(i).getDifference() < bestMatching.getDifference())
                bestMatching = allMatchings.get(i);

        Log.d(TAG, "matchUpHorizons: The best matching is " + bestMatching);

        return new Horizon(photoMMs, elevMMsObj.getIndexes(), bestMatching.getPhotoCoords(),
                bestMatching.getPhotoSeries());
    }

    private static List<Point> getTheNextElevationMM(List<Point> photoMaxMinPair, List<Point> elevationMMs, int i)
    {
        if (i + 1 >= elevationMMs.size()) {
            Log.e(TAG, "mUH: Can't access index " + (i + 1) + ". Size is " + elevationMMs.size()
                    + ". Try a broader search as could only find these elevation max mins: " + elevationMMs);
            return null;
        } else {
            List<Point> elevationMaxMinPair = new ArrayList<>();
            if (i % 2 == 1)                     // If odd index, must start with a minima
                elevationMaxMinPair.add(null);  // So null out the first (would-be max) index

            elevationMaxMinPair.add(elevationMMs.get(i));       // add the first max/min
            elevationMaxMinPair.add(elevationMMs.get(i + 1));   // add the first min/max after the max/min
            return elevationMaxMinPair;
        }
    }

    // Depending on if maxima or minima was found first, decide which elevation index to start at
    public static int getFirstElevationIndex(boolean photoInTheOrderMaxMin, boolean elevaInTheOrderMaxMin) {

        if (photoInTheOrderMaxMin)    // Found a max, then min in the photo's horizon
            return elevaInTheOrderMaxMin ? 0 : 2;    // Get the index of the first maxima in the elevations
        else // Came across a minima first in the photo's horizon
            return 1;  // The index of the first minima of the elevations
    }

    // Transforms coordinate system so that transformMM matches with baseMM
    private static Matching howWellMatched(List<Point> transformMM, List<Point> baseMM,
                                           List<Point> transformCoords, List<Point> baseCoords)
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

        //Log.d("matching", "Scale x by " + scaleX + " and translate by " + translateX);
        //Log.d("matching", "Scale y by " + scaleY + " and translate by " + translateY);


        int numMatched = 0;
        double diffSum = 0;
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        List<Point> coords = new ArrayList<>(); // Hold the series as points as is Parcelable
        Log.d(TAG, "howWellMatched: Matching photo max mins " + transformMM + " to elevation mms " + baseMM);
        Log.d(TAG, "howWellMatched: Photo coords are " + transformCoords);
        Log.d(TAG, "howWellMatched: Elev coords are " + baseCoords);
        for (Point c : transformCoords)
            if (c != null) { // Only check where an edge was detected

                // Transform this coordinate
                int tX = (int) (c.getX() * scaleX + translateX);
                double tY = c.getY() * scaleY + translateY;
                Log.d(TAG, "howWellMatched:Translated " + c + " to " + tX + ", " + tY);

                // Get diff in height if this transformed coords can be found in the other coords
                Point matchingBasePoint;
                if (tX >= 0 && tX < baseCoords.get(baseCoords.size() - 1).getX()
                        && (matchingBasePoint = findPointWithX(baseCoords, tX)) != null) {
                    // Find the difference between the heights of both points with common x values
                    diffSum += Math.abs(matchingBasePoint.getY() - tY);
                    Log.d(TAG, "howWellMatched: Diff between " + matchingBasePoint.getY() + " and " + tY + " is " + Math.abs(matchingBasePoint.getY() - tY));

                    numMatched++;
                }

                // Build up a series to plot
                series.appendData(new DataPoint(tX, tY), true, transformCoords.size());
                series.setColor(Color.BLACK);
                coords.add(new Point(tX, tY));
                Log.d(TAG, "howWellMatched: Add to series as " + tX + ", " + tY);
            }
        Log.d(TAG, " ");
        return new Matching(coords, series, diffSum / numMatched);
    }

    // Return the coordinate that has this x value
    private static Point findPointWithX(List<Point> coords, int x) {
        for (Point coord : coords)
            if ((int)coord.getX() >= x - 10 && (int)coord.getX() <= x + 10)
                return coord;

        //Log.e(TAG, "findPointWithX: Can't find a coordinate with an x of " + x + " in " + coords.toString());
        return null;
    }

    // maximasMinimas is a list of the positions of maximas and minimas
    // Even indexes represent maximum points, odd; minimas
    // The 'best' is the greatest difference in height
    private static List<Point> findBestMaximaMinima(List<Point> maximasMinimas)
    {
        List<Point> bestMaximaMinima = new ArrayList<>();

        double maxYDiff, thisYDiff;
        maxYDiff = Integer.MIN_VALUE;  // Low number so it gets updated by any height
        int bestIndex = -1;
        int i = (maximasMinimas.get(0) == null) ? 1 : 0; // skip null index

        // For each pair of max-min / min-max find the greatest difference in height
        for (; i < maximasMinimas.size() - 1; i++) {
            //Log.d(TAG, "findBestMaximaMinima: Checking pair " + maximasMinimas.get(i)
              //                                              + " and " + maximasMinimas.get(i+1));
            if (maximasMinimas.get(i) != null
                    && (thisYDiff = Math.abs(maximasMinimas.get(i).getY() - maximasMinimas.get(i+1).getY()))
                    > maxYDiff) {
                //Log.d(TAG, "findBestMaximaMinima: Yes this difference (" + thisYDiff + ") is bigger than our current max difference " + maxYDiff);
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
    // In the event that 'width' number of coordinates ahead of this one doesn't exist,
    // it'll return the gradient of the remaining coords. Gradient is 0 if this is the last coord
    public static double gradientAhead(List<Point> coords, int startingIndex, int searchWidth)
    {
        double sum = 0;
        int offset = 0;
        int thisY, nextY;

        for (; offset < searchWidth; offset++) {
            if (startingIndex + offset + 1 < coords.size()) {
                thisY = (int) coords.get(startingIndex + offset).getY();
                nextY = (int) coords.get(startingIndex + offset + 1).getY();
                sum +=  nextY - thisY;
            } else
                break;
        }

        // Get the average of any existing ones ahead
        if (offset > 0)
            sum /= offset;

        if (debug)
            Log.d(TAG, "gradientAhead: The gradient ahead of " + coords.get(startingIndex)
                    + " is " + sum);
        return sum;
    }

    // Find all maximum and minimum point of a line when given the coordinates
    // Returned in the form where maximas are are even indexes, minima at odd indexes
    public static MaximasMinimas findMaximasMinimas(List<Point> coords, boolean loosenThreshold)
    {
        int arrayIndex = 0;
        double nextGradient = Integer.MAX_VALUE;
        MaximasMinimas mms = new MaximasMinimas(new ArrayList<Point>(), new ArrayList<Integer>());
        boolean wereGoingUp = true;    //  Whether the hill is heading up or down. Updated.
        double threshold = getThreshold(coords) * (loosenThreshold ? 4 : 1);
        int searchWidth = getSearchWidth(coords);
        if (debug)
            Log.d(TAG, "findMaximasMinimas: Using a noise threshold of " + threshold + " and searching a width of " + searchWidth);

        while ((arrayIndex + searchWidth - 1) < coords.size()) { // While there is a horizon ahead
            if (debug) {
                Log.d("gradient", " ");
                Log.d("gradient", "findMaximasMinimas: Ahead of " + coords.get(arrayIndex) + ", index "
                        + arrayIndex + " is steepness " + gradientAhead(coords, arrayIndex, searchWidth));
            }

            // Skip over any flat areas at the start
            if (arrayIndex == 0) {
                arrayIndex = skipOverInitialFlatAreas(coords, threshold, searchWidth);
                if (debug)
                    Log.d("gradient", "findMaximasMinimas: Skipped flat area until " + coords.get(arrayIndex) + ", index "
                        + arrayIndex);

                // Now there's an up or down direction, remember it
                if (arrayIndex + searchWidth - 1 < coords.size())
                    wereGoingUp = gradientAhead(coords, arrayIndex, searchWidth) < 0;
                else
                    break;
                if (debug)
                    Log.d("gradient", "The first direction is " + (wereGoingUp ? "up" : "down"));
            }

            // Keep climbing/descending horizon until you get a straight path
            while (arrayIndex + searchWidth - 1 < coords.size()
                    && Math.abs(nextGradient = gradientAhead(coords, arrayIndex, searchWidth)) > threshold) {

                mms = addAnyPointyPeaks(coords, arrayIndex, wereGoingUp, nextGradient, mms, threshold);
                wereGoingUp = (nextGradient < 0);   // y coords are +ve downwards for bitmaps

                if(debug) {
                    Log.d("gradient", "findMaximasMinimas: Ahead of " + coords.get(arrayIndex) + ", index "
                            + arrayIndex + " is " + nextGradient + " which is significantly "
                            + (wereGoingUp ?  "up" : "down") +", so check what it is from "
                            + coords.get(arrayIndex + searchWidth - 1) + ", index "
                            + (arrayIndex + searchWidth - 1) + " if possible.");
                }
                arrayIndex += 1;    // Check the gradient from EVERY point so as to not miss any min/max
            }

            // If this is too close to the edge that we can't see the next gradient, exit
            if (outOfBounds(arrayIndex, searchWidth, coords))
                break;

            // Now you've got a straight area ahead
            if (debug)
                Log.d("gradient", "findMaximasMinimas: Ahead of " + coords.get(arrayIndex) + ", index "
                        + arrayIndex + " is " + nextGradient + " which is quite flat, could be max/min");
            int iAtStartOfFlat = arrayIndex++;  // Increment index so it is at the second flat point

            // Keep going along the flat area until you reach a strong gradient again
            arrayIndex = reachEndOfFlat(coords, arrayIndex, searchWidth, threshold);

            // If that while was exited due to reaching the end of the horizon, exit
            if (outOfBounds(arrayIndex, searchWidth, coords))
                break;

            if (debug)
                Log.d("gradient", "findMaximasMinimas: Skipped flat area until " + coords.get(arrayIndex) + ", index "
                        + arrayIndex);

            // Got a gradient ahead again, see which direction it is to
            // determine if the flat was a maxima or minima

            // Get the middle point of the possible maxima/minima
            //   \________/
            //       ^
            int iEndOfFlat = arrayIndex;
            int centreOfFlat = (int) Math.ceil((double) (iEndOfFlat - iAtStartOfFlat) / 2);
            int iOfMaxOrMin = iAtStartOfFlat + centreOfFlat;

            nextGradient = gradientAhead(coords, arrayIndex, searchWidth);
            // Updates the mms with any new maximas or minimas found
            wereGoingUp = addAnyMaximaMinima(coords, iOfMaxOrMin, mms, wereGoingUp, nextGradient);
        }
        return mms;
    }

    public static int getSearchWidth(List<Point> coords) {
        return (int) Math.floor(coords.size() / 50) + 1;  // At least 1
    }

    // Find a value that can be used to determine if an area is an in/decline or just flat
    // based on the coordinates
    private static double getThreshold(List<Point> coords) {

        // Surely the smallest difference is 1? Use this. No need to adjust threshold
        // either as this WILL give results
        return 1;

        /*
        Point smallestY, biggestY;
        smallestY = biggestY = null;

        for (Point coord : coords)
            if (coord != null) {
                if (smallestY == null || coord.getY() < smallestY.getY())
                    smallestY = coord;
                else if (biggestY == null || coord.getY() > biggestY.getY())
                    biggestY = coord;
            }
            
        return (biggestY.getY() - smallestY.getY()) / 1000;*/
    }

    private static boolean outOfBounds(int index, int searchWidth, List<Point> coords) {
        return index + searchWidth - 1 >= coords.size();
    }

    // Keep going along the horizon until you reach a strong gradient again
    private static int reachEndOfFlat(List<Point> coords, int index, int searchWidth, double noiseThreshold) {
        while ((index + searchWidth - 1) < coords.size() && Math.abs(gradientAhead(coords, index, searchWidth)) <= noiseThreshold)
            index++;
        return index;
    }

    private static boolean addAnyMaximaMinima(List<Point> coords, int maxOrMinIndex, MaximasMinimas mms,
                                              boolean wereGoingUp, double nextGradient)
    {
        boolean areNowGoingUp = nextGradient < 0;
                                                        //  MAXIMA   _______
        if (wereGoingUp && !areNowGoingUp) {            //          /       \
            if (debug)
                Log.d("gradient", "Maxima found at " + coords.get(maxOrMinIndex).toString());
            wereGoingUp = false; // Bug fix to avoid adding duplicate pointy points

            // Ensure that maximas are stored at even indexes
            if (mms.getMaximasMinimas().size() % 2 == 1)
                mms.getMaximasMinimas().add(null);

            mms.getMaximasMinimas().add(coords.get(maxOrMinIndex));
            mms.getIndexes().add(maxOrMinIndex);

                                                            //  MINIMA
        } else if (!wereGoingUp && areNowGoingUp) {         //          \_______/
            if (debug)
                Log.d("gradient", "Minima found at " + coords.get(maxOrMinIndex).toString());
            wereGoingUp = true; // Bug fix to avoid adding duplicate pointy points

            // Ensure that minimas are stored at odd indexes
            if (mms.getMaximasMinimas().size() % 2 == 0)
                mms.getMaximasMinimas().add(null);

            mms.getMaximasMinimas().add(coords.get(maxOrMinIndex));
            mms.getIndexes().add(maxOrMinIndex);

                            // \_____   or    _____/
        } else              //       \       /
            if (debug)
                Log.d("gradient", "The gradient after is " + nextGradient
                        +  " which doesn't make a max or min considering that before,"
                        + " we were going " + (wereGoingUp ? "up" : "down"));
        return wereGoingUp;
    }

    // Todo: Fix this, not going up doesn't necessarily mean not going down so 'weregoingUp' bool makes no sense
    // Todo: Pass through prevGradient, compare it against the threshold
    private static MaximasMinimas addAnyPointyPeaks(List<Point> coords, int arrayIndex, boolean wereGoingUp,
                                          double nextGradient, MaximasMinimas mms, double threshold)
    {
        // Check if the direction has flipped, if so, this is a maxima or minima
        if (wereGoingUp & nextGradient > threshold) {
            if (debug)
                Log.d("gradient", "Adding Pointy maxima at " + coords.get(arrayIndex)
                        + ", index " + arrayIndex + ", where the gradient ahead is also significant at "
                        + nextGradient + ". We're checking that it's more than " + threshold);
            if (mms.getMaximasMinimas().size() % 2 == 1) // Done this so that maximas are at even indexes
                mms.getMaximasMinimas().add(null);
        } else if (!wereGoingUp & nextGradient < -threshold) {
            if (debug)
                Log.d("gradient", "Adding Pointy minima at " + coords.get(arrayIndex) + ", index " + arrayIndex);
            if (mms.getMaximasMinimas().size() % 2 == 0) // Done this so that minima are odd
                mms.getMaximasMinimas().add(null);
        }
        if ((wereGoingUp & nextGradient > threshold) || (!wereGoingUp & nextGradient < -threshold)) {
            mms.getMaximasMinimas().add(coords.get(arrayIndex));
            mms.getIndexes().add(arrayIndex);
        }

        return mms;
    }

    private static int skipOverInitialFlatAreas(List<Point> coords, double threshold, int searchWidth)
    {
        int index = 0;
        while (coords.size() > index && coords.get(index) != null // There is an edge here
                && index + searchWidth - 1 < coords.size()  // We are still within the photo
                && Math.abs(gradientAhead(coords, index, searchWidth)) < threshold) // Ahead is 'flat'
            index += searchWidth;   // Carry on
        return index;
    }

    // True if the next 'searchWidth' coords after the index are all there
    public static boolean aheadExists(List<Point> coords, int index, int searchWidth)
    {
        // In case you're mad enough to ask to check no points ahead
        if (searchWidth == 0)
            return (index < coords.size());

        // Check that there are that many coordinates ahead
        if (index + searchWidth >= coords.size())
            return false;

        // Check none of the coordinates are null
        for (int i = index; i <= index + searchWidth; i++)
            if (coords.get(i) == null)
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

    static List<Point> convertToPoints(List<Integer> intList, int pointWidth)
    {
        List<Point> pointList = new ArrayList<>();
        for (int i = 0; i < intList.size(); i++)
            if (intList.get(i) != -1)
                pointList.add(new Point(i * pointWidth + (pointWidth-1)/2, intList.get(i)));
            else
                pointList.add(null);

        return pointList;
    }
}
