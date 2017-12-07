package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import static com.example.recogniselocation.thirdyearproject.ImageToDetect.fineWidth;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.xPos;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yPos;

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    @SuppressLint("StaticFieldLeak")
    private Activity activity;
    private int photoID = R.drawable.rocky_mountains;

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
        double distanceBetweenPlots = MapFunctions.findDistanceBetweenPlots(highPoints.get(0));
        Log.d("Hi", "Distance between points is now " + distanceBetweenPlots 
                + " and as there are " + MapsActivity.noOfPaths + " paths, x axis goes up to " 
                + distanceBetweenPlots * MapsActivity.noOfPaths);
        List<Point> elevationsCoords = drawOnGraph(highPoints, distanceBetweenPlots);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        elevationsCoords = convertCoordSystem(elevationsCoords);

        // Detect the edge from an image
        EdgeDetection edgeDetection;
        edgeDetection = ImageToDetect.detectEdge(BitmapFactory.decodeResource(activity.getResources(), photoID));
        List<List<Integer>> edgeCoords2D = edgeDetection.coords;

        // Quick fix to simplify coordinates
        // It is originally a list of a list, to take into account many points in one column
        // but as thinning should have been used (but we may not have it 'on' to test
        // other algorithms) there should only be one point per column, so List<Int> will do
        List<Integer> edgeCoordsIntegers = HorizonMatching.removeDimensionFromCoords(edgeCoords2D);

        List<Point> edgeCoords = HorizonMatching.convertToPoints(edgeCoordsIntegers, fineWidth);

        // Match up the horizons
        HorizonMatching.matchUpHorizons(edgeCoords, elevationsCoords, edgeDetection.bmp, activity);
    }

    // From right up being positive to right down
    private List<Point> convertCoordSystem(List<Point> coords)
    {
        Point maxPoint = findMaxPoint(coords);

        // Now we know how tall the y axis can go we can convert the coordinate system
        // by flipping the y coordinates
        for (int i = 0; i < coords.size(); i++)
            coords.set(i, new Point(coords.get(i).getX(),
                        maxPoint.getY() - coords.get(i).getY() + 1)); // +1 deals with / 0
        return coords;
    }

    private Point findMaxPoint(List<Point> coords) {
        Point maxPoint = coords.get(0);

        for (Point p : coords) {
            if (p.getY() > maxPoint.getY())
                maxPoint = p;
        }
        return maxPoint;
    }

    private List<Point> drawOnGraph(List<Result> points, double distanceBetweenPlots)
    {
        List<Point> horizonCoords = new ArrayList<>();
        
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        int count = -1;
        int x;
        double y;
        x = 0;
        
        for (Result point : points) {
            x = ++count * (int)distanceBetweenPlots;
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