package com.example.recogniselocation.thirdyearproject;

import android.Manifest;
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

                // Print out your co-ordinates
                Log.d("Hi", "Location: " + xPos + ", " + yPos);
                textView.append("\n" + xPos + ", " + yPos);

                // Find what you are looking at
                findArea();
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
        } else {
            getLocation();
        }

    }

    private void findArea() {
        // Get visible peaks from seven paths
        List<Double> visiblePeaks = getVisiblePeaks();

        // Draw points on blank screen

        // Draw path of the points on blank

        // Draw path on a map
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    getLocation();
        }
    }

    private void getLocation() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("hi", "Button was clicked");
                // Get your coordinates and direction
                locationManager.requestLocationUpdates("gps", 1000, 5, locationListener);
            }
        });
    }

    private List<Double> getVisiblePeaks() {
        // Calculate end points of each path

        noOfPaths = 7; //ToDo: Make this configurable
        double step = 45 / (noOfPaths - 1);
        double yourDirection = 30;
        double start = yourDirection - (step * 2);
        List<Point> endCoords = new ArrayList<>();

        // 0 -> 6
        for (int i = 0; i < 7; i++) {
            endCoords.add(new Point(xPos + 0.1 * Math.sin(Math.toRadians(start + i * step)),
                                    yPos + 0.1 * Math.cos(Math.toRadians(start + i * step))));
        }

        Log.d("Hi", endCoords.toString());

        // Now we have the end co ords, we can get heights of 7 paths

        // Get the elevations of the first path, first

        try {
            // Build up the web requests for each path
            String urls = "";
            for (int i = 0; i < noOfPaths; i++)
                urls += "https://maps.googleapis.com/maps/api/elevation/json?path="
                        + xPos + "," + yPos + "|" +
                        endCoords.get(i).getX() + "," + endCoords.get(i).getY() +
                        "&samples=10&key=AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U!";
            if (!urls.equals("")) {
                new RetrieveURLTask().execute(urls);
            }
        } catch (Exception e) {
            Log.d("Hi", "Failed " + e);
            e.printStackTrace();
        }

        List<Double> visiblePeaks = new ArrayList<>();
        return visiblePeaks;
    }
}
