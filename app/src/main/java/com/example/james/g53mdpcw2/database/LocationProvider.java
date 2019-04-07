/*
 *  LocationProvider.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.g53mdpcw2.database;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class LocationProvider extends ContentProvider {

    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(LocationProviderContract.AUTHORITY, LocationProviderContract.TABLE_NAME, 1);
        uriMatcher.addURI(LocationProviderContract.AUTHORITY, LocationProviderContract.TABLE_NAME + "/#", 2);
    }

    private DBHelper dbHelper = null;

    @Override
    public boolean onCreate() {

        Log.d("mdplifecycle", "LocationProvider.onCreate()");

        this.dbHelper = new DBHelper(this.getContext(), "mydb", null, 1);
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Log.d("mdplifecycle", "LocationProvider.query()");

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)) {

            case 2:
                selection = "_ID = " + uri.getLastPathSegment();
            case 1:
                return db.query(LocationProviderContract.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            default:
                return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        Log.d("mdplifecycle", "LocationProvider.insert()");

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        long id = db.insert(LocationProviderContract.TABLE_NAME, null, values);
        db.close();

        Uri nu = ContentUris.withAppendedId(uri, id);
        getContext().getContentResolver().notifyChange(nu, null);

        return nu;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        Log.d("mdplifecycle", "LocationProvider.delete()");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsDeleted = 0;

        switch (uriMatcher.match(uri)) {

            case 1:
                rowsDeleted = db.delete(LocationProviderContract.TABLE_NAME, selection, selectionArgs);
                break;
            case 2:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {

                    rowsDeleted = db.delete(LocationProviderContract.TABLE_NAME, LocationProviderContract._ID + "=" + id, null);
                } else {

                    rowsDeleted = db.delete(LocationProviderContract.TABLE_NAME, LocationProviderContract._ID + "=" + id + " and " + selection, selectionArgs);
                }

                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        Log.d("mdplifecycle", "LocationProvider.update()");

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rowsUpdated = 0;

        switch (uriMatcher.match(uri)) {

            case 1:
                rowsUpdated = db.update(LocationProviderContract.TABLE_NAME, values, selection, selectionArgs);
                break;
            case 2:
                String id = uri.getLastPathSegment();

                if (TextUtils.isEmpty(selection)) {

                    rowsUpdated = db.update(LocationProviderContract.TABLE_NAME, values, LocationProviderContract._ID + "=" + id, null);
                } else {

                    rowsUpdated = db.update(LocationProviderContract.TABLE_NAME, values, LocationProviderContract._ID + "=" + id + " and " + selection, selectionArgs);
                }

                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    @Override
    public String getType(Uri uri) {

        Log.d("mdplifecycle", "LocationProvider.getType()");

        String contentType;

        if (uri.getLastPathSegment() == null) {

            contentType = LocationProviderContract.CONTENT_TYPE_MULTIPLE;
        } else {

            contentType = LocationProviderContract.CONTENT_TYPE_SINGLE;
        }

        return contentType;
    }
}
