package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.jjoe64.graphview.GraphView;

public class GraphActivity extends Activity {

    static GraphView graph;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graph_activity);

        graph = (GraphView) findViewById(R.id.graph);
    }
}