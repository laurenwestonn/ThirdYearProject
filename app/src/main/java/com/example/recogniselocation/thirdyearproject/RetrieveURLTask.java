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
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.Series;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.getHighestVisiblePoint;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.samplesPerPath;
import static com.example.recogniselocation.thirdyearproject.ImageManipulation.fineWidth;

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
        Gson gson = new GsonBuilder().create();
        double yourElevation = gson.fromJson(strResponses.get(0), Response.class).getResults().get(samplesPerPath).getElevation();

        // Find the highest point in each path for each response
        List<Result> highPoints = getHighPoints(strResponses, yourElevation);
        Log.d("onPostExecute", "Got high points " + highPoints);
        // Get the graph data
        GraphData gd = APIFunctions.findGraphData(highPoints);
        List<Point> elevationsCoords = gd.getCoords();


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
        photoCoords = invertY(photoCoords); // To match the graph's coordinate system: Up Right +ve

        Log.d(TAG, "onPostExecute: Going to match up horizons");

        // Todo: Check why I am inverting elevations coords, should already be in the graph coord system as
        // todo: this is the only thing elev coords are used for.. Check against other photos - does the elevation look right?
        Horizon horizon = HorizonMatching.matchUpHorizons(photoCoords, elevationsCoords);
        // Todo: Possibly just send the horizon object as one, not as its elements separately
        List<Point> photoSeriesCoords = horizon.getPhotoSeriesCoords();     // Up Right +ve
        List<Integer> matchedElevCoordsIndexes = horizon.getElevMMIndexes();// To get LatLng
        List<Point> matchedPhotoCoords = invertY(horizon.getPhotoMMs());    // Down Right +ve
        photoCoords = invertY(photoCoords); // Down Right +ve



        /////// MATCH UP HORIZONS //////

        ////// START NEXT ACTIVITY //////
        Intent intent = new Intent(activity.getString(R.string.PHOTO_ACTIVITY));

        // For the photo activity
        intent.putExtra("drawableID", Start.drawableID);  // Bitmap is too big, find it via ID
        intent.putParcelableArrayListExtra("photoCoords", (ArrayList<Point>) photoCoords);      // To draw the edge
        intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList<Point>) matchedPhotoCoords);  // To mark on the matched points

        // For the map activity
        intent.putParcelableArrayListExtra("highPoints", (ArrayList<Result>) highPoints);
        intent.putExtra("yourLocation", Start.yourLocation);
        intent.putIntegerArrayListExtra("matchedElevCoordsIndexes", (ArrayList<Integer>) matchedElevCoordsIndexes);  // To mark on the matched points

        // For the graph activity (already have the photo coords)
        intent.putParcelableArrayListExtra("elevationsCoords", (ArrayList<Point>) elevationsCoords);
        intent.putParcelableArrayListExtra("photoSeriesCoords", (ArrayList<Point>) photoSeriesCoords);

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
        Gson gson = new GsonBuilder().create();
        Response response = gson.fromJson(strResponse, Response.class);
        return response.getResults();
    }

    // Flip on the x axis, doesn't matter where on the axes it ends up as gets scaled anyway
    public static List<Point> invertY(List<Point> coords)
    {
        List<Point> inverted = new ArrayList<>();
        for(Point p : coords)
            if (p != null)
                inverted.add(new Point(p.getX(), p.getY() * -1));
            else
                Log.e(TAG, "invertSign: Can't invert a null point! " + coords);

        return inverted;
    }

    private static Point findMaxPoint(List<Point> coords) {
        Point maxPoint = coords.get(0);

        for (Point p : coords)
            if (p.getY() > maxPoint.getY())
                maxPoint = p;

        return maxPoint;
    }
}