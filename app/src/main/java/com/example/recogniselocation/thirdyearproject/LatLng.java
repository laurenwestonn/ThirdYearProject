package com.example.recogniselocation.thirdyearproject;


import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

public class LatLng implements Parcelable {
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

    double getLat() { return lat; }

    double getLng() { return lng; }

    @Override
    public String toString() {
        return lat + "," + lng;
    }

    protected LatLng(Parcel in) {
        lat = in.readDouble();
        lng = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(lat);
        dest.writeDouble(lng);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<LatLng> CREATOR = new Parcelable.Creator<LatLng>() {
        @Override
        public LatLng createFromParcel(Parcel in) {
            return new LatLng(in);
        }

        @Override
        public LatLng[] newArray(int size) {
            return new LatLng[size];
        }
    };
}