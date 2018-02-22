package com.example.recogniselocation.thirdyearproject;

import java.util.List;

class MaximasMinimas {
    private List<Point> coords;
    private List<Integer> indexes;

    public MaximasMinimas(List<Point> coords, List<Integer> indexes)
    {
        this.coords = coords;
        this.indexes = indexes;
    }

    List<Point> getCoords()
    {
        return coords;
    }

    List<Integer> getIndexes()
    {
        return indexes;
    }
}
