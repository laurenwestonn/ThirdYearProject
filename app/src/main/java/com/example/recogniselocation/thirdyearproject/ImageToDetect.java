package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class ImageToDetect extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_to_detect);
    }

    public void detectHorizon(View view) {
        ImageButton imageButton = (ImageButton) view;
        Bitmap bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap();
        EdgeDetection edgeDetection = null;

        // Get the image off the button as a bitmap
        if (bmp != null)
            edgeDetection = ImageManipulation.detectEdge(bmp);

        if (edgeDetection != null)
            ((ImageButton) view).setImageBitmap(edgeDetection.bmp);
    }

}