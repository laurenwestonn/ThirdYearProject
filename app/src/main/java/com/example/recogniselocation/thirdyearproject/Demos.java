package com.example.recogniselocation.thirdyearproject;

import android.util.Log;

import static android.content.ContentValues.TAG;

// A place to hold all of the demos
public class Demos {

    public static LocationDirection getDemo(int id)
    {
        LocationDirection demo = null;
        switch (id) {
            case R.id.demo1: // Wast Water
                demo = new LocationDirection("wast_water", new LatLng(54.43619, -3.30942), 70);
                break;
            case R.id.demo2: // Wast Water two
                demo = new LocationDirection("wast_water_two", new LatLng(54.4436654, -3.2830789), 80);
                break;
            case R.id.demo3: // Blencathra
                demo = new LocationDirection("blencathra", new LatLng(54.6486243, -3.0915329), 215);
                break;
            case R.id.demo4: // Rocky Mountains
                demo = new LocationDirection("rocky_mountains", new LatLng(51.6776886, -116.4644593), 230);
                break;
            default:
                Log.e(TAG, "getDemo: Couldn't find demo " + id);
        }

        return demo;
    }
}