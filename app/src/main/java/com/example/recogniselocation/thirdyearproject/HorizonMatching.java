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
        if (startingIndex + width - 1 > coords.size()) {
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
        int nextGradient = 99999;
        List<Point> maxMin = new ArrayList<>();
        boolean wereGoingUp = true;    //  Whether the hill is heading up or down. Updated.


        while ((x + searchWidth - 1) < coords.size()) {
            Log.d("gradient", " " );
            Log.d("gradient", "Finding coords from " + x);

            // Skip over initial flat areas
            if (x == 0) {
                Log.d("gradient", "First time going through coords, skip initially flat areas");
                while (Math.abs(gradientAhead(coords, x, searchWidth)) < noiseThreshold)
                    // This one is flat too, carry on
                    x += searchWidth - 1;

                // Now there's an up or down direction, remember it
                if (x + searchWidth - 1 < coords.size())
                    wereGoingUp = gradientAhead(coords, x, searchWidth) < 0;

                Log.d("gradient", "The first direction is "+ (wereGoingUp ? "up" : "down"));
            }

            // Keep climbing/descending horizon until you get a straight path
            while (x + searchWidth - 1 < coords.size()
                    && Math.abs(nextGradient = gradientAhead(coords, x, searchWidth)) > noiseThreshold) {

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

                x += searchWidth - 1;
                Log.d("gradient", "Gradient ahead of index " + (x - searchWidth + 1)
                        + " is " + nextGradient + " so check what it is from " + x + " if possible.");
                wereGoingUp = (nextGradient < 0);   // y coords are +ve downwards for bitmaps
            }

            // If this is too close to the edge that we can't see the next gradient, exit
            if (x + searchWidth - 1 >= coords.size())
                break;

            // Now you've got a straight area ahead
            Log.d("gradient", "The gradient ahead of " + x + " is quite flat (" + nextGradient + "), could be maxima/minima");
            int xFlatStart = x++;  // Increment X so it is at the second flat point


            // Keep going along the flat area until you reach a strong gradient again
            while ((x + searchWidth - 1) < coords.size() && Math.abs(nextGradient = gradientAhead(coords, x, searchWidth)) <= noiseThreshold)
                x++;

            // If that while was exited due to reaching the end of the horizon, exit
            if (x + searchWidth - 1 >= coords.size())
                break;


            Log.d("gradient", "At the end of the flat areas");

            // Got a gradient ahead again, see which direction it is to
            // determine if the flat was a maxima or minima

            // Get the middle point of the possible maxima/minima
            //   \________/
            //       ^
            int xAtEndOfFlat = x;
            int widthOfFlatArea = xAtEndOfFlat - xFlatStart;
            Log.d("gradient", "Flat / 2:....   " + widthOfFlatArea + " / 2 = " + widthOfFlatArea/2 + ". Ceil: " +  Math.ceil(widthOfFlatArea / 2));
            int centreOfFlat = (int) Math.ceil((double)widthOfFlatArea / 2);
            int xOfMaxOrMin = xFlatStart + centreOfFlat;

            Log.d("gradient", "Flat area ended after " + x +". The min/max would be at x coord " + xOfMaxOrMin + " because " + xFlatStart + " + " + centreOfFlat);

            if (wereGoingUp && nextGradient > 0) {
                Log.d("gradient", "Maxima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
                if (maxMin.size() % 2 == 1) // Done this so that maximas are even
                    maxMin.add(null);
                maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));
                wereGoingUp = !wereGoingUp; // Bug fix to avoid adding duplicate pointy points
            } else if (!wereGoingUp && nextGradient < 0) {
                Log.d("gradient", "Minima found at " + xOfMaxOrMin + ", " + coords.get(xOfMaxOrMin));
                if (maxMin.size() % 2 == 0) // Done this so that minimas are odd
                    maxMin.add(null);
                maxMin.add(new Point(xOfMaxOrMin, coords.get(xOfMaxOrMin)));
                wereGoingUp = !wereGoingUp; // Bug fix to avoid adding duplicate pointy points
            } else
                Log.d("gradient", "The gradient after is " + nextGradient
                        +  " which doesn't make a max or min considering that before ths"
                        + " we were going " + (wereGoingUp ? "up" : "down"));
            }
        return maxMin;
    }
}
