package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

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
}