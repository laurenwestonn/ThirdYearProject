package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
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

import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.noOfPaths;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.xPos;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yPos;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    private double yourElevation;
    private List<Result> highPoints= new ArrayList<>(noOfPaths);

    public Activity activity;

    public RetrieveURLTask(Activity a)
    {
        this.activity = a;
    }

    protected List<String> doInBackground(String... urls)
    {
        return connectToURL(urls[0]);
    }

    private List<String> connectToURL(String urls)
    {
        // As we appended "url," every time, we need to remove the last splitter
        urls = urls.substring(0, urls.length() - 1);

        // The URLs are comma separated. Split and do the same for each
        String[] urlArr = urls.split("!");
        for (String url : urlArr) {
            Log.d("Hi", "URL: " + url);
        }

        List<String> responseList = new ArrayList<>(noOfPaths);

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

                    //Todo: Check that I did get all 10 results!

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

    protected void onPostExecute(List<String> responses)
    {
        int isFirstResponse = 1;
        int loop = 1;
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
                        } else {
                            MapFunctions.findHighestVisiblePoint(results, yourElevation, highPoints);
                        }
                    } else {
                        Log.e("Hi", "Results didn't come back correctly for " + loop);
                    }
                } catch(Exception e){
                    Log.e("Hi", "On post execute failure\n" + e);
                }
            } else {
                Log.e("Hi", "Response " + loop + " was null");
            }
            loop++;
        }

        /*
        Log.d("Hi", "Got all highest points:");
        for (Result highPoint : highPoints)
            Log.d("Hi", highPoint.toString());
        */

        MapFunctions.findDiffBetweenElevations(highPoints);

        MapFunctions.plotPoints(googleMap, highPoints, xPos, yPos);

        // Draw the horizon
        Log.d("Hi", "Distance between points is now " + MapFunctions.findDistanceBetweenPlots(highPoints.get(0)));
        List<Integer> horizonCoords = new ArrayList<>();
        drawOnGraph(highPoints, MapFunctions.findDistanceBetweenPlots(highPoints.get(0)), horizonCoords);

        EdgeDetection edgeDetection;
        List<List<Integer>> edgeCoords;
        edgeDetection = ImageToDetect.detectEdge(BitmapFactory.decodeResource(activity.getResources(), R.drawable.blencathra));
        edgeCoords = edgeDetection.coords;

        Log.d("Hi", "Flipped from " + horizonCoords);
        horizonCoords = convertCoordSystem(horizonCoords);
        Log.d("Hi", "to           " + horizonCoords);
    }

    private List<Integer> convertCoordSystem(List<Integer> coords)
    {
        // From right up being positive to right down
        // Flip the y coords

        // Get max y
        int maxY = 0;
        for (Integer y : coords) {
            if (y > maxY)
                maxY = y;
        }

        // Now we know how wide the y axis can go Todo: does this work for negative values?
        // we can convert the coordinate system
        for (int i = 0; i < coords.size(); i++)
            coords.set(i, maxY - coords.get(i));

        return coords;
    }

    private void drawOnGraph(List<Result> points, double distanceBetweenPlots, List<Integer> horizonCoords)
    {
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        int count = -1;
        double x, y;
        x = 0;
        
        for (Result point : points) {
            x = ++count * distanceBetweenPlots;
            y = point.getDifference();

            horizonCoords.add((int)y);

            Log.d("Hi", "Plotting at " + x + ", " + y);
            series.appendData(new DataPoint(x,y), true, points.size());
        }
        
        MapsActivity.graph.addSeries(series);
        setBounds(MapsActivity.graph,0,  x, series.getLowestValueY(), series.getHighestValueY());
    }

    private void setBounds(GraphView graph, double minX, double maxX, double minY, double maxY)
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