package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import static com.example.recogniselocation.thirdyearproject.APIFunctions.noOfPaths;

public class MapFunctions extends Activity {

    private static final int LONLAT_TO_METRES = 111111; // roughly


    // Draw a line around the points, add a marker to where you are
    public static void plotPoints(GoogleMap map, List<Result> highPoints, double x, double y)
    {
        // Centre the camera around the middle of the points and your location
        LatLng midHorizon = highPoints.get(noOfPaths / 2).getLocation();
        double avLat = (x + midHorizon.getLat()) / 2;
        double avLng = (y + midHorizon.getLng()) / 2;
        goToLocation(avLat, avLng, 11);

        addMarkerAt(map, x, y, "You are here!");

        // Plot a line and add markers for each of the visible peaks
        showVisiblePeaks(highPoints);
    }

    public static void goToLocation(double lat, double lng, float zoom) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(new com.google.android.gms.maps.model.LatLng(lat, lng), zoom);
        OriginalMapsActivity.googleMap.moveCamera(update);
        Log.d("MapFunctions", "Moved to location " + lat + ", " + lng);
    }

    // Add marker to map at  x and y that says the string
    public static void addMarkerAt(GoogleMap map, double x, double y, String msg)
    {
        map.addMarker(new MarkerOptions()
                .title(msg)
                .position(new com.google.android.gms.maps.model.LatLng(x, y)));
    }

    // Add marker to map at  x and y with no message
    public static void addMarkerAt(GoogleMap map, double x, double y)
    {
        map.addMarker(new MarkerOptions()
                .title("You are here!")
                .position(new com.google.android.gms.maps.model.LatLng(x, y)));
    }

    public static void showVisiblePeaks(List<Result> highPoints)
    {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.YELLOW);

        for (Result highPoint : highPoints) {
            // Show the path of the peaks
            polylineOptions.add(new com.google.android.gms.maps.model.LatLng(
                    highPoint.getLocation().getLat(),
                    highPoint.getLocation().getLng()));

            // Show a marker at each peak if there aren't many
            //  - Many markers looks cluttered
            if (noOfPaths <= 15)
                addMarkerAt(OriginalMapsActivity.googleMap, highPoint.getLocation().getLat(), highPoint.getLocation().getLng());

        }
        OriginalMapsActivity.googleMap.addPolyline(polylineOptions);
    }

    private static double diffFromFirst(double comparisonDistance, double thisPeaksAngle, double comparisonElevation)
    {
        // If this peak was at the distance of the first one, how big would it be?
        double perceivedElevation = comparisonDistance * Math.tan(thisPeaksAngle);

        //Log.d("Hi", "Perceived height, got from a distance of " + comparisonDistance + " and an angle of " + thisPeaksAngle + " was calculated as " + perceivedElevation);
        return perceivedElevation - comparisonElevation;
    }

    public static List<Result> findDiffBetweenElevations(List<Result> highPoints)
    {
        double firstDistance = highPoints.get(0).getDistance();
        double firstElevation = firstDistance * Math.tan(highPoints.get(0).getAngle());

        for (Result highPoint : highPoints)
            highPoint.setDifference(diffFromFirst(firstDistance, highPoint.getAngle(), firstElevation) + firstElevation);

        return highPoints;
    }

}
