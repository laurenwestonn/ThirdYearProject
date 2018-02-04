package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import static com.example.recogniselocation.thirdyearproject.HorizonMatching.getFirstElevationIndex;

public class HorizonMatchingTest {
    @Test
    public void getTheNextElevationMMTest() throws Exception
    {
        ///////////////////// Photo: max, min  ///////////////////////
        // The maxima and minima chosen from the photo are in a pair where the first one is the max

        // Elevation: max, min

        // Elevation: null, min, max

        ///////////////// Photo: null, min, max    ///////////////////////
        // The maxima and minima chosen from the photo are in a pair where the first one is the max

        // Elevation: max, min

        // Elevation: null, min, max

        //getTheNextElevationMM();
    }


    @Test
    public void getNextElevationIndexTest() throws Exception
    {
        boolean photoIsInOrderMaxMin = true;
        boolean elevaIsInOrderMaxMin = true;

        // Both start with a maxima
        assertThat(getFirstElevationIndex(photoIsInOrderMaxMin, elevaIsInOrderMaxMin), is(0));

        // Photo starts with a minima, so skip the first maxima index of the elevations
        assertThat(getFirstElevationIndex(!photoIsInOrderMaxMin, elevaIsInOrderMaxMin), is(1));

        // Photo starts with a maxima, but the elevations start with a minima
        // so skip the elevations until you reach the first maxima
        assertThat(getFirstElevationIndex(photoIsInOrderMaxMin, !elevaIsInOrderMaxMin), is(2));

        // Photo starts with a minima, so skip the first maxima index of the elevations
        assertThat(getFirstElevationIndex(!photoIsInOrderMaxMin, !elevaIsInOrderMaxMin), is(1));
    }

