package com.example.davea.mapping_gps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public GoogleMap gMap;

    //Location:
    public Location currentLocation;
    LocationManager locationManager;
    LocationListener locationListener;

    //Constants:
    final int UPDATE_INTERVAL = 2000;   //when on, update location data every UPDATE_INTERVAL milliseconds

    //UI:
    Button start, reset;
    TextView TV;

    //variables:
    public boolean on = false;
    double currentLongitude;
    double currentLatitude;
    LatLng currentPosition[] = new LatLng[10000];
    int numPins = 0;
    String name = "X";
/*
    //Time:
    //create calendar to convert epoch time to readable time
    Calendar cal = Calendar.getInstance();
    //create simple date format to show just 12hr time
    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm:ss aa");
*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        setup();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on = !on;
                if(on) {
                    TV.setText("running");
                    locationDetails();
                }
                else TV.setText("paused");
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(on){
                    on = false;
                    TV.setText("paused - press again to reset");
                }
                else {
                    TV.setText("reset");
                    gMap.clear();
                }
            }
        });

    }


    public void setup(){
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        start = findViewById(R.id.btnStartStop);
        reset = findViewById(R.id.btnReset);
        TV = findViewById(R.id.TV);
        TV.setText("Press START to begin");
    }

    public void getPermissions(){
        //if at least Marshmallow, need to ask user's permission to get GPS data
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //if permission is not yet granted, ask for it
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    Toast.makeText(this, "Need GPS permissions for app to function", Toast.LENGTH_LONG);
                }
                //once permission is granted, set up location listener
                //updating every UPDATE_INTERVAL milliseconds, regardless of distance change
                else locationManager.requestLocationUpdates("gps", UPDATE_INTERVAL, 0, locationListener);
            }
            else locationManager.requestLocationUpdates("gps", UPDATE_INTERVAL, 0, locationListener);
        }
        else {
            assert locationManager != null;
            locationManager.requestLocationUpdates("gps", UPDATE_INTERVAL, 0, locationListener);
        }
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
        gMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng wichita = new LatLng(37.6913, 262.6503);
        gMap.moveCamera(CameraUpdateFactory.newLatLng(wichita));

    }

    public void locationDetails() {
        if(on) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //when location changes, display accuracy of that reading
                    //currentLocation = location;
                    if (on) {
                        currentLongitude = location.getLongitude();
                        currentLatitude = location.getLatitude();
                        currentPosition[numPins] = new LatLng(currentLatitude, currentLongitude);
                        setName(location);
                        gMap.addMarker(new MarkerOptions().position(currentPosition[numPins]).title(name));
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition[numPins]));
                        numPins++;
                        //drawMarker(location);
                    }
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                    //not used right now
                }

                @Override
                public void onProviderEnabled(String provider) {
                    //not used right now
                }

                @Override
                public void onProviderDisabled(String provider) {
                    //not used right now
                }
            };

            getPermissions();
        }

    }

    /*
    From https://developer.android.com/reference/android/location/Location#getAccuracy()
    "We define horizontal accuracy as the radius of 68% confidence.
    In other words, if you draw a circle centered at this location's latitude and longitude,
    and with a radius equal to the accuracy, then there is a 68% probability that the true
    location is inside the circle. This accuracy estimation is only concerned with horizontal
    accuracy, and does not indicate the accuracy of bearing, velocity or altitude if those are
    included in this Location. If this location does not have a horizontal accuracy, then 0.0
    is returned. All locations generated by the LocationManager include horizontal accuracy."
     */
    public void setName(Location currentLocation){
        //convert epoch time to calendar data
        //cal.setTimeInMillis(currentLocation.getTime());
        //print accuracy value on screen along with coordinates and time
        name = ("" + currentLocation.getAccuracy());
    }

/*
    private void drawMarker(Location location) {
        if (gMap != null) {
            gMap.clear();
            LatLng gps = new LatLng(location.getLatitude(), location.getLongitude());
            gMap.addMarker(new MarkerOptions()
                    .position(gps)
                    .title("Current Position"));
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gps, 12));
        }

    }*/
}
