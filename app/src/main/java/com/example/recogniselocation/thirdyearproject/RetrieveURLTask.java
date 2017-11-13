package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.media.Image;
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
        double distanceBetweenPlots = MapFunctions.findDistanceBetweenPlots(highPoints.get(0));
        drawOnGraph(highPoints, distanceBetweenPlots, horizonCoords);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        horizonCoords = convertCoordSystem(horizonCoords);

        // Detect the edge from an image
        EdgeDetection edgeDetection;
        List<List<Integer>> edgeCoords;
        edgeDetection = ImageToDetect.detectEdge(BitmapFactory.decodeResource(activity.getResources(), R.drawable.blencathra));
        edgeCoords = edgeDetection.coords;


        // Find the peak coordinates for the
        // constructed elevation horizon and the edge detected points
        Point elevationPeak = getElevPeak(horizonCoords, distanceBetweenPlots);
        Point detectedPeak = getDetectedPeak(edgeCoords);
        Log.d("Hi", "Peak of the elevations is " + elevationPeak.getX()+ ", " + elevationPeak.getY());
        Log.d("Hi", "Peak of edge detection is " + detectedPeak.getX() + ", " + detectedPeak.getY());   // Todo: This one doesn't seem right

        double diffX = detectedPeak.getX() / elevationPeak.getX();
        double diffY = detectedPeak.getY() / elevationPeak.getY();

        Log.d("Hi", "Elevation coords " + horizonCoords.toString());
        Log.d("Hi", "Multiply all the elevation coords by " + diffX + " and " + diffY);
        for (int i = 0; i < horizonCoords.size(); i++)  // Allowing for the peak, which is 0
            horizonCoords.set(i, (int)(horizonCoords.get(i) * diffY));
        Log.d("Hi", "Multiplied the y of elevation coords " + horizonCoords.toString());


    }

    private Point getDetectedPeak(List<List<Integer>> edgeCoords) {
        // Find coordinates of the edge detected peak
        // edgeCoords now is a list of the y coords for each column
        // if thinning has been done, there will only be one entry in each column list
        // Todo: Should we assume thinning has been done and use only a List<Integer>?
        // The spacing of the lists are incremental by one, to save space
        // but they represent points that are fineWidth apart (plus fineWidthFromCentre)
        int detectedPeakY, count, peakIndex;
        count = peakIndex = 0;
        detectedPeakY = 100000000;  // Initialise to massive value so that we can find smaller
        for (List<Integer> col : edgeCoords) {
            //Assuming thinning has been done, only one y coord per col
            if(col.size() > 0 && col.get(0) < detectedPeakY) {
                detectedPeakY = col.get(0);
                peakIndex = count;
            }
            count++;
        }
        int detectedPeakX = (peakIndex * ImageToDetect.fineWidth + ImageToDetect.fineWidthFromCentre);

        return new Point(detectedPeakX, detectedPeakY);
    }

    private Point getElevPeak(List<Integer> horizonCoords, double distanceBetweenPlots) {
        // Find the coordinates of the elevations peak (assuming there's only one)
        // The conversion of the coord system would have set the highest y as 1
        int elevPeakY = 1;
        // Coords are without the distance between, to save space. Consider it here
        int elevPeakX = (int) (horizonCoords.indexOf(elevPeakY) * distanceBetweenPlots);

        return new Point(elevPeakX, elevPeakY);
    }

    // From right up being positive to right down
    private List<Integer> convertCoordSystem(List<Integer> coords)
    {
        int maxY = findMaxY(coords);

        // Now we know how tall the y axis can go we can convert the coordinate system
        // by flipping the y coordinates
        for (int i = 0; i < coords.size(); i++)
            coords.set(i, maxY - coords.get(i) + 1);    // +1 deals with divide by zero issues

        return coords;
    }

    private int findMaxY(List<Integer> coords) {
        // Get max y
        int maxY = 0;
        for (Integer y : coords) {
            if (y > maxY)
                maxY = y;
        }
        return maxY;
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