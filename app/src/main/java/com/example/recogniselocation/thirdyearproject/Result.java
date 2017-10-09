package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class Result {
    //ToDo: Make these private with getters and setters
    public LatLng location;
    public double elevation;
    public double distance;
    public transient double resolution;

    public Result(LatLng givenLocation, double givenElevation, double givenDistance)
    {
        this.location = givenLocation;
        this.elevation = givenElevation;
        this.distance = givenDistance;
    }

    public String toString()
    {
        return "Location: (" + location.lat + ", " + location.lng
                + ") \tElevation: " + elevation + "\tDistance: " + distance;
    }
}
