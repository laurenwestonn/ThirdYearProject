package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import static com.example.recogniselocation.thirdyearproject.HorizonMatching.getFirstElevationIndex;
import static com.example.recogniselocation.thirdyearproject.HorizonMatching.getTheNextElevationMM;

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

}