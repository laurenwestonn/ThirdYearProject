package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import static android.content.ContentValues.TAG;

// This is the class that was originally used when you tapped the photo on the old main page
// Now I'll adapt it to be my full screen photo (as it kinda already was) to be used in the final UI
public class PhotoActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);

        // Perform the program
        // Get the result of the edge detection that was passed through
        Edge edge = getIntent().getParcelableExtra("Edge");

        if (edge == null)
            Log.e(TAG, "onCreate: Edge came through as null!");
        else if (edge.getCoords() == null)
            Log.e(TAG, "onCreate: Coords came through as null, and bitmap probably has too " + edge.getBitmap());
        else
            Log.d(TAG, "onCreate: Yay in the photo activity we got the edge " + edge.getCoords());
        
        // Put the result of it on the image
        this.findViewById(R.id.photo).setBackground(null);
        this.findViewById(R.id.photo).setBackgroundColor(Color.YELLOW);
        Log.d(TAG, "onCreate: Made image yellow");

        BitmapDrawable drawable;

        // TODO: FIND OUT WHY THIS DOESN'T PUT THE BITMAP ON
        if (edge.getBitmap() != null) {
            drawable = new BitmapDrawable(this.getResources(), edge.getBitmap());
            this.findViewById(R.id.photo).setBackground(drawable);
            Log.d(TAG, "onCreate: Put the edge detected image on the button");
        }
        else
            Log.e(TAG, "onCreate: Bitmap got in the photo activity is null! " + edge.getBitmap());

        this.findViewById(R.id.photo).setBackgroundColor(Color.RED);
        Log.d(TAG, "onCreate: Made image red");
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