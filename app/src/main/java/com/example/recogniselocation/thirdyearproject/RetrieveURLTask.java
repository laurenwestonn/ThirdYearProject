package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.getHighestVisiblePoint;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.samplesPerPath;
import static com.example.recogniselocation.thirdyearproject.ImageManipulation.fineWidth;
import static com.example.recogniselocation.thirdyearproject.MapActivity.googleMap;
import static com.example.recogniselocation.thirdyearproject.Start.yourLocation;

public class RetrieveURLTask extends AsyncTask<List<String>, Void, List<String>>  {

    @SuppressLint("StaticFieldLeak")
    private Activity activity;
    //private int photoID = R.drawable.rocky_mountains; Not using this for the next version

    RetrieveURLTask(Activity a)
    {
        this.activity = a;
    }

    // Sending off the URLs and passing on the responses to onPostExecute
    protected List<String> doInBackground(List<String>... urls)
    {
        Log.d("RetrieveURLTask", "Going to take some time getting the results from the API");
        return APIFunctions.requestURL(urls[0]);
    }

    // Interpreting the responses
    protected void onPostExecute(List<String> strResponses)
    {
        ///////// CONSTRUCT HORIZON FROM ELEVATIONS /////////
        // Set up Gson to convert string responses to GSON objects
        Log.d("onPostExecute", "API gave response " + strResponses);
        Gson gson = new GsonBuilder().setLenient().create(); //Todo is lenient needed? could just do new Gson instead, like I used to have.
        double yourElevation = gson.fromJson(strResponses.get(0), Response.class).getResults().get(samplesPerPath).getElevation();

        // Find the highest point in each path for each response
        List<Result> highPoints = getHighPoints(strResponses, yourElevation);
        Log.d("onPostExecute", "Got high points " + highPoints);

        // Todo: Check I've set up graph and map properly.. Log.d's are to show if error happened here, remove when sure
        // Show results of the highest peaks in all directions ahead on the map and graph
        Log.d(TAG, "onPostExecute: Going to plot on the map");
        MapFunctions.plotPoints(googleMap, highPoints, yourLocation);
        Log.d(TAG, "onPostExecute: Going to plot on the graph");
        List<Point> elevationsCoords = APIFunctions.drawOnGraph(highPoints);
        ///////// CONSTRUCT HORIZON FROM ELEVATIONS /////////


        /////// EDGE DETECTION //////
        int photoID = Start.drawableID;    // Todo: Deal with using an uploaded photo of your location
        Bitmap bmp = BitmapFactory.decodeResource(activity.getResources(), photoID);
        Edge edge = ImageManipulation.detectEdge(
                bmp,false, false, true, true);
        List<List<Integer>> photoCoords2D = edge.getCoords();
        Log.d(TAG, "onPostExecute: Edge Detected");
        /////// EDGE DETECTION //////


        /////// MATCH UP HORIZONS //////
        // Quick fix to simplify coordinates
        // It is originally a list of a list, to take into account many points in one column
        // but as thinning should have been used (but we may not have it 'on' to test
        // other algorithms) there should only be one point per column, so List<Int> will do
        List<Integer> photoCoordsIntegers = HorizonMatching.removeDimensionFromCoords(photoCoords2D);
        int pointWidth = (fineWidth-1)/2;
        List<Point> photoCoords = HorizonMatching.convertToPoints(photoCoordsIntegers, pointWidth);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        elevationsCoords = convertCoordSystem(elevationsCoords);

        Log.d(TAG, "onPostExecute: Going to match up horizons");
        HorizonMatching.matchUpHorizons(photoCoords, elevationsCoords, edge.getBitmap(), activity);
        /////// MATCH UP HORIZONS //////

        ////// START NEXT ACTIVITY //////
        Intent intent = new Intent(activity.getString(R.string.PHOTO_ACTIVITY));
        // Todo: Send the required results on to the activity

        // For the photo activity
        intent.putExtra("drawableID", Start.drawableID);  // Bitmap is too big, find it via ID
        //intent.putParcelableArrayListExtra(photoCoords);      // To draw the edge // Todo: make Point parcelable
        //intent.putIntegerArrayListExtra(matchedPhotoPoints);  // To mark on the matched points // Todo: make Point parcelable

        // For the map activity
        //intent.putExtra("highPoints", highPoints);  // Todo: make Result parcelable
        //intent.putExtra("yourLocation", Start.yourLocation); How to send a LatLng

        // For the graph activity (already have the photo coords)
        //intent.putParcelableArrayListExtra("elevationsCoords", elevationsCoords);  // Todo: make Point parcelable
        //intent.putExtra("matchedMapPoints", matchedMapPoints);  // To mark on the matched points Todo: save the matched up points

        Log.d(TAG, "buttonClicked: Put at the relevant info into the intent. Start the activity.");
        activity.startActivity(intent);
        activity.finish();

    }

