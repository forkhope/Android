package com.example.myintent;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.ContactsContract.CommonDataKinds.Phone; // For Phone.CONTENT_TYPE
import android.database.Cursor;

import java.util.List;

public class MainActivity extends Activity {
    private final static String LOG_TAG = "MyIntent";

    public final static int PICK_CONTACT_RESULT = 1;
    public final static int SEND_TEXT_REQUEST = 2;

    public final static String EXTRA_TEXT = "com.example.myintent.text";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = getIntent();
        String intentType = intent.getType();
        Log.d(LOG_TAG, "onCreate: intent = " + intent);
        Log.d(LOG_TAG, "onCreate: intentType = " + intentType);
        // 实际调试发现,通过Launcher启动该Activity时, intentType 是空,所有要做判断,
        // 否则会触发空指针异常
        if (intentType != null) {
            if (intentType.indexOf("text/plain") != -1) {
                String text = intent.getStringExtra(EXTRA_TEXT);
                Log.d(LOG_TAG, "get ACTION_SEND, text/plan: " + text);

                Intent result = new Intent("com.example.myintent.RESULT_ACTION", Uri.parse("content://result_uri"));
                setResult(RESULT_OK, result);
                // 执行这个语句,以便在结束处理之后,能够自动返回调用它的Activity.
                finish();
            }
        }
    }

    public void setupDial(View view) {
        Uri number = Uri.parse("tel:10086");
        Intent callIntent = new Intent(Intent.ACTION_DIAL, number);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(callIntent, 0);
        boolean isIntentSafe = activities.size() > 0;
        Log.d(LOG_TAG, "activities = " + activities);
        Log.d(LOG_TAG, "size = " + activities.size() + ", isIntentSafe = " + isIntentSafe);

        if (isIntentSafe) {
            startActivity(callIntent);
        }
    }

    public void showMap(View view) {
        Uri location = Uri.parse("geo:0,0?q=1600+Amphitheatre+Parkway,+Mountain+View,+California");
        // Uri location = Uri.parse("geo:37.422219,-122.08364?z=14"); // z param is zoom level
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, location);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(mapIntent, 0);
        boolean isIntentSafe = activities.size() > 0;
        Log.d(LOG_TAG, "activities = " + activities);
        Log.d(LOG_TAG, "size = " + activities.size() + ", isIntentSafe = " + isIntentSafe);

        if (isIntentSafe) {
            startActivity(mapIntent);
        }
    }

    public void showWebpage(View view) {
        // 下面的网址要以"http://"开头,不能只写为"www.bing.com",否则下面的isIntentSafe会是false
        Uri webpage = Uri.parse("http://www.bing.com");
        Intent webIntent = new Intent(Intent.ACTION_VIEW, webpage);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> activities = packageManager.queryIntentActivities(webIntent, 0);
        boolean isIntentSafe = activities.size() > 0;
        Log.d(LOG_TAG, "activities = " + activities);
        Log.d(LOG_TAG, "size = " + activities.size() + ", isIntentSafe = " + isIntentSafe);

        String title = getResources().getString(R.string.chooser_title);
        Intent chooser = Intent.createChooser(webIntent, title);

        if (webIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        }
    }

    public void pickContact(View view) {
        Intent pickContactIntent = new Intent(Intent.ACTION_PICK, Uri.parse("content://contacts"));
        pickContactIntent.setType(Phone.CONTENT_TYPE);
        startActivityForResult(pickContactIntent, PICK_CONTACT_RESULT);
    }

    public void setupDisplay(View view) {
        Intent intent = new Intent(this, DisplayActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "requestCode = " + requestCode);
        if (requestCode == PICK_CONTACT_RESULT) {
            Log.d(LOG_TAG, "resultCode = " + resultCode + ", RESULT_OK = " + RESULT_OK);
            Log.d(LOG_TAG, "data = " + data);

            Uri contactUri = data.getData();
            String[] projection = { Phone.NUMBER };

            Cursor c = getContentResolver()
                    .query(contactUri, projection, null, null, null);
            c.moveToFirst();

            int column = c.getColumnIndex(Phone.NUMBER);
            String number = c.getString(column);
            Log.d(LOG_TAG, "number = " + number);
        }
    }
}
