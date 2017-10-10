package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class Result {
    private LatLng location;
    private double elevation;
    private double distance;
    private double angle;

    public transient double resolution;

    public Result(LatLng givenLocation, double givenElevation, double givenDistance, double givenAngle)
    {
        this.location = givenLocation;
        this.elevation = givenElevation;
        this.distance = givenDistance;
        this.angle = givenAngle;
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

    public double getangle() {
        return angle;
    }

    public String toString()
    {
        return "Location: (" + location.getLat() + ", " + location.getLng()
                + ") \tElevation: " + elevation + "\tDistance: " + distance
                + "\tAngle: " + angle;
    }
}
