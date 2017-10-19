package com.example.recogniselocation.thirdyearproject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.xPos;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yPos;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    private Exception e;
    private double yourElevation;
    private List<Result> highPoints= new ArrayList<>(MapsActivity.noOfPaths);
    LineGraphSeries<DataPoint> series = new LineGraphSeries<>();

    protected List<String> doInBackground(String... urls) {
        return connectToURL(urls[0]);
    }

    private List<String> connectToURL(String urls) {
        // As we appended "url," every time, we need to remove the last splitter
        urls = urls.substring(0, urls.length() - 1);

        // The URLs are comma separated. Split and do the same for each
        String[] urlArr = urls.split("!");
        for (String url : urlArr) {
            Log.d("Hi", "URL: " + url);
        }

        List<String> responseList = new ArrayList<>(MapsActivity.noOfPaths);

        String inputLine;
        HttpURLConnection con = null;
        BufferedReader in;

        for (String url : urlArr) {
            try {
                URL urlObj = new URL(url);
                con = (HttpURLConnection) urlObj.openConnection();

                // If the connection was successful
                if (con.getResponseCode() == 200) {
                    // Get the response
                    con.setRequestMethod("GET");
                    con.connect();

                    //Todo: Check that I did get all 10 results

                    // Build up the response in a string
                    StringBuilder response = new StringBuilder();
                    in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }

                    in.close();
                    responseList.add(response.toString());
                } else {
                    Log.d("Hi", "The connection wasn't successful: " + con.getResponseMessage());
                }
            } catch(Exception e) {
                Log.d("Hi", "Problem connecting to URL: " + e.toString());
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }
        return responseList;
    }

    protected void onPostExecute(List<String> responses) {
        int isFirstResponse = 1;

        for (String response : responses) {
            // If we got a response, parse it
            if (response != null) {
                try {
                    // Parse the JSON response
                    Gson gson = new Gson();
                    Response results = gson.fromJson(response, Response.class);

                    // If the results came back correctly
                    if (results != null) {
                        if (isFirstResponse == 1) {
                            isFirstResponse = 0; // Treat the others differently, they are paths
                            yourElevation = results.getResults().get(0).getElevation();
                        } else
                            findHighestVisiblePoint(results);
                    }
                } catch(Exception e){
                    Log.d("Hi", "On post execute failure\n" + e);
                }

            }
        }

        Log.d("Hi", "Got all highest points:");
        for (Result highPoint : highPoints)
            Log.d("Hi", highPoint.toString());

        findDifferenceBetweenPoints(highPoints);

        plotPoints(googleMap, highPoints, xPos, yPos);

        // Draw the horizon
        drawOnGraph(highPoints);

    }

    private void findDifferenceBetweenPoints(List<Result> highPoints) {
        double firstAng = highPoints.get(0).getAngle();

        for (Result highPoint : highPoints)
            highPoint.setDifference(diffFromFirst(firstAng, highPoint.getDistance(), highPoint.getElevation()));
    }

    public void findHighestVisiblePoint(Response results) {
        // Find the highest visible point
        double hiLat, hiLng, hiEl, hiDis;
        hiLat = hiLng = hiEl = hiDis = 0;
        // Say that the current highest is the first, compare with the rest
        double currentHighestAng = Math.atan(
                (results.getResults().get(0).getElevation() - yourElevation) /    // First one away
                        0.1 * (1.0 / 10.0));                                // from you, i.e. step


        // Go through each result to see if you can see any that are higher
        int loopCount = 1;
        for(Result r : results) {
            if (loopCount > 1) {    //We're comparing the first against the rest
                double thisOnesDistance = 0.1 * loopCount / 10; //Fraction of path length we're at now
                double angleOfThisElevation = Math.atan(
                        (r.getElevation() - yourElevation) /
                                thisOnesDistance);  // Distance of the first one away
                // from you, i.e. step
                if (angleOfThisElevation > currentHighestAng) {
                    hiEl = r.getElevation() - yourElevation;
                    hiLat = r.getLocation().getLat();
                    hiLng = r.getLocation().getLng();
                    hiDis = thisOnesDistance;
                    currentHighestAng = angleOfThisElevation;   //ToDo: do I need to set all these?
                }
            }
            loopCount++;
        }
        if (hiDis != 0) { // If we found a highest visible peak
            highPoints.add(new Result(
                                    new LatLng(hiLat, hiLng),
                                    hiEl,
                                    hiDis,
                                    currentHighestAng,
                                    0));
        }
    }

    // Draw a line around the points, add a marker to where you are
    private void plotPoints(GoogleMap map, List<Result> highPoints, double x, double y) {
        // Centre the camera around the middle of the points and your location
        double avLat = (x
                + highPoints.get(MapsActivity.noOfPaths / 2).getLocation().getLat())
                / 2;
        double avLng = (y
                + highPoints.get(MapsActivity.noOfPaths / 2).getLocation().getLng())
                / 2;
        MapsActivity.goToLocation(avLat, avLng, 12);

        addMarkerAt(map, x, y, "You are here!");

        // Plot a line and add markers for each of the visible peaks
        showVisiblePeaks(highPoints);
    }

    // Add marker to map at  x and y that says the string
    private void addMarkerAt(GoogleMap map, double x, double y, String msg) {
        map.addMarker(new MarkerOptions()
                .title(msg)
                .position(new com.google.android.gms.maps.model.LatLng(x, y)));
    }

    // Add marker to map at  x and y with no message
    private void addMarkerAt(GoogleMap map, double x, double y) {
        map.addMarker(new MarkerOptions()
                .title("You are here!")
                .position(new com.google.android.gms.maps.model.LatLng(x, y)));
    }

    private void showVisiblePeaks(List<Result> highPoints) {
        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.YELLOW);

        for (Result highPoint : highPoints) {
            // Show the path of the peaks
            polylineOptions.add(new com.google.android.gms.maps.model.LatLng(
                    highPoint.getLocation().getLat(),
                    highPoint.getLocation().getLng()));

            // Show a marker at each peak if there aren't many
            //  - Many markers looks cluttered
            if (MapsActivity.noOfPaths <= 15)
                addMarkerAt(MapsActivity.googleMap, highPoint.getLocation().getLat(), highPoint.getLocation().getLng());

        }
        MapsActivity.googleMap.addPolyline(polylineOptions);
    }

    private double diffFromFirst(double comparisonAngle, double thisPeaksDistance, double thisElevation) {
        double perceivedHeight = thisPeaksDistance * Math.tan(comparisonAngle);
        Log.d("Hi", "perceived height, got from a distance of " + thisPeaksDistance + " and an angle of " + comparisonAngle + " was calculated as " + perceivedHeight
        + "\nThis elevation is " + thisElevation + ", therefore, the difference is " + (thisElevation-perceivedHeight));
        return thisElevation - perceivedHeight;
    }

    private void drawOnGraph(List<Result> points) {
        double distanceBetweenPlots = 5;
        int count = 0;

        Log.d("Hi", "The first result should be at 0,0.. should it? Think about it");
        for (Result highPoint : points) {
            double x = count++ * distanceBetweenPlots;
            double y = highPoint.getDifference();
            Log.d("Hi", "Plotting at " + x + ", " + y);
            series.appendData(new DataPoint(x,y), true, points.size());
        }
        MapsActivity.graph.addSeries(series);
    }
}