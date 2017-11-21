package com.example.recogniselocation.thirdyearproject;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LaUrE on 21/11/2017.
 */

public class HorizonMatching {

    // Gets the average difference in y between the next 'width' coords
    public static int gradientAhead(List<Integer> coords, int startingIndex, int width)
    {
        if (startingIndex + width > coords.size()) {
            Log.e("Hi", "Wont be able to access " + width + " spaces from index " +
                    startingIndex + " when the width of the coords is " + coords.size());

        }

        int sum = 0;
        for (int count = 0; count < width - 1; count++) {
            Log.d("gradient", "Diff between " + coords.get(startingIndex+1+count) + " and "
                    + coords.get(startingIndex+count) + " is "
                    + (coords.get(startingIndex + 1+count) - coords.get(startingIndex+count)));
            sum +=  coords.get(startingIndex + 1 + count) - coords.get(startingIndex + count);
        }

        return sum;
    }

    // Maxima in even numbers, Minima in odd
    public static List<Point> findMaximaMinima(List<Integer> coords, int noiseThreshold, int searchWidth) {
        int x = 0;
        int nextGradient = 999999;
        List<Point> maxMin = new ArrayList<>();
        int flatCount = 0;
        boolean wereGoingUp = false;    // States whether the hill is heading up or down
                                        // ToDo: decide what this should be at first

        while ((x + searchWidth) < coords.size()) {
            Log.d("gradient", "" );
            Log.d("gradient", "Searching coords from " + x);
            // Keep going along horizon until you get a straight path Todo: Does this work with pointy peaks?
            while (Math.abs(nextGradient = gradientAhead(coords, x, searchWidth)) > noiseThreshold) {
                x += searchWidth;
                wereGoingUp = (nextGradient < 0);   // y coords are +ve downwards for bitmaps
                Log.d("gradient", "Gradient ahead of " + (x - searchWidth) + " is " + nextGradient + " so check what it is from " + x);
            }

            // Now you've got a straight area ahead
            Log.d("gradient", "The gradient ahead of " + x + " is quite flat (" + nextGradient + "), could be maxima/minima");
            flatCount++;
            int xBeforeFlat = x;  // Increment X so it is at the next point, 1 after, not 1 searchWidth after


            Log.d("gradient", "Checking if " + gradientAhead(coords, ++x, searchWidth) + " <= " + noiseThreshold);
            // Keep going along the flat area until you reach a strong gradient again
            while (Math.abs(nextGradient = gradientAhead(coords, x++, searchWidth)) <= noiseThreshold) {
                Log.d("gradient", "Found another flat area(" + flatCount  + "), check the next area");
                flatCount++;
            }
            // Don't forget the flat areas you've found from the search area in front of you
            flatCount += searchWidth - 1;

            // Got a gradient ahead again, see which direction it is to
            // determine if the flat was a maxima or minima

            // Get the middle point of the possible maxima/minima
            //   \________/
            //       ^
            int xAtEndOfFlat = x;
            int widthOfFlatArea = xAtEndOfFlat - xBeforeFlat;
            Log.d("gradient", "Flat / 2:....   " + widthOfFlatArea + " / 2 = " + widthOfFlatArea/2 + ". Ceil: " +  Math.ceil(widthOfFlatArea / 2));
            int centreOfFlat = (int) Math.ceil((double)widthOfFlatArea / 2);
            int xOfMaxOrMin = xBeforeFlat + centreOfFlat;

            Log.d("gradient", "Flat area ended after " + x +". The min/max would be at x coord " + xOfMaxOrMin + " because " + xBeforeFlat + " + " + centreOfFlat);

            if (wereGoingUp && nextGradient > 0) {
                Log.d("gradient", "Maxima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
                if (maxMin.size() % 2 == 1) // Done this so that maximas are even Todo: Will this be true?
                    maxMin.add(null);
                maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));
            } else if (!wereGoingUp && nextGradient < 0) {
                Log.d("gradient", "Minima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
                if (maxMin.size() % 2 == 0) // Done this so that minimas are odd Todo: Will this be true?
                    maxMin.add(null);
                maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));
            } else
                Log.d("gradient", "The gradient after is " + nextGradient +
                        " which doesn't make a max or min considering that before this we were going " + (wereGoingUp ? "up" : "down"));
        }
        return maxMin;
    }
}
