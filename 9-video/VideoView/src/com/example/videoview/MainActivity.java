package com.example.videoview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.VideoView;
import android.widget.MediaController;
import android.media.MediaPlayer;
import android.content.pm.ActivityInfo;
import android.util.Log;

public class MainActivity extends Activity implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {
    public static final String TAG = "VideoPlayer";
    private VideoView mVideoView;
    private int mPositionWhenPaused = -1;
    private MediaController mMediaController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        //Set the screen to landscape.
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        mVideoView = (VideoView) findViewById(R.id.video_view);

        //Create media controller，组件可以控制视频的播放，暂停，回复，seek等操作，不需要你实现
        mMediaController = new MediaController(this);
        mVideoView.setMediaController(mMediaController);
    }

    public void onStart() {
        // Play Video
        mVideoView.setVideoPath("/sdcard/stream.h264");
        mVideoView.start();

        super.onStart();
    }

    public void onPause() {
        // Stop video when the activity is pause.
        mPositionWhenPaused = mVideoView.getCurrentPosition();
        mVideoView.stopPlayback();

        super.onPause();
    }

    public void onResume() {
        // Resume video player
        if(mPositionWhenPaused >= 0) {
            mVideoView.seekTo(mPositionWhenPaused);
            mPositionWhenPaused = -1;
        }

        super.onResume();
    }

    public boolean onError(MediaPlayer player, int arg1, int arg2) {
        return false;
    }

    public void onCompletion(MediaPlayer mp) {
        this.finish();
    }
}
