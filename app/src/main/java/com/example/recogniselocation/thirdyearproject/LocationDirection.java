package com.example.recogniselocation.thirdyearproject;

// Hold the longitude and latitude and direction of the location that the programme will use
public class LocationDirection {

    private String name;
    private LatLng loc;
    private double dir; // in degrees

    LocationDirection(String name, LatLng loc, double dir)
    {
        this.name = name;
        this.loc = loc;
        this.dir = dir;
    }

    LatLng getLocation()
    {
        return loc;
    }

    double getDirection()
    {
        return dir;
    }

    String getName()
    {
        return name;
    }

    @Override
    public String toString() {
        return name + ": " + loc + " facing " + dir + " degree due east anti clockwise.";
    }
}
