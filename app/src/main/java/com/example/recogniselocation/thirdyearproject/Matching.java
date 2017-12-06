package com.example.recogniselocation.thirdyearproject;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

class Matching {

    private LineGraphSeries<DataPoint> series;
    private double difference;

    Matching(LineGraphSeries<DataPoint> series, double diff)
    {
        this.series = series;
        this.difference = diff;
    }

    public double getDifference() {
        return difference;
    }

    public LineGraphSeries<DataPoint> getSeries() {
        return series;
    }

    @Override
    public String toString() {
        return "\nDifference: " + difference + "\tSeries: " + series;
    }
}
