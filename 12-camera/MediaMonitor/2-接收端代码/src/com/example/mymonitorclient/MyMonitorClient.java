package com.example.mymonitorclient;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.widget.EditText;
import android.widget.TableLayout;
import android.util.Log;

public class MyMonitorClient extends Activity {
    private static final String LOG_TAG = "MyMonitorClient";

	private String mIpAddress = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "onCreate");

      	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("login dialog");

		TableLayout loginForm = (TableLayout) getLayoutInflater().inflate(R.layout.login, null);
		final EditText ipText = (EditText) loginForm.findViewById(R.id.ip_edit_text);
		builder.setView(loginForm);
		builder.setPositiveButton("login", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mIpAddress = ipText.getText().toString().trim();
                    Log.d(LOG_TAG, "mIpAddress = " + mIpAddress);

					Intent intent = new Intent(MyMonitorClient.this, MainActivity.class);
					intent.putExtra("mIpAddress", mIpAddress);
					startActivity(intent);
				}
			});
		builder.setNegativeButton("cancel",  new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					System.exit(1);
				}
			});
		builder.create().show();
	}
}
