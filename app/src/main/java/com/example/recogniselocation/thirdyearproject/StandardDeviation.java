package com.example.recogniselocation.thirdyearproject;

import java.util.List;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class StandardDeviation {

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
