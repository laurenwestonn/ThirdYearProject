package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import static android.content.ContentValues.TAG;

public class MapActivity extends Activity implements OnMapReadyCallback {

    // Todo: Do I need to extend fragment activity?
    static GoogleMap googleMap;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
    }

    @Override
    public void onMapReady(GoogleMap givenGoogleMap) {
        googleMap = givenGoogleMap;
        googleMap.setMapType(googleMap.MAP_TYPE_TERRAIN);
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
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the graph");
                //Todo: Pass results to the intent
                Intent intent = new Intent(this.getString(R.string.GRAPH_ACTIVITY));
                startActivity(intent);
                finish();
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }
    }
}