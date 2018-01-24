package com.example.recogniselocation.thirdyearproject;

public class Result {
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
}
