/*
 *  LocationService.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.g53mdpcw2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.james.g53mdpcw2.database.LocationProviderContract;

public class LocationService extends Service {

    protected static boolean tracking = false;
    protected static boolean serviceRunning = false;
    protected double latBuf = 0;
    protected double longBuf = 0;
    private int sessionID;
    private boolean switchState = false;
    private LocationManager locationManager;
    private MyLocationListener locationListener;

    private final IBinder binder = new MyBinder();
    private final String CHANNEL_ID = "100";
    private final int NOTIFICATION_ID = 001;

    public LocationService() {

        Log.d("mdplifecycle", "LocationService()");
    }

    @Override
    public void onCreate() {

        super.onCreate();

        Log.d("mdplifecycle", "LocationService.onCreate()");

        startForeground(NOTIFICATION_ID, getNotification());
        serviceRunning = true;
    }

    @Override
    public IBinder onBind(Intent intent) {

        Log.d("mdplifecycle", "LocationService.onBind()");

        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {

        Log.d("mdplifecycle", "LocationService.onUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        Log.d("mdplifecycle", "LocationService.onDestroy()");

        // Restarts service if onDestroy is called while it is tracking in the background, i.e. when switchState is true
        if (switchState) {

            Intent broadcastIntent = new Intent(this, RestartServiceBroadcastReceiver.class);
            sendBroadcast(broadcastIntent);
        } else {

            serviceRunning = false;
            tracking = false;

            if (locationListener != null)
                locationManager.removeUpdates(locationListener);
        }
    }

    // Generate notification for pending intent to MainActivity
    private Notification getNotification() {

        Log.d("mdplifecycle", "LocationService.getNotification()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            CharSequence name = "channel name";
            String description = "channel description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Intent intent1 = new Intent(this, MainActivity.class);
            intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_man_running_1f3c3_200d_2642_fe0f)
                    .setContentIntent(pendingIntent)
                    .setContentText("Click here to return to the app.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setOngoing(true);

            return builder.build();
        }

        return null;
    }

    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {

            Log.d("mdplifecycle", "MyLocationListener.onLocationChanged() to " + location.getLatitude() + ", " + location.getLongitude());

            Log.d("mdpdebug", "" + latBuf + " - " + longBuf);

            float[] results = new float[3];

            ContentValues values = new ContentValues();
            values.put(LocationProviderContract.LATITUDE, location.getLatitude());
            values.put(LocationProviderContract.LONGITUDE, location.getLongitude());

            if ((latBuf == 0 && longBuf == 0) || !serviceRunning || !MainActivity.tracking) {

                values.put(LocationProviderContract.DISTANCE, 0);
            } else {

                Location.distanceBetween(latBuf, longBuf, location.getLatitude(), location.getLongitude(), results);

                values.put(LocationProviderContract.DISTANCE, results[0]);
                Log.d("mdpdebug", "MyLocationListener.onLocationChanged() to " + location.getLatitude() + ", " + location.getLongitude() + ", " + results[0]);
            }

            values.put(LocationProviderContract.SESSION, sessionID);

            getContentResolver().insert(LocationProviderContract.LOCATIONS_URI, values);

            latBuf = location.getLatitude();
            longBuf = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

            Log.d("mdplifecycle", "MyLocationListener.onStatusChanged() to " + provider + ", " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {

            Log.d("mdplifecycle", "MyLocationListener.onProviderEnabled(): " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {

            Log.d("mdplifecycle", "MyLocationListener.onProviderDisabled(): " + provider);
        }
    }

    public class MyBinder extends Binder {

        public void startTracking() {

            Log.d("mdplifecycle", "LocationService.MyBinder.startTracking()");

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationListener = new MyLocationListener();

            try {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        2,
                        5,
                        locationListener);
            } catch (SecurityException e) {

                Log.d("mdperror", e.toString());
            }

            latBuf = 0;
            longBuf = 0;

            tracking = true;
            sessionID++;
        }

        public void stopTracking() {

            Log.d("mdplifecycle", "LocationService.MyBinder.stopTracking()");

            if (locationListener != null)
                locationManager.removeUpdates(locationListener);

            latBuf = 0;
            longBuf = 0;

            tracking = false;
        }

        public void setSessionID(int i) {

            Log.d("mdplifecycle", "LocationService.MyBinder.setSessionID()");

            sessionID = i;
        }

        public int getSessionID() {

            Log.d("mdplifecycle", "LocationService.MyBinder.getSessionID()");

            return sessionID;
        }

        public void setSwitchState(boolean value) {

            switchState = value;
            Log.d("mdplifecycle", "LocationService.MyBinder.setSwitchState()");
        }

        public boolean getSwitchState() {

            Log.d("mdplifecycle", "LocationService.MyBinder.getSwitchState()");

            return switchState;
        }

        public boolean getTrackingState() {

            Log.d("mdplifecycle", "LocationService.MyBinder.getTrackingState()");

            return tracking;
        }
    }
}
