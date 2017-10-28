package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 28/10/2017.
 */

public class StandardDeviation {

    public static double calculateSD(int numArray[])
    {
        int sum = 0;
        double SD = 0;

        for (int num : numArray) {
            sum += num;
        }

        double mean = sum / numArray.length;

        for (int num : numArray) {
            SD += Math.pow(num - mean, 2);
        }

        return Math.sqrt(SD / numArray.length);
    }
}
