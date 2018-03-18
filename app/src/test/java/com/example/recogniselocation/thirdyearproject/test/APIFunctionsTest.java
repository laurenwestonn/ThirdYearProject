package com.example.recogniselocation.thirdyearproject.test;

import com.example.recogniselocation.thirdyearproject.APIFunctions;
import com.example.recogniselocation.thirdyearproject.LatLng;
import com.example.recogniselocation.thirdyearproject.LocationDirection;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class APIFunctionsTest {

    @Test
    public void checkIndexingString() {
        String s = "01&s=L67";
        int indexOfSamples = s.lastIndexOf("&s=");
        indexOfSamples += 3;
        assertThat(indexOfSamples, is(5));
        assertThat(s.charAt(indexOfSamples), is('L'));

    }
    @Test
    // Tests that the right number of requests are made with the right number of locations
    // per request with the right number of samples per request
    public void getURLsManyPathsManyGroups() throws Exception {

        // Isn't supposed to work for one path
        // Todo: Throw exception for this

        int dir = 0;
        LatLng yourLoc = new LatLng(0,0);
        int widthOfSearch = 180;
        int noOfPaths = 2;
        int noOfPathsPerGroup = 2;
        int samplesPerPath = 1;
        double searchLength = 0.1;
        String key = "AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U"; // Todo: Get the string from the debug google_maps_api.xml
        String urlStart = "https://maps.googleapis.com/maps/api/elevation/json?path="; // Todo: Get the string from the debug google_maps_api.xml
        List<String> result;
        int indexOfSamples;

        // Only start and end path exists in a group. One group. One sample per path
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        assertThat(result.size(), is(1));
        //  First path's end | HERE | Last path's end
        assertTrue(result.get(0).matches("https://maps\\.googleapis\\.com/maps/api/elevation/json\\?path=[^|]*\\|" + yourLoc + "\\|[^|]*"));
        indexOfSamples = result.get(0).indexOf("&samples=") + 9;
        assertThat(result.get(0).charAt(indexOfSamples), is('3'));


        // Start, middle and end path exists in a group. One group. One sample per path
        noOfPaths = 3;
        noOfPathsPerGroup = 3;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        assertThat(result.size(), is(1));
        // First End | HERE | Mid end | HERE | Last end
        assertTrue(result.get(0).matches("https://maps\\.googleapis\\.com/maps/api/elevation/json\\?path=[^|]*\\|" + yourLoc + "\\|[^|]*\\|" + yourLoc + "\\|[^|]*"));
        indexOfSamples = result.get(0).indexOf("&samples=") + 9;
        assertThat(result.get(0).charAt(indexOfSamples), is('5'));

        // Start, middle and end path exists in a group. Two full groups. One sample per path
        noOfPaths = 6;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        assertThat(result.size(), is(2));
        // Last request: First End | HERE | Mid end | HERE | Last end
        assertTrue(result.get(1).matches("https://maps\\.googleapis\\.com/maps/api/elevation/json\\?path=[^|]*\\|" + yourLoc + "\\|[^|]*\\|" + yourLoc + "\\|[^|]*"));
        indexOfSamples = result.get(1).indexOf("&samples=") + 9;
        assertThat(result.get(1).charAt(indexOfSamples), is('5'));

        // Start, middle and end path exists in a group. Two groups - last partial. One sample per path
        noOfPaths = 5;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        assertThat(result.size(), is(2));
        // Last request: First End | HERE | Last end
        assertTrue(result.get(1).matches("https://maps\\.googleapis\\.com/maps/api/elevation/json\\?path=[^|]*\\|" + yourLoc + "\\|[^|]*"));
        indexOfSamples = result.get(1).indexOf("&samples=") + 9;
        assertThat(result.get(1).charAt(indexOfSamples), is('3'));

        // Start, middle and end path exists in a group. Two groups - last only has one. One sample per path
        noOfPaths = 4;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        assertThat(result.size(), is(2));
        // Last request: First End | HERE
        assertTrue(result.get(1).matches("https://maps\\.googleapis\\.com/maps/api/elevation/json\\?path=[^|]*\\|" + yourLoc + "[^|]*"));
        indexOfSamples = result.get(1).indexOf("&samples=") + 9;
        assertThat(result.get(1).charAt(indexOfSamples), is('2'));

    }

    @Test
    // Checks the right number of samples are sent when a request has
    // just the start path
    // start and an end path
    // start and a mid and an end path
    // For 2 samples per path, i.e. many. The test before tested for 1.
    public void getURLsManySamplesPerPath() {

        int dir = 0;
        LatLng yourLoc = new LatLng(0,0);
        int widthOfSearch = 180;
        int noOfPaths = 2;
        int noOfPathsPerGroup = 2;
        int samplesPerPath = 2;
        double searchLength = 0.1;
        String key = "AIzaSyBtNG5C0b9-euGrqAUhqbiWc_f7WSjNZ-U"; // Todo: Get the string from the debug google_maps_api.xml
        String urlStart = "https://maps.googleapis.com/maps/api/elevation/json?path="; // Todo: Get the string from the debug google_maps_api.xml
        List<String> result;
        int indexOfSamples;

        // Only two path exists in a group. One group. Two samples per path
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        //  First path's end | HERE | Last path's end
        indexOfSamples = result.get(0).indexOf("&samples=") + 9;
        assertThat(result.get(0).charAt(indexOfSamples), is('5'));


        // Start, middle and end path exists in a group. One group. Two sample per path
        noOfPaths = 3;
        noOfPathsPerGroup = 3;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        // First End | HERE | Mid end | HERE | Last end
        indexOfSamples = result.get(0).indexOf("&samples=") + 9;
        assertThat(result.get(0).charAt(indexOfSamples), is('9'));

        // Three paths exists in a group. Two groups - last partial. Two samples per path
        // Last should have 5 samples
        noOfPaths = 5;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        // Last request: First End | HERE | Last end
        indexOfSamples = result.get(1).indexOf("&samples=") + 9;
        assertThat(result.get(1).charAt(indexOfSamples), is('5'));

        // Start, middle and end path exists in a group. Two groups - last only has one. One sample per path
        // Last should have 3 samples
        noOfPaths = 4;
        result = APIFunctions.getURLsToRequest(new LocationDirection("", yourLoc, dir), widthOfSearch, noOfPaths, noOfPathsPerGroup, samplesPerPath, searchLength, key, urlStart);
        // Last request: First End | HERE
        indexOfSamples = result.get(1).indexOf("&samples=") + 9;
        assertThat(result.get(1).charAt(indexOfSamples), is('3'));


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