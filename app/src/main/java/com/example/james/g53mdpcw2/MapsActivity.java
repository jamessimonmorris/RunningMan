/*
 *  MapActivity.java
 *
 *  v1.0.0
 *
 *  2019-01-08
 *
 *  University of Nottingham (C)
 */

package com.example.james.g53mdpcw2;

import android.graphics.Bitmap;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;

import static android.graphics.Color.parseColor;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private float[] coords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Log.d("mdplifecycle", "MapsActivity.onCreate()");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        coords = getIntent().getFloatArrayExtra("coords");
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        Log.d("mdplifecycle", "MapsActivity.onMapReady()");

        // coords[0] set to 0 if a date datapoint is clicked, and 1 if a session datapoint is clicked
        if (coords[0] == 0) {

            populateDateMap();
        } else {

            populateSessMap();
        }
    }

    // Populates map for multiple routes if a date was selected
    protected void populateDateMap() {

        Log.d("mdplifecycle", "MapsActivity.populateDateMap()");

        int i = 1;

        IconGenerator iconGenerator = new IconGenerator(this);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        int session = (int) coords[3];

        while (!(coords[i+3] == 0 && coords[i+4] == 0)) {

            Log.d("mapdebug", "" + i);

            LatLng end = new LatLng(coords[i], coords[i+1]);
            mMap.addCircle(new CircleOptions().center(end).radius(2).fillColor(parseColor("#0069C0")).strokeColor(parseColor("#0069C0")));

            Bitmap endIconBitmap = iconGenerator.makeIcon("End");
            mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(endIconBitmap))
                    .position(end)
            );

            PolylineOptions poly = new PolylineOptions();

            while ((int) coords[i+2] == session) {

                poly.add(new LatLng(coords[i++], coords[i--]));
                builder.include(new LatLng(coords[i++], coords[i++]));
                i++;
            }

            i -= 3;

            mMap.addPolyline(poly).setColor(parseColor("#2196f3"));

            LatLng start = new LatLng(coords[i++], coords[i++]);
            mMap.addCircle(new CircleOptions().center(start).radius(2).fillColor(parseColor("#0069C0")).strokeColor(parseColor("#0069C0")));

            IconGenerator iconGenerator1 = new IconGenerator(this);
            Bitmap startIconBitmap = iconGenerator1.makeIcon("Start");
            mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromBitmap(startIconBitmap))
                    .position(start)
            );

            session--;
            i++;
        }

        LatLngBounds bounds = builder.build();

        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 300);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                mMap.animateCamera(cu);
            }
        });
    }

    // Populates map for single route if a session was selected
    protected void populateSessMap() {

        Log.d("mdplifecycle", "MapsActivity.populateSessMap()");

        int i = 1;
        PolylineOptions poly = new PolylineOptions();

        IconGenerator iconGenerator = new IconGenerator(this);

        LatLng end = new LatLng(coords[1], coords[2]);
        mMap.addCircle(new CircleOptions().center(end).radius(2).fillColor(parseColor("#0069C0")).strokeColor(parseColor("#0069C0")));

        Bitmap endIconBitmap = iconGenerator.makeIcon("End");
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(endIconBitmap))
                .position(end)
                .title("End")
        );

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        while (!(coords[i+2] == 0 && coords[i+3] == 0)) {

            poly.add(new LatLng(coords[i++], coords[i--]));
            builder.include(new LatLng(coords[i++], coords[i++]));
        }

        mMap.addPolyline(poly).setColor(parseColor("#2196f3"));

        i -= 2;

        LatLng start = new LatLng(coords[i], coords[i+1]);
        mMap.addCircle(new CircleOptions().center(start).radius(2).fillColor(parseColor("#0069C0")).strokeColor(parseColor("#0069C0")));

        IconGenerator iconGenerator1 = new IconGenerator(this);
        Bitmap startIconBitmap = iconGenerator1.makeIcon("Start");
        mMap.addMarker(new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromBitmap(startIconBitmap))
                .position(start)
                .title("Start")
        );

        LatLngBounds bounds = builder.build();

        final CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 300);

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {

                mMap.animateCamera(cu);
            }
        });
    }
}
