package com.example.recogniselocation.thirdyearproject;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

class Matching {

    private List<Point> photoCoords;
    private LineGraphSeries<DataPoint> photoSeries;
    private double difference;
    private int elevStartIndex;

    Matching(List<Point> photoCoords, LineGraphSeries<DataPoint> photoSeries, double diff, int elevStartIndex)
    {
        this.photoCoords = photoCoords;
        this.photoSeries = photoSeries;
        this.difference = diff;
        this.elevStartIndex = elevStartIndex;
    }

    double getDifference() {
        return difference;
    }

    LineGraphSeries<DataPoint> getPhotoSeries() {
        return photoSeries;
    }

    List<Point> getPhotoCoords() { return photoCoords; }

    int getElevStartIndex() { return elevStartIndex; }

    @Override
    public String toString() {
        return "\nDifference: " + difference + "\tPhoto Coords: " + photoCoords + "\tPhoto Series: " + photoSeries;
    }
}
