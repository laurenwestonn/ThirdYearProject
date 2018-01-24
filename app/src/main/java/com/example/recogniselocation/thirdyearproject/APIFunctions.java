package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.util.Log;

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
    private static final int LONLAT_TO_METRES = 111111; // roughly

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
        int samplesPerGroup = samplesPerPath * noOfPathsPerGroup * 2 - samplesPerPath * 2 + 1;
        int i = 0;

        // Have to get elevations of paths in groups as can only request 512 samples in one request
        for (; i < noOfPaths; i++) {
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
        if (i % noOfPathsPerGroup < noOfPathsPerGroup-1) {
            int noOfPathsInThisGroup = i % noOfPathsPerGroup;
            samplesPerGroup = samplesPerPath * noOfPathsInThisGroup * 2 - samplesPerPath * 2 + 1;
            urls.append("&samples=").append(samplesPerGroup).append("&key=").append(key);
        }

        // Requesting the elevations from the Google Maps API
        Log.d("APIFunctions", "Requesting URLs " + urls.toString());
        try { new RetrieveURLTask(activity).execute(urls.toString()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // Given a string of URLs, send the requests and return the responses
    static List<String> requestURL(String urls)
    {
        String[] urlArr = urls.substring(0, urls.length()).split("!");

        List<String> urlResponses = new ArrayList<>(); // Store each response
        StringBuilder response = new StringBuilder();   //Todo: Understand; do I need a builder?
        HttpURLConnection con = null;

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

    // Find highest visible point of this path
    static Result getHighestVisiblePoint(List<Result> path, double yourElevation)
    {
        double currentHiAng = Integer.MIN_VALUE;
        int loop = 0;
        double hiLat, hiLng, hiEl, hiDis;
        hiLat = hiLng = hiEl = hiDis = 0;

        for (Result r : path) {
            double thisOnesDistance = (searchLength * LONLAT_TO_METRES)
                    * (samplesPerPath-loop++) / samplesPerPath;
            double angleOfThisElevation = Math.atan(
                    (r.getElevation() - yourElevation) / thisOnesDistance); // Distance of the first one away
                                                                            // from you, i.e. step
            if (angleOfThisElevation > currentHiAng) {
                hiLat = r.getLocation().getLat();
                hiLng = r.getLocation().getLng();
                hiEl = r.getElevation() - yourElevation;
                hiDis = thisOnesDistance;
                currentHiAng = angleOfThisElevation;
            }
        }
        double highestAngle = currentHiAng;

        Log.d(TAG, "getHighestVisiblePoint: The highest point in path " + path.toString()
        + " \nis " + new Result(new LatLng(hiLat, hiLng),hiEl, hiDis, highestAngle,0 ).toString());

        if (highestAngle != Integer.MIN_VALUE) // If we found a highest visible peak
            return new Result(new LatLng(hiLat, hiLng),hiEl, hiDis, highestAngle,0 );
        else {
            Log.e("Hi", "Didn't find a high point here, don't add to highPoints");
            return null;
        }
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
