package com.example.mysavedata;

import android.content.Context;
import android.util.Log;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.mysavedata.FeedReaderContract.FeedEntry;

public class FeedReaderDbHelper extends SQLiteOpenHelper {
    public final static String LOG_TAG = "MySaveData";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES = 
        "CREATE TABLE " + FeedEntry.TABLE_NAME + "(" +
        FeedEntry._ID + " INTERGER PRIMARY KEY," +
        FeedEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + COMMA_SEP +
        FeedEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
        FeedEntry.COLUMN_NAME_CONTENT + TEXT_TYPE +
        ")";

    private static final String SQL_DELETE_ENTRIES =
        "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(LOG_TAG, "FeedReaderDbHelper: construction");
    }

    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, "FeedReaderDbHelper: onCreate: db = " + db);
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onDelete(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
