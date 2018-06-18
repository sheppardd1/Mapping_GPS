package com.example.davea.mapping_gps;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public GoogleMap gMap;

    //Location:
    public Location currentLocation;
    LocationManager locationManager;
    LocationListener locationListener;

    //Constants:
    final int UPDATE_INTERVAL = 3000;   //when on, update location data every UPDATE_INTERVAL milliseconds
    final int ARRAY_SIZE_MAX = 10000;
    static final String filename = "GPS_data.txt";

    //Files:
    File dataFile;

    //UI:
    Button start, reset, viewData;
    TextView TV;

    //variables:
    public boolean on = false;
    double currentLongitude;
    double currentLatitude;
    LatLng currentPosition;
    public int numPins = 0;
    String name = "X";
    Float dataArray[] = new Float[ARRAY_SIZE_MAX];
    static String fileContents; //is static so that it can be accessed in other activity
    boolean wasReset = true;
    String time;
    String timeArray[] = new String[ARRAY_SIZE_MAX];
    float average = -999;
    boolean setStartTime = false;
    boolean zoomed = false;

    //Time:
    //create calendar to convert epoch time to readable time
    Calendar cal;
    //create simple date format to show just 12hr time
    SimpleDateFormat dateFormatTime;
    SimpleDateFormat dateFormatDayAndTime;


