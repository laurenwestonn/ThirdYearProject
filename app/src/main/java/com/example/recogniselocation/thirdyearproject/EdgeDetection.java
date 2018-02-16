package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.util.List;

public class EdgeDetection {

    private List<List<Integer>> coords;
    private Bitmap bmp;

    EdgeDetection(List<List<Integer>> coords, Bitmap bmp)
    {
        this.coords = coords;
        this.bmp = bmp;
    }

    List<List<Integer>> getCoords()
    {
        return coords;
    }

    Bitmap getBitmap()
    {
        return bmp;
    }


}
