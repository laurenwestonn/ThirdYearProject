package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginException;

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.getHighestVisiblePoint;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.samplesPerPath;
import static com.example.recogniselocation.thirdyearproject.ImageManipulation.fineWidth;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yourLocation;

public class RetrieveURLTask extends AsyncTask<List<String>, Void, List<String>>  {

    @SuppressLint("StaticFieldLeak")
    private Activity activity;
    private int photoID = R.drawable.wast_water_two;

    RetrieveURLTask(Activity a)
    {
        this.activity = a;
    }

    protected List<String> doInBackground(List<String>... urls)
    {
        Log.d("RetrieveURLTask", "Going to take some time getting the results from the API");
        return APIFunctions.requestURL(urls[0]);
    }

    protected void onPostExecute(List<String> strResponses)
    {
        Log.d("onPostExecute", "API gave response " + strResponses);
        List<Result> highPoints = new ArrayList<>();
        Gson gson = new GsonBuilder().setLenient().create(); //Todo is lenient needed? could just do new Gson instead, like I used to have.
        double yourElevation = gson.fromJson(strResponses.get(0), Response.class).getResults().get(samplesPerPath).getElevation();
        Log.d(TAG, "getHighestVisiblePoint: Your Elevation is " + yourElevation);

        for (String strResponse : strResponses) {
            // Convert this string response to a Response object
            gson = new GsonBuilder().setLenient().create(); //Todo is lenient needed? could just do new Gson instead, like I used to have.
            Response response = gson.fromJson(strResponse, Response.class);
            List<Result> results = response.getResults();
            List<Result> path = new ArrayList<>();
            int i = 0;

            for (; i < samplesPerPath; i++)
                path.add(results.get(samplesPerPath - i - 1));  // First path is backwards
            //Log.e(TAG, "onPostExecute: This first path is " + path);
            // Store only the highest point of this first path from the response
            // 1st param is the first path; 2nd is your location's elevation,
            // while skipping past this index as your location isn't part of the next path
            highPoints.add(getHighestVisiblePoint(path, yourElevation));
            /*if (strResponse.equals(strResponses.get(3))) {*/
                MapFunctions.addMarkerAt(googleMap, path.get(path.size()-1).getLocation().getLat(),
                        path.get(path.size()-1).getLocation().getLng(), "First end of a group at index " + (i - 1));
                MapFunctions.addMarkerAt(googleMap, path.get(0).getLocation().getLat(),
                        path.get(0).getLocation().getLng(), "First start of a group. Index 0 - " + (i - 1));
            /*}*/
            path.clear();

            i++; // Skip your location

            // If the group is just a start and an end path,
            // i.e. is only: (Index after) first path, skip one for your location, plus the last path
            // then skip your location to trigger code to add any 'last path's
            if (i + 1 + samplesPerPath == results.size())
                i++;

            /*if (strResponse.equals(strResponses.get(3)))
                Log.d(TAG, "onPostExecute: Skipped your location to be at the start of the next ones. " + i + " should be 31. 30 is your location");*/
            int startI;
            boolean duplicate = false;
            // The paths in the middle (these have duplicates where we've headed back to your location
            while (i < results.size() - samplesPerPath) {
                startI = i;

                // Use the ones heading from your location to the end of the path
                // Ignore the ones heading back to your location
                for (; i < startI + samplesPerPath; i++)
                    if (!duplicate)
                        path.add(results.get(i));

                if (path.size() != 0) {
                    highPoints.add(getHighestVisiblePoint(path, yourElevation));
                    //Log.e(TAG, "onPostExecute: a Mid path is " + path);
                    //Log.e(TAG, "onPostExecute: Hi point from that is " + highPoints.get(highPoints.size()-1));

                    /*
                    if (path.size() != 0 && strResponse.equals(strResponses.get(3))) {*/
                        MapFunctions.addMarkerAt(googleMap, path.get(path.size() - 1).getLocation().getLat(),
                                path.get(path.size() - 1).getLocation().getLng(), "End of a middle path at " + (i-1));
                        MapFunctions.addMarkerAt(googleMap, path.get(0).getLocation().getLat(),
                                path.get(0).getLocation().getLng(), "Start of a middle path: " + startI + " - " + (i-1));
                    /*}*/
                    path.clear();
                }

                // Next results will be the opposite - needed or not
                duplicate = !duplicate;

            }

            startI = i;
            // The last path
            for (; i < results.size(); i++) {
                path.add(results.get(i));
            }
            if (path.size() != 0) {
                //Log.e(TAG, "onPostExecute: Last path " + path);
                /*
                if (strResponse.equals(strResponses.get(3))) {*/
                    MapFunctions.addMarkerAt(googleMap, path.get(path.size() - 1).getLocation().getLat(),
                            path.get(path.size() - 1).getLocation().getLng(), "Last end of a group at " + i);
                    MapFunctions.addMarkerAt(googleMap, path.get(0).getLocation().getLat(),
                            path.get(0).getLocation().getLng(), "Start of an end path: " + startI + " - " + i);
                /*}*/
                highPoints.add(getHighestVisiblePoint(path, yourElevation));
            }
        }

        // Find the differences between the elevations so we can plot them
        highPoints = MapFunctions.findDiffBetweenElevations(highPoints);
        Log.d("onPostExecute", "Got high points " + highPoints.toString());

        // Show results of the highest peaks in all directions ahead on the map and graph
        MapFunctions.plotPoints(googleMap, highPoints, yourLocation.getLat(), yourLocation.getLng());
        List<Point> elevationsCoords = APIFunctions.drawOnGraph(highPoints);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        elevationsCoords = convertCoordSystem(elevationsCoords);

        // Detect the edge from an image
        EdgeDetection edgeDetection;
        edgeDetection = ImageManipulation.detectEdge(BitmapFactory.decodeResource(activity.getResources(), photoID));
        Log.d(TAG, "onPostExecute: Edge Detected");
        List<List<Integer>> edgeCoords2D = edgeDetection.coords;

        // Quick fix to simplify coordinates
        // It is originally a list of a list, to take into account many points in one column
        // but as thinning should have been used (but we may not have it 'on' to test
        // other algorithms) there should only be one point per column, so List<Int> will do
        List<Integer> edgeCoordsIntegers = HorizonMatching.removeDimensionFromCoords(edgeCoords2D);
        int pointWidth = (fineWidth-1)/2;
        List<Point> edgeCoords = HorizonMatching.convertToPoints(edgeCoordsIntegers, pointWidth);

        // Match up the horizons
        Log.d(TAG, "onPostExecute: Going to match up horizons");
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

        for (Point p : coords)
            if (p.getY() > maxPoint.getY())
                maxPoint = p;

        return maxPoint;
    }
}