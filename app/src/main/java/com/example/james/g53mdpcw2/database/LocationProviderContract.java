/*
 *  LocationProviderContract.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.g53mdpcw2.database;

import android.net.Uri;

public class LocationProviderContract {

    public static final String AUTHORITY = "com.example.james.g53mdpcw2.database.LocationProvider";
    public static final String TABLE_NAME = "locations";

    public static final Uri LOCATIONS_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

    public static final String _ID = "_id";
    public static final String DATETIME = "time";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String DISTANCE = "distance";
    public static final String SESSION = "session";

    public static final String CONTENT_TYPE_SINGLE = "vnd.android.cursor.item/LocationProvider.data.text";
    public static final String CONTENT_TYPE_MULTIPLE = "vnd.android.cursor.dir/LocationProvider.data.text";
}
