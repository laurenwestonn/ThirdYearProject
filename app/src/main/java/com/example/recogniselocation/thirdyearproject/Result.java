package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class Result {
    private LatLng location;
    private double elevation;
    private double distance;
    public transient double resolution;

    public Result(LatLng givenLocation, double givenElevation, double givenDistance)
    {
        this.location = givenLocation;
        this.elevation = givenElevation;
        this.distance = givenDistance;
    }

    public LatLng getLocation() {
        return location;
    }

    public double getElevation() {
        return elevation;
    }

    public double getDistance() {
        return distance;
    }

    public String toString()
    {
        return "Location: (" + location.getLat() + ", " + location.getLng()
                + ") \tElevation: " + elevation + "\tDistance: " + distance;
    }
}
