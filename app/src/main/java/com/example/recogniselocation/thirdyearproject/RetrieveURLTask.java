package com.example.recogniselocation.thirdyearproject;

import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LaUrE on 07/10/2017.
 */

public class RetrieveURLTask extends AsyncTask<String, Void, List<String>>  {

    private Exception e;

    protected List<String> doInBackground(String... urls) {
        return connectToURL(urls[0]);
    }

    private List<String> connectToURL(String urls) {
        // As we appended "url," every time, we need to remove the last comma
        urls = urls.substring(0, urls.length() - 1);

        // The URLs are comma separated. Split and do the same for each
        String[] urlArr = urls.split("!");

        List<String> responseList = new ArrayList<>(7);

        String inputLine;
        HttpURLConnection con = null;
        BufferedReader in;

        for (String url : urlArr) {
            try {
                Log.d("Hi", "URL:" + url);
                URL urlObj = new URL(url);
                con = (HttpURLConnection) urlObj.openConnection();

                // If the connection was successful
                if (con.getResponseCode() == 200) {
                    // Get the response
                    con.setRequestMethod("GET");
                    con.connect();


                    //Todo: Check that I did get all 10 results

                    // Build up the response in a string
                    StringBuilder response = new StringBuilder();
                    in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    while ((inputLine = in.readLine()) != null)
                        response.append(inputLine);

                    in.close();
                    Log.d("Hi", "Got response of " + response.toString());
                    responseList.add(response.toString());
                } else {
                    Log.d("Hi", "The connection wasn't successful: " + con.getResponseMessage());
                }
            } catch(Exception e) {
                Log.d("Hi", "Problem connecting to URL: " + e.toString());
            } finally {
                if (con != null)
                    con.disconnect();
            }
        }
        return responseList;
    }

    protected void onPostExecute(List<String> responses) {

        List<Result> highPoints= new ArrayList<>(7) ;

        for (String response : responses) {
            Log.d("Hi", "Another response");
            // If we got a response, parse it
            if (response != null) {
                try {
                    // Parse the JSON response
                    Gson gson = new Gson();
                    Response results = gson.fromJson(response, Response.class);

                    // The highest position you can see
                    double hiLat = 91; //Lat cannot be 91, use this to check if it was set
                    double hiLng = 0;
                    double hiEl = results.getResults().get(0).getElevation();
                    double hiDis = 0;
                    int loopCount = 0;

                    for(Result r : results) {
                        if (r.getElevation() > hiEl) {
                            hiEl = r.getElevation();
                            hiLat = r.getLocation().getLat();
                            hiLng = r.getLocation().getLng();
                            hiDis = 45 / (7 - 1) * loopCount++; //ToDo: find the step size
                        }
                        Log.d("Hi", r.toString());
                    }
                    if (hiLat <= 90)
                        highPoints.add(new Result(new LatLng(hiLat, hiLng), hiEl, hiDis));

                } catch(Exception e){
                    Log.d("Hi", "On post execute failure\n" + e);
                }

            }
        }
        Log.d("Hi", "Highest points are:");
        Log.d("Hi", highPoints.toString());
    }
}