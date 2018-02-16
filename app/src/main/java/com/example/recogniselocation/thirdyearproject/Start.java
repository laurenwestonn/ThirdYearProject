package com.example.recogniselocation.thirdyearproject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

public class Start extends AppCompatActivity {

    private static final String TAG = "Start";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        Log.d(TAG, "onCreate: Started the start :)");
    }



    // Deal with button clicks
    public void buttonClicked(View view) {
        Log.d(TAG, "buttonClicked: A button was clicked");

        // Find which demo location you need
        LocationOrientation loc = Demos.getDemo(view.getId());
        Log.d(TAG, "onCreate: Got location " + loc);

        // Use the location orientation to perform the location recognition

        // Get the bitmap of the image
        Bitmap bmp = BitmapFactory.decodeResource(getResources(),
                getResources().getIdentifier(loc.getName(), "drawable", getPackageName()));

        // Detect the image
        EdgeDetection edgeDetection = ImageManipulation.detectEdge(bmp);

        if (edgeDetection != null)
            Log.d(TAG, "buttonClicked: Yep be detected the edge of " + loc.getName() + " to be " + edgeDetection.getCoords());

        switch (view.getId()) {
            case R.id.demo1: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo2: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo3: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            case R.id.demo4: {
                Intent intent = new Intent(this.getString(R.string.PHOTO_ACTIVITY));
                Bundle b = new Bundle();
                //b.putInt("Demo", view.getId()); // Pass through which demo was requested
                intent.putExtra("Demo", view.getId());
                startActivity(intent);
                finish();
                break;
            }
            default:
                Log.d(TAG, "buttonClicked: didn't recognise id " + view.getId() + " of view " + view.toString());
            // Do for the other demos... or just do all in this one and have one
            // more for the photo button
        }

        // Perform recognition
    }

}
