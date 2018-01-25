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

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.getHighestVisiblePoint;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.noOfPathsPerGroup;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.samplesPerPath;
import static com.example.recogniselocation.thirdyearproject.ImageManipulation.fineWidth;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.MapsActivity.yourLocation;

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
        Log.d("RetrieveURLTask", "Going to take some time getting the results from the API");
        return APIFunctions.requestURL(urls[0]);
    }

    protected void onPostExecute(List<String> strResponses)
    {
        Log.d("onPostExecute", "API gave response " + strResponses);
        List<Result> highPoints = new ArrayList<>();

        for (String strResponse : strResponses) {
            // Convert this string response to a Response object
            Gson gson = new GsonBuilder().setLenient().create(); //Todo is lenient needed? could just do new Gson instead, like I used to have.
            Response response = gson.fromJson(strResponse, Response.class);
            List<Result> results = response.getResults();
            List<Result> path = new ArrayList<>();
            int i = 0;

            for (; i < samplesPerPath; i++)
                path.add(results.get(samplesPerPath - i - 1));  // First path is backwards

            // Store only the highest point of this first path from the response
            // 1st param is the first path; 2nd is your location's elevation,
            // while skipping past this index as your location isn't part of the next path
            double yourElevation = results.get(i++).getElevation();
            highPoints.add(getHighestVisiblePoint(path, yourElevation));
            path.clear();

            // The paths in the middle (these have duplicates where we've headed back to your location
            for (; i < results.size() - samplesPerPath; i++) {

                // From your location to the paths end. These results make up one path
                if (i % samplesPerPath*2 < samplesPerPath)
                    path.add(results.get(i));
                // The others (from the path's end back to your location) are duplicate, ignore

                // A path is complete when it has all samples
                if (path.size() == samplesPerPath) {
                    highPoints.add(getHighestVisiblePoint(path, yourElevation));

                    // Clear the path to build up the next one
                    path.clear();
                }
            }
            path.clear();   // Todo: This shouldn't be needed if is done correctly, should only ever exit in the last if

            // The last path
            for (; i < results.size(); i++) {
                path.add(results.get(i));
            }
            highPoints.add(getHighestVisiblePoint(path, yourElevation));

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