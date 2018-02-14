package com.example.recogniselocation.thirdyearproject;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
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

        // Check get the demo that was passed through
        Bundle b = getIntent().getExtras();
        LocationOrientation loc;
        if (b != null) {
            loc = Demos.getDemo(b.getInt("Demo"));
            Log.d(TAG, "onCreate: Got location " + loc);
        }
        else
            Log.e(TAG, "onCreate: Couldn't find bundle");


        // Perform the program

        // Put the result of it on the image
    }

    public void detectHorizon(View view) {
        ImageButton imageButton = (ImageButton) view;
        Bitmap bmp = ((BitmapDrawable)imageButton.getDrawable()).getBitmap();
        EdgeDetection edgeDetection = null;

        // Get the image off the button as a bitmap
        if (bmp != null)
            edgeDetection = ImageManipulation.detectEdge(bmp);

        if (edgeDetection != null)
            ((ImageButton) view).setImageBitmap(edgeDetection.bmp);
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