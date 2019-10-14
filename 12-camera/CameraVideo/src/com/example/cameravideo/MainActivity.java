package com.example.cameravideo;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.hardware.Camera;
import android.view.View;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
import android.widget.FrameLayout;
import android.widget.Button;
import android.util.Log;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

public class MainActivity extends Activity {
    private static final String LOG_TAG = "MyCameraVideo";

    private static final int MEDIA_TYPE_VIDEO = 1;

    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;

    private Button mCaptureButton;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Create our Preview view and set it as the content of our activity
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRecording) {
                        // stop recording and release MediaRecorder
                        releaseMediaRecorder(); // release the MediaRecorder object

                        // inform the user that recording has stopped
                        setCaptureButtonText(R.string.capture_text);
                        isRecording = false;
                    } else {
                        // initialize video camera
                        if (prepareMediaRecorder()) {
                            // Camera is available and unlocked, MediaRecorder is
                            // prepared, now you can start recording
                            mMediaRecorder.start();

                            // inform the user that recording has started
                            setCaptureButtonText(R.string.stop_text);
                            isRecording = true;
                        } else {
                            // prepare didn't work, release the MediaRecorder
                            Log.d(LOG_TAG, "MediaRecorder prepare failed!");
                            releaseMediaRecorder();
                        }
                    }
                }
            }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "CameraVideo: onResume");

        mCamera = getCameraInstance();
        if (mCamera != null) {
            mPreview.setCamera(mCamera);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "CameraVideo: onPause");

        releaseMediaRecorder();
        releaseCameraAndPreview();
    }

    private void setCaptureButtonText(int id) {
        mCaptureButton.setText(id);
    }

    /** A safe way to get an instance of the Camera object. */
    private Camera getCameraInstance() {
        Camera c = null;

        try {
            releaseCameraAndPreview();
            c = Camera.open(); // attemp to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private boolean prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(LOG_TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(LOG_TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();      // stop the recording
            mMediaRecorder.reset();     // clear recorder configuration
            mMediaRecorder.release();   // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();             // lock camera for later use
            isRecording = false;
            setCaptureButtonText(R.string.capture_text);
        }
    }

    private void releaseCameraAndPreview() {
        mPreview.setCamera(null);
        if (mCamera != null) {
            mCamera.release();  // release the camera for other applications
            mCamera = null;
        }
    }

    private File getOutMediaFile(int type) {
        // To be safe, you should check that the SD Card mounted
        // using Environment.getExternalStorageState() before doing this.
        String state = Environment.getExternalStorageState();
        if (! Environment.MEDIA_MOUNTED.equals(state)) {
            Log.d(LOG_TAG, "External Storage (SD Card) doesn't mount!");
            return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraVideo");

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()) {
            if (! mediaStorageDir.mkdir()) {
                Log.d(LOG_TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
}
