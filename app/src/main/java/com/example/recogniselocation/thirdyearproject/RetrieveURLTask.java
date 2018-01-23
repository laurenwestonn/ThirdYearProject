package com.example.recogniselocation.thirdyearproject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import java.util.List;

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

    protected void onPostExecute(String response)
    {
        Log.d("onPostExecute", "API gave response " + response);
        List<Result> highPoints = APIFunctions.getHighestVisiblePoints(response);
        Log.d("onPostExecute", "Got high points " + highPoints.toString());

        // Show results of the highest peaks in all directions ahead on the map and graph
        MapFunctions.plotPoints(googleMap, highPoints, yourLocation.getLat(), yourLocation.getLng());
        List<Point> elevationsCoords = APIFunctions.drawOnGraph(highPoints);

        // Convert these coordinates to be in line with the bitmaps coordinate system
        elevationsCoords = convertCoordSystem(elevationsCoords);

        // Detect the edge from an image
        EdgeDetection edgeDetection;
        edgeDetection = ImageManipulation.detectEdge(BitmapFactory.decodeResource(activity.getResources(), photoID));
        List<List<Integer>> edgeCoords2D = edgeDetection.coords;

        // Quick fix to simplify coordinates
        // It is originally a list of a list, to take into account many points in one column
        // but as thinning should have been used (but we may not have it 'on' to test
        // other algorithms) there should only be one point per column, so List<Int> will do
        List<Integer> edgeCoordsIntegers = HorizonMatching.removeDimensionFromCoords(edgeCoords2D);
        int pointWidth = (fineWidth-1)/2;
        List<Point> edgeCoords = HorizonMatching.convertToPoints(edgeCoordsIntegers, pointWidth);

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

        for (Point p : coords)
            if (p.getY() > maxPoint.getY())
                maxPoint = p;

        return maxPoint;
    }
}