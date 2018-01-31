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

public class APIFunctions {

    private static int widthOfSearch = 160;
    static int noOfPaths = widthOfSearch / 4;
    private static int noOfPathsPerGroup = 5;   // (This + duplicates) * samplesPerPath needs to be <= 512. More middle paths causes more distortion so only 3 mid is ok
    static int samplesPerPath = widthOfSearch / 6;
    static double searchLength = 0.1;  // radius of the search
    private static final int LONLAT_TO_METRES = 111111; // roughly

    // MapsActivity calls this once it knows your direction and location
    static void getElevations(double dir, LatLng loc, Activity activity)
    {
        Log.d("APIFunctions", "Building up URLs to request");
        List<String> urls = getURLsToRequest(dir, loc, widthOfSearch, noOfPaths, noOfPathsPerGroup,
                samplesPerPath, searchLength, activity.getString(R.string.google_maps_key));

        // Requesting the elevations from the Google Maps API
        Log.d("APIFunctions", "Requesting URLs " + urls);
        try { new RetrieveURLTask(activity).execute(urls); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // Returns a list of the URLs to request
    public static List<String> getURLsToRequest(double dir, LatLng loc, int widthOfSearch,
                                                int noOfPaths, int noOfPathsPerGroup,
                                                int samplesPerPath, double searchLength, String key) {

        double step = widthOfSearch / (noOfPaths - 1);
        double start = dir + step/2 + step*(noOfPaths/2-1);
        List<String> urls = new ArrayList<>();
        StringBuilder url = new StringBuilder();
        int samplesPerGroup = getSamplesPerGroup(noOfPathsPerGroup, samplesPerPath);
        int i = 0;

        for (; i < noOfPaths; i++) {
            // Coordinate at the end of path i
            LatLng endCoordinate = getEndCoordinate(i, start, step, loc, searchLength);

            // Use this point to build up the next path in the request
            // Each request has *noOfPathsPerGroup* paths - can only request 512 samples per request

            // First path of a group
            if (i % noOfPathsPerGroup == 0) {
                // Begin the html and start your path at this end coordinate then go back to your location
                url.append("https://maps.googleapis.com/maps/api/elevation/json?path=") //Todo: use url_start from strings.xml
                        .append(endCoordinate).append("|")
                        .append(loc);
            }
            // Path is in the middle of this group and isn't the last path we have, ever
            else if (i % noOfPathsPerGroup < noOfPathsPerGroup-1 && noOfPaths - i != 1)
                // Go to this end coordinate and then back to your location
                url.append("|").append(endCoordinate).append("|").append(loc);
            // Last path of a group
            else
                url.append("|").append(endCoordinate);

            // End of this group - finish off the URL
            if (i % noOfPathsPerGroup == noOfPathsPerGroup - 1) {
                url.append("&samples=").append(samplesPerGroup).append("&key=").append(key);

                // Make sure this URL gets returned later, and clear it for any next URLs
                urls.add(url.toString());
                url = new StringBuilder();
            }
        }
        i--;    // Stay at the last used index to perform next calculations more understandably

        // If we ended in the middle of a group, don't forget the end of the url
        int noOfPathsInThisGroup;
        if ((noOfPathsInThisGroup = i % noOfPathsPerGroup + 1) < noOfPathsPerGroup){
            samplesPerGroup = getSamplesPerGroup(noOfPathsInThisGroup, samplesPerPath);
            url.append("&samples=").append(samplesPerGroup).append("&key=").append(key);
            urls.add(url.toString());
        }

        return urls;
    }

    private static LatLng getEndCoordinate(int i, double start, double step, LatLng loc,
                                           double searchLength) {

        double sin = Math.sin(Math.toRadians(((start - i * step) % 360 + 360) % 360));
        double cos = Math.cos(Math.toRadians(((start - i * step) % 360 + 360) % 360));

        return new LatLng(
                loc.getLat() + searchLength * sin,
                loc.getLng() + searchLength * cos);
    }


    // Get the number of elevations you need per group of paths
    private static int getSamplesPerGroup(int noOfPathsPerGroup, int samplesPerPath) {
        int samplesPerGroup;
        if (noOfPathsPerGroup >= 3)
            samplesPerGroup = samplesPerPath * noOfPathsPerGroup * 2 - samplesPerPath * 2 + 1;
        else
            samplesPerGroup = samplesPerPath * noOfPathsPerGroup + 1;
        if (samplesPerGroup > 512) {
            Log.e(TAG, "getElevations: Requesting too many samples. Capping at 512 but you should ask for less samples per path");
            samplesPerGroup = 512;
        }
        return samplesPerGroup;
    }

    // Given a string of URLs, send the requests and return the responses
    static List<String> requestURL(List<String> urls)
    {
        Log.d(TAG, "requestURL: Got " + urls.size() + " urls.. " + urls);

        List<String> urlResponses = new ArrayList<>(); // Store each response
        StringBuilder response;   // StringBuilders are better for appending in a while
        HttpURLConnection con = null;

        for (String url : urls)
        {
            Log.d(TAG, "requestURL: Trying URL " + url);
            response = new StringBuilder(); // clearing the string builder each time
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
        int distanceUnit = 1;
        double hiLat, hiLng, hiEl, hiDis;
        hiLat = hiLng = hiEl = hiDis = 0;

        for (Result r : path) {
            double thisOnesDistance = (searchLength * LONLAT_TO_METRES)
                    * distanceUnit++ / samplesPerPath;
            double angleOfThisElevation = Math.atan(
                    (r.getElevation() - yourElevation) / thisOnesDistance); // Distance of the first one away
                                                                            // from you, i.e. step
            if (angleOfThisElevation > currentHiAng) {
                /*Log.d(TAG, "getHighestVisiblePoint: Ooh ("+ r.getLocation() +") at distance "
                        + thisOnesDistance + "\t at elevation " + r.getElevation() + " at angle " + angleOfThisElevation
                        + "\t is bigger than our current max " + currentHiAng);*/
                hiLat = r.getLocation().getLat();
                hiLng = r.getLocation().getLng();
                hiEl = r.getElevation() - yourElevation;
                hiDis = thisOnesDistance;
                currentHiAng = angleOfThisElevation;
            }
        }
        double highestAngle = currentHiAng;

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
