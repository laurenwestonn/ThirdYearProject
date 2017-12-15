package com.example.recogniselocation.thirdyearproject;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.jjoe64.graphview.GraphView;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //ToDo: Make these configurable
    int demo = 5;   // 0: Your location. 2: Kinder Scout. 3:Wast Water, 4:Blencathra, 5: Rocky Mountains
    double yourDirection = 60; // Due East anticlockwise

    private LocationManager locationManager;
    private LocationListener locationListener;
    static GraphView graph;
    static GoogleMap googleMap;
    public static LatLng yourLocation;

    // https://www.darrinward.com/lat-long/?id=59dcd03715f6d6.39982706
    // Great tool to plot map points, for before the time I make my own

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (googleServicesAvailable())
            initMap();

        graph = (GraphView) findViewById(R.id.graph);

        // Set up the listener to be ready to get used when the button is clicked
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                detectWhatsAheadOfYou(location, demo);
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

        // Once you click the button, listen out for changes in location
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("hi", "Button was clicked. Check permissions");

                // If using your actual location, we'll have to track it
                if (demo == 0) {

                    int ALLOWED = PackageManager.PERMISSION_GRANTED;

                    if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) == ALLOWED
                            && ActivityCompat.checkSelfPermission(MapsActivity.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) == ALLOWED) {
                        locationManager.requestLocationUpdates("gps", 0, 5, locationListener);
                    } else
                        ActivityCompat.requestPermissions(MapsActivity.this, new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.INTERNET
                        }, 10);

                } else // You're doing a demo, and faking your location. Go ahead
                    detectWhatsAheadOfYou(null, demo);
            }
        });
    }

    private void detectWhatsAheadOfYou(Location location, int demo) {
        // Get your location, or the location of a demo
        yourLocation = getYourCoordinates(location, demo);

        // Inform the user of their location
        String loc = getString(R.string.location_text) + yourLocation.toString();
        TextView textView = (TextView) findViewById(R.id.text);
        textView.setText(loc);

        // Find what you are looking at
        APIFunctions.getElevations(yourDirection, yourLocation, this, getString(R.string.google_maps_key));
    }

    private LatLng getYourCoordinates(Location yourLocation, int demo) {
        LatLng yourCoords;
        switch (demo) {
            case 2:
                // Coords of in front of kinder scout
                yourCoords = new LatLng(53.382105, -1.9060239);
                break;
            case 3: // Wast Water
                yourCoords = new LatLng(54.43619, -3.30942);
                yourDirection = 70;
                break;
            case 4: // Blencathra
                yourCoords = new LatLng(54.6486243, -3.0915329);
                yourDirection = 215;
                break;
            case 5: // Rocky Mountains
                yourCoords = new LatLng(51.6776886, -116.4657593);
                yourDirection = 140;
                break;
            default:
                // Save your co-ordinates
                yourCoords = new LatLng(yourLocation.getLatitude(), yourLocation.getLongitude());
        }
        return yourCoords;
    }

    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);
        if (isAvailable == ConnectionResult.SUCCESS) {
            Toast.makeText(this, "Connected to google services", Toast.LENGTH_LONG).show();
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    private void initMap() {
        setContentView(R.layout.activity_maps);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment == null)
            Log.e("Hi", "Couldn't find mapFragment");
        else {
            Log.d("Hi", "Found mapFragment");
            mapFragment.getMapAsync(this);
        }
    }

    public void loadLocation() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MapsActivity.this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates("gps", 0, 10, locationListener);
        } else {
            requestPermissions(new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.INTERNET
            }, 10);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    loadLocation();
                else
                    requestPermissions(new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 10);
        }
    }

    @Override
    public void onMapReady(GoogleMap givenGoogleMap) {
        googleMap = givenGoogleMap;
        googleMap.setMapType(googleMap.MAP_TYPE_TERRAIN);
    }

    public void buttonClicked(View view) {
        switch (view.getId()) {
            case R.id.imageButton: {
                Intent intent = new Intent(this.getString(R.string.CUSTOM_ACTION_IMAGETODETECT));
                startActivity(intent);
            }
            case R.id.edgeDetection: {
                Intent intent = new Intent(this.getString(R.string.CUSTOM_ACTION_IMAGETODETECT));
                startActivity(intent);
            }
        }
    }
}
