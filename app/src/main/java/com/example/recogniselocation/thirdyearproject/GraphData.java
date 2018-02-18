package com.example.recogniselocation.thirdyearproject;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

class GraphData {

    private List<Point> coords;
    private LineGraphSeries<DataPoint> series;

    public GraphData(List<Point> coords, LineGraphSeries<DataPoint> series)
    {
        this.coords = coords;
        this.series = series;
    }

    List<Point> getCoords()
    {
        return coords;
    }
    LineGraphSeries<DataPoint> getSeries()
    {
        return series;
    }
}
