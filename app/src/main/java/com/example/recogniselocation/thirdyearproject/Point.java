package com.example.recogniselocation.thirdyearproject;

import android.os.Parcel;
import android.os.Parcelable;

public class Point implements Parcelable {
    private double x;
    private double y;

    Point(double givenX, double givenY)
    {
        x = givenX;
        y = givenY;
    }

    protected Point(Parcel in) {
        x = in.readDouble();
        y = in.readDouble();
    }

    public static final Creator<Point> CREATOR = new Creator<Point>() {
        @Override
        public Point createFromParcel(Parcel in) {
            return new Point(in);
        }

        @Override
        public Point[] newArray(int size) {
            return new Point[size];
        }
    };

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        else
            return (this.getX() == ((Point) obj).getX() &&
                this.getY() == ((Point) obj).getY());

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(x);
        dest.writeDouble(y);
    }
}

