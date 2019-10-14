package com.example.myactionbar;

import android.app.Activity;
import android.os.Bundle;

public class DisplaySearchActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_activity);

        getActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
