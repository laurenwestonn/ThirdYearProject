package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
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

public class GraphActivity extends Activity {

    static GraphView graph;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);

        graph = (GraphView) findViewById(R.id.graph);

        // Get the coordinates for the graph

        // Todo: Call drawOnGraph with these coords (series)
        //drawOnGraph(series);

        // Todo: and for the matched horizon
        //drawOnGraph(photoMatchedSeries);

    }

    void drawOnGraph(LineGraphSeries<DataPoint> series)
    {
        GraphActivity.graph.addSeries(series);
        setBounds(GraphActivity.graph,
                series.getLowestValueX(), series.getHighestValueX(),
                series.getLowestValueY(), series.getHighestValueY());
        HorizonMatching.graphHeight =  series.getHighestValueY();
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
        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                Intent intent = new Intent(this.getString(R.string.START_ACTIVITY));
                startActivity(intent);
                finish();
                break;
            }
            case R.id.before: {
                Log.d(TAG, "Go back to the map");
                //Todo: Pass all results to the intent
                Intent intent = new Intent(this.getString(R.string.MAP_ACTIVITY));

                // For the photo activity
                int drawableID = getIntent().getIntExtra("drawableID", 0);
                List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
                List<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
                intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
                intent.putParcelableArrayListExtra("photoCoords", (ArrayList) photoCoords);      // To draw the edge
                intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList) matchedPhotoCoords);  // To mark on the matched points

                // For the map activity
                LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
                intent.putExtra("yourLocation", yourLocation);
                List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
                intent.putParcelableArrayListExtra("highPoints", (ArrayList) highPoints);

                startActivity(intent);
                finish();
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the photo");
                //Todo: Pass all results to the intent

                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));

                // For the photo activity
                int drawableID = getIntent().getIntExtra("drawableID", 0);
                List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
                List<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
                intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
                intent.putParcelableArrayListExtra("photoCoords", (ArrayList) photoCoords);      // To draw the edge
                intent.putParcelableArrayListExtra("matchedPhotoCoords", (ArrayList) matchedPhotoCoords);  // To mark on the matched points

                // For the map activity
                LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
                intent.putExtra("yourLocation", yourLocation);
                List<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
                intent.putParcelableArrayListExtra("highPoints", (ArrayList) highPoints);

                startActivity(intent);
                finish();
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }
    }
}