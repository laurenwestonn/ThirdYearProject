package com.example.recogniselocation.thirdyearproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

public class ActStart extends AppCompatActivity {

    private static final String TAG = "ActStart";
    private LocationManager locationManager;
    static Uri uri = null;
    private static final int REQUEST_CAMERA = 123;
    private static final int REQUEST_TAKE_PHOTO = 0;
    int ALLOWED = PackageManager.PERMISSION_GRANTED;

    // Can't send more than the URLs to async, so access these publicly in FunctionsRetrieveURLs later
    static LatLng yourLocation;
    static int drawableID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        uri = null;
        drawableID = 0;

        // Set up the location manager and listener, detects what is ahead EVERY TIME LOCATION CHANGES?*
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "onLocationChanged: Location changed to " + location);
                yourLocation = new LatLng(location.getLatitude(), location.getLongitude());
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

        // Get your location
        if (ActivityCompat.checkSelfPermission(ActStart.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == ALLOWED
                && ActivityCompat.checkSelfPermission(ActStart.this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == ALLOWED
                && ActivityCompat.checkSelfPermission(ActStart.this,
                Manifest.permission.INTERNET) == ALLOWED)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 5, locationListener);
        else    // If not allowed, ask if you can get the location
            ActivityCompat.requestPermissions(ActStart.this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.INTERNET
            }, 10);

    }   // On create

    // Deal with all button clicks
    public void buttonClicked(View view) {
        if (view.getId() == R.id.camera) { // Find your location
            uri = dispatchTakePictureIntent();
        } else {    // Find the location of this demo
            LocationDirection locDir = Demos.getDemo(view.getId());
            yourLocation = locDir.getLocation();
            if ((drawableID = ActStart.this.getResources().getIdentifier(
                    locDir.getName(),"drawable", ActStart.this.getPackageName() ))
                    == 0)
                Log.e(TAG, "buttonClicked: Couldn't find the ID for the drawable " + locDir.getName());

            recogniseAsync(locDir, null);
        }
    }

    // Use the location and direction to perform the location recognition
    private void recogniseAsync(LocationDirection locDir, Uri uri)
    {
        Toast.makeText(this, R.string.loading_msg, Toast.LENGTH_LONG).show();

        // If using your actual location, we'll have to track it
        if (uri != null) {
            Log.d(TAG, "onCreate: Going to recognise from your location." );

            if (ActivityCompat.checkSelfPermission(ActStart.this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == ALLOWED
                    && ActivityCompat.checkSelfPermission(ActStart.this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION) == ALLOWED
                    && ActivityCompat.checkSelfPermission(ActStart.this,
                    Manifest.permission.INTERNET) == ALLOWED) {
                // Get your location
                yourLocation = new LatLng(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                locDir = new LocationDirection(null, yourLocation,60); //Todo: Get your direction, I've just hardcoded 60 degrees :/

            } else
                ActivityCompat.requestPermissions(ActStart.this, new String[]{
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                        android.Manifest.permission.INTERNET
                }, 10);

        } else // You're doing a demo, and faking your location. Go ahead
            Log.d(TAG, "onCreate: Going to recognise from " + locDir);

        FunctionsAPI.getElevations(locDir, this);
    }

    // The below got from https://developer.android.com/training/camera/photobasics.html#TaskCaptureIntent

    // ActStart the intent to take a photo
    private Uri dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "dispatchTakePictureIntent: Couldn't make file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                // Request camera permission if is not allowed yet
                if (checkSelfPermission(android.Manifest.permission.CAMERA ) != PackageManager.PERMISSION_GRANTED ) {
                    requestPermissions(new String[] {  android.Manifest.permission.CAMERA  },
                            REQUEST_CAMERA);
                }

                // Starting an activity that returns a result - a photo
               startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                return photoURI;
            }
        }
        return null;
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = DateFormat.getDateTimeInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) 
    {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    Log.d(TAG, "onRequestPermissionsResult: Camera is now allowed");
                else
                    Toast.makeText(ActStart.this, "Camera access denied", Toast.LENGTH_SHORT)
                            .show();
                    
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                Uri uri = data.getData();
                if (uri != null)
                    Log.d(TAG, "onActivityResult: Got the uri " + uri.toString());
                else
                    Log.e(TAG, "onActivityResult: Couldn't find uri from intent "
                            + data.toString());

                recogniseAsync(null, uri);
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Photo wasn't taken", Toast.LENGTH_SHORT).show();
        }
    }
}
