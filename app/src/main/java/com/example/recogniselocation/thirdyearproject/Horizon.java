package com.example.recogniselocation.thirdyearproject;

import android.graphics.Canvas;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.util.Iterator;
import java.util.List;

class Horizon implements Series<DataPoint> {

    private List<Point> photoMaximasMinimas;
    private List<Integer> elevMaximasMinimasIndexes;
    List<Point> photoSeriesCoords;
    private LineGraphSeries<DataPoint> photoSeries;

    Horizon(List<Point> photoMMs, List<Integer> elevMMIndexes,
            List<Point> photoSeriesCoords,
            LineGraphSeries<DataPoint> photoSeries) {
        this.photoMaximasMinimas = photoMMs;
        this.elevMaximasMinimasIndexes = elevMMIndexes;
        this.photoSeriesCoords = photoSeriesCoords;
        this.photoSeries = photoSeries;
    }

    public List<Point> getPhotoMMs()
    {
        return photoMaximasMinimas;
    }

    public List<Integer> getElevMMIndexes()
    {
        return elevMaximasMinimasIndexes;
    }

    public LineGraphSeries<DataPoint> getSeries()
    {
        return photoSeries;
    }

    public List<Point> getPhotoSeriesCoords()
    {
        return photoSeriesCoords;
    }

    @Override
    public double getLowestValueX() {
        return 0;
    }

    @Override
    public double getHighestValueX() {
        return 0;
    }

    @Override
    public double getLowestValueY() {
        return 0;
    }

    @Override
    public double getHighestValueY() {
        return 0;
    }

    @Override
    public Iterator<DataPoint> getValues(double v, double v1) {
        return null;
    }

    @Override
    public void draw(GraphView graphView, Canvas canvas, boolean b) {

    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public int getColor() {
        return 0;
    }

    @Override
    public void setOnDataPointTapListener(OnDataPointTapListener onDataPointTapListener) {

    }

    @Override
    public void onTap(float v, float v1) {

    }

    @Override
    public void onGraphViewAttached(GraphView graphView) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
