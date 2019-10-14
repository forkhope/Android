package com.example.myintent;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;
import android.view.View;

import java.util.List;
import org.apache.http.protocol.HTTP;

public class DisplayActivity extends Activity {
    private final static String LOG_TAG = "MyIntent";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display);
    }

    public void sendMessage(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        // 这里一定要设置类型,否则查询到的intent数目会是0
        intent.setType(HTTP.PLAIN_TEXT_TYPE);
        intent.putExtra(MainActivity.EXTRA_TEXT, "This is a text");

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, 0);
        boolean isIntentSafe = activities.size() > 0;
        Log.d(LOG_TAG, "DisplayActivity: activities = " + activities);
        Log.d(LOG_TAG, "DisplayActivity: size = " + activities.size() + ", isIntentSafe = " + isIntentSafe);

        if (isIntentSafe) {
            startActivityForResult(intent, MainActivity.SEND_TEXT_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "DisplayActivity: requestCode = " + requestCode);
        Log.d(LOG_TAG, "DisplayActivity: resultCode = " + resultCode);
        Log.d(LOG_TAG, "DisplayActivity: data = " + data);
    }
}
