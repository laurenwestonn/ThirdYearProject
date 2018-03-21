package com.example.recogniselocation.thirdyearproject;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class StandardDeviation implements Parcelable {

    private double mean;
    private double sd;
    private int minRange;
    private int maxRange;

    StandardDeviation(List<Point> coords, int heightFromCentre)
    {
        if (coords.size() > 0) {
            mean = calcYMean(coords);
            sd = calcSD(coords, mean);
            minRange = (int) (mean - 3 * sd) - heightFromCentre;  // Get more above the horizon
            maxRange = (int) (mean + 1.5 * sd) + heightFromCentre;    // as there's more likely to be more noise below

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

    protected StandardDeviation(Parcel in) {
        mean = in.readDouble();
        sd = in.readDouble();
        minRange = in.readInt();
        maxRange = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(mean);
        dest.writeDouble(sd);
        dest.writeInt(minRange);
        dest.writeInt(maxRange);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<StandardDeviation> CREATOR = new Parcelable.Creator<StandardDeviation>() {
        @Override
        public StandardDeviation createFromParcel(Parcel in) {
            return new StandardDeviation(in);
        }

        @Override
        public StandardDeviation[] newArray(int size) {
            return new StandardDeviation[size];
        }
    };
}