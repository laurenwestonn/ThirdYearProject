package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.util.List;

import static android.content.ContentValues.TAG;

// This is the class that was originally used when you tapped the photo on the old main page
// Now I'll adapt it to be my full screen photo (as it kinda already was) to be used in the final UI
public class PhotoActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);

        // Perform the program

        // Get values passed through to this activity via the intent
        // Todo: Get the others passed through as well as the ID
        // Bitmap is too big to send so find image from resources using ID sent
        int drawableID = getIntent().getIntExtra("drawableID", 0);

        if (drawableID == 0)
            Log.e(TAG, "onCreate: ID didn't come through. Thinks its 0");
        else
            Log.d(TAG, "onCreate: Yay ID came through as " + drawableID);


        // Get a mutable bitmap of the image
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inMutable = true;
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableID, opt);

        // Colour onto the bitmap the edge we've detected, marking the matched points
        List<Point> photoCoords = getIntent().getParcelableArrayListExtra("photoCoords");
        bmp = markEdgeCoords(bmp, photoCoords);

        //List<Point> matchedCoords = getIntent().getParcelableArrayListExtra("matchedPhotoCoords");
        //bmp = markMaximasMinimasOnPhoto(bmp, matchedCoords);

        // Put bitmap onto the image button
        this.findViewById(R.id.photo).setBackground(new BitmapDrawable(this.getResources(), bmp));
        Log.d(TAG, "onCreate: Put the image with id " + drawableID + " on the button");
    }

    private Bitmap markEdgeCoords(Bitmap bmp, List<Point> photoCoords) {
        return ImageManipulation.colourBitmapCoords(bmp, photoCoords, Color.WHITE, 20);
    }

    // Colour maxima in red, minima in blue
    private static Bitmap markMaximaMinimaOnPhoto(Bitmap bmp, List<Point> photoMM, Activity a)
    {
        // If the first is a maxima, start from the first index
        int maxInd = photoMM.size() == 2 ? 0 : 2;
        ImageManipulation.colourArea(bmp,   (int) photoMM.get(maxInd).getX(),
                (int) photoMM.get(maxInd).getY(),
                Color.RED, 40, 40);
        ImageManipulation.colourArea(bmp,   (int) photoMM.get(1).getX(),
                (int) photoMM.get(1).getY(),
                Color.BLUE, 40, 40);


        // Put this on the image button
        ImageButton imageButton = (ImageButton) a.findViewById(R.id.photo);
        BitmapDrawable drawable = new BitmapDrawable(a.getResources(), bmp);
        imageButton.setBackground(drawable);


        return bmp;
    }

    // Mark of the maximas and minimas in varying colours
    private static Bitmap markMaximasMinimasOnPhoto(Bitmap bmp, List<Point> photoMMs)
    {
        int colour = Color.BLACK;

        for (Point p : photoMMs) {
            bmp = ImageManipulation.colourArea(bmp, (int) p.getX(), (int) p.getY(), colour, 40, 40);
            colour += 1000;
        }
        return bmp;
    }


    public void detectHorizon(View view) {
        ImageButton imageButton = (ImageButton) view;
        Bitmap bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap();
        Edge edge = null;

        // Get the image off the button as a bitmap
        if (bmp != null)
            edge = ImageManipulation.detectEdge(bmp, false, false, true, true);

        if (edge != null)
            ((ImageButton) view).setImageBitmap(edge.getBitmap());
    }


    // Deal with button clicks
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: A button was clicked");
        switch (view.getId()) {
            case R.id.back: {
                Log.d(TAG, "buttonClicked: Go back to the start page");
                Intent intent = new Intent(this.getString(R.string.START_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.next: {
                Log.d(TAG, "buttonClicked: Go to the next page (not yet implemented)");
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
                // Do for the other demos... or just do all in this one and have one
                // more for the photo button
        }
    }

}