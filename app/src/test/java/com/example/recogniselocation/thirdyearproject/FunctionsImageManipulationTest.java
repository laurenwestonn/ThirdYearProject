package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FunctionsImageManipulationTest {

    @Test
    // Assumes the point are given in ascending y order
    public void bestPointInColTest() throws Exception
    {
        List<Point> col = new ArrayList<>();

        // One point
        col.add(new Point(10, 1));
        assertThat(FunctionsImageManipulation.bestPointInCol(col, null), is(new Point(10,1)));

        // Two points - pick higher one (lowest y)
        col.add(new Point(10, 20));
        assertThat(FunctionsImageManipulation.bestPointInCol(col, null), is(new Point(10,1)));

        // Three points - pick middle one
        col.add(new Point(10, 30));
        assertThat(FunctionsImageManipulation.bestPointInCol(col, null), is(new Point(10,20)));

        // Four points - pick middle higher one - 2nd lowest y
        col.add(new Point(10, 50));
        assertThat(FunctionsImageManipulation.bestPointInCol(col, null), is(new Point(10,20)));
    }

}