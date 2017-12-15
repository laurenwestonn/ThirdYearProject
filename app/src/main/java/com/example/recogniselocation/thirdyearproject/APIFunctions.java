package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class APIFunctions {

    public static int noOfPaths = 60;
    public static int widthOfSearch = 180;
    public static int noOfSamples = 20;
    public static double searchLength = 0.1;  // radius of the search

    // MapsActivity calls this once it knows your direction and location
    static void getElevations(double dir, LatLng loc, Activity activity, String key)
    {
        Log.d("APIFunctions", "Building up the URLs");
        double step = widthOfSearch / (noOfPaths - 1);
        double start = dir + step/2 + step*(noOfPaths/2-1);
        List<LatLng> endCoords = new ArrayList<>();
        List<LatLng> startCoords = new ArrayList<>();

        // Get the coordinates of the start and the end of each path
        for (int i = 0; i < noOfPaths; i++) {
            double sin = Math.sin(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            double cos = Math.cos(Math.toRadians(((start - i * step) % 360 + 360) % 360));
            // Start from the first position away from you in each direction
            startCoords.add(new LatLng(
                    loc.getLat() + searchLength / noOfSamples * sin,
                    loc.getLng() + searchLength / noOfSamples * cos
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
                + loc.getLat() + "," + loc.getLng() + "&key=" + key + "!");
        // The other requests are to get elevations along paths
        for (int i = 0; i < noOfPaths; i++)
            urls.append("https://maps.googleapis.com/maps/api/elevation/json?path=")
                    .append(startCoords.get(i).getLat()).append(",")
                    .append(startCoords.get(i).getLng()).append("|")
                    .append(endCoords.get(i).getLat()).append(",")
                    .append(endCoords.get(i).getLng()).append("&samples=")
                    .append(noOfSamples).append("&key=").append(key).append("!");

        // Requesting the elevations from the Google Maps API
        Log.d("APIFunctions", "Requesting URLs");
        try { new RetrieveURLTask(activity).execute(urls.toString()); }
        catch (Exception e) { e.printStackTrace(); }
    }

    // Given a string of concatenated URLS, send the requests and return the responses
    static List<String> requestURLS(String urls, String separator)
    {
        // Get an array of the URLs, taking into account that "!" was appended after each URL.
        String urlsString = urls.substring(0, urls.length() - 1);
        String[] urlArr = urlsString.split(separator);

        List<String> responseList = new ArrayList<>(noOfPaths);

        for (String url : urlArr) {
            HttpURLConnection con = null;
            try {
                URL urlObj = new URL(url);
                con = (HttpURLConnection) urlObj.openConnection();

                // If the connection was successful
                if (con.getResponseCode() == 200) {
                    // Get the response
                    con.setRequestMethod("GET");
                    con.connect();

                    //Todo: Check that I did get all 10 results!

                    // Build up the response in a string
                    StringBuilder response = new StringBuilder();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);

                    in.close();
                    responseList.add(response.toString());
                } else  Log.d("Hi", "Connection failed: " + con.getResponseMessage());
            } catch(Exception e) {
                Log.d("Hi", "Problem connecting to URL: " + e.toString());
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }
        return responseList;
    }

    // Interpret the string responses into response object
    static List<Result> getHighestVisiblePoints(List<String> stringResponses)
    {
        List<Result> highPoints= new ArrayList<>();
        boolean isFirstResponse = true;
        double yourElevation = 0;

        // Parse any stringResponses, find highest visible point in each path
        for (String response : stringResponses)
            if (response != null) {
                    Response results = new Gson().fromJson(response, Response.class);
                    if (results != null) {
                        if (isFirstResponse) {  //Treat first differently, is just your elevation
                            isFirstResponse = false;
                            yourElevation = results.getResults().get(0).getElevation();
                        } else  // Response is a path of elevations
                            highPoints.add(MapFunctions.findHighestVisiblePoint(results, yourElevation));
                    }
            }

        if (highPoints.size() != (stringResponses.size() - 1))
            Log.e("InterpretResponses", "getHighestVisiblePoints: Didn't find " + (stringResponses.size()-1) + " responses");

        return highPoints;
    }
}
