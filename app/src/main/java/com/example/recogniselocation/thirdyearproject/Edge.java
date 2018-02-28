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
    private Bitmap bmp = null;

    Edge(List<Point> coords, Bitmap bmp)
    {
        this.coords = coords;
        this.bmp = bmp;
    }

    List<Point> getCoords()
    {
        return coords;
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

        // Bitmap is too big to send, just gonna send the name of the image to locate later
        //bmp = in.readParcelable(Bitmap.class.getClassLoader());


/*
        List<List<Integer>> c = new ArrayList<>();
        in.readList(c, ElementType.class.getClassLoader());

        this.coords = c; // The string is correct, but how to convert [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [542], [722], [662], [602], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [902], [722], [662], [602], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [554], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722], [722]]
        this.bmp = in.readParcelable(Bitmap.class.getClassLoader());
*/
        Log.d(TAG, "Edge: Got parcel coords " + coords);
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
        if (bmp != null) {
            //dest.writeByte((byte) (0X01));
            //dest.writeValue(bmp);
            Log.d(TAG, "writeToParcel: Bitmap existed as " + bmp.toString() + ". " + bmp.getConfig());
        } else {
            dest.writeByte((byte) (0x00));
            Log.d(TAG, "writeToParcel: Couldn't write bitmap to parcel");
        }

        /*
        dest.writeStringArray(
                new String[]{String.valueOf(this.coords),
                         String.valueOf(this.bmp)});
                         */
    }




    public static final Parcelable.Creator<Edge> CREATOR= new Parcelable.Creator<Edge>() {

        @Override
        public Edge createFromParcel(Parcel source) {

            return new Edge(source);
            /*
            List<List<Integer>> coords = new ArrayList<>();
            source.readList(coords, ElementType.class.getClassLoader());

            Bitmap bmp = source.readParcelable(Bitmap.class.getClassLoader());

            return new Edge(coords, bmp);  //using parcelable constructor*/
        }

        @Override
        public Edge[] newArray(int size) {
            return new Edge[size];
        }
    };



}