    @Test
    public void gradientAheadTest() throws Exception
    {
        // Gradient constantly goes up by one
        // Searching a width of 1           Up 1 / across 1 = 1
        List<Point> coords = Arrays.asList(new Point(0,0), new Point(1,1), new Point(2,2));
        assertThat(HorizonMatching.gradientAhead(coords, 0, 1), is(1.0));

        // Searching a width of 2           Up 2 / across 2 = 1
        assertThat(HorizonMatching.gradientAhead(coords, 0, 2), is(1.0));

        // Searching a width of 3           Out of bounds
        assertThat(HorizonMatching.gradientAhead(coords, 0, 3), is(1.0));

        // Searching a width of 0           Index exists, gradient is 0
        assertThat(HorizonMatching.gradientAhead(coords, 0, 0), is(0.0));

        // Searching a width of 0           Index doesn't exist, gradient is -1 (out of bounds)
        assertThat(HorizonMatching.gradientAhead(coords, 3, 0), is(0.0));

        // Searching from last index        So gradient defaults to 0
        assertThat(HorizonMatching.gradientAhead(coords, 2, 5), is(0.0));



        // Gradient goes more steeply
        // Width of 1                       Up 2 / across 1 = 2
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,4));
        assertThat(HorizonMatching.gradientAhead(coords, 0, 1), is(2.0));

        // Width of 2                       Up 4 / across 2 = 2
        assertThat(HorizonMatching.gradientAhead(coords, 0, 2), is(2.0));

        // Negative Gradient
        coords = Arrays.asList(new Point(0,6), new Point(1,4), new Point(2,2));
        assertThat(HorizonMatching.gradientAhead(coords, 0, 1), is(-2.0));

        // Varying Gradient
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,3));
        assertThat(HorizonMatching.gradientAhead(coords, 0, 2), is(1.5));

        // Varying Gradient, quitting half way as have reached the end of the coordinates
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,3));
        assertThat(HorizonMatching.gradientAhead(coords, 1, 1), is(1.0));


    }

    @Test
    public void aheadExistsTest() throws Exception {

        List<Point> coords = Arrays.asList(new Point(0,0), new Point(1,1), new Point(2,2));

        // 3 points, searching 0 ahead where this index exists
        assertTrue(HorizonMatching.aheadExists(coords,0,0));

        // 3 points, searching 0 ahead where this index does not exist
        assertFalse(HorizonMatching.aheadExists(coords,3,0));

        // 3 points, searching 1 ahead
        assertTrue(HorizonMatching.aheadExists(coords,0,1));

        // 3 points, searching 2 (max) ahead
        assertTrue(HorizonMatching.aheadExists(coords,0,2));

        // 3 points, searching 3 ahead (out of bounds)
        assertFalse(HorizonMatching.aheadExists(coords,0,3));

        // Starting at the second point, valid search
        assertTrue(HorizonMatching.aheadExists(coords, 1, 1));

        // Starting at the second point, invalid search
        assertFalse(HorizonMatching.aheadExists(coords, 1, 2));

    }

    @Test
    public void findMaximaMinimaTest() throws Exception {

        // Remember that in this coordinate system, the larger y will be the minimum
        // Because of how I flipped the coordinate system, the highest max that there could
        // be will have a y value of 1

        // Pointy max, pointy min /\/
        List<Point> input = Arrays.asList(new Point(0,50), new Point(1,1), new Point(2,50), new Point(3, 1));
        List<Point> expectedOutput = Arrays.asList(new Point(1,1), new Point(2, 50));
        assertThat(Arrays.asList(2,3), is(Arrays.asList(2,3)));
        assertThat(new Point(2,3), is(new Point(2,3)));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));

        // Pointy min, pointy max  \/\
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3, 50));
        expectedOutput = Arrays.asList(null, new Point(1,50), new Point(2, 1));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));

        // Flat min, pointy max \___/\
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,50), new Point(3,50), new Point(4,1), new Point(5, 50));
        expectedOutput = Arrays.asList(null, new Point(2,50), new Point(4, 1));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));
        //                        ___
        // Pointy min, flat max \/   \
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3,1), new Point(4,1), new Point(5, 50));
        expectedOutput = Arrays.asList(null, new Point(1,50), new Point(3, 1));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));
        //                         ___
        // Flat min, flat max \___/   \
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,50), new Point(3,50), new Point(4,1), new Point(5, 1), new Point(6, 1), new Point(7, 50));
        expectedOutput = Arrays.asList(null, new Point(2,50), new Point(5, 1));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));
        //                             _._
        // Smooth min, smooth max \___/   \
        input = Arrays.asList(new Point(0,1), new Point(1,45), new Point(2,50), new Point(3,47), new Point(4,5), new Point(5, 2), new Point(6, 10), new Point(7, 50));
        expectedOutput = Arrays.asList(null, new Point(2,50), new Point(5, 2));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));

        // Many max/min  \  ./\   /\.
        //                \./  \./  \
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,20), new Point(3,1), new Point(4,50), new Point(5, 1), new Point(6, 10), new Point(7, 50));
        expectedOutput = Arrays.asList(null, new Point(1,50), new Point(3, 1), new Point(4, 50), new Point(5, 1));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));

        // Does it ignore minimas that are too small    \  /~\  /
        //                                               \/   \/        What is too small? Maybe test thresholding? Is picking up a difference of 0.1 when graph is 0 -> 50
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3, 2), new Point(4, 1), new Point(5, 50), new Point(6, 1));
        expectedOutput = Arrays.asList(null, new Point(1,50), new Point(3, 2 ), new Point(5, 50));
        assertThat(HorizonMatching.findMaximaMinima(input, false), is(expectedOutput));

        // Can't have two minimas in a row, in between must be a maxima. No test for that as doesn't exist

    }

    @Test
    public void getSearchWidthTest() throws Exception {
        List<Point> coords = new ArrayList<>();
        coords.add(new Point(1,1));
        assertThat(HorizonMatching.getSearchWidth(coords), is(1));


        coords.add(new Point(2,2));
        assertThat(HorizonMatching.getSearchWidth(coords), is(1));


        for (int i = 3; i <= 49; i++)
            coords.add(new Point(i,i));

        // 49 coords
        assertThat(HorizonMatching.getSearchWidth(coords), is(1));

        // 50 coords
        coords.add(new Point(50,50));
        assertThat(HorizonMatching.getSearchWidth(coords), is(2));

        // 51 coords
        coords.add(new Point(51,51));
        assertThat(HorizonMatching.getSearchWidth(coords), is(2));

    }
}