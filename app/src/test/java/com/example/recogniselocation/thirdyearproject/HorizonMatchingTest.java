package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import java.util.Arrays;
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
}