package com.example.recogniselocation.thirdyearproject;

import java.util.List;

class Matching {

    private List<Point> photoCoords;
    private double difference;
    private int elevStartIndex;

    Matching(List<Point> photoCoords, double diff, int elevStartIndex)
    {
        this.photoCoords = photoCoords;
        this.difference = diff;
        this.elevStartIndex = elevStartIndex;
    }

    double getDifference() {
        return difference;
    }

    List<Point> getPhotoCoords() { return photoCoords; }

    int getElevStartIndex() { return elevStartIndex; }

    @Override
    public String toString() {
        return "\nDifference: " + difference + "\tPhoto Coords: " + photoCoords;
    }
}
