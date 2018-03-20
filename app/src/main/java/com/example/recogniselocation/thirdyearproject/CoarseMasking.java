package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.util.List;

class CoarseMasking {

    private List<Point> coords;
    private Bitmap bmp;

    CoarseMasking(List<Point> coords, Bitmap bmp)
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


}
