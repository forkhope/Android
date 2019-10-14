package com.example.cameraphoto;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.util.Log;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "MyCamera";
    
    public static final int MEDIA_TYPE_IMAGE = 1;

    private Camera mCamera;
    private CameraPreview mPreview;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create our Preview view and set it as the content of our activity
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        // Create an instance of Camera
        mCamera = safeCameraOpen();
        if (mCamera != null) {
            mPreview.setCamera(mCamera);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCameraAndPreview();
    }

    private Camera safeCameraOpen() {
        Camera c = null;
        try {
            releaseCameraAndPreview();
            c = Camera.open();
        } catch (Exception e) {
            Log.e(LOG_TAG, "failed to open Camera");
            e.printStackTrace();
        }

        return c;
    }

    private void releaseCameraAndPreview() {
        mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(LOG_TAG, "Error creating media file, check storage permissions.");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(LOG_TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(LOG_TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "MyCameraApp");

        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdirs()) {
                Log.d(LOG_TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        }
        else {
            return null;
        }

        return mediaFile;
    }
}
