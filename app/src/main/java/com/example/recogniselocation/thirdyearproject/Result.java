package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class Result {
    private LatLng location;
    private double elevation;
    private double distance;
    private double angle;
    private double difference;

    public transient double resolution;

    public Result(LatLng givenLocation, double givenElevation, double givenDistance,
                                            double givenAngle, double givenDifference)
    {
        this.location = givenLocation;
        this.elevation = givenElevation;
        this.distance = givenDistance;
        this.angle = givenAngle;
        this.difference = givenDifference;
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

    public double getAngle() {
        return angle;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double newDifference) {
        this.difference = newDifference;
    }

    public String toString()
    {
        return "Location: (" + location.getLat() + ", " + location.getLng()
                + ") \tElevation: " + elevation + "\tDistance: " + distance
                + "\tAngle: " + angle + "\tDifference: " + difference;
    }
}
