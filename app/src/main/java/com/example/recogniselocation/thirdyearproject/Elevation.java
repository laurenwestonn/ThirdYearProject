package com.example.recogniselocation.thirdyearproject;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.example.recogniselocation.thirdyearproject.MapsActivity.noOfPaths;

public class Elevation {

    public static List<String> interpretURLResponses(String[] urlArr) {

        List<String> responseList = new ArrayList<>(noOfPaths);

        for (String url : urlArr) {
            HttpURLConnection con = null;
            try {
                URL urlObj = new URL(url);
                con = (HttpURLConnection) urlObj.openConnection();

                // If the connection was successful
                if (con.getResponseCode() == 200) {
                    // Get the response
                    con.setRequestMethod("GET");
                    con.connect();

                    //Todo: Check that I did get all 10 results!

                    // Build up the response in a string
                    StringBuilder response = new StringBuilder();
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));

                    String inputLine;
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);

                    in.close();
                    responseList.add(response.toString());
                } else  Log.d("Hi", "Connection failed: " + con.getResponseMessage());
            } catch(Exception e) {
                Log.d("Hi", "Problem connecting to URL: " + e.toString());
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }
        return responseList;
    }
}
