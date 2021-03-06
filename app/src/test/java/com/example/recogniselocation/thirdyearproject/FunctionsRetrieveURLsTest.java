package com.example.recogniselocation.thirdyearproject;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class FunctionsRetrieveURLsTest {
    @Test
    public void getIndexOfLastPathTest() throws Exception {
        // Start, mid, and a full end path
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(13, 3), is(10));

        // Start, mid, and a partial end path
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(12, 3), is(10));

        // Start, mid, and an end path with 1 value
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(11, 3), is(10));

        // Start and a full end path
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(7, 3), is(4));

        // Start and a partial end path
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(7, 3), is(4));

        // Start and an end path with 1 value
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(7, 3), is(4));

        // MANY middle paths
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(19, 3), is(16));

        // Different no. of samples
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(9, 2), is(7));
        assertThat(FunctionsRetrieveURLs.getIndexOfLastPath(9, 4), is(5));
    }

}