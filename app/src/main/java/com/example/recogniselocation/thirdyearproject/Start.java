package com.example.recogniselocation.thirdyearproject;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

public class Start extends AppCompatActivity {

    private static final String TAG = "Start";
    private LocationManager locationManager;
    private LocationListener locationListener;

    // Todo: Try pass these to the Async method if possible
    static LatLng yourLocation;
    static int drawableID;

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
                APIFunctions.getElevations(locDir, Start.this);
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
        if (view.getId() != R.id.camera) {
            locDir = Demos.getDemo(view.getId());
            yourLocation = locDir.getLocation();
            if ((drawableID = Start.this.getResources().getIdentifier(
                    locDir.getName(),"drawable", Start.this.getPackageName() ))
                    == 0)
                Log.e(TAG, "buttonClicked: Couldn't find the ID for the drawable " + locDir.getName());
        }

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
            APIFunctions.getElevations(locDir, this);
        //////// RECOGNITION ///////

        // Method above is asynchronous, wont return here after.
    }

}
