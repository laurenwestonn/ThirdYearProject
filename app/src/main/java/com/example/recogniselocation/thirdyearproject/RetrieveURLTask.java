package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.getHighestVisiblePoint;
import static com.example.recogniselocation.thirdyearproject.APIFunctions.samplesPerPath;

public class RetrieveURLTask extends AsyncTask<List<String>, Void, List<String>>  {

    @SuppressLint("StaticFieldLeak")
    private Activity activity;

    private boolean sdDetail = false;
    private boolean useThinning = true;
    private boolean showEdgeOnly = true;

    RetrieveURLTask(Activity a)
    {
        this.activity = a;
    }

    // Sending off the URLs and passing on the responses to onPostExecute
    @SafeVarargs
    protected final List<String> doInBackground(List<String>... urls)
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


        /////// PHOTO EDGE DETECTION //////
        Bitmap bmp = null;
        if (Start.uri != null) {    // Actual Location - load photo from where was saved
            try {   // Resize, phones can take large photos
                bmp = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), Start.uri);
                bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, false);
                Log.d(TAG, "onPostExecute: Bitmap got is " + bmp.getWidth() + " x " + bmp.getHeight() + ". " + bmp.getConfig());
            } catch(Exception e) {
                Log.e(TAG, "onPostExecute: Couldn't find bitmap: " + e.getMessage());
            }
        } else { // Faked demo, get photo from /drawable/
            int photoID = Start.drawableID;
            bmp = BitmapFactory.decodeResource(activity.getResources(), photoID);
        }
        Edge edge = ImageManipulation.detectEdge(
                bmp, sdDetail, useThinning, showEdgeOnly);
        List<Point> photoCoords = edge.getCoords();
        List<Point> coarsePhotoCoords = edge.getCoarseCoords();
        /////// PHOTO EDGE DETECTION //////

        // Will be going to the photo activity next
        Intent intent = new Intent(activity.getString(R.string.PHOTO_ACTIVITY));

        if (photoCoords != null) {
            Log.d(TAG, "onPostExecute: Photo's edge Detected");

            /////// MATCH UP HORIZONS //////
            Log.d(TAG, "onPostExecute: Going to match up horizons");
            photoCoords = invertY(photoCoords); // To match the graph's coordinate system: Up Right +ve

            // Todo: Check flippedness of graph results against other photos - does the elevation look right?
            Horizon horizon = HorizonMatching.matchUpHorizons(photoCoords, elevationsCoords);
            /////// MATCH UP HORIZONS //////


            // Pass these photo coords and the matched info to the next activity
            // Todo: Possibly just send the horizon object as one, not as its elements separately
            // Can't send series via intent (isn't parcelable) Send Points & convert within activity
            List<Point> photoSeriesCoords = horizon.getPhotoSeriesCoords();     // Up Right +ve
            List<Integer> matchedElevCoordsIndexes = horizon.getElevMMIndexes();// To get LatLng
            List<Point> matchedPhotoCoords = invertY(horizon.getPhotoMMs());    // Down Right +ve
            photoCoords = invertY(photoCoords); // Down Right +ve

            intent.putParcelableArrayListExtra("photoCoords", (ArrayList<Point>) photoCoords);      // To draw the edge
            intent.putParcelableArrayListExtra("coarsePhotoCoords", (ArrayList<Point>) coarsePhotoCoords);      // To draw the coarse edge
            intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList<Point>) matchedPhotoCoords);  // To mark on the matched points
            // For the map activity
            intent.putIntegerArrayListExtra("matchedElevCoordsIndexes", (ArrayList<Integer>) matchedElevCoordsIndexes);  // To mark on the matched points
            // For the graph activity
            if (photoSeriesCoords != null && photoSeriesCoords.size() > 0)
                intent.putParcelableArrayListExtra("photoSeriesCoords", (ArrayList<Point>) photoSeriesCoords);


        } else
            Log.e(TAG, "onPostExecute: Couldn't find edge coords of photo");



        ////// START NEXT ACTIVITY //////

        // Pass the basic data to the next activity (the elevations data)
        // For the photo activity
        if (Start.uri == null)  // If we saved a photo, use this. Only use DrawableID for a demo
            intent.putExtra("drawableID", Start.drawableID);  // Bitmap is too big, find it via ID

        // For the map activity
        intent.putParcelableArrayListExtra("highPoints", (ArrayList<Result>) highPoints);
        intent.putExtra("yourLocation", Start.yourLocation);

        // For the graph activity (already have the photo coords)
        intent.putParcelableArrayListExtra("elevationsCoords", (ArrayList<Point>) elevationsCoords);

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

    static int getIndexOfLastPath(int resultsLength, int samplesPerPath) {
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
    private static List<Point> invertY(List<Point> coords)
    {
        List<Point> inverted = new ArrayList<>();
        for(Point p : coords)
            if (p != null)
                inverted.add(new Point(p.getX(), p.getY() * -1));
            else
                inverted.add(null);

        return inverted;
    }
}