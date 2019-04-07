/*
 *  MainActivity.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.runningman;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.james.runningman.database.LocationProviderContract;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    protected static boolean tracking = false;
    protected static boolean switchState = false;
    Handler h = new Handler();
    private LocationService.MyBinder myService = null;
    private GraphView graph;
    private boolean viewWeekly = false;
    private float distanceSession;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            Log.d("mdplifecycle", "MainActivity.ServiceConnection()");

            myService = (LocationService.MyBinder) service;
            switchState = myService.getSwitchState();

            Switch mySwitch = (Switch) findViewById(R.id.switch1);
            mySwitch.setChecked(switchState);

            tracking = myService.getTrackingState();

            Button myButton = (Button) findViewById(R.id.button);

            if (tracking) {

                myButton.setText("Stop Tracking");
            } else {

                myButton.setText("Start Tracking");
            }

            String[] projection = new String[]{
                    LocationProviderContract._ID,
                    LocationProviderContract.DATETIME,
                    LocationProviderContract.LATITUDE,
                    LocationProviderContract.LONGITUDE,
                    LocationProviderContract.DISTANCE,
                    LocationProviderContract.SESSION
            };

            Cursor cursor = getContentResolver().query(LocationProviderContract.LOCATIONS_URI, projection, null, null, null);

            cursor.moveToLast();

            try {

                myService.setSessionID(cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION)));
            } catch (CursorIndexOutOfBoundsException e) {

                Log.d("mdperror", e.toString());
            }

            Log.d("mdplifecycle", "MainActivity.ServiceConnection.onServiceConnected()");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            Log.d("mdplifecycle", "MainActivity.ServiceConnection.onServiceDisconnected()");

            myService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("mdplifecycle", "MainActivity.onCreate()");

        this.bindService(new Intent(MainActivity.this, LocationService.class), serviceConnection, Context.BIND_AUTO_CREATE);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

        if (!LocationService.serviceRunning) {

            this.startService(new Intent(MainActivity.this, LocationService.class));
            Log.d("mdpdebug", "service started");
        }

        initialiseGraph();
        query();

        getContentResolver().registerContentObserver(
                LocationProviderContract.LOCATIONS_URI,
                true,
                new ChangeObserver(h)
        );

        final Switch mySwitch = (Switch) findViewById(R.id.switch1);

        // Switch listener - necessary in order to allow user to select background tracking
        mySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (!tracking) {

                    mySwitch.setChecked(false);

                    Toast.makeText(MainActivity.this, "Cannot toggle while app is not tracking.", Toast.LENGTH_SHORT).show();
                } else {
                    if (!isChecked && tracking) {

                        mySwitch.setText("Only tracking while app is open");
                        switchState = false;
                    } else {

                        mySwitch.setText("     Tracking while app is closed");
                        switchState = true;
                    }
                }

                myService.setSwitchState(switchState);
            }
        });
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        Log.d("mdplifecycle", "MainActivity.onDestroy() ");

        if (serviceConnection != null) {

            unbindService(serviceConnection);
            serviceConnection = null;
        } else {

            Log.d("mdpdebug", "null serviceConnection");
        }

        if (switchState == false) {
            if (distanceSession == 0) {

                getContentResolver().delete(LocationProviderContract.LOCATIONS_URI, LocationProviderContract.SESSION+"=?", new String[] { String.valueOf(myService.getSessionID()) });

                myService.setSessionID(myService.getSessionID() - 1);
            }

            if (tracking) {

                myService.stopTracking();
            }

            stopService(new Intent(this, LocationService.class));
            Log.d("mdpdebug", "service stopped");
        }
    }

    // Prepares the GraphView for data viewing, based on whether the user wants to see weekly view or session view
    protected void initialiseGraph() {

        Log.d("mdplifecycle", "MainActivity.initialiseGraph()");

        graph = (GraphView) findViewById(R.id.graph);

        if (viewWeekly) {

            // Get current date and generate labels for week view
            String currDay = new SimpleDateFormat("yyyy-MM-dd").format(new Date()).substring(8, 10);

            int day = Integer.parseInt(currDay);
            String[] displayDays = new String[5];

            for (int i = 2; i < 7; i++) {

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DATE, -i);
                String date = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

                displayDays[i - 2] = date;
            }

            graph.getGridLabelRenderer().setNumHorizontalLabels(7);

            StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
            staticLabelsFormatter.setHorizontalLabels(new String[]{ displayDays[4], displayDays[3], displayDays[2], displayDays[1], displayDays[0], "Yesterday", "Today" });

            graph.getGridLabelRenderer().setHighlightZeroLines(false);
            graph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.CENTER);
            graph.getGridLabelRenderer().setLabelHorizontalHeight(210);
            graph.getGridLabelRenderer().setHorizontalLabelsAngle(90);
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

            Viewport vp = graph.getViewport();

            vp.setMinX(1);
            vp.setMaxX(7);
            vp.setXAxisBoundsManual(true);
        } else {

            graph.getGridLabelRenderer().setNumHorizontalLabels(5);

            StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
            staticLabelsFormatter.setHorizontalLabels(new String[]{"1", "2", "3", "4", "Latest"});

            graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

            graph.getGridLabelRenderer().setVerticalLabelsAlign(Paint.Align.CENTER);
            graph.getGridLabelRenderer().setLabelHorizontalHeight(40);
            graph.getGridLabelRenderer().setHorizontalLabelsAngle(0);
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

            Viewport vp = graph.getViewport();

            vp.setMinX(1);
            vp.setMaxX(5);
            vp.setXAxisBoundsManual(true);
        }
    }

    protected void query() {

        Log.d("mdplifecycle", "MainActivity.query()");

        String[] projection = new String[]{
                LocationProviderContract._ID,
                LocationProviderContract.DATETIME,
                LocationProviderContract.LATITUDE,
                LocationProviderContract.LONGITUDE,
                LocationProviderContract.DISTANCE,
                LocationProviderContract.SESSION
        };

        final Cursor cursor = getContentResolver().query(LocationProviderContract.LOCATIONS_URI, projection, null, null, null);

        cursor.moveToFirst();

        try {

            Log.d("fulldbtable", "Time: " + cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)) + ", Lat: " + cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LATITUDE)) + ", Long: " + cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LONGITUDE)) + ", Distance: " + cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.DISTANCE)) + ", Session: " + cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION)));
        } catch (CursorIndexOutOfBoundsException e) {}

        generateDistances(cursor);

        int sess = 0;
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        cursor.moveToLast();

        try {
            if (viewWeekly) {

                date = cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).substring(0, 10);
            }

            sess = cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION));
        } catch (CursorIndexOutOfBoundsException e) {

            Log.d("mdperror", e.toString());
        }

        final int startSess = sess;

        double[] distances;
        int count = 0;

        // Initialise distances[] for either 5 or 7 data points based on view setting
        if (viewWeekly) {

            distances = new double[7];
            count = 6;
        } else {

            distances = new double[5];
            count = 4;
        }

        int day = 0;
        int currDay = Integer.parseInt(new SimpleDateFormat("yyyy-MM-dd").format(new Date()).substring(8, 10));
        int datesInDb = 0;
        String dateBuffer = "dateBuffer";

        distanceSession = 0;

        do {
            try {
                do {
                    if (cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION)) == startSess) {

                        distanceSession += cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.DISTANCE));
                    }

                    if (!cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).startsWith(dateBuffer)) {

                        datesInDb++;
                        dateBuffer = cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).substring(0, 10);
                    }

                    distances[count] += cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.DISTANCE));
                }
                while (cursor.moveToPrevious() && ((cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION)) == sess && !viewWeekly) || (cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).startsWith(date) && viewWeekly)));
            } catch (CursorIndexOutOfBoundsException | NullPointerException e) {

                Log.d("mdperror", e.toString());
            }

//            Log.d("mdpdebug", ""+count+": "+distances[count]);

            count--;
            sess--;

            if (viewWeekly) {

//                Log.d("mdpdebug", date);

                Calendar cal = Calendar.getInstance();
                cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(date, new ParsePosition(0)));
                cal.add(Calendar.DATE, -1);
                date = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            }
        } while (count >= 0);

        TextView textView2 = (TextView) findViewById(R.id.textViewSession);

        if (distanceSession < 1000) {

            textView2.setText("Distance tracked this session: " + String.format("%.2f", distanceSession) + "m");
        } else {

            textView2.setText("Distance tracked this session: " + String.format("%.2f", distanceSession / 1000) + "km");
        }

        populateGraph(distances, datesInDb, startSess, cursor);
    }

    // Populate graph with distances
    protected void populateGraph(double[] distances, int datesInDb, final int startSess, final Cursor cursor) {

        graph.removeAllSeries();

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{});

        if(!viewWeekly) {
            if (startSess >= 5) {

                series = new LineGraphSeries<>(new DataPoint[]{
                        new DataPoint(1, distances[0]),
                        new DataPoint(2, distances[1]),
                        new DataPoint(3, distances[2]),
                        new DataPoint(4, distances[3]),
                        new DataPoint(5, distances[4])
                });
            } else {
                switch (startSess) {

                    case 4:
                        series = new LineGraphSeries<>(new DataPoint[]{
                                new DataPoint(2, distances[1]),
                                new DataPoint(3, distances[2]),
                                new DataPoint(4, distances[3]),
                                new DataPoint(5, distances[4])
                        });
                        break;
                    case 3:
                        series = new LineGraphSeries<>(new DataPoint[]{
                                new DataPoint(3, distances[2]),
                                new DataPoint(4, distances[3]),
                                new DataPoint(5, distances[4])
                        });
                        break;
                    case 2:
                        series = new LineGraphSeries<>(new DataPoint[]{
                                new DataPoint(4, distances[3]),
                                new DataPoint(5, distances[4])
                        });
                        break;
                    case 1:
                        series = new LineGraphSeries<>(new DataPoint[]{
                                new DataPoint(5, distances[4])
                        });
                        break;
                    case 0:
                        series = new LineGraphSeries<>(new DataPoint[]{});
                        break;
                }
            }
        } else {

            int offset = 0;

            try {

                cursor.moveToLast();

                offset = Integer.parseInt(new SimpleDateFormat("yyyy-MM-dd").format(new Date()).substring(8, 10)) - Integer.parseInt(cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).substring(8, 10));
            } catch (CursorIndexOutOfBoundsException e) {

                Log.d("mdperror", e.toString());
            }

//            Log.d("mdpdebug", ""+offset);

            if (datesInDb >= 7) {

                if (offset == 0) {

                    series = new LineGraphSeries<>(new DataPoint[]{
                            new DataPoint(1, distances[0]),
                            new DataPoint(2, distances[1]),
                            new DataPoint(3, distances[2]),
                            new DataPoint(4, distances[3]),
                            new DataPoint(5, distances[4]),
                            new DataPoint(6, distances[5]),
                            new DataPoint(7, distances[6])
                    });
                } else {

                    series = new LineGraphSeries<>(new DataPoint[]{
                            new DataPoint(2 - offset, distances[0]),
                            new DataPoint(3 - offset, distances[1]),
                            new DataPoint(4 - offset, distances[2]),
                            new DataPoint(5 - offset, distances[3]),
                            new DataPoint(6 - offset, distances[4]),
                            new DataPoint(7 - offset, distances[5])
                    });
                }
            } else {
                switch (datesInDb) {

                    case 6:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(2, distances[1]),
                                    new DataPoint(3, distances[2]),
                                    new DataPoint(4, distances[3]),
                                    new DataPoint(5, distances[4]),
                                    new DataPoint(6, distances[5]),
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(2 - offset, distances[1]),
                                    new DataPoint(3 - offset, distances[2]),
                                    new DataPoint(4 - offset, distances[3]),
                                    new DataPoint(5 - offset, distances[4]),
                                    new DataPoint(6 - offset, distances[5]),
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 5:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(3, distances[2]),
                                    new DataPoint(4, distances[3]),
                                    new DataPoint(5, distances[4]),
                                    new DataPoint(6, distances[5]),
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(3 - offset, distances[2]),
                                    new DataPoint(4 - offset, distances[3]),
                                    new DataPoint(5 - offset, distances[4]),
                                    new DataPoint(6 - offset, distances[5]),
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 4:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(4, distances[3]),
                                    new DataPoint(5, distances[4]),
                                    new DataPoint(6, distances[5]),
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(4 - offset, distances[3]),
                                    new DataPoint(5 - offset, distances[4]),
                                    new DataPoint(6 - offset, distances[5]),
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 3:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(5, distances[4]),
                                    new DataPoint(6, distances[5]),
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(5 - offset, distances[4]),
                                    new DataPoint(6 - offset, distances[5]),
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 2:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(6, distances[5]),
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(6 - offset, distances[5]),
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 1:
                        if (offset == 0) {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(7, distances[6])
                            });
                        } else {

                            series = new LineGraphSeries<>(new DataPoint[]{
                                    new DataPoint(7 - offset, distances[6])
                            });
                        }
                        break;
                    case 0:
                        series = new LineGraphSeries<>(new DataPoint[]{});
                        break;
                }
            }
        }

        series.setDrawDataPoints(true);

        // Data Point listener to enable user to view routes on certain session/day
        series.setOnDataPointTapListener(new OnDataPointTapListener() {

            @Override
            public void onTap (Series series,final DataPointInterface dataPoint){

                Log.d("mdplifecycle", "MainActivity.query.onTap()");

                String msg = "";

                if (!viewWeekly) {

                    msg = "View route for ";

                    switch (dataPoint.toString().substring(1, 2)) {

                        case "1":
                            msg = msg.concat("session 1?");
                            break;
                        case "2":
                            msg = msg.concat("session 2?");
                            break;
                        case "3":
                            msg = msg.concat("session 3?");
                            break;
                        case "4":
                            msg = msg.concat("session 4?");
                            break;
                        case "5":
                            msg = msg.concat("latest session?");
                            break;
                    }
                } else {

                    msg = "View route(s) for ";

                    switch (dataPoint.toString().substring(1, 2)) {

                        case "1":
                            msg = msg.concat("6 days ago?");
                            break;
                        case "2":
                            msg = msg.concat("5 days ago?");
                            break;
                        case "3":
                            msg = msg.concat("4 days ago?");
                            break;
                        case "4":
                            msg = msg.concat("3 days ago?");
                            break;
                        case "5":
                            msg = msg.concat("2 days ago?");
                            break;
                        case "6":
                            msg = msg.concat("yesterday?");
                            break;
                        case "7":
                            msg = msg.concat("today?");
                            break;
                    }
                }

                if (((dataPoint.toString().substring(1, 2).equals("5") && !viewWeekly) || (dataPoint.toString().substring(1, 2).equals("7"))) && tracking) {

                    Toast.makeText(MainActivity.this, "Cannot view whilst tracking.", Toast.LENGTH_SHORT).show();
                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(msg)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    Log.d("mdpdebug", "yes clicked");

                                    cursor.moveToLast();

                                    int pos = 0;

                                    if (viewWeekly) {
                                        switch (dataPoint.toString().substring(1, 2)) {

                                            case "1":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 6);
                                                break;
                                            case "2":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 5);
                                                break;
                                            case "3":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 4);
                                                break;
                                            case "4":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 3);
                                                break;
                                            case "5":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 2);
                                                break;
                                            case "6":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 1);
                                                break;
                                            case "7":
                                                pos = generateDateCoords(cursor, new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 0);
                                                break;
                                            default:
                                                break;
                                        }
                                    } else {
                                        switch (dataPoint.toString().substring(1, 2)) {

                                            case "1":
                                                pos = generateSessCoords(cursor, startSess, 4);
                                                break;
                                            case "2":
                                                pos = generateSessCoords(cursor, startSess, 3);
                                                break;
                                            case "3":
                                                pos = generateSessCoords(cursor, startSess, 2);
                                                break;
                                            case "4":
                                                pos = generateSessCoords(cursor, startSess, 1);
                                                break;
                                            case "5":
                                                pos = generateSessCoords(cursor, startSess, 0);
                                                break;
                                            default:
                                                break;
                                        }
                                    }

                                    cursor.moveToPosition(pos);

                                    float[] coords = new float[4096];
                                    int count = 1;

                                    if (viewWeekly) {

                                        coords[0] = 0;
                                        String date = cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).substring(0, 10);

                                        do {

                                            coords[count++] = cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LATITUDE));
                                            coords[count++] = cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LONGITUDE));
                                            coords[count++] = cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION));
                                        }
                                        while (cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).startsWith(date) && cursor.moveToPrevious());
                                    } else {

                                        coords[0] = 1;
                                        int session = cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION));

                                        do {

                                            coords[count++] = cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LATITUDE));
                                            coords[count++] = cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.LONGITUDE));
                                        }
                                        while (cursor.getInt(cursor.getColumnIndex(LocationProviderContract.SESSION)) == session && cursor.moveToPrevious());
                                    }

                                    Bundle bundle = new Bundle();
                                    bundle.putFloatArray("coords", coords);

                                    Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                                    intent.putExtras(bundle);

                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    Log.d("mdpdebug", "no clicked");
                                }
                            }).create().show();
                }
            }
        });

        graph.addSeries(series);
    }

    // Moves through database to generate daily and monthly distances based on db's date
    protected void generateDistances(Cursor cursor) {

        float distanceToday = 0;
        float distanceMonth = 0;

        cursor.moveToFirst();

        do {
            try {

                if (cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).startsWith("" + new SimpleDateFormat("yyyy-MM").format(new Date()))) {

                    distanceMonth += cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.DISTANCE));

                    if (cursor.getString(cursor.getColumnIndex(LocationProviderContract.DATETIME)).startsWith("" + new SimpleDateFormat("yyyy-MM-dd").format(new Date()))) {

                        distanceToday += cursor.getFloat(cursor.getColumnIndex(LocationProviderContract.DISTANCE));
                    }
                }
            } catch (CursorIndexOutOfBoundsException e) {

                Log.d("mdperror", e.toString());
            }
        } while(cursor.moveToNext());

        TextView textView = (TextView) findViewById(R.id.textViewToday);
        TextView textView1 = (TextView) findViewById(R.id.textViewMonth);

        if (distanceToday < 1000) {

            textView.setText("Distance tracked today: " + String.format("%.2f", distanceToday) + "m");
        } else {

            textView.setText("Distance tracked today: " + String.format("%.2f", distanceToday/1000) + "km");
        }

        if (distanceMonth < 1000) {

            textView1.setText("Distance tracked this month: " + String.format("%.2f", distanceMonth) + "m");
        } else {

            textView1.setText("Distance tracked this month: " + String.format("%.2f", distanceMonth/1000) + "km");
        }
    }

    // Return cursor position for beginning of selected session (start parameter)
    protected int generateSessCoords(Cursor c, int start, int loops) {

        Log.d("mdplifecycle", "MainActivity.generateSessCoords()");

        for (int i = 0; i < loops; i++) {
            while (c.getInt(c.getColumnIndex(LocationProviderContract.SESSION)) == start) {

                c.moveToPrevious();
            }

            start--;
        }

        return c.getPosition();
    }

    // Return cursor position for beginning of selected day (start parameter)
    protected int generateDateCoords(Cursor c, String start, int loops) {

        Log.d("mdplifecycle", "MainActivity.generateDateCoords()");

        for (int i = 0; i < loops; i++) {
            while (c.getString(c.getColumnIndex(LocationProviderContract.DATETIME)).startsWith(start)) {

                c.moveToPrevious();
            }

            int day = Integer.parseInt(start.substring(8,10)) - 1;

            if (day < 10)
                start = start.substring(0,8) + "0" + day;
            else
                start = start.substring(0,8) + day;
        }

        return c.getPosition();
    }

    public void onViewButtonClick(View v) {

        Log.d("mdplifecycle", "MainActivity.onViewButtonClick()");

        Button button = (Button) findViewById(R.id.button1);

        if (viewWeekly) {

            viewWeekly = false;

            button.setText("View weekly");
        } else {

            viewWeekly = true;

            button.setText("View sessions");
        }

        initialiseGraph();
        query();
    }

    public void onTrackingButtonClick(View v) {

        Log.d("mdplifecycle", "MainActivity.onTrackingButtonClick()");

        Button myButton = (Button) findViewById(R.id.button);

        if (!tracking) {

            myService.startTracking();
            tracking = true;

            myButton.setText("Stop Tracking");
        } else {

            myService.stopTracking();

            myButton.setText("Start Tracking");

            Switch mySwitch = (Switch) findViewById(R.id.switch1);
            mySwitch.setChecked(false);

            tracking = false;

            if (distanceSession == 0) {

                getContentResolver().delete(LocationProviderContract.LOCATIONS_URI, LocationProviderContract.SESSION+"=?", new String[] { String.valueOf(myService.getSessionID()) });

                myService.setSessionID(myService.getSessionID() - 1);
            }
        }
    }

    public void onResetButtonClick(View v) {

        if (tracking) {

            Toast.makeText(MainActivity.this, "Cannot reset data whilst tracking.", Toast.LENGTH_SHORT).show();
        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Are you sure you want to reset all data?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Log.d("mdpdebug", "yes clicked");

                            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                            builder1.setMessage("This is an irreversible process. Are you sure you want to proceed?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            Log.d("mdpdebug", "yes clicked");

                                            getContentResolver().delete(LocationProviderContract.LOCATIONS_URI, null, null);

                                            myService.setSessionID(0);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {

                                            Log.d("mdpdebug", "no clicked");
                                        }
                                    }).create().show();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            Log.d("mdpdebug", "no clicked");
                        }
                    }).create().show();
        }
    }

    class ChangeObserver extends ContentObserver {

        public ChangeObserver(Handler h) {

            super(h);

            Log.d("mdplifecycle", "MainActivity.ChangeObserver()");
        }

        @Override
        public void onChange(boolean selfChange) {

            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {

            Log.d("mdplifecycle", "MainActivity.ChangeObserver.onChange()");

            query();
        }
    }
}
