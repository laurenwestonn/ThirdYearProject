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
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

// This is the class that was originally used when you tapped the photo on the old main page
// Now I'll adapt it to be my full screen photo (as it kinda already was) to be used in the final UI
public class PhotoActivity extends Activity {

    static boolean pShowCoarse;
    static Bitmap origBitmap;
    static List<Point> coarsePhotoCoords;
    static List<Point> photoCoords;
    static List<Point> matchedCoords;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);

        pShowCoarse = RetrieveURLTask.showCoarse;

        // Get values passed through to this activity via the intent
        // Bitmap is too big to send so find image from resources using ID sent
        int drawableID = getIntent().getIntExtra("drawableID", 0);

        if (Start.uri == null && drawableID == 0)
            Log.e(TAG, "onCreate: Photo ID didn't come through and we're not using a real photo");

        // Get a mutable bitmap of the image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        origBitmap = null;
        if (Start.uri != null) {
            // Get the photo you took from your location
            try {
                origBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Start.uri);
                // Make the image smaller so the app can deal with it
                origBitmap = Bitmap.createScaledBitmap(origBitmap, origBitmap.getWidth() / 2, origBitmap.getHeight() / 2, false);
                Log.d(TAG, "onPostExecute: Bitmap got is " + origBitmap.getWidth() + " x " + origBitmap.getHeight() + ". " + origBitmap.getConfig());
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Couldn't get bitmap: " + e.getMessage());
            }
        } else {
            origBitmap = BitmapFactory.decodeResource(getResources(), drawableID, opt);
        }

        // Get the results
        coarsePhotoCoords = getIntent().getParcelableArrayListExtra("coarsePhotoCoords");
        photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
        matchedCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");



        Bitmap resultBMP = drawResultsOnBitmap(origBitmap, pShowCoarse);
        // Put bitmap onto the image button
        this.findViewById(R.id.photo).setBackground(new BitmapDrawable(this.getResources(), resultBMP));

    }

    private static Bitmap drawResultsOnBitmap(Bitmap bmp, boolean showCoarse) {
        if (bmp != null) {
            Bitmap resultBMP = bmp.copy(bmp.getConfig(), true);
            if (showCoarse) {
                if (coarsePhotoCoords != null)
                    return markCoarseEdgeCoords(resultBMP, coarsePhotoCoords);
                else
                    return null;
            } else {
                if (photoCoords != null)
                    resultBMP = markEdgeCoords(resultBMP, photoCoords);
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

    private static Bitmap markCoarseEdgeCoords(Bitmap bmp, List<Point> photoCoords) {
        int width = (bmp.getHeight() / 17) * 2 + 1; // Width got from coarseMask method
        return ImageManipulation.colourBitmapCoords(
                bmp, photoCoords, Color.argb(255, 250, 150, 50), width);
    }

    private static Bitmap markEdgeCoords(Bitmap bmp, List<Point> photoCoords) {
        int width = bmp.getWidth() / 125;
        return ImageManipulation.colourBitmapCoords(
                bmp, photoCoords, Color.argb(255, 250, 150, 50), width);
    }

    // Mark of the maximas and minimas in varying colours
    private static Bitmap markMaximasMinimasOnPhoto(Bitmap bmp, List<Point> photoMMs)
    {
        int minColour = Color.BLUE;
        int maxColour = Color.RED;
        boolean max = true;

        for (Point p : photoMMs) {
            if (p != null) {
                if (max) {
                    bmp = ImageManipulation.colourArea(bmp, (int) p.getX(), (int) p.getY(), maxColour, 40, 40);
                    maxColour += 254 / photoMMs.size() / 2; // Varying reds
                } else {
                    bmp = ImageManipulation.colourArea(bmp, (int) p.getX(), (int) p.getY(), minColour, 40, 40);
                    minColour += 254 / photoMMs.size() / 2; // Varying blues
                }
            }
            max = !max;
        }
        return bmp;
    }

    public void changeMask(View v)
    {
        pShowCoarse = !pShowCoarse;
        // Put bitmap onto the image button
        v.setBackground(new BitmapDrawable(this.getResources(),
                drawResultsOnBitmap(origBitmap, pShowCoarse)));
    }

    // Deal with button clicks
    public void buttonClicked(View view)
    {
        Log.d(TAG, "buttonClicked: A button was clicked");
        Intent intent = null;

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
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
        }

        if (view.getId() == R.id.before || view.getId() == R.id.next) {
            // For the photo activity
            int drawableID = getIntent().getIntExtra("drawableID", 0);
            intent.putExtra("drawableID", drawableID);  // Bitmap is too big, find it via ID
            ArrayList<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
            intent.putParcelableArrayListExtra("photoCoords", photoCoords);      // To draw the edge
            List<Point> coarsePhotoCoords = getIntent().getParcelableArrayListExtra("coarsePhotoCoords");
            intent.putParcelableArrayListExtra("coarsePhotoCoords", (ArrayList<Point>) coarsePhotoCoords);      // To draw the coarse edge
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

            Log.d(TAG, "PhotoAct: Sending the matchedIndexes " + matchedElevCoordsIndexes);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        } else {
            Log.e(TAG, "buttonClicked: Couldn't find an intent for id " + view.getId());
        }
    }

}