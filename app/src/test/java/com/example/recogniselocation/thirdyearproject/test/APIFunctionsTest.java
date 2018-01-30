package com.example.recogniselocation.thirdyearproject.test;

import com.example.recogniselocation.thirdyearproject.APIFunctions;
import com.example.recogniselocation.thirdyearproject.LatLng;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class APIFunctionsTest {
    @Test
    public void getElevations() throws Exception {
    }
    @Test
    public void getURLsToRequest() throws Exception {

        // Isn't supposed to work for one path
        // Todo: Throw exception for this

        int dir = 0;
        LatLng latLng = new LatLng(0,0);
        int widthOfSearch = 180;
        int noOfPaths = 2;
        int noOfPathsPerGroup = 2;
        int samplesPerPath = 1;
        double searchLength = 0.1;
        String key = "AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U"; // Todo: Get the string from the debug google_maps_api.xml
        List<String> result;

        // Only start and end path exists in a group. One group. One sample per path
        result = APIFunctions.getURLsToRequest(dir, latLng, widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key);
        assertThat(result.size(), is(1));


        // Start, middle and end path exists in a group. One group. One sample per path
        noOfPaths = 3;
        noOfPathsPerGroup = 3;
        result = APIFunctions.getURLsToRequest(dir, latLng, widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key);
        assertThat(result.size(), is(1));

        // Start, middle and end path exists in a group. Two full groups. One sample per path
        noOfPaths = 6;
        result = APIFunctions.getURLsToRequest(dir, latLng, widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key);
        assertThat(result.size(), is(2));

        // Start, middle and end path exists in a group. Two groups - last partial. One sample per path
        noOfPaths = 5;
        result = APIFunctions.getURLsToRequest(dir, latLng, widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key);
        assertThat(result.size(), is(2));

        // Start, middle and end path exists in a group. Two groups - last only has one. One sample per path
        noOfPaths = 4;
        result = APIFunctions.getURLsToRequest(dir, latLng, widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key);
        assertThat(result.size(), is(2));

    }

    @Test
    public void requestURL() throws Exception {
    }

    @Test
    public void getHighestVisiblePoint() throws Exception {
    }

    @Test
    public void drawOnGraph() throws Exception {
    }

    @Test
    public void findDistanceBetweenPlots() throws Exception {
    }

    @Test
    public void setBounds() throws Exception {
    }

}