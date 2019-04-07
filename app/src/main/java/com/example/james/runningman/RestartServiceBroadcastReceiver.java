/*
 *  RestartServiceBroadcastReceiver.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.runningman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Broadcast receiver that is set to restart the LocationService if it is destroyed after a user has enabled background tracking
public class RestartServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        context.startService(new Intent(context, LocationService.class));

        Log.d("mdplifecycle", "RestartServiceBroadcastReceiver.onReceive()");
    }
}
