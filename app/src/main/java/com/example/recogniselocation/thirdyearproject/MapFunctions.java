package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

import static com.example.recogniselocation.thirdyearproject.MapsActivity.noOfPaths;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.widthOfSearch;

/**
 * Created by LaUrE on 12/11/2017.
 */

public class MapFunctions extends Activity {

    public int Ltest = 1;
    private static final int LONLAT_TO_METRES = 111111; // roughly

    // Draw a line around the points, add a marker to where you are
    public static void plotPoints(GoogleMap map, List<Result> highPoints, double x, double y)
    {
        // Centre the camera around the middle of the points and your location
        double avLat = (x
                + highPoints.get(noOfPaths / 2).getLocation().getLat())
                / 2;
        double avLng = (y
                + highPoints.get(noOfPaths / 2).getLocation().getLng())
                / 2;
        MapsActivity.goToLocation(avLat, avLng, 12);

        addMarkerAt(map, x, y, "You are here!");

        // Plot a line and add markers for each of the visible peaks
        showVisiblePeaks(highPoints);
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
                addMarkerAt(MapsActivity.googleMap, highPoint.getLocation().getLat(), highPoint.getLocation().getLng());

        }
        MapsActivity.googleMap.addPolyline(polylineOptions);
    }

    public static double findDistanceBetweenPlots(Result comparisonPoint)
    {
        double step = widthOfSearch / (noOfPaths - 1);
        double distanceToFirstPeakInMetres = comparisonPoint.getDistance();

        return distanceToFirstPeakInMetres / Math.sin(Math.toRadians((180-step) / 2))
                * Math.sin(Math.toRadians(step));
    }

    private static double diffFromFirst(double comparisonDistance, double thisPeaksAngle, double comparisonElevation)
    {
        // If this peak was at the distance of the first one, how big would it be?
        double perceivedElevation = comparisonDistance * Math.tan(thisPeaksAngle);

        //double perceivedElevation = thisPeaksDistance * Math.tan(comparisonAngle);
        Log.d("Hi", "Perceived height, got from a distance of " + comparisonDistance + " and an angle of " + thisPeaksAngle + " was calculated as " + perceivedElevation);
        return perceivedElevation - comparisonElevation;
    }

    public static void findDiffBetweenElevations(List<Result> highPoints)
    {
        double firstDistance = highPoints.get(0).getDistance();
        double firstElevation = firstDistance * Math.tan(highPoints.get(0).getAngle());

        for (Result highPoint : highPoints)
            highPoint.setDifference(diffFromFirst(firstDistance, highPoint.getAngle(), firstElevation) + firstElevation);
    }

    public static void findHighestVisiblePoint(Response results, double yourElevation, List<Result> highPoints)
    {
        // Find the highest visible point
        double hiLat, hiLng, hiEl, hiDis;
        hiLat = hiLng = hiEl = hiDis = 0;

        // Initial 'highest' value is small so that it will get overridden
        double currentHighestAng = 0;

        // Go through each result to see if you can see any that are higher
        int loopCount = 1;
        for(Result r : results) {
            //Fraction of path length we're at now
            double thisOnesDistance = (MapsActivity.lengthOfSearch * LONLAT_TO_METRES) * loopCount / MapsActivity.noOfSamples;
            double angleOfThisElevation = Math.atan(
                    (r.getElevation() - yourElevation) / thisOnesDistance);  // Distance of the first one away
            // from you, i.e. step
            if (angleOfThisElevation > currentHighestAng) {
                hiLat = r.getLocation().getLat();
                hiLng = r.getLocation().getLng();
                hiEl = r.getElevation() - yourElevation;
                hiDis = thisOnesDistance;
                currentHighestAng = angleOfThisElevation;
            }
            loopCount++;
        }
        double highestAngle = currentHighestAng;
        if (highestAngle != 0) { // If we found a highest visible peak
            highPoints.add(new Result(
                    new LatLng(hiLat, hiLng),
                    hiEl,
                    hiDis,
                    highestAngle,
                    0));
        } else {
            Log.e("Hi", "Didn't find a high point, don't add to highPoints");
        }
    }
}
