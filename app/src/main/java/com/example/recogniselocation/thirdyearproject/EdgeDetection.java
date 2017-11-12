package com.example.recogniselocation.thirdyearproject;

import android.graphics.Bitmap;

import java.io.LineNumberInputStream;
import java.util.List;

/**
 * Created by LaUrE on 12/11/2017.
 */

public class EdgeDetection {

    List<List<Integer>> coords;
    Bitmap bmp;

    public EdgeDetection(List<List<Integer>> coords, Bitmap bmp)
    {
        this.coords = coords;
        this.bmp = bmp;
    }
}