    private List<Result> getHighPoints(List<String> strResponses, double yourElevation) {
        List<Result> highPoints = new ArrayList<>();
        for (String strResponse : strResponses) {

            List<Result> results = getResults(strResponse);

            // Store the highest visible point of the first path of this response
            highPoints.add(getHighestVisiblePoint(getFirstPath(results, samplesPerPath), yourElevation));

            int indexOfSecondPath = samplesPerPath + 1; // Skipping the first path and your location
            int indexOfLastPath = getIndexOfLastPath(results.size(), samplesPerPath);

            // Get the highest visible points for each of the paths in the middle of a response
            if (indexOfSecondPath != indexOfLastPath)
                highPoints = findMidHighPoints(highPoints, results, indexOfSecondPath, indexOfLastPath, yourElevation);

            // The last path
            highPoints = getLastPath(highPoints, results, yourElevation, indexOfLastPath);

            // Todo: Draw on the area searched using a poly line of the end points and your location

        }
        // Find the differences between the elevations so we can plot them
        highPoints = MapFunctions.findDiffBetweenElevations(highPoints);

        return highPoints;
    }

    public static int getIndexOfLastPath(int resultsLength, int samplesPerPath) {
        int distFromEnd = (resultsLength - 2) % samplesPerPath;
        return resultsLength - distFromEnd - 1;
    }

    // First path is got in reverse, get it the proper way: from your location outwards
    private List<Result> getFirstPath(List<Result> results, int length) {
        List<Result> path = new ArrayList<>();
        for (int i = 0; i < length; i++)
            path.add(results.get(length - i - 1));
        return path;
    }

    // The paths in the middle (these have duplicates where we've headed back to your location
    private List<Result> findMidHighPoints(List<Result> highPoints, List<Result> results,
                                           int indexOfSecondPath, int indexOfLastPath,
                                           double yourElevation) {
        int startI;
        int i = indexOfSecondPath; // Skipping the first path and your location
        boolean duplicate = false;
        List<Result> path = new ArrayList<>();

        // Go through the middle paths of this result
        while (i < indexOfLastPath) {
            startI = i;

            // Use the ones heading from your location to the end of the path
            // Ignore the ones heading back to your location
            for (; i < startI + indexOfSecondPath - 1; i++)
                if (!duplicate)
                    path.add(results.get(i));

            if (path.size() != 0) {
                highPoints.add(getHighestVisiblePoint(path, yourElevation));
                //Log.e(TAG, "onPostExecute: a Mid path is " + path);
                //Log.e(TAG, "onPostExecute: Hi point from that is " + highPoints.get(highPoints.size()-1));
                /*
                if (path.size() != 0 && strResponse.equals(strResponses.get(3))) {
                    MapFunctions.addMarkerAt(googleMap, path.get(path.size() - 1).getLocation().getLat(),
                            path.get(path.size() - 1).getLocation().getLng(), "End of a middle path at " + (i-1));
                    MapFunctions.addMarkerAt(googleMap, path.get(0).getLocation().getLat(),
                            path.get(0).getLocation().getLng(), "Start of a middle path: " + startI + " - " + (i-1));
                }*/
                path.clear();
            }

            // Next results will be the opposite - needed or not
            duplicate = !duplicate;

        }
        return highPoints;
    }

    // Will be at the biggest *samplesPerPath* long. Smaller if we run out of results
    private List<Result> getLastPath(List<Result> highPoints, List<Result> results, double yourElevation, int i) {
        List<Result> path = new ArrayList<>();
        for (; i < results.size(); i++)
            path.add(results.get(i));
        if (path.size() != 0)
            highPoints.add(getHighestVisiblePoint(path, yourElevation));
        return highPoints;
    }

    // Get the results as an object from the string reponse of the request
    private List<Result> getResults(String strResponse) {
        Gson gson = new GsonBuilder().setLenient().create(); //Todo is lenient needed? could just do new Gson instead, like I used to have.
        Response response = gson.fromJson(strResponse, Response.class);
        return response.getResults();
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