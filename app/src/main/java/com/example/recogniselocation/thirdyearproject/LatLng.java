package com.example.recogniselocation.thirdyearproject;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class LatLng {
    private double lat;
    private double lng;

    public LatLng(double givenLat, double givenLng)
    {
        this.lat = givenLat;
        this.lng = givenLng;
    }

    public double getLat() { return lat; }

    public double getLng() { return lng; }

    @Override
    public String toString() {
        return "(" + lat + ", " + lng + ")";
    }
}
