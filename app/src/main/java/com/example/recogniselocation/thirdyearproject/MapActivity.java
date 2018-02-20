package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
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

    // Todo: Do I need to extend fragment activity?
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
        googleMap.setMapType(googleMap.MAP_TYPE_TERRAIN);

        // Draw results onto the map
        LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
        List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
        MapFunctions.plotPoints(googleMap, highPoints, yourLocation);
        Log.d(TAG, "onCreate: Plotted the high points on the map, and your location. Need to mark max and min points");
    }

    // Deal with button clicks
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: A button was clicked");
        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                Intent intent = new Intent(this.getString(R.string.START_ACTIVITY));
                startActivity(intent);
                finish();
                break;
            }
            case R.id.before: {
                Log.d(TAG, "Go back to the photo");
                //Todo: Pass all results to the intent
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));

                // For the photo activity
                int drawableID = getIntent().getIntExtra("drawableID", 0);
                List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
                List<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
                intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
                intent.putParcelableArrayListExtra("photoCoords", (ArrayList) photoCoords);      // To draw the edge
                intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList) matchedPhotoCoords);  // To mark on the matched points

                // For the map activity
                LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
                intent.putExtra("yourLocation", yourLocation);
                List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
                intent.putParcelableArrayListExtra("highPoints", (ArrayList) highPoints);

                startActivity(intent);
                finish();
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the graph");
                //Todo: Pass all results to the intent
                Intent intent = new Intent(this.getString(R.string.GRAPH_ACTIVITY));

                // For the photo activity
                int drawableID = getIntent().getIntExtra("drawableID", 0);
                List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
                List<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
                intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
                intent.putParcelableArrayListExtra("photoCoords", (ArrayList) photoCoords);      // To draw the edge
                intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList) matchedPhotoCoords);  // To mark on the matched points

                // For the map activity
                LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
                intent.putExtra("yourLocation", yourLocation);
                List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
                intent.putParcelableArrayListExtra("highPoints", (ArrayList) highPoints);

                startActivity(intent);
                finish();
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }
    }
}