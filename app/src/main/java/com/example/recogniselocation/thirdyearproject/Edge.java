package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;


import java.util.ArrayList;
import java.util.List;

public class Edge implements Parcelable {

    private List<Point> coords;
    private List<Point> coarseCoords;
    private StandardDeviation sd = null;
    private Bitmap bmp = null;

    Edge(List<Point> coords, List<Point> coarseCoords, StandardDeviation sd, Bitmap bmp)
    {
        this.coords = coords;
        this.coarseCoords = coarseCoords;
        this.sd = sd;
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

    StandardDeviation getSD()
    {
        return sd;
    }

    Bitmap getBitmap()
    {
        return bmp;
    }

    protected Edge(Parcel in) {
        if (in.readByte() == 0x01) {
            coords = new ArrayList<>();
            in.readList(coords, Point.class.getClassLoader());
        } else {
            coords = null;
        }
        if (in.readByte() == 0x01) {
            coarseCoords = new ArrayList<>();
            in.readList(coarseCoords, Point.class.getClassLoader());
        } else {
            coarseCoords = null;
        }
        sd = (StandardDeviation) in.readValue(StandardDeviation.class.getClassLoader());
        bmp = (Bitmap) in.readValue(Bitmap.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (coords == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(coords);
        }
        if (coarseCoords == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(coarseCoords);
        }
        dest.writeValue(sd);
        dest.writeValue(bmp);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Edge> CREATOR = new Parcelable.Creator<Edge>() {
        @Override
        public Edge createFromParcel(Parcel in) {
            return new Edge(in);
        }

        @Override
        public Edge[] newArray(int size) {
            return new Edge[size];
        }
    };
}