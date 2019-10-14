package com.example.myactivity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;        // 引入 Build 包
import android.util.Log;        // 引入 Log 包
import android.app.ActionBar;   // 引入 ActionBar 包
import android.view.View;
import android.content.Intent;

public class MainActivity extends Activity {
    private final static String LOG_TAG = "MyActivity";
 
    private final static String STATE_SCORE = "playerScore";
    private int mCurrentScore;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(LOG_TAG, "1: >>>>>>>>>> onCreate");
        setContentView(R.layout.main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ActionBar actionbar = getActionBar();
            actionbar.setHomeButtonEnabled(false);
        }

        Log.d(LOG_TAG, "onCreate: savedInstanceState: " + savedInstanceState);
        if (savedInstanceState != null) {
            mCurrentScore = savedInstanceState.getInt(STATE_SCORE);
            Log.d(LOG_TAG, "onCreate: mCurrentScore: " + mCurrentScore);
        } else {
            mCurrentScore = 0;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(LOG_TAG, "7: >>>>>>>>> onRestart");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "2: >>>>>>>>> onStart");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int score = savedInstanceState.getInt(STATE_SCORE);
        Log.d(LOG_TAG, "onRestoreInstanceState: score: " + score);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "3: >>>>>>>>> onResume");
        mCurrentScore = 88;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onSaveInstanceState: mCurrentScore: " + mCurrentScore);
        savedInstanceState.putInt(STATE_SCORE, mCurrentScore);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "4: <<<<<<<<< onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(LOG_TAG, "5: <<<<<<<<< onStop");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "6: <<<<<<<<< onDestroy");
    }

    public void startDisplayActivity(View view) {
        Intent intent = new Intent(this, DisplayActivity.class);
        startActivity(intent);
    }
}
