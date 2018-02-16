package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.util.List;

public class StandardDeviation {

    private double mean;
    private double sd;
    private int minRange;
    private int maxRange;
    private Bitmap bmp;

    StandardDeviation(List<Integer> range, int heightFromCentre) {
        mean = calcMean(range);
        sd = calcSD(range, mean);
        minRange = (int) (mean - 3*sd) - heightFromCentre;  // Get more above the horizon
        maxRange = (int) (mean + sd) + heightFromCentre;    // as there's more likely to be more noise below

        if (minRange < 0)
            minRange = 0;
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

    private static double calcSD(List<Integer> numArray, double mean)
    {
        double sd = 0;

        for (int num : numArray) {
            sd += Math.pow(num - mean, 2);
        }

        return Math.sqrt(sd / numArray.size());
    }

    private static double calcMean(List<Integer> numArray) {
        int sum = 0;

        for (int num : numArray) {
            sum += num;
        }

        return sum / numArray.size();
    }

    void setBitmap(Bitmap bmp)
    {
        this.bmp = bmp;
    }

    public double getMean() {
        return mean;
    }

    public double getSd() {
        return sd;
    }

    public int getMinRange() {
        return minRange;
    }

    public int getMaxRange() {
        return maxRange;
    }

    public Bitmap getBmp() {
        return bmp;
    }
}
