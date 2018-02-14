package com.example.recogniselocation.thirdyearproject;

// Hold the longitude and latitude and direction of the location that the programme will use
public class LocationOrientation {

    String name;
    LatLng loc;
    double dir; // in degrees

    public LocationOrientation(String name, LatLng loc, double dir)
    {
        this.name = name;
        this.loc = loc;
        this.dir = dir;
    }

    public LatLng getLocation()
    {
        return loc;
    }

    public double getDirection()
    {
        return dir;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString() {
        return name + ": " + loc + " facing " + dir + " degree due east anti clockwise.";
    }
}
