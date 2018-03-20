package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FunctionsImageManipulationTest {
    @Test
    public void thinPointTest() throws Exception {

        List<Point> c = new ArrayList<>();

        // On the left and right
        //     [1]
        // [4] [0] [3]
        //     [2]
        c.add(new Point(1,1));  // 'Centre' at index 0
        c.add(new Point(1,0));
        c.add(new Point(1,2));
        c.add(new Point(2,1));
        c.add(new Point(0, 1));
        assertFalse(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));

        // Just on the right
        //    [1]
        //    [0]  [3]
        //    [2]
        c.remove(c.size()-1);
        assertTrue(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));

        // Two neighbours - a line
        //    [1]
        //    [0]
        //    [2]
        c.remove(c.size()-1);
        assertFalse(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));


        // Just on the left
        //     [1]
        // [3] [0]
        //     [2]
        c.add(new Point(0, 1));
        assertTrue(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));

        // One neighbour
        //    [1]
        //    [0]
        //
        c.remove(c.size()-1);
        c.remove(c.size()-1);
        assertFalse(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));

        // No neighbours
        //
        //    [0]
        //
        c.remove(c.size()-1);
        assertFalse(FunctionsImageManipulation.skeletonisePoint(c, c.get(0), 1));

    }


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