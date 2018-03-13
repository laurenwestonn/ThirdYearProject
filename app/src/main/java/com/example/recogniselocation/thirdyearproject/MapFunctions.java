package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import static com.example.recogniselocation.thirdyearproject.APIFunctions.noOfPaths;

public class MapFunctions extends Activity {

    private static final int LONLAT_TO_METRES = 111111; // roughly


    // Draw a line around the points, add a marker to where you are
    public static void plotPoints(GoogleMap map, List<Result> highPoints, LatLng yourLoc)
    {
        // Centre the camera around the middle of the points and your location
        LatLng midHorizon = highPoints.get(noOfPaths / 2).getLocation();
        double avLat = (yourLoc.getLat() + midHorizon.getLat()) / 2;
        double avLng = (yourLoc.getLng() + midHorizon.getLng()) / 2;
        goToLocation(map, avLat, avLng);

        addMarkerAt(map, yourLoc, "You are here!",
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET));

        // Draw a line to show the visible peaks
        drawVisiblePeaksPolygon(highPoints, yourLoc);
    }

    public static void goToLocation(GoogleMap map, double lat, double lng) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                new com.google.android.gms.maps.model.LatLng(lat, lng), 11);
        Log.d("df", "goToLocation: Moving camera for map " + map);
        map.moveCamera(update);
        Log.d("MapFunctions", "Moved to location " + lat + ", " + lng);
    }

    // Todo: Only show the markers that are corresponding to the photo
    // Add a list of LatLng markers to the map
    public static void addMarkersAt(GoogleMap map, List<LatLng> locations)
    {
        if (locations != null && locations.size() > 0) {
            boolean even = locations.get(0) != null;
            for (LatLng l : locations) {
                if (l != null) {
                    BitmapDescriptor icon = even ? BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) :
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
                    addMarkerAt(map, l, (even ? "Peak" : "Trough"), icon);
                    even = !even;
                }
            }
        }
    }

    // Add marker to map at the specified location that says the string
    public static void addMarkerAt(GoogleMap map, LatLng p, String msg, BitmapDescriptor icon)
    {
        map.addMarker(new MarkerOptions()
                .title(msg)
                .position(new com.google.android.gms.maps.model.LatLng(p.getLat(), p.getLng()))
                .icon(icon));
    }

    public static void drawVisiblePeaksPolygon(List<Result> highPoints, LatLng yourLoc)
    {
        // Initialise polygon at your position
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.fillColor(Color.argb(50, 250, 150, 50));
        polygonOptions.strokeColor(Color.argb(255, 250, 150, 50));
        polygonOptions.strokeWidth(3);
        polygonOptions.add(new com.google.android.gms.maps.model.LatLng(
                yourLoc.getLat(),
                yourLoc.getLng()));

        for (Result highPoint : highPoints) {
            // Add a point for each of the peaks
            polygonOptions.add(new com.google.android.gms.maps.model.LatLng(
                    highPoint.getLocation().getLat(),
                    highPoint.getLocation().getLng()));
        }
        MapActivity.googleMap.addPolygon(polygonOptions);
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
