package com.example.recogniselocation.thirdyearproject;

import android.os.Parcel;
import android.os.Parcelable;

public class Result implements Parcelable {
    private LatLng location;
    private double elevation;
    private double distance;
    private double angle;
    private double difference;

    Result(LatLng givenLocation, double givenElevation, double givenDistance,
           double givenAngle, double givenDifference)
    {
        this.location = givenLocation;
        this.elevation = givenElevation;
        this.distance = givenDistance;
        this.angle = givenAngle;
        this.difference = givenDifference;
    }

    LatLng getLocation() {
        return location;
    }

    double getElevation() {
        return elevation;
    }

    double getDistance() {
        return distance;
    }

    double getAngle() {
        return angle;
    }

    double getDifference() {
        return difference;
    }

    void setDifference(double newDifference) {
        this.difference = newDifference;
    }

    public String toString()
    {
        return "Location: (" + location + ")"
                + "\tElevation: " +  elevation + "\tDistance: " + distance
                + "\tAngle: " +         angle + "\tDifference: " + difference;
    }

    private Result(Parcel in) {
        location = (LatLng) in.readValue(LatLng.class.getClassLoader());
        elevation = in.readDouble();
        distance = in.readDouble();
        angle = in.readDouble();
        difference = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(location);
        dest.writeDouble(elevation);
        dest.writeDouble(distance);
        dest.writeDouble(angle);
        dest.writeDouble(difference);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Result> CREATOR = new Parcelable.Creator<Result>() {
        @Override
        public Result createFromParcel(Parcel in) {
            return new Result(in);
        }

        @Override
        public Result[] newArray(int size) {
            return new Result[size];
        }
    };
}