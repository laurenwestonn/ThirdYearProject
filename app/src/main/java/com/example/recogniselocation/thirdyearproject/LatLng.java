package com.example.recogniselocation.thirdyearproject;


import android.location.Location;

public class LatLng {
    private double lat;
    private double lng;

    public LatLng(double givenLat, double givenLng)
    {
        this.lat = givenLat;
        this.lng = givenLng;
    }

    public LatLng(Location loc)
    {
        this.lat = loc.getLatitude();
        this.lng = loc.getLongitude();
    }

    public double getLat() { return lat; }

    public double getLng() { return lng; }

    @Override
    public String toString() {
        return lat + "," + lng;
    }
}
