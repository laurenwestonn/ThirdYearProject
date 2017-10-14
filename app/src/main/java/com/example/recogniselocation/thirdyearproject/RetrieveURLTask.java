package com.example.recogniselocation.thirdyearproject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.recogniselocation.thirdyearproject.MapsActivity.xPos;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yPos;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    private Exception e;
    private double yourElevation;
    private List<Result> highPoints= new ArrayList<>(MapsActivity.noOfPaths) ;

    protected List<String> doInBackground(String... urls) {
        return connectToURL(urls[0]);
    }

    private List<String> connectToURL(String urls) {
        // As we appended "url," every time, we need to remove the last splitter
        urls = urls.substring(0, urls.length() - 1);

        // The URLs are comma separated. Split and do the same for each
        String[] urlArr = urls.split("!");

        List<String> responseList = new ArrayList<>(MapsActivity.noOfPaths);

        String inputLine;
        HttpURLConnection con = null;
        BufferedReader in;

        for (String url : urlArr) {
            try {
                Log.d("Hi", "URL:" + url);
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
            Log.d("Hi", "\nAnother response");
            // If we got a response, parse it
            if (response != null) {
                try {
                    // Parse the JSON response
                    Gson gson = new Gson();
                    Response results = gson.fromJson(response, Response.class);

                    // If the results came back correctly
                    if (results != null) {
                        if (isFirstResponse == 1) {
                            yourElevation = results.getResults().get(0).getElevation();
                            isFirstResponse = 0; // Treat the others differently, they are paths
                        } else {
                            findHighestVisiblePoints(results);
                            plotHighest();
                        }
                    }
                } catch(Exception e){
                    Log.d("Hi", "On post execute failure\n" + e);
                }

            }
        }
        Log.d("Hi", "Highest points are:");
        for (Result highPoint : highPoints)
            Log.d("Hi", highPoint.getLocation().toString());
    }

    private void plotHighest() {
        // Update your position to be the average latitude and longitude,
        // I'm using the average of your position, and the middle peak found
        double avLat = (xPos
                        + highPoints.get(MapsActivity.noOfPaths / 2).getLocation().getLat())
                        / 2;
        double avLng = (MapsActivity.yPos
                        + highPoints.get(MapsActivity.noOfPaths / 2).getLocation().getLng())
                        / 2;
        Log.d("Hi", "Average latitude is average of " + xPos + " + " + highPoints.get(MapsActivity.noOfPaths / 2).getLocation().getLat());
        Log.d("Hi", "Updating the location to be inbetween points: " + avLat + ", " + avLng);
        MapsActivity.goToLocation(avLat, avLng, 13);
        MapsActivity.googleMap.addMarker(new MarkerOptions()
                .title("You are here!")
                .position(new com.google.android.gms.maps.model.LatLng(
                        xPos,
                        yPos)));

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.YELLOW);

        for (Result highPoint : highPoints) {
            Log.d("Hi", "plotting at " + highPoint.getLocation());

            polylineOptions.add(new com.google.android.gms.maps.model.LatLng(
                    highPoint.getLocation().getLat(),
                    highPoint.getLocation().getLng()));

        }
        Polyline polyline = MapsActivity.googleMap.addPolyline(polylineOptions);
    }

    public void findHighestVisiblePoints(Response results) {
        // Find the highest visible point
        double hiLat, hiLng, hiEl, hiDis;
        hiLat = hiLng = hiEl = hiDis = 0;
        double hiAng = Math.atan(
                (results.getResults().get(0).getElevation() - yourElevation) /    // First one away
                        0.1 * (1.0 / 10.0));                                // from you, i.e. step

        // Go through each result to see if you can see any that are higher
        int loopCount = 1;
        for(Result r : results) {
            double thisOnesDistance = 0.1 * loopCount / 10; //Fraction of path length we're at now
            double angleOfThisElevation = Math.atan(
                    (r.getElevation() - yourElevation) /    //ToDo: Get your elevation and minus it
                            thisOnesDistance);  // Distance of the first one away
            // from you, i.e. step
            Log.d("Hi", r.toString());
            Log.d("Hi", "Looking at distance " + thisOnesDistance);
            Log.d("Hi", "Is " + angleOfThisElevation + " > " + hiAng + "?");
            if (loopCount > 1 && angleOfThisElevation > hiAng) {    // Initialised as the first step away, no need to check first
                Log.d("Hi", "Yes, update details");
                hiEl = r.getElevation() - yourElevation;
                hiLat = r.getLocation().getLat();
                hiLng = r.getLocation().getLng();
                hiDis = thisOnesDistance;
                hiAng = angleOfThisElevation;
            }
            loopCount++;
        }
        if (hiDis != 0) // We're not looking at distance 0, that is where you are
            highPoints.add(new Result(new LatLng(hiLat, hiLng), hiEl, hiDis, hiAng));
    }
}