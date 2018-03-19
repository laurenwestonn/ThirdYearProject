package com.example.recogniselocation.thirdyearproject;

import java.util.List;

class MaximasMinimas {
    private List<Point> maximasMinimas;
    private List<Integer> indexes;

    MaximasMinimas(List<Point> maximasMinimas, List<Integer> indexes)
    {
        this.maximasMinimas = maximasMinimas;
        this.indexes = indexes;
    }

    List<Point> getMaximasMinimas()
    {
        return maximasMinimas;
    }

    List<Integer> getIndexes()
    {
        return indexes;
    }
}
