package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

// This is the class that was originally used when you tapped the photo on the old main page
// Now I'll adapt it to be my full screen photo (as it kinda already was) to be used in the final UI
public class ActPhoto extends Activity {

    static boolean requireCoarse = false;
    static Bitmap origBitmap;
    static List<Point> coarsePhotoCoords;   // These need to be global variables so that
    static StandardDeviation sd;            // they can be accessed whenever someone
    static List<Point> photoCoords;         // taps the screen to view the coarse coords
    static List<Point> matchedCoords;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);

        // Get values passed through to this activity via the intent
        // Bitmap is too big to send so find image from resources using ID sent
        int drawableID = getIntent().getIntExtra("drawableID", 0);

        if (ActStart.uri == null && drawableID == 0)
            Log.e(TAG, "onCreate: Photo ID didn't come through and we have no URI for a photo");

        // Get a mutable bitmap of the image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        origBitmap = null;
        if (ActStart.uri != null) {
            // Get the photo you took from your location
            try {
                origBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), ActStart.uri);
                // Make the image smaller so the app can deal with it
                origBitmap = Bitmap.createScaledBitmap(origBitmap, origBitmap.getWidth() / 2, origBitmap.getHeight() / 2, false);
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Couldn't get bitmap: " + e.getMessage());
            }
        } else {
            origBitmap = BitmapFactory.decodeResource(getResources(), drawableID, opt);
        }

        // Get the results
        coarsePhotoCoords = getIntent().getParcelableArrayListExtra("coarsePhotoCoords");
        sd = getIntent().getParcelableExtra("sd");
        photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
        matchedCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");


        // Draw on results and put onto the image button
        Bitmap resultBMP = drawResultsOnBitmap(origBitmap, requireCoarse);
        this.findViewById(R.id.photo).setBackground(new BitmapDrawable(this.getResources(), resultBMP));
    }

    private static Bitmap drawResultsOnBitmap(Bitmap bmp, boolean showCoarse) {
        if (bmp != null) {
            Bitmap resultBMP = bmp.copy(bmp.getConfig(), true);
            if (showCoarse) {
                if (coarsePhotoCoords != null)
                    return markCoarseEdgeCoords(resultBMP, coarsePhotoCoords, sd);
                else
                    return null;
            } else {
                if (photoCoords != null)
                    ;//resultBMP = markEdgeCoords(resultBMP, photoCoords);  //Uncomment to display the edge detected
                // Mark the maximas and minimas (common with the map)
                if (matchedCoords != null)
                    resultBMP = markMaximasMinimasOnPhoto(resultBMP, matchedCoords);
                return resultBMP;
            }
        } else {
            Log.e(TAG, "onCreate: Couldn't find bitmap");
            return null;
        }
    }

    // Mark the coarse edge coordinates found, and the SD range found from these points
    private static Bitmap markCoarseEdgeCoords(Bitmap bmp, List<Point> photoCoords, StandardDeviation sd) {
        int width = (bmp.getHeight() / 17) * 2 + 1; // Width got from coarseMask method
        bmp = colourBitmapCoords(bmp, photoCoords, Color.argb(255, 250, 150, 50), width);
        return colourSD(bmp, sd);
    }

    private static Bitmap colourSD(Bitmap bmp, StandardDeviation sd) {
        // Draw the horizontal middle of the edges as found by the coarse masking's Standard Deviation
        bmp = FunctionsImageManipulation.colourArea(bmp, bmp.getWidth()/2, (int)sd.getMean(), Color.YELLOW,
                bmp.getWidth()-1, 10);
        // Draw the range decided to be the horizon by the coarse masking's Standard Deviation
        int drawnSDRadius = 15;
        bmp = FunctionsImageManipulation.colourArea(bmp, bmp.getWidth()/2,sd.getMinRange()+drawnSDRadius,
                Color.RED,bmp.getWidth()-1, 30);
        return FunctionsImageManipulation.colourArea(bmp, bmp.getWidth()/2,sd.getMaxRange()-drawnSDRadius,
                Color.RED,bmp.getWidth()-1, 30);
    }

    private static Bitmap markEdgeCoords(Bitmap bmp, List<Point> photoCoords) {
        int width = bmp.getWidth() / 125;
        return colourBitmapCoords(
                bmp, photoCoords, Color.argb(255, 250, 150, 50), width);
    }

    static Bitmap colourBitmapCoords(Bitmap bmp, List<Point> coords, int colour, int size) {
        for (Point p : coords)
            bmp = FunctionsImageManipulation.colourArea(bmp, (int) p.getX() , (int) p.getY(), colour, size, size);
        return bmp;
    }

    // Mark of the maximas and minimas in varying colours
    private static Bitmap markMaximasMinimasOnPhoto(Bitmap bmp, List<Point> photoMMs)
    {
        int minColour = Color.BLUE;
        int maxColour = Color.RED;
        int diam = bmp.getHeight() / 34 + 1;
        boolean max = true;

        for (Point p : photoMMs) {
            if (p != null) {
                if (max) {
                    bmp = FunctionsImageManipulation.colourArea(bmp, (int) p.getX(), (int) p.getY(), maxColour, diam, diam);
                    maxColour += 254 / photoMMs.size() / 2; // Varying reds
                } else {
                    bmp = FunctionsImageManipulation.colourArea(bmp, (int) p.getX(), (int) p.getY(), minColour, diam, diam);
                    minColour += (254 / photoMMs.size() / 2) << 16; // Varying blues
                }
            }
            max = !max;
        }
        return bmp;
    }

    public void changeMask(View v)
    {
        requireCoarse = !requireCoarse;
        // Put bitmap onto the image button
        v.setBackground(new BitmapDrawable(this.getResources(),
                drawResultsOnBitmap(origBitmap, requireCoarse)));
    }

    // Deal with button clicks
    public void buttonClicked(View view)
    {
        Log.d(TAG, "buttonClicked: A button was clicked");
        Intent intent;

        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                intent = new Intent(this.getString(R.string.START_ACTIVITY));
                break;
            }
            case R.id.before: {
                Log.d(TAG, "Go back to the graph");
                intent = new Intent(this.getString(R.string.GRAPH_ACTIVITY));
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the map");
                intent = new Intent(this.getString(R.string.MAP_ACTIVITY));
                break;
            }
            default:
                intent = new Intent(this.getString(R.string.START_ACTIVITY));
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }

        if (view.getId() == R.id.before || view.getId() == R.id.next) { // Send data to the next activity
            // For the photo activity
            int drawableID = getIntent().getIntExtra("drawableID", 0);
            intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
            ArrayList<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
            intent.putParcelableArrayListExtra("photoCoords", photoCoords);      // To draw the edge
            List<Point> coarsePhotoCoords = getIntent().getParcelableArrayListExtra("coarsePhotoCoords");
            intent.putParcelableArrayListExtra("coarsePhotoCoords", (ArrayList<Point>) coarsePhotoCoords);      // To draw the coarse edge
            StandardDeviation sd = getIntent().getParcelableExtra("sd");
            intent.putExtra("sd", sd);      // To mark coarse range
            ArrayList<Point> matchedPhotoCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
            intent.putParcelableArrayListExtra("matchedPhotoCoords", matchedPhotoCoords);  // To mark on the matched points

            // For the map activity
            LatLng yourLocation = getIntent().getParcelableExtra("yourLocation");
            intent.putExtra("yourLocation", yourLocation);
            ArrayList<Result> highPoints = getIntent().getParcelableArrayListExtra("highPoints");
            intent.putParcelableArrayListExtra("highPoints", highPoints);
            ArrayList<Integer> matchedElevCoordsIndexes = getIntent().getIntegerArrayListExtra("matchedElevCoordsIndexes");
            intent.putIntegerArrayListExtra("matchedElevCoordsIndexes", matchedElevCoordsIndexes);  // To mark on the matched points

            // For the graph activity
            List<Point> elevationsCoords = getIntent().getParcelableArrayListExtra("elevationsCoords");
            intent.putParcelableArrayListExtra("elevationsCoords", (ArrayList<Point>) elevationsCoords);
            List<Point> photoSeriesCoords = getIntent().getParcelableArrayListExtra("photoSeriesCoords");
            intent.putParcelableArrayListExtra("photoSeriesCoords", (ArrayList<Point>) photoSeriesCoords);
        }

        startActivity(intent);
        finish();
    }

}