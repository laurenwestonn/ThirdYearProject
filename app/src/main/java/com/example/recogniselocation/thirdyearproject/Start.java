package com.example.recogniselocation.thirdyearproject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class Start extends AppCompatActivity {

    private static final String TAG = "Start";
    private LocationManager locationManager;
    private LocationListener locationListener;
    static LatLng yourLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        // Set up the location manager and listener, detects what is ahead EVERY TIME LOCATION CHANGES?*
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Todo: Get your direction, I've just hardcoded 60 degrees :/
                // *Todo: Check, this isn't going to get called every 5 ms is it? If so, only carry on if has changed from last time (global variabel required)
                yourLocation = new LatLng(location);
                LocationDirection locDir = new LocationDirection(null, new LatLng(location),60);
                detectWhatsAheadOfYou(locDir);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            // Checks if GPS is turned off. Take user to settings to enable
            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };


    }   // On create



    // Deal with button clicks
    public void buttonClicked(View view) {
        // Find which, if any, demo is to be used
        LocationDirection locDir = null;
        if (view.getId() != R.id.camera)
            locDir = Demos.getDemo(view.getId());

        // Use the location and direction to perform the location recognition
        Log.d(TAG, "onCreate: Going to recognise from "
                + ((locDir == null) ? "your location." : locDir));

        //////// RECOGNITION ///////
        // If using your actual location, we'll have to track it
        if (locDir == null) {

            int ALLOWED = PackageManager.PERMISSION_GRANTED;

            if (ActivityCompat.checkSelfPermission(Start.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == ALLOWED
                    && ActivityCompat.checkSelfPermission(Start.this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) == ALLOWED) {
                locationManager.requestLocationUpdates("gps", 0, 5, locationListener);
            } else
                ActivityCompat.requestPermissions(Start.this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.INTERNET
                }, 10);

        } else // You're doing a demo, and faking your location. Go ahead
            detectWhatsAheadOfYou(locDir);
        //////// RECOGNITION ///////


        // Method above is asynchronous, wont return here after.

        /*/////// EDGE DETECTION ///////
        // Get a mutable bitmap of the image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                getResources().getIdentifier(locDir.getName(), "drawable", getPackageName()), opt);

        // Detect the image
        // Todo: allow customisation
        boolean showCoarse = false;   // Show results of the coarse or the fine?
        boolean sdDetail = false;     // Want to draw SD and log info under tag "sd"?
        boolean useThinning = true;   // Thin to have only one point per column?
        boolean showEdgeOnly = true;  // Colour in just the edge, or all searched area?
        Edge edge = ImageManipulation.detectEdge(bmp, showCoarse, sdDetail, useThinning, showEdgeOnly);

        if (edge != null)
            Log.d(TAG, "buttonClicked: Yep be detected the edge of " + locDir.getName() + " to be " + edge.getCoords());
        /////// EDGE DETECTION ///////*/
        /*
        switch (view.getId()) {
            case R.id.demo1: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                intent.putExtra("Edge", edge);  // Can't seem to pass a 2D list, so pass this obj
                intent.putExtra("locName", locDir.getName());  // Name of the image we're working with
                Log.d(TAG, "buttonClicked: Put the edge into the extras");
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo2: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo3: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo4: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
            // Do for the other demos... or just do all in this one and have one
            // more for the photo button
        }
        */
    }


    private void detectWhatsAheadOfYou(LocationDirection locDir) {
        // Inform the user of their location
        String info;
        if (locDir.getName() != null)
            info = getString(R.string.youre_at) + " " + locDir.toString();
        else
            info = getString(R.string.youre_at) + " " + locDir.getLocation();
        ((TextView) findViewById(R.id.text)).setText(info);

        // Find what you are looking at
        APIFunctions.getElevations(locDir, this);
    }

}
