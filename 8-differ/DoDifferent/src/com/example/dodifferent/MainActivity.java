package com.example.dodifferent;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String hello = getResources().getString(R.string.hello_world);
        TextView textview = (TextView) findViewById(R.id.message);
        textview.setText(hello);
    }
}
