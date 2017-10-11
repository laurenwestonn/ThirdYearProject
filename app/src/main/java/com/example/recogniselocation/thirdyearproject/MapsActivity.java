package com.example.recogniselocation.thirdyearproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity  {

    private Button button;
    private TextView textView;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private double xPos, yPos;
    public int noOfPaths;
    String APIkey = "AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U";
    double lengthOfSearch = 0.1;
    int noOfSamples = 10;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.text);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Save your co-ordinates
                xPos = location.getLatitude();
                yPos = location.getLongitude();


                // Overwriting coords with coords of a valley SW of a sydney mountain
                //xPos = -33.758731;
                //yPos = 150.240165;

                // Coords of in front of kinder scout
                //xPos = 53.382105;
                //yPos = -1.9060239;

                // https://www.darrinward.com/lat-long/?id=59dcd03715f6d6.39982706
                // Great tool to plot map points, for before the time I make my own

                // Print out your co-ordinates
                Log.d("Hi", "You're at : " + xPos + ", " + yPos);
                textView.append("\n" + xPos + ", " + yPos);

                // Find what you are looking at
                getVisiblePeaks();
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



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("hi", "Button was clicked. Check permissions");

                if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET
                    }, 10);
                } else {
                    locationManager.requestLocationUpdates("gps", 1000, 5, locationListener);
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void loadLocation() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
        } else {
            locationManager.requestLocationUpdates("gps", 1000, 5, locationListener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    loadLocation();
                else
                    requestPermissions(new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.INTERNET
                    }, 10);
        }
    }

    private void getVisiblePeaks() {
        // Calculate end points of each path

        noOfPaths = 7; //ToDo: Make this configurable
        double step = 45 / (noOfPaths - 1);
        double yourDirection = 30;
        double start = yourDirection - (step * 2);
        List<LatLng> endCoords = new ArrayList<>();
        List<LatLng> startCoords = new ArrayList<>();

        // 0 -> 6
        for (int i = 0; i < 7; i++) {
            double sinOfThisStep = Math.sin(Math.toRadians(start + i * step));
            double cosOfThisStep = Math.cos(Math.toRadians(start + i * step));
            // Start from the first position away from you in each direction
            startCoords.add(new LatLng(
                                    xPos + lengthOfSearch / noOfSamples * sinOfThisStep,
                                    yPos + lengthOfSearch / noOfSamples * cosOfThisStep
                                    ));
            // End at the length of your search in each direction
            endCoords.add(new LatLng(
                                    xPos + lengthOfSearch * sinOfThisStep,
                                    yPos + lengthOfSearch * cosOfThisStep
                                    ));
        }

        Log.d("Hi", endCoords.toString());

        // Now we have the end co ords, we can get heights of 7 paths

        // Get the elevations of the first path, first

        try {
            // Build up the web requests for each path
            // The first request is to get the elevation of where you are
            String urls = "https://maps.googleapis.com/maps/api/elevation/json?path="
                    + xPos + "," + yPos
                    + "&samples=" + noOfSamples
                    + "&key=" + APIkey + "!";
            // The other requests are to get elevations along paths
            for (int i = 0; i < noOfPaths; i++)
                urls += "https://maps.googleapis.com/maps/api/elevation/json?path="
                        + startCoords.get(i).getLat() + "," + startCoords.get(i).getLng() + "|"
                        + endCoords.get(i).getLat() + "," + endCoords.get(i).getLng()
                        + "&samples=" + noOfSamples
                        + "&key=" + APIkey + "!";
            if (!urls.equals("")) {
                new RetrieveURLTask().execute(urls);
            }
        } catch (Exception e) {
            Log.d("Hi", "Failed " + e);
            e.printStackTrace();
        }
    }
}
