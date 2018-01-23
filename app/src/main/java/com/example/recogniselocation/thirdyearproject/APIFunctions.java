package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.util.Log;

import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

class APIFunctions {

    static int noOfPaths = 20;
    static int noOfPathsPerGroup = 8;
    private static int widthOfSearch = 180;
    static int samplesPerPath = 20;
    static double searchLength = 0.1;  // radius of the search

    // MapsActivity calls this once it knows your direction and location
    static void getElevations(double dir, LatLng loc, Activity activity, String key)
    {
        Log.d("APIFunctions", "Building up the URL");
        double step = widthOfSearch / (noOfPaths - 1);
        double start = dir + step/2 + step*(noOfPaths/2-1);
        List<LatLng> endCoords = new ArrayList<>();

        //Todo: Simplify these two for loops into one
        // Get the lon lat of the end of each path
        for (int i = 0; i < noOfPaths; i++) {
            double sin = Math.sin(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            double cos = Math.cos(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            // End at the length of your search in each direction
            endCoords.add(new LatLng(
                    loc.getLat() + searchLength * sin,
                    loc.getLng() + searchLength * cos
            ));
        }

        // Use these coordinates to build up each web request, containing *noOfPathsPerGroup* paths
        StringBuilder urls = new StringBuilder("");
        //Todo: Figure this out (below). How to find number of samples for the last group? May not be *noOfPathsPerGroup* long
        int samplesPerGroup = 0; //samplesPerPath * noOfPathsPerGroup * 2 - samplesPerPath * 2 + 1;
        int i = 0;

        // The other requests are to get elevations along paths
        for (; i < noOfPaths; i++) {
            // Have to do in groups of paths as can only request 512 samples a day
            if (i % noOfPathsPerGroup == 0) // First path of a group
                // Begin the html and start your path at this end coordinate then go back to your location
                urls.append("https://maps.googleapis.com/maps/api/elevation/json?path=")
                        .append(endCoords.get(i)).append("|")
                        .append(loc);
            else if (i % noOfPathsPerGroup < noOfPathsPerGroup-1)    // Paths in the middle of a group
                // Go to this end coordinate and then back to your location
                urls.append("|").append(endCoords.get(i)).append("|").append(loc);
            else    // Last path of a group
                // Go to this last end coordinate for this path and a splitter to say we've finished
                urls.append("|").append(endCoords.get(i))
                        .append("&samples=").append(samplesPerGroup).append("&key=").append(key)
                        .append("!");   // Splitter to mark the end of this group
        }

        // If we ended in the middle of a path, don't forget the end of the url
        if (i % noOfPathsPerGroup < noOfPathsPerGroup-1)
            urls.append("&samples=").append(samplesPerGroup).append("&key=").append(key);

        /*
        // Get the coordinates of the start and the end of each path
        for (int i = 0; i < noOfPaths; i++) {
            double sin = Math.sin(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            double cos = Math.cos(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            // Start from the first position away from you in each direction
            startCoords.add(new LatLng(
                    loc.getLat() + searchLength / samplesPerPath * sin,
                    loc.getLng() + searchLength / samplesPerPath * cos
            ));
            // End at the length of your search in each direction
            endCoords.add(new LatLng(
                    loc.getLat() + searchLength * sin,
                    loc.getLng() + searchLength * cos
            ));
        }

        // Use these coordinates to build up web requests

        // The first request is to get the elevation of where you are
        StringBuilder urls = new StringBuilder("https://maps.googleapis.com/maps/api/elevation/json?locations="
                + loc.toString() + "&key=" + key + "!");
        // The other requests are to get elevations along paths
        for (int i = 0; i < noOfPaths; i++)
            urls.append("https://maps.googleapis.com/maps/api/elevation/json?path=")
                    .append(startCoords.get(i).toString()).append("|")
                    .append(endCoords.get(i).toString()).append("&samples=")
                    .append(samplesPerPath).append("&key=").append(key).append("!");
        */

        // Requesting the elevations from the Google Maps API
        Log.d("APIFunctions", "Requesting URLs " + urls.toString());
        try { new RetrieveURLTask(activity).execute(urls.toString()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // Given a string of URLs, send the requests and return the responses
    static List<String> requestURL(String urls)
    {
        String[] urlArr = urls.substring(0, urls.length()).split("!");

        List<String> urlResponses = new ArrayList<>();
        HttpURLConnection con = null;
        StringBuilder response = new StringBuilder();   //Todo: Understand; do I need a builder?

        for (String url : urlArr)
        {
            Log.e(TAG, "requestURL: Trying url " + url);
            try {
                URL urlObj = new URL(url);
                con = (HttpURLConnection) urlObj.openConnection();

                // If the connection was successful
                if (con.getResponseCode() == 200) {
                    // Get the response
                    con.setRequestMethod("GET");
                    con.connect();

                    //Todo: Check that I did get all results!
                    // Build up the response in a string
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);
                    urlResponses.add(response.toString());

                    in.close();
                } else Log.e("Hi", "Connection failed: " + con.getResponseMessage());
            } catch(Exception e) {
                Log.e("Hi", "Problem connecting to URL: " + e.toString());
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }

        return urlResponses;
    }

    // Interpret the string response into response object
    static List<Result> getHighestVisiblePoints(String response)
    {
        List<Result> highPoints= new ArrayList<>();
        double yourElevation = 0;

        // Parse any stringResponses, find highest visible point in each path
        if (response != null) {
            Response results = new Gson().fromJson(response, Response.class);
            if (results != null) {
                yourElevation = results.getResults().get(samplesPerPath).getElevation();
                highPoints = MapFunctions.findHighestVisiblePoints(results, yourElevation);
            }
        }

        // Find the differences between the elevations so we can plot them
        highPoints = MapFunctions.findDiffBetweenElevations(highPoints);

        return highPoints;
    }

    static List<Point> drawOnGraph(List<Result> points)
    {
        List<Point> horizonCoords = new ArrayList<>();

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        int count = -1;
        int x;
        double y;
        x = 0;

        for (Result point : points) {
            x = ++count * (int) findDistanceBetweenPlots(points.get(0));
            y = point.getDifference();

            horizonCoords.add(new Point(x,y));

            //Log.d("Hi", "Plotting at " + x + ", " + y);
            series.appendData(new DataPoint(x,y), true, points.size());
        }

        MapsActivity.graph.addSeries(series);
        setBounds(MapsActivity.graph,0,  x, series.getLowestValueY(), series.getHighestValueY());
        HorizonMatching.graphHeight =  series.getHighestValueY();

        return horizonCoords;
    }

    // Allows you to draw onto the graph
    public static double findDistanceBetweenPlots(Result comparisonPoint)
    {
        double step = widthOfSearch / (noOfPaths - 1);
        double distanceToFirstPeakInMetres = comparisonPoint.getDistance();

        return distanceToFirstPeakInMetres / Math.sin(Math.toRadians((180-step) / 2))
                * Math.sin(Math.toRadians(step));
    }

    static void setBounds(GraphView graph, double minX, double maxX, double minY, double maxY)
    {
        // Set bounds on the x axis
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(minX);
        graph.getViewport().setMaxX(maxX);
        // Set bounds on the y axis
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(minY);
        graph.getViewport().setMaxY(maxY);
    }
}
