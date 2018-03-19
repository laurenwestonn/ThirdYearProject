package com.example.recogniselocation.thirdyearproject;


import android.support.annotation.NonNull;

import java.util.Iterator;
import java.util.List;

public class Response implements Iterable<Result> {
    private List<Result> results;
    public String status;

    List<Result> getResults() {
        return results;
    }

    @NonNull
    @Override
    public Iterator<Result> iterator() {
        return results.iterator();
    }
}
