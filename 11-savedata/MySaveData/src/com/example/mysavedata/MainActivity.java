package com.example.mysavedata;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.SharedPreferences;
import android.content.Context;
import android.util.Log;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

import com.example.mysavedata.FeedReaderContract.FeedEntry;

public class MainActivity extends Activity {
    public final static String LOG_TAG = "MySaveData";

    // private int mId = 0;

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
                }
        return false;
    }

    public void printSpace() {
        try {
            String albumName = "testdir";
            File file = new File(getExternalFilesDir(
                        Environment.DIRECTORY_PICTURES), albumName);
            if (!file.mkdirs()) {
                Log.e(LOG_TAG, "Directory not created");
            }
            Log.e(LOG_TAG, "getAlbumStorageDir: file = " + file);

            long freeSpace = file.getFreeSpace();
            long totalSpace = file.getTotalSpace();
            Log.d(LOG_TAG, "printSpace: freeSpace = " + freeSpace);
            Log.d(LOG_TAG, "printSpace: totalSpace = " + totalSpace);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getAlbumStorageDir(String albumName) {
        // Get the directory for the user's public pictures directory. 
        File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
        Log.e(LOG_TAG, "1: file = " + file);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        Log.e(LOG_TAG, "getAlbumStorageDir: file = " + file);

        Log.d(LOG_TAG, "getFreeSpace() = " + file.getFreeSpace());
        Log.d(LOG_TAG, "getTotalSpace() = " + file.getTotalSpace());

        return file;
    }

    public File getPrivateAlbumStorageDir(Context context, String albumName) {
        // Get the directory for the app's private pictures directory. 
        File file = new File(context.getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        Log.e(LOG_TAG, "getPrivateAlbumStorageDir: file = " + file);

        Log.d(LOG_TAG, "getPrivateAlbumStorageDir: getFreeSpace() = " + file.getFreeSpace());
        Log.d(LOG_TAG, "getPrivateAlbumStorageDir: getTotalSpace() = " + file.getTotalSpace());

        return file;
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String FILENAME = "hello_file";
        String string = "hello world!";

        try {
            Log.d(LOG_TAG, "getFilesDir() = " + getFilesDir());
            Log.d(LOG_TAG, "getCacheDir() = " + getCacheDir());

            Log.d(LOG_TAG, "isExternalStorageWritable() = " + isExternalStorageWritable());
            Log.d(LOG_TAG, "isExternalStorageReadable() = " + isExternalStorageReadable());

            Log.d(LOG_TAG, "getExternalFilesDir(Environment.DIRECTORY_PICTURES) = "
                    + getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            Log.d(LOG_TAG, "getExternalFilesDir(null) = "
                    + getExternalFilesDir(null));
            Log.d(LOG_TAG, "Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) = "
                    + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            getAlbumStorageDir("myalbum");
            getPrivateAlbumStorageDir(this, "myprivatealbum");

            printSpace();

            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(string.getBytes());
            fos.close();

            byte[] buffer = new byte[1024];
            int size;
            FileInputStream fis = openFileInput(FILENAME);
            size = fis.read(buffer, 0, 1024);
            Log.d(LOG_TAG, "read size = " + size);
            // String str = new String(buffer);
            String str = new String(buffer, 0, size);
            Log.d(LOG_TAG, "str.length() = " + str.length() + ", str = " + str);

            File file = File.createTempFile("mycachefile", null, getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences sharedPref = getSharedPreferences(
                "com.example.mysavedata.PREFERENCE_KEY", Context.MODE_PRIVATE);
        long highScore = sharedPref.getInt("saved_high_score", 0);
        Log.d(LOG_TAG, "onResume: saved_high_score = " + highScore);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        long lowScore = sharedPref.getInt("saved_low_score", 0);
        Log.d(LOG_TAG, "onResume: saved_low_score = " + lowScore);

        // mId = sharedPref.getInt("sql_id", 0);;
        // Log.d(LOG_TAG, "onResume: sql_id = " + mId);

        FeedReaderDbHelper mDbHelper = new FeedReaderDbHelper(this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        String title = "Hello";
        String content = "World";

        int mId = 0;
        ContentValues values = new ContentValues();
        values.put(FeedEntry._ID, mId);
        values.put(FeedEntry.COLUMN_NAME_ENTRY_ID, mId);
        values.put(FeedEntry.COLUMN_NAME_TITLE, title);
        values.put(FeedEntry.COLUMN_NAME_CONTENT, content);

        long newRowId;
        newRowId = db.insert(
                FeedEntry.TABLE_NAME,
                FeedEntry.COLUMN_NAME_NULLABLE,
                values);

        String[] projection = {
            FeedEntry._ID,
            FeedEntry.COLUMN_NAME_TITLE,
        };

        String sortOrder =
            FeedEntry.COLUMN_NAME_ENTRY_ID + " DESC";

        String selection = FeedEntry.COLUMN_NAME_TITLE + " = ?";
        String[] selectionArgs = { "Hello" };

        Cursor cursor = db.query(
                FeedEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
                );

        if (cursor.moveToFirst()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(FeedEntry._ID));
            Log.d(LOG_TAG, "itemId = " + itemId);
        }

        values = new ContentValues();
        values.put(FeedEntry.COLUMN_NAME_TITLE, "HELP");

        selection = FeedEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs1 = { "0" };
        int count = db.update(
                FeedEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs1);
        Log.d(LOG_TAG, "update: count = " + count);

        selection = FeedEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs2 = { "0" };

        cursor = db.query(
                FeedEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs2,
                null,
                null,
                sortOrder
                );

        if (cursor.moveToFirst()) {
            long itemId = cursor.getLong(cursor.getColumnIndexOrThrow(FeedEntry._ID));
            Log.d(LOG_TAG, "itemId = " + itemId);
            String new_title = cursor.getString(cursor.getColumnIndexOrThrow(FeedEntry.COLUMN_NAME_TITLE));
            Log.d(LOG_TAG, "new_title = " + new_title);
        }


        selection = FeedEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs3 = { "0" };
        db.delete(FeedEntry.TABLE_NAME, selection, selectionArgs3);
    }

    @Override
    public void onStop() {
        super.onStop();

        SharedPreferences sharedPref = getSharedPreferences(
                "com.example.mysavedata.PREFERENCE_KEY", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("saved_high_score", 88);
        editor.commit();
        Log.d(LOG_TAG, "onStop: saved_high_score = 88");

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        editor.putInt("saved_low_score", 11);
        editor.putString("saved_name", "John");

        // mId += 1;
        // Log.d(LOG_TAG, "mId = " + mId);
        // editor.putInt("sql_id", mId);
        editor.commit();
        Log.d(LOG_TAG, "onStop: saved_low_score = 1");
    }
}
