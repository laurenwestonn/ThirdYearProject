package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class FunctionsAPI {

    private static int widthOfSearch = 360;
    static int noOfPaths = widthOfSearch / 4;
    private static int noOfPathsPerGroup = 2;   // AT LEAST 2. Any higher introduces skew (This + duplicates) * samplesPerPath needs to be <= 512. More middle paths causes more distortion so only 3 mid is ok
    static int samplesPerPath = widthOfSearch / 6;
    private static double searchLength = 0.05;  // radius of the search
    private static final int LONLAT_TO_METRES = 111111; // roughly

    // This is called at the start, once you know your location and direction
    static void getElevations(LocationDirection locDir, Activity activity)
    {
        Log.d("FunctionsAPI", "Building up URLs to request");
        List<String> urls = getURLsToRequest(locDir, widthOfSearch, noOfPaths, noOfPathsPerGroup,
                samplesPerPath, searchLength, activity.getString(R.string.google_maps_key),
                activity.getString(R.string.url_start));

        // Requesting the elevations from the Google Maps API
        Log.d("FunctionsAPI", "Requesting URLs " + urls);
        try { new FunctionsRetrieveURLs(activity).execute(urls); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // Returns a list of the URLs to request
    public static List<String> getURLsToRequest(LocationDirection locDir, int widthOfSearch,
                                                int noOfPaths, int noOfPathsPerGroup,
                                                int samplesPerPath, double searchLength, String key,
                                                String urlStart)
    {
        if (noOfPathsPerGroup < 2) {
            Log.e(TAG, "getElevations: Must request at least 2 paths per request."
                    + " You're requesting " + noOfPathsPerGroup);
            return null;
        }

        double step = widthOfSearch / (noOfPaths - 1);
        double start = locDir.getDirection() + step/2 + step*(noOfPaths/2-1);
        List<String> urls = new ArrayList<>();
        StringBuilder url = new StringBuilder();
        int samplesPerGroup = getSamplesPerGroup(noOfPathsPerGroup, samplesPerPath);
        LatLng loc = locDir.getLocation();
        int i = 0;

        for (; i < noOfPaths; i++) {
            // Coordinate at the end of path i
            LatLng endCoordinate = getEndCoordinate(i, start, step, loc, searchLength);

            // Use this point to build up the next path in the request
            // Each request has *noOfPathsPerGroup* paths - can only request 512 samples per request

            // First path of a group
            if (i % noOfPathsPerGroup == 0) {
                // Begin the html and start your path at this end coordinate then go back to your location
                url.append(urlStart).append(endCoordinate).append("|").append(loc);
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
                                           double searchLength)
    {
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

                    // Build up the response in a string
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    int count = -5; // 5 start up and close lines in the response
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                        count++;
                    }
                    urlResponses.add(response.toString());

                    //Todo: Resend if you don't get enough samples
                    int noOfSamples = findNoOfSamples(url);
                    if ((count /= 8) != noOfSamples)    // Responses are 8 lines long
                        Log.e(TAG, "requestURL: Wanted to get " + noOfSamples
                                + " samples but only got " + count);

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

    // Find the number of samples requested in a given url
    private static int findNoOfSamples(String url) {
        StringBuilder strNoOfSamples = new StringBuilder();
        int noOfSamples;
        int indexOfSample = url.indexOf("&samples=") + 9;
        char newC;
        while (indexOfSample < url.length() && Character.isDigit(newC = url.charAt(indexOfSample++)))
            strNoOfSamples.append(newC);
        noOfSamples = Integer.parseInt(strNoOfSamples.toString());

        return noOfSamples;
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
                    (r.getElevation() - yourElevation) / thisOnesDistance);

            if (angleOfThisElevation > currentHiAng) {
                hiLat = r.getLocation().getLat();
                hiLng = r.getLocation().getLng();
                hiEl = r.getElevation() - yourElevation;
                hiDis = thisOnesDistance;
                currentHiAng = angleOfThisElevation;
            }
        }
        double highestAngle = currentHiAng;
        //Log.d(TAG, "getHighestVisiblePoint: Highest angle of that path is at " + hiLat + ", " + hiLng + " at an angle of " + highestAngle + " and a distance of " + hiDis);
        if (highestAngle != Integer.MIN_VALUE) // If we found a highest visible peak
            return new Result(new LatLng(hiLat, hiLng),hiEl, hiDis, highestAngle,0 );
        else {
            Log.e("Hi", "Didn't find a high point here, don't add to highPoints");
            return null;
        }
    }

    static List<Point> findGraphData(List<Result> points)
    {
        List<Point> horizonCoords = new ArrayList<>();

        int count = -1;
        int x;
        double y;
        int distBetwPoints = (int) findDistanceBetweenPlots(points.get(0));

        for (Result point : points) {
            x = ++count * distBetwPoints;
            y = point.getDifference();

            horizonCoords.add(new Point(x,y));
        }

        return horizonCoords;
    }

    // Allows you to draw onto the graph
    private static double findDistanceBetweenPlots(Result comparisonPoint)
    {
        double step = widthOfSearch / (noOfPaths - 1);
        double distanceToFirstPeakInMetres = comparisonPoint.getDistance();

        return distanceToFirstPeakInMetres / Math.sin(Math.toRadians((180-step) / 2))
                * Math.sin(Math.toRadians(step));
    }
}
