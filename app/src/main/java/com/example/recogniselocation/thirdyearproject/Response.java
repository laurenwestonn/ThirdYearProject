package com.example.recogniselocation.thirdyearproject;


import java.util.Iterator;
import java.util.List;

public class Response implements Iterable<Result> {
    private List<Result> results;
    public String status;

    public List<Result> getResults() {
        return results;
    }

    @Override
    public Iterator<Result> iterator() {
        return results.iterator();
    }
}
