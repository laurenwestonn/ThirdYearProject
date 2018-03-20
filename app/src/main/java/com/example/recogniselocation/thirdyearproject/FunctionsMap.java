package com.example.recogniselocation.thirdyearproject;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.List;

class FunctionsMap {

    static void centreCameraAround(GoogleMap map, List<LatLng> coords, LatLng yourLocation)
    {
        if (coords == null) // Just centre around your location
            goToLocation(map, yourLocation.getLat(), yourLocation.getLng());
        else {  // Was able to match horizons. Centre around your location AND a matched point
            double avLat = (yourLocation.getLat() + coords.get(1).getLat()) / 2;
            double avLng = (yourLocation.getLng() + coords.get(1).getLng()) / 2;
            goToLocation(map, avLat, avLng);
        }
    }

    private static void goToLocation(GoogleMap map, double lat, double lng) {
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(
                new com.google.android.gms.maps.model.LatLng(lat, lng), 13);
        Log.d("FunctionsMap", "goToLocation: Moving camera for map " + map);
        map.moveCamera(update);
        Log.d("FunctionsMap", "Moved to location " + lat + ", " + lng);
    }

    // Add a list of LatLng markers to the map
    static void addMarkersAt(GoogleMap map, List<LatLng> locations)
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
    static void addMarkerAt(GoogleMap map, LatLng p, String msg, BitmapDescriptor icon)
    {
        map.addMarker(new MarkerOptions()
                .title(msg)
                .position(new com.google.android.gms.maps.model.LatLng(p.getLat(), p.getLng()))
                .icon(icon));
    }

    static void drawVisiblePeaksPolygon(List<Result> highPoints, LatLng yourLoc)
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
        ActMap.googleMap.addPolygon(polygonOptions);
    }

    static void drawPolygon(GoogleMap map, List<LatLng> locations)
    {
        // Initialise polygon at your position
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.fillColor(Color.argb(50, 200, 100, 50));
        polygonOptions.strokeColor(Color.argb(255, 200, 100, 50));
        polygonOptions.strokeWidth(3);

        for (LatLng loc : locations) {
            // Add a point for each of the peaks
            polygonOptions.add(new com.google.android.gms.maps.model.LatLng(
                    loc.getLat(),
                    loc.getLng()));
        }
        map.addPolygon(polygonOptions);
    }

    static void drawPolygon(GoogleMap map, List<Result> locations, LatLng yourLoc)
    {
        // Initialise polygon at your position
        PolygonOptions polygonOptions = new PolygonOptions();
        polygonOptions.fillColor(Color.argb(50, 200, 100, 50));
        polygonOptions.strokeColor(Color.argb(255, 200, 100, 50));
        polygonOptions.strokeWidth(3);
        polygonOptions.add(new com.google.android.gms.maps.model.LatLng(
                yourLoc.getLat(),
                yourLoc.getLng()));

        for (Result loc : locations) {
            // Add a point for each of the peaks
            polygonOptions.add(new com.google.android.gms.maps.model.LatLng(
                    loc.getLocation().getLat(),
                    loc.getLocation().getLng()));
        }
        map.addPolygon(polygonOptions);
    }

    private static double diffFromFirst(double comparisonDistance, double thisPeaksAngle, double comparisonElevation)
    {
        // If this peak was at the distance of the first one, how big would it be?
        double perceivedElevation = comparisonDistance * Math.tan(thisPeaksAngle);

        //Log.d("Hi", "Perceived height, got from a distance of " + comparisonDistance + " and an angle of " + thisPeaksAngle + " was calculated as " + perceivedElevation);
        return perceivedElevation - comparisonElevation;
    }

    static List<Result> findDiffBetweenElevations(List<Result> highPoints)
    {
        double firstDistance = highPoints.get(0).getDistance();
        double firstElevation = firstDistance * Math.tan(highPoints.get(0).getAngle());

        for (Result highPoint : highPoints)
            highPoint.setDifference(diffFromFirst(firstDistance, highPoint.getAngle(), firstElevation) + firstElevation);

        return highPoints;
    }

}