//TODO: check for read and write permissions - needed for newer android versions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (UPDATE_INTERVAL < 1000) {
            Toast.makeText(this, "WARNING: UPDATE-INTERVAL IS TOO LOW\nIT MUST BE >= 1000 IN ORDER FOR THIS APP TO FUNCTION AS INTENDED", Toast.LENGTH_LONG).show();
        }

        setup();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on = !on;
                if (on) {
                    if(wasReset) gMap.clear();
                    TV.setText("RUNNING");
                    locationDetails();
                    if (wasReset) {
                        wasReset = false;
                    }
                } else TV.setText("PAUSED");
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (on) {
                    on = false;
                    TV.setText("PAUSED - press again to write");
                } else if (numPins != 0) {      //if data has been collected
                    long sum = (long) 0.0;
                    for (int i = 0; i < numPins; i++) {
                        sum += dataArray[i];
                    }
                    average = sum / ((float) numPins);
                    TV.setText("");
                    reset();
                } else TV.setText("Press START to begin"); //if numPins == 0, then it does not need to be reset because it's already empty
            }
        });

        viewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!on) startActivity(new Intent(getApplicationContext(), ViewData.class));
            }
        });
    }


    public void setup() {

        cal = Calendar.getInstance();
        dateFormatTime = new SimpleDateFormat("hh:mm:ss aa");
        dateFormatDayAndTime = new SimpleDateFormat("MMM dd, yyyy hh:mm aa");

        for (int i = 0; i < ARRAY_SIZE_MAX; i++) {
            dataArray[i] = null;
            timeArray[i] = null;
        }
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        start = findViewById(R.id.btnStartStop);
        reset = findViewById(R.id.btnReset);
        viewData = findViewById(R.id.btnViewData);
        TV = findViewById(R.id.TV);
        if (UPDATE_INTERVAL < 1000) {
            TV.setText("WARNING: UPDATE_INTERVAL IS TOO LOW\nIT MUST BE >= 1000 IN ORDER FOR THIS APP TO FUNCTION AS INTENDED");
        } else TV.setText("Press START to begin");

        //create file
        dataFile = new File(filename);
    }

    public void reset() {
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);  //open file and set to output stream
            for (int i = 0; dataArray[i] != null && i < ARRAY_SIZE_MAX; i++) {
                writeData(i, outputStream);
                dataArray[i] = null;
                timeArray[i] = null;
            }
            outputStream.close(); //close file
        } catch (Exception e) {
            e.printStackTrace();
        }

        numPins = 0;
        wasReset = true;
        setStartTime = false;
    }

    public void writeData(int i, FileOutputStream outputStream) {
        if (i == 0) {    //print time range of data points as header of data
            if (fileContents != null)
                fileContents += "------------------------------\n Start: " + time + "\n";
                //if file was empty to begin with, we don't want to print out "null" at the beginning
            else fileContents = "------------------------------\n Start: " + time + "\n";
            //convert epoch time to calendar data
            cal.setTimeInMillis(currentLocation.getTime());
            //print accuracy value on screen along with coordinates and time
            time = dateFormatDayAndTime.format(cal.getTime());
            fileContents += " Stop:  " + time + "\n------------------------------\n";
        }

        //set fileContents to number, accuracy value, and timestamp [example: "#1)  9.0"  ]
        fileContents += String.format("%-8s%s", "#" + (i + 1) + ") ", String.format("%-10s%s", dataArray[i], timeArray[i] + "\n"));
        if (dataArray[i + 1] == null) {  //end of data that must be written is reached
            fileContents += "\nAverage: " + average + "\n\n"; //write the average and add some endlines
            try {   //write file
                outputStream.write(fileContents.getBytes());    //write fileContents into file
                TV.setText(TV.getText() + "File Written");
            } catch (IOException e) {
                e.printStackTrace();
                TV.setText(TV.getText() + "\nERROR - File not written - IOException e");
            }
        }

    }


    /*
     * "Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app."
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        Criteria criteria = new Criteria();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    Toast.makeText(this, "Need GPS permissions for app to function", Toast.LENGTH_LONG).show();
                }
                //once permission is granted, set up location listener
                //updating every UPDATE_INTERVAL milliseconds, regardless of distance change
                else
                    locationManager.requestLocationUpdates("gps", UPDATE_INTERVAL, 0, locationListener);
                return;
            }
        }
        Location lastLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (lastLocation != null) {
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Sets the zoom
                    .bearing(0)                // Sets the orientation of the camera to east
                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            zoomed = true;
        }
        else{
            LatLng wichita = new LatLng(37.6913, 262.6503);
            gMap.moveCamera(CameraUpdateFactory.newLatLng(wichita));
        }
    }


    //NOTE: This function executes more times than it should without (numPins == 0 || !(timeArray[numPins - 1].equals(dateFormatTime.format(location.getTime())) )) in place
    //IMPORTANT: This function will only work as long as UPDATE_INTERVAL >= 1000 because of the above statement's necessity
    public void locationDetails() {
        if(on) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    //when location changes, display accuracy of that reading
                    //currentLocation = location;
                    if (on && (numPins == 0 || !(timeArray[numPins - 1].equals(dateFormatTime.format(location.getTime())) ))) {
                            //note: if numPins == 0, then timeArray[numPins - 1] is an invalid reference, however that
                            // statement will not be analyzed if numPins == 0, so there is no crash as long as numPins == 0 is analyzed first
                        currentLocation = location;
                        if(!setStartTime) {
                            //convert epoch time to calendar data
                            cal.setTimeInMillis(currentLocation.getTime());
                            time = dateFormatDayAndTime.format(cal.getTime());
                            setStartTime = true;
                        }
                        if(!zoomed){   //if map was not previously zoomed in, zoom it in now on current location
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 13));

                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))      // Sets the center of the map to location user
                                    .zoom(18)                   // Sets the zoom
                                    .bearing(0)                // Sets the orientation of the camera to east
                                    .tilt(0)                   // Sets the tilt of the camera to 30 degrees
                                    .build();                   // Creates a CameraPosition from the builder
                            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            zoomed = true;
                        }
                        //get lat and long
                        currentLongitude = location.getLongitude();
                        currentLatitude = location.getLatitude();
                        //set lat and long into LatLng type variable
                        currentPosition = new LatLng(currentLatitude, currentLongitude);
                        //put accuracy value into array
                        dataArray[numPins] = location.getAccuracy();
                        //get time stamp
                        //cal.setTimeInMillis(currentLocation.getTime());
                        timeArray[numPins] = dateFormatTime.format(location.getTime());
                        //set label for marker
                        name = (dataArray[numPins] + " #" + (++numPins));
                        //add marker
                        gMap.addMarker(new MarkerOptions().position(currentPosition).title(name));
                        //update camera position
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(currentPosition));

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


    public void getPermissions(){
        //if at least Marshmallow, need to ask user's permission to get GPS data
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //if permission is not yet granted, ask for it
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    Toast.makeText(this, "Need GPS permissions for app to function", Toast.LENGTH_LONG).show();
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
