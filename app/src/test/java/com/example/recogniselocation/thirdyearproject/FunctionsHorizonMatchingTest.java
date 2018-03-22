package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.recogniselocation.thirdyearproject.FunctionsHorizonMatching.getSearchWidth;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import static com.example.recogniselocation.thirdyearproject.FunctionsHorizonMatching.getFirstElevationIndex;

public class FunctionsHorizonMatchingTest {
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
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 1), is(1.0));

        // Searching a width of 2           Up 2 / across 2 = 1
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 2), is(1.0));

        // Searching a width of 3           Out of bounds
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 3), is(1.0));

        // Searching a width of 0           Index exists, gradient is 0
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 0), is(0.0));

        // Searching a width of 0           Index doesn't exist, gradient is -1 (out of bounds)
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 3, 0), is(0.0));

        // Searching from last index        So gradient defaults to 0
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 2, 5), is(0.0));



        // Gradient goes more steeply
        // Width of 1                       Up 2 / across 1 = 2
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,4));
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 1), is(2.0));

        // Width of 2                       Up 4 / across 2 = 2
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 2), is(2.0));

        // Negative Gradient
        coords = Arrays.asList(new Point(0,6), new Point(1,4), new Point(2,2));
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 1), is(-2.0));

        // Varying Gradient
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,3));
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 0, 2), is(1.5));

        // Varying Gradient, quitting half way as have reached the end of the coordinates
        coords = Arrays.asList(new Point(0,0), new Point(1,2), new Point(2,3));
        assertThat(FunctionsHorizonMatching.gradientAhead(coords, 1, 1), is(1.0));


    }

    @Test
    public void aheadExistsTest() throws Exception {

        List<Point> coords = Arrays.asList(new Point(0,0), new Point(1,1), new Point(2,2));

        // 3 points, searching 0 ahead where this index exists
        assertTrue(FunctionsHorizonMatching.aheadExists(coords,0,0));

        // 3 points, searching 0 ahead where this index does not exist
        assertFalse(FunctionsHorizonMatching.aheadExists(coords,3,0));

        // 3 points, searching 1 ahead
        assertTrue(FunctionsHorizonMatching.aheadExists(coords,0,1));

        // 3 points, searching 2 (max) ahead
        assertTrue(FunctionsHorizonMatching.aheadExists(coords,0,2));

        // 3 points, searching 3 ahead (out of bounds)
        assertFalse(FunctionsHorizonMatching.aheadExists(coords,0,3));

        // Starting at the second point, valid search
        assertTrue(FunctionsHorizonMatching.aheadExists(coords, 1, 1));

        // Starting at the second point, invalid search
        assertFalse(FunctionsHorizonMatching.aheadExists(coords, 1, 2));

    }

    @Test
    public void findMaximasMinimasTest() throws Exception {

        // Remember that this is the graph coordinate system - +ve is top right

        // Pointy min, pointy max  \/\
        List<Point> input = Arrays.asList(new Point(0,50), new Point(1,1), new Point(2,50), new Point(3, 1));
        List<Point> expectedOutput = Arrays.asList(null, new Point(1,1), new Point(2, 50));
        assertThat(Arrays.asList(2,3), is(Arrays.asList(2,3)));
        assertThat(new Point(2,3), is(new Point(2,3)));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));

        // Pointy max, pointy min /\/
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3, 50));
        expectedOutput = Arrays.asList(new Point(1,50), new Point(2, 1));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));
        //                       ___
        // Flat max, pointy min /   \/
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,50), new Point(3,50), new Point(4,1), new Point(5, 50));
        expectedOutput = Arrays.asList(new Point(2,50), new Point(4, 1));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));

        // Pointy max, flat min   /\___/
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3,1), new Point(4,1), new Point(5, 50));
        expectedOutput = Arrays.asList(new Point(1,50), new Point(3, 1));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));
        //                     ___
        // Flat max, flat min /   \__/
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,50), new Point(3,50), new Point(4,1), new Point(5, 1), new Point(6, 1), new Point(7, 50));
        expectedOutput = Arrays.asList(new Point(2,50), new Point(5, 1));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));
        //                         _._
        // Smooth max, smooth min /   \._./
        input = Arrays.asList(new Point(0,1), new Point(1,45), new Point(2,50), new Point(3,47), new Point(4,5), new Point(5, 2), new Point(6, 10), new Point(7, 50));
        expectedOutput = Arrays.asList(new Point(2,50), new Point(5, 2));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));

        // Many max/min    ./\   /\. /
        //                ./  \./  \/
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,20), new Point(3,1), new Point(4,50), new Point(5, 1), new Point(6, 10), new Point(7, 50));
        expectedOutput = Arrays.asList(new Point(1,50), new Point(3, 1), new Point(4, 50), new Point(5, 1));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));

        // Does it ignore minimas that are too small    /\   /\
        //                                             /  \~/  \        What is too small? Maybe test thresholding? Is picking up a difference of 0.1 when graph is 0 -> 50
        input = Arrays.asList(new Point(0,1), new Point(1,50), new Point(2,1), new Point(3, 2), new Point(4, 1), new Point(5, 50), new Point(6, 1));
        expectedOutput = Arrays.asList(new Point(1,50), new Point(3, 2 ), new Point(5, 50));
        assertThat(FunctionsHorizonMatching.findMaximasMinimas(input, 6,1, getSearchWidth(input)).getMaximasMinimas(), is(expectedOutput));

        // Can't have two minimas in a row, in between must be a maxima. No test for that as doesn't exist

    }

    @Test
    public void getSearchWidthTest() throws Exception {
        List<Point> coords = new ArrayList<>();
        coords.add(new Point(1,1));
        assertThat(getSearchWidth(coords), is(1));


        coords.add(new Point(2,2));
        assertThat(getSearchWidth(coords), is(1));


        for (int i = 3; i <= 39; i++)
            coords.add(new Point(i,i));

        // 39 coords
        assertThat(getSearchWidth(coords), is(1));

        // 40 coords
        coords.add(new Point(40,40));
        assertThat(getSearchWidth(coords), is(2));

        // 41 coords
        coords.add(new Point(41,41));
        assertThat(getSearchWidth(coords), is(2));

    }
}