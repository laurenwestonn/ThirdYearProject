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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);

        // Get values passed through to this activity via the intent
        // Todo: Get the others passed through as well as the ID
        // Bitmap is too big to send so find image from resources using ID sent
        int drawableID = getIntent().getIntExtra("drawableID", 0);

        if (Start.uri == null && drawableID == 0)
            Log.e(TAG, "onCreate: Photo ID didn't come through and we're not using a real photo");

        // Get a mutable bitmap of the image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap bmp = null;
        if (Start.uri != null) {
            // Get the photo you took from your location
            try {
                bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), Start.uri);
                Log.d(TAG, "onPostExecute: Bitmap got is " + bmp.getWidth() + " x " + bmp.getHeight() + ". " + bmp.getConfig());
            } catch (Exception e) {
                Log.e(TAG, "onCreate: Couldn't get bitmap: " + e.getMessage());
            }
        } else {
            bmp = BitmapFactory.decodeResource(getResources(), drawableID, opt);
        }

        if (bmp != null) {
            // Colour onto the bitmap the edge we've detected
            List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
            if (photoCoords != null)
                bmp = markEdgeCoords(bmp, photoCoords);
            // Mark the maximas and minimas (common with the map)
            List<Point> matchedCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
            if (matchedCoords != null)
                bmp = markMaximasMinimasOnPhoto(bmp, matchedCoords);

            // Put bitmap onto the image button
            this.findViewById(R.id.photo).setBackground(new BitmapDrawable(this.getResources(), bmp));

        } else {
            Log.e(TAG, "onCreate: Couldn't find bitmap");
        }

    }

    private Bitmap markEdgeCoords(Bitmap bmp, List<Point> photoCoords) {
        int width = bmp.getWidth() / 125;
        return ImageManipulation.colourBitmapCoords(
                bmp, photoCoords, Color.argb(255, 250, 150, 50), width);
    }

    // Mark of the maximas and minimas in varying colours
    private static Bitmap markMaximasMinimasOnPhoto(Bitmap bmp, List<Point> photoMMs)
    {
        int minColour = Color.BLUE;
        int maxColour = Color.RED;
        boolean max = true;//photoMMs.get(0) != null;

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

    // Deal with button clicks
    public void buttonClicked(View view) {
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