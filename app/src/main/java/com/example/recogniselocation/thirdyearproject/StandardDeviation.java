package com.example.recogniselocation.thirdyearproject;

import java.util.List;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class StandardDeviation {

    double mean;
    double sd;
    int minRange;
    int maxRange;

    public StandardDeviation(List<Integer> range, int heightFromCentre) {
        mean = calcMean(range);
        sd = calcSD(range, mean);
        minRange = (int) (mean - 3*sd) - heightFromCentre;  // Get more above the horizon
        maxRange = (int) (mean + sd) + heightFromCentre;    // as there's more likely to be more noise below
    }

    public static double calcSD(List<Integer> numArray)
    {
        int sum = 0;
        double sd = 0;

        for (int num : numArray) {
            sum += num;
        }

        double mean = sum / numArray.size();

        for (int num : numArray) {
            sd += Math.pow(num - mean, 2);
        }

        return Math.sqrt(sd / numArray.size());
    }

    public static double calcSD(List<Integer> numArray, double mean)
    {
        double sd = 0;

        for (int num : numArray) {
            sd += Math.pow(num - mean, 2);
        }

        return Math.sqrt(sd / numArray.size());
    }

    public static double calcMean(List<Integer> numArray) {
        int sum = 0;

        for (int num : numArray) {
            sum += num;
        }

        return sum / numArray.size();
    }
}
