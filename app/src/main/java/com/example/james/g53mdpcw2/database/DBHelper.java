/*
 *  DBHelper.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.g53mdpcw2.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {

        super(context, name, factory, version);

        Log.d("mdplifecycle", "DBHelper.DBHelper()");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        Log.d("mdplifecycle", "DBHelper.onCreate()");

        db.execSQL(
                "CREATE TABLE " +
                LocationProviderContract.TABLE_NAME + " (" +
                LocationProviderContract._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                LocationProviderContract.DATETIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                LocationProviderContract.LATITUDE + " FLOAT NOT NULL, " +
                LocationProviderContract.LONGITUDE + " FLOAT NOT NULL, " +
                LocationProviderContract.DISTANCE + " FLOAT NOT NULL, " +
                LocationProviderContract.SESSION + " INTEGER NOT NULL" +
                ");"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.d("mdplifecycle", "DBHelper.onUpgrade()");

        db.execSQL("DROP TABLE IF EXISTS " + LocationProviderContract.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.d("mdplifecycle", "DBHelper.onDowngrade()");

        db.execSQL("DROP TABLE IF EXISTS " + LocationProviderContract.TABLE_NAME);
        onCreate(db);
    }
}
