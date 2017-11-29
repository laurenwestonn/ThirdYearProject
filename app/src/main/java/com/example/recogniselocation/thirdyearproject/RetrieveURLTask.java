package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
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

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.noOfPaths;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.xPos;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yPos;

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    @SuppressLint("StaticFieldLeak")
    private Activity activity;

    RetrieveURLTask(Activity a)
    {
        this.activity = a;
    }

    protected List<String> doInBackground(String... urls)
    {
        return connectToURL(urls[0]);
    }

    private List<String> connectToURL(String urls)
    {
        // "!" was appended to the end of every URL. Get an array of the URLs
        urls = urls.substring(0, urls.length() - 1);
        String[] urlArr = urls.split("!");

        return Elevation.interpretURLResponses(urlArr);
    }

    protected void onPostExecute(List<String> responses)
    {
        List<Result> highPoints= new ArrayList<>();
        boolean isFirstResponse = true;
        double yourElevation = 0;

        // Parse any responses, find highest visible point in each path
        for (String response : responses)
            if (response != null)
                try {
                    Response results = new Gson().fromJson(response, Response.class);
                    if (results != null) {
                        if (isFirstResponse) {  //Treat first differently, is just your elevation
                            isFirstResponse = false;
                            yourElevation = results.getResults().get(0).getElevation();

                        } else  // Response is a path of elevations
                            highPoints.add(MapFunctions.findHighestVisiblePoint(results, yourElevation));
                    } else
                        Log.e("Hi", "Results didn't come back correctly");
                } catch(Exception e){
                    Log.e("Hi", "On post execute failure\n" + e);
                }
            else  Log.e("Hi", "Response was null");

        // We now have the highest peaks in all directions ahead.
        // Find the differences between these so we can show the horizon on the map
        highPoints = MapFunctions.findDiffBetweenElevations(highPoints);
        MapFunctions.showPointsOnMap(googleMap, highPoints, xPos, yPos);

        // Draw the horizon
        List<Integer> horizonCoords = new ArrayList<>();
        double distanceBetweenPlots = MapFunctions.findDistanceBetweenPlots(highPoints.get(0));
        Log.d("Hi", "Distance between points is now " + distanceBetweenPlots);
        drawOnGraph(highPoints, distanceBetweenPlots, horizonCoords);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        horizonCoords = convertCoordSystem(horizonCoords);

        // Detect the edge from an image
        EdgeDetection edgeDetection;
        edgeDetection = ImageToDetect.detectEdge(BitmapFactory.decodeResource(activity.getResources(), R.drawable.blencathra));
        List<List<Integer>> edgeCoords2D = edgeDetection.coords;

        // Quick fix to simplify coordinates
        // It is originally a list of a list, to take into account many points in one column
        // but as thinning should have been used (but we may not have it 'on' to test
        // other algorithms) there should only be one point per column, so List<Int> will do
        List<Integer> edgeCoords = HorizonMatching.removeDimensionFromCoords(edgeCoords2D);

/*
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
*/

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