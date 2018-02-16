package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.util.List;

public class CoarseMasking {

    private List<Integer> ys;
    private Bitmap bmp;

    CoarseMasking(List<Integer> ys, Bitmap bmp)
    {
        this.ys = ys;
        this.bmp = bmp;
    }

    List<Integer> getYs()
    {
        return ys;
    }

    Bitmap getBitmap()
    {
        return bmp;
    }


}
