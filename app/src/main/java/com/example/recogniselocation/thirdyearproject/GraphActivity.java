package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static java.lang.Math.min;
import static java.lang.StrictMath.max;

public class GraphActivity extends Activity {

    static GraphView graph;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);

        graph = (GraphView) findViewById(R.id.graph);

        // Get the coordinates for the graph
        List<Point> elevationsCoords = getIntent().getParcelableArrayListExtra("elevationsCoords");
        List<Point> photoSeriesCoords = getIntent().getParcelableArrayListExtra("photoSeriesCoords");

        LineGraphSeries<DataPoint> elevSeries = coordsToSeries(
                (elevationsCoords));
        Log.d(TAG, "GraphAct: photoSeriesCoords: " + photoSeriesCoords.toString());
        LineGraphSeries<DataPoint> photoSeries = coordsToSeries(
                (photoSeriesCoords));
        Log.d(TAG, "GraphAct: photoSeries: Y Range: " + photoSeries.getLowestValueY() + ", " + photoSeries.getHighestValueY());

        drawMultipleSeriesOnGraph(photoSeries, Color.argb(255, 250, 100, 0),
                elevSeries, Color.BLACK);
    }

    private void drawMultipleSeriesOnGraph(LineGraphSeries<DataPoint> photoSeries, int photoColour,
                                           LineGraphSeries<DataPoint> elevSeries, int elevColour)
    {
        photoSeries.setColor(photoColour);
        GraphActivity.graph.addSeries(photoSeries);

        elevSeries.setColor(elevColour);
        GraphActivity.graph.addSeries(elevSeries);

        setBounds(GraphActivity.graph,
                min(photoSeries.getLowestValueX(),elevSeries.getLowestValueX()),
                max(photoSeries.getHighestValueX(),elevSeries.getHighestValueX()),
                min(photoSeries.getLowestValueY(),elevSeries.getLowestValueY()),
                max(photoSeries.getHighestValueY(),elevSeries.getHighestValueY()));
    }

    private LineGraphSeries<DataPoint> coordsToSeries(List<Point> coords) {
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        for (Point p : coords)
            series.appendData(new DataPoint(p.getX(), p.getY()), true, coords.size());
        return series;
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

    // Deal with button clicks
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: A button was clicked");
        Intent intent = null;

        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                intent = new Intent(this.getString(R.string.START_ACTIVITY));
                break;
            }
            case R.id.before: {
                Log.d(TAG, "Go back to the map");
                intent = new Intent(this.getString(R.string.MAP_ACTIVITY));
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the photo");
                intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }

        if (view.getId() == R.id.before || view.getId() == R.id.next) {
            // For the photo activity
            int drawableID = getIntent().getIntExtra("drawableID", 0);
            intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
            ArrayList<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
            intent.putParcelableArrayListExtra("photoCoords", photoCoords);      // To draw the edge
            ArrayList<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
            intent.putParcelableArrayListExtra("matchedPhotoCoords", matchedPhotoCoords);  // To mark on the matched points

            // For the map activity
            LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
            intent.putExtra("yourLocation", yourLocation);
            ArrayList<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
            intent.putParcelableArrayListExtra("highPoints", highPoints);
            ArrayList<Integer> matchedElevCoordsIndexes = getIntent().getIntegerArrayListExtra("matchedElevCoordsIndexes");
            intent.putIntegerArrayListExtra("matchedElevCoordsIndexes", matchedElevCoordsIndexes);  // To mark on the matched points

            // For the graph activity
            List<Point> elevationsCoords = getIntent().getParcelableArrayListExtra("elevationsCoords");
            intent.putParcelableArrayListExtra("elevationsCoords", (ArrayList<Point>) elevationsCoords);
            List<Point> photoSeriesCoords = getIntent().getParcelableArrayListExtra("photoSeriesCoords");
            intent.putParcelableArrayListExtra("photoSeriesCoords", (ArrayList<Point>) photoSeriesCoords);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "buttonClicked: Couldn't find an intent for id " + view.getId());
        }


    }
}