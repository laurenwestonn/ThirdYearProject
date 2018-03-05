package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class Edge implements Parcelable {

    private List<Point> coords;
    private List<Point> coarseCoords;
    private Bitmap bmp = null;

    Edge(List<Point> coords, List<Point> coarseCoords, Bitmap bmp)
    {
        this.coords = coords;
        this.coarseCoords = coarseCoords;
        this.bmp = bmp;
    }

    List<Point> getCoords()
    {
        return coords;
    }

    List<Point> getCoarseCoords()
    {
        return coarseCoords;
    }

    Bitmap getBitmap()
    {
        return bmp;
    }

    void setBitmap(Bitmap b)
    {
        bmp = b;
    }



    //parcel part
    public Edge(Parcel in){
        Log.d(TAG, "Edge: In the edge constructor for parcels");

        if (in.readByte() == 0x01) {
            coords = new ArrayList<>();
            in.readList(coords, String.class.getClassLoader());
            Log.d(TAG, "Edge: Got the coords " + coords);
        } else {
            coords = null;
            Log.e(TAG, "Edge: Didn't find the coordinates from the parcel");
        }
        Log.d(TAG, "Edge: Got parcel coords " + coords);

        if (in.readByte() == 0x01) {
            coarseCoords = new ArrayList<>();
            in.readList(coarseCoords, String.class.getClassLoader());
            Log.d(TAG, "Edge: Got the coarse coords " + coarseCoords);
        } else {
            coarseCoords = null;
            Log.e(TAG, "Edge: Didn't find the coarse coordinates from the parcel");
        }

        Log.d(TAG, "Edge: Got parcel coarse coords " + coarseCoords);
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (coords != null) {
            dest.writeByte((byte) (0x01));
            dest.writeList(coords);
            Log.d(TAG, "writeToParcel: Written coords to parcel");
        } else {
            dest.writeByte((byte) (0x00));
            Log.d(TAG, "writeToParcel: Couldn't write coords to parcel");
        }
        if (coarseCoords != null) {
            dest.writeByte((byte) (0x01));
            dest.writeList(coarseCoords);
            Log.d(TAG, "writeToParcel: Written coarse coords to parcel");
        } else {
            dest.writeByte((byte) (0x00));
            Log.d(TAG, "writeToParcel: Couldn't write coarse coords to parcel");
        }
        if (bmp != null) {
            Log.d(TAG, "writeToParcel: Bitmap existed as " + bmp.toString() + ". " + bmp.getConfig());
        } else {
            dest.writeByte((byte) (0x00));
            Log.d(TAG, "writeToParcel: Couldn't write bitmap to parcel");
        }
    }




    public static final Parcelable.Creator<Edge> CREATOR= new Parcelable.Creator<Edge>()
    {

        @Override
        public Edge createFromParcel(Parcel source)
        {
            return new Edge(source);
        }

        @Override
        public Edge[] newArray(int size) {
            return new Edge[size];
        }
    };



}
