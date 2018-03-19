package com.example.recogniselocation.thirdyearproject;

import java.util.List;

class Horizon {

    private List<Point> photoMaximasMinimas;
    private List<Integer> elevMaximasMinimasIndexes;
    private List<Point> photoSeriesCoords;  // Points which directly represent series plots
                                            // Used to build up a series within the activity
                                            // As can't send series - they're not parcelable

    Horizon(List<Point> photoMMs, List<Integer> elevMMIndexes,
            List<Point> photoSeriesCoords) {
        this.photoMaximasMinimas = photoMMs;
        this.elevMaximasMinimasIndexes = elevMMIndexes;
        this.photoSeriesCoords = photoSeriesCoords;
    }

    List<Point> getPhotoMMs()
    {
        return photoMaximasMinimas;
    }

    List<Integer> getElevMMIndexes()
    {
        return elevMaximasMinimasIndexes;
    }

    List<Point> getPhotoSeriesCoords()
    {
        return photoSeriesCoords;
    }

}
