package com.example.cameraphoto;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.util.Log;

import java.io.IOException;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String LOG_TAG = "MyCamera";

    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);

        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        setCamera(mCamera);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void setCamera(Camera camera) {
        if (mCamera == camera) {
            return;
        }

        stopPreviewAndFreeCamera();

        mCamera = camera;

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();

            mCamera = null;
        }
    }
}
