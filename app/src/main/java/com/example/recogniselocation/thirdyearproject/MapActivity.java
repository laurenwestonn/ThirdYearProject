package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class MapActivity extends Activity implements OnMapReadyCallback {

    static GoogleMap googleMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (googleServicesAvailable())
            initMap();
    }

    // Checks if you can connect to google services and gives Toast of result
    public boolean googleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int isAvailable = api.isGooglePlayServicesAvailable(this);

        if (isAvailable == ConnectionResult.SUCCESS) {
            return true;
        } else if (api.isUserResolvableError(isAvailable)) {
            Dialog dialog = api.getErrorDialog(this, isAvailable, 0);
            dialog.show();
        } else {
            Toast.makeText(this, "Can't connect to play services", Toast.LENGTH_LONG).show();
        }
        return false;
    }

    // Initialise the content and assign the map
    private void initMap() {
        setContentView(R.layout.map_activity);
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment == null)
            Log.e("Hi", "Couldn't find mapFragment");
        else {
            Log.d("Hi", "Found mapFragment");
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap givenGoogleMap) {
        googleMap = givenGoogleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        // Show results on the map:
        // Draw the line of the high points
        LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
        List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
        MapFunctions.plotPoints(googleMap, highPoints, yourLocation);
        // Mark matched maximas and minimas in appropriate colours
        List<Integer> matchedElevCoordsIndexes = getIntent().getIntegerArrayListExtra("matchedElevCoordsIndexes");
        if (matchedElevCoordsIndexes != null) {
            List<LatLng> matchedElevCoords = getLatLngFromResultIndexes(highPoints, matchedElevCoordsIndexes);
            MapFunctions.addMarkersAt(googleMap, matchedElevCoords);
        }

    }

    // Get the locations from the results, that are requested via the indexes
    private List<LatLng> getLatLngFromResultIndexes(List<Result> results, List<Integer> indexes) {
        List<LatLng> reqResults = new ArrayList<>();

        for (int i : indexes) {
            if (i == -1)    // For when it starts with a minima
                reqResults.add(null);
            else
                reqResults.add(results.get(i).getLocation());
        }

        return reqResults;
    }

    // Deal with button clicks
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: A button was clicked");
        Intent intent = null;

        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                intent = new Intent(this.getString(R.string.START_ACTIVITY));
                break;
            }
            case R.id.before: {
                Log.d(TAG, "Go back to the photo");
                intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the graph");
                intent = new Intent(this.getString(R.string.GRAPH_ACTIVITY));
                break;
            }
            case R.id.terrainToggle: {
                Log.d(TAG, "buttonClicked: Toggle the terrain");
                if (googleMap.getMapType() == GoogleMap.MAP_TYPE_TERRAIN) {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    ((Button) view).setText(this.getString(R.string.satellite));
                } else {
                    googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                    ((Button) view).setText(this.getString(R.string.terrain));
                }
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
                intent = new Intent(this.getString(R.string.START_ACTIVITY));
        }

        if (intent != null) {
            if (view.getId() == R.id.before || view.getId() == R.id.next) { // Send data to the next activity
                // For the photo activity
                int drawableID = getIntent().getIntExtra("drawableID", 0);
                intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
                ArrayList<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
                intent.putParcelableArrayListExtra("photoCoords", photoCoords);      // To draw the edge
                List<Point> coarsePhotoCoords = getIntent().getParcelableArrayListExtra("coarsePhotoCoords");
                intent.putParcelableArrayListExtra("coarsePhotoCoords", (ArrayList<Point>) coarsePhotoCoords);      // To draw the coarse edge
                ArrayList<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
                intent.putParcelableArrayListExtra("matchedPhotoCoords", matchedPhotoCoords);  // To mark on the matched points

                // For the map activity
                LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
                intent.putExtra("yourLocation", yourLocation);
                ArrayList<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
                intent.putParcelableArrayListExtra("highPoints", highPoints);
                ArrayList<Integer> matchedElevCoordsIndexes = getIntent().getIntegerArrayListExtra("matchedElevCoordsIndexes");
                intent.putIntegerArrayListExtra("matchedElevCoordsIndexes", matchedElevCoordsIndexes);  // To mark on the matched points

                // For the graph activity
                List<Point> elevationsCoords = getIntent().getParcelableArrayListExtra("elevationsCoords");
                intent.putParcelableArrayListExtra("elevationsCoords", (ArrayList<Point>) elevationsCoords);
                List<Point> photoSeriesCoords = getIntent().getParcelableArrayListExtra("photoSeriesCoords");
                intent.putParcelableArrayListExtra("photoSeriesCoords", (ArrayList<Point>) photoSeriesCoords);
            }
            startActivity(intent);
            finish();
        }
    }
}