package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ImageManipulationTest {
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
        assertFalse(ImageManipulation.thinPoint(c, c.get(0), 1));

        // Just on the right
        //    [1]
        //    [0]  [3]
        //    [2]
        c.remove(c.size()-1);
        assertTrue(ImageManipulation.thinPoint(c, c.get(0), 1));

        // Two neighbours - a line
        //    [1]
        //    [0]
        //    [2]
        c.remove(c.size()-1);
        assertFalse(ImageManipulation.thinPoint(c, c.get(0), 1));


        // Just on the left
        //     [1]
        // [3] [0]
        //     [2]
        c.add(new Point(0, 1));
        assertTrue(ImageManipulation.thinPoint(c, c.get(0), 1));

        // One neighbour
        //    [1]
        //    [0]
        //
        c.remove(c.size()-1);
        c.remove(c.size()-1);
        assertFalse(ImageManipulation.thinPoint(c, c.get(0), 1));

        // No neighbours
        //
        //    [0]
        //
        c.remove(c.size()-1);
        assertFalse(ImageManipulation.thinPoint(c, c.get(0), 1));

    }

}