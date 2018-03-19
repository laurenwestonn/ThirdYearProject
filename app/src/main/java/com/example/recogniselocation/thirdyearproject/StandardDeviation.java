package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.util.List;

public class StandardDeviation {

    private double mean;
    private double sd;
    private int minRange;
    private int maxRange;
    private Bitmap bmp;

    StandardDeviation(List<Point> coords, int heightFromCentre)
    {
        if (coords.size() > 0) {
            mean = calcYMean(coords);
            sd = calcSD(coords, mean);
            minRange = (int) (mean - 3 * sd) - heightFromCentre;  // Get more above the horizon
            maxRange = (int) (mean + sd) + heightFromCentre;    // as there's more likely to be more noise below

            if (minRange < 0)
                minRange = 0;
        }
    }

    private static double calcSD(List<Point> coords, double yMean)
    {
        double sd = 0;

        for (Point p : coords)
            sd += Math.pow(p.getY() - yMean, 2);

        return Math.sqrt(sd / coords.size());
    }

    private static double calcYMean(List<Point> coords)
    {
        int sum = 0;

        for (Point p : coords)
            sum += p.getY();

        return sum / coords.size();
    }

    void setBitmap(Bitmap bmp)
    {
        this.bmp = bmp;
    }

    double getMean() {
        return mean;
    }

    double getSd() {
        return sd;
    }

    int getMinRange() {
        return minRange;
    }

    int getMaxRange() {
        return maxRange;
    }

    public Bitmap getBitmap() {
        return bmp;
    }
}
