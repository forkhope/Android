package com.example.mediaplayer;

import android.app.Activity;
import android.os.Bundle;
import android.media.MediaPlayer;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.media.AudioManager;

public class MainActivity extends Activity implements SurfaceHolder.Callback {
    /** Called when the activity is first created. */
    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button mPlayButton;
    private Button mPauseButton;
    private Button mStopButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPlayButton = (Button) findViewById(R.id.button_play);
        mPauseButton = (Button) findViewById(R.id.button_pause);
        mStopButton = (Button) findViewById(R.id.button_stop);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface_view);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        // mSurfaceHolder.setFixedSize(320, 220);
        // mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        mPlayButton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mMediaPlayer.start();
            }});
        mPauseButton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mMediaPlayer.pause();
            }});
        mStopButton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                mMediaPlayer.stop();
            }});
    }

    @Override
    public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder arg0) {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setDisplay(mSurfaceHolder);

        try {
            mMediaPlayer.setDataSource("/sdcard/test.h264");
            mMediaPlayer.prepare();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
        }
        mMediaPlayer.release();
    }
}
