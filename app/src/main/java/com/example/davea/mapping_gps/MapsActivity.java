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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Google Map:
    public GoogleMap gMap;

    //Constants:
    static final String filename = "GPS_data.txt";  //name of file where data is saved

    //Location:
    public Location currentLocation;
    static LocationManager locationManager;
    static LocationListener locationListener;

    //Files:
    File dataFile;

    //UI:
    Button start, save, viewData;  //self-explanatory buttons
    TextView TV;    //only textview

    //variables:
    double currentLongitude;
    double currentLatitude;
    LatLng currentPosition;
    public int numPins = 0; //number of markers on map
    LinkedList<Float> dataList = new LinkedList<Float>();
    float average = -999;   //average of the accuracy readings from one session. Initialize to -999 so any errors are obvious
    static String fileContents; //Stuff that will be written to the file. It is static so that it can be accessed in other activity
    String markerLabel = "X";  //label for the marker (accuracy and number of marker example: 3.0 #2)
    String time;    //the time in the dateFormatDayAndTime format (defined later). Used for giving start and end times of each session
    LinkedList<String> timeList = new LinkedList<String>(); //List of all the times that the datapoints were taken
    boolean wasReset = false;    //true if session data has been reset
    boolean setStartTime = false;   //true if start time of session has been set. Ensures that start time is only set at the beginning of a session
    public boolean on = false;  //true if session is running, not paused or stopped
    boolean zoomed = false; //true if camera has zoomed in on location yet
    boolean locationPermissionGranted = false;  //true once location permission is granted. Used to ensure that location updates are only requested once
    static boolean setInterval = false; //true if user has specified GPS refresh rate
    static int interval = 0;    //refresh rate of GPS
    static Float trueLat = null;    //inputted correct latitude (see GetInterval Activity)
    static Float trueLng = null;    //inputted correct longitude (see GetInterval Activity)
    static boolean setTrueLatLng = false;    //specifies if user inputs true values of lat and long
    float distanceError[] = new float[3];
    LinkedList<Float> distanceErrorList = new LinkedList<Float>();

    //Time:
    //create calendar to convert epoch time to readable time
    Calendar cal;
    //create simple date format to show just 12hr time. Defined later on.
    SimpleDateFormat dateFormatTime;
    SimpleDateFormat dateFormatDayAndTime;

    //criteria for location:
    Criteria locationCriteria = new Criteria();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setup();

        if (interval == 0 && setInterval) {
            Toast.makeText(this, "WARNING: UPDATE INTERVAL IS SET TO 0.\n(CONTINUOUS UPDATES)\nTHIS MAY RESULT IN HIGH POWER CONSUMPTION", Toast.LENGTH_LONG).show();
        }

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on = !on;   //invert on. Toggles between start and paused
                if (on) {
                    if(wasReset)gMap.clear();  //clear map when starting after a reset

                    TV.setText("RUNNING");  //display session status
                    locationDetails();  //get location info
                    if (wasReset) { //set wasReset to false
                        wasReset = false;
                    }
                } else {
                    TV.setText("PAUSED");    //if not on after pressing start, session must be paused
                }
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (on) {
                    on = false; //paused if currently on

                    if(numPins > 0) { //if data has been recorded
                        TV.setText("PAUSED - press again to write");
                    }
                    else{   //if there is no data thus far
                        TV.setText("Press START to begin");
                    }

                } else if (numPins != 0) {      //if data has been collected but was not on
                    long sum = (long) 0.0;  //initialize sum to 0
                    for (int i = 0; i < numPins; i++) { //get sum of all accuracy reading during the session
                        sum += dataList.get(i);
                    }
                    average = sum / ((float) numPins); //compute average
                    TV.setText(""); //clear TextView
                    reset();    //reset data values and write to file
                } else TV.setText("Press START to begin"); //if numPins == 0, then it does not need to be reset because it's already empty
            }
        });

        viewData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!on) startActivity(new Intent(getApplicationContext(), ViewData.class));    //go to activity where file contents are viewed
            }
        });
    }


    public void setup() {

        if(!setInterval) {
            startActivity(new Intent(getApplicationContext(), GetInterval.class));
        }

        final int UPDATE_INTERVAL = interval;

        cal = Calendar.getInstance();   //instantiate a calendar
        //define the data formats
        dateFormatTime = new SimpleDateFormat("HH:mm:ss");
        dateFormatDayAndTime = new SimpleDateFormat("MMM dd, yyyy hh:mm aa");

        //empty the lists
        dataList.clear();
        timeList.clear();

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE); //set up location manager

        //define buttons and textview
        start = findViewById(R.id.btnStartStop);
        save = findViewById(R.id.btnSave);
        viewData = findViewById(R.id.btnViewData);
        TV = findViewById(R.id.TV);

        TV.setText("Press START to begin"); //print starting message in textview

        dataFile = new File(filename);//create file


        //specify that we want very high accuracy for GPS location reading
        locationCriteria.setAccuracy(Criteria.ACCURACY_FINE);
        locationCriteria.setPowerRequirement(Criteria.POWER_HIGH);
        locationCriteria.setAltitudeRequired(false);
        locationCriteria.setSpeedRequired(false);
        locationCriteria.setCostAllowed(true);
        locationCriteria.setBearingRequired(false);
        locationCriteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
        locationCriteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
    }

    public void reset() {

        if(numPins > 0) {//don't bother with this if there is no data to write
            FileOutputStream outputStream;  //declare output stream
            try {   //attempt to open and write to file
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);  //open file and set to output stream

                for (int i = 0; i < numPins; i++) {
                    writeData(i, outputStream); //write data
                }
                //empty the lists
                dataList.clear();
                timeList.clear();

                outputStream.close(); //close file
            } catch (Exception e) { //if file is not found, catch exception
                e.printStackTrace();
                TV.setText("ERROR: \nDATA NOT WRITTEN");  //error message if file not found
            }
        }

        //reset values
        numPins = 0;
        wasReset = true;
        setStartTime = false;
        locationManager.removeUpdates(locationListener);
        locationPermissionGranted = false; // ensures that locationManager is restarted by forcing locationDetails() to call getPermissions()
        zoomed = false;
    }

    public void writeData(int i, FileOutputStream outputStream) {
        //print header:
        if (i == 0) {    //print time range of data points as header of data
            if (fileContents != null)   //if file already has data in there, use "+=" to add to it
                fileContents += "------------------------------\n Start: " + time + "\n";
            //if file was empty to begin with, we don't want to print out "null" at the beginning, so use "=" instead of "+="
            else fileContents = "------------------------------\n Start: " + time + "\n";

            //convert epoch time to calendar data
            cal.setTimeInMillis(currentLocation.getTime());

            //print accuracy value on screen along with coordinates and time
            time = dateFormatDayAndTime.format(cal.getTime());
            fileContents += " Stop:  " + time + "\n------------------------------\n";
            if(setTrueLatLng) fileContents += "#  | Accuracy |  Error (m)  | Time\n";
            else fileContents += "#  | Accuracy | Time\n";
        }

        //set fileContents to number, accuracy value, and timestamp [example: "#1)  9.0"  ] with fancy formatting
        //fileContents += ("#" + (i + 1) + ") \t\t" +(dataList.get(i).toString() + " \t\t" + (String.format("%.2f", distanceErrorList.get(i))) + " \t\t" + timeList.get(i) + "\n"));
        if(setTrueLatLng) {
            fileContents += String.format("%-5s %s", ((i + 1) + ")"), String.format("%-10s %s", (dataList.get(i).toString()), String.format("%-11s %s", (String.format("%.2f", distanceErrorList.get(i))), (timeList.get(i) + "\n"))));
            //note: distanceErrorList.get(i) will never be more than 11 chars since the earth's circumference is about 40 million meters (XXXXXXXX.XX), so formatting will never truncate digits
        }
        else{
            fileContents += String.format("%-7s %s", ("#" + (i + 1) + ")"), String.format("%-10s %s", (dataList.get(i).toString()), (timeList.get(i) + "\n")));

        }

        if (i == numPins - 1) {  //end of data that must be written is reached
            fileContents += "\nAverage Accuracy: " + average + "\n\n"; //write the average and add some endlines
            try {   //write file
                outputStream.write(fileContents.getBytes());    //write fileContents into file
                TV.setText(TV.getText() + "File Written");
            } catch (IOException e) {   //catch exception and print warning
                e.printStackTrace();
                TV.setText(TV.getText() + "\nERROR - File not written - IOException e");
                Toast.makeText(this,"ERROR\nFile Not Written", Toast.LENGTH_LONG).show();
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
        gMap = googleMap;   //set gMap to the 'inputted' googleMap

        Criteria criteria = new Criteria(); //not completely sure how this works, but it does
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {    //[can't just call getPermissions() here because app would crash if you tried]
            //ensure we have the right permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //if permission still not granted, tell user app will not work without it
                    Toast.makeText(this, "Need GPS permissions for app to function", Toast.LENGTH_LONG).show();
                    getPermissions();//Not yet tested here - hopefully this is the right place to put this.
                }
                //once permission is granted, set up location listener
                //updating every UPDATE_INTERVAL milliseconds, regardless of distance change
                else
                    locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
                    //locationManager.requestLocationUpdates("gps", interval, 0, locationListener);
                    locationPermissionGranted = true;
                return;
            }
        }

        //get last known location
        Location lastLocation = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
        if (lastLocation != null) {
            //zoom in camera on last known location when app is initialized
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), 13));

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()))      // Sets the center of the map to location user
                    .zoom(18)                   // Set the zoom value
                    .bearing(0)                // Point North
                    .tilt(0)                   // No tilt
                    .build();                   // Creates a CameraPosition from the builder
            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));  //set camera to position defined above
            zoomed = true;  //camera has now been zoomed, does not need to happen again
        }
        else{
            //if no last-known location, then center map over Wichita
            LatLng wichita = new LatLng(37.6913, 262.6503);
            gMap.moveCamera(CameraUpdateFactory.newLatLng(wichita));
        }
    }


    public void locationDetails() {

        if(on) { //ensures the program is "on"
            locationListener = new LocationListener() { //setting up new location listener
                @Override
                public void onLocationChanged(Location location) {
                    //when location changes, display accuracy of that reading
                    //currentLocation = location;
                    if (on) {
                        currentLocation = location; //set current location
                        if(!setStartTime) { // if start time not yet set
                            //convert epoch time to calendar data
                            cal.setTimeInMillis(currentLocation.getTime()); //get time and put into calendar
                            time = dateFormatDayAndTime.format(cal.getTime());  //format date and time and set to string time
                            setStartTime = true;    //start time is now set
                        }
                        if(!zoomed){   //if map was not previously zoomed in, zoom it in now on current location
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 13));

                            CameraPosition cameraPosition = new CameraPosition.Builder()
                                    .target(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()))      // Sets the center of the map to location user
                                    .zoom(18)                   // Set the zoom
                                    .bearing(0)                // Point north
                                    .tilt(0)                   // No tilt
                                    .build();                   // Creates a CameraPosition from the builder
                            gMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                            zoomed = true;  //camera is now zoomed
                        }
                        //get lat and long
                        currentLongitude = location.getLongitude();
                        currentLatitude = location.getLatitude();

                        //if user inputted the true lat and long, then calculate distance between GPS's location and true coordinates
                        if(setTrueLatLng) {
                            location.distanceBetween(trueLat, trueLng, currentLatitude, currentLongitude, distanceError);
                            distanceErrorList.add(distanceError[0]);
                        }

                        //set lat and long into LatLng type variable
                        currentPosition = new LatLng(currentLatitude, currentLongitude);

                        //get accuracy and put value into linked list.
                        dataList.add(location.getAccuracy());

                        //display values on screen
                        TV.setText("Running - #" + (numPins + 1) + " - " + location.getAccuracy());

                        //get time stamp
                        timeList.add(dateFormatTime.format(System.currentTimeMillis()));

                        //set label for marker (accuracy and marker number)
                        markerLabel = dataList.get(numPins) + " #" + (++numPins);

                        //create instance of MarkerOptions
                        MarkerOptions markerOptions = new MarkerOptions();
                        //set marker options:
                        markerOptions.position(currentPosition);    //location of marker
                        markerOptions.title(markerLabel);   //label for marker

                        //set color of marker based on accuracy reading
                        if(dataList.get(numPins - 1) < 10) {   //if small error margin, marker is green
                            //note: numPins was previously incremented, so use numPins-1 as index
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        }
                        else if (dataList.get(numPins - 1) < 25)   //if 10 <= accuracy < 25, yellow marker
                        {
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                        }
                        else if (dataList.get(numPins - 1) < 100){   //else red marker for 25 <= accuracy < 100
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                        }
                        else { //else if accuracy >= 100
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN));
                        }

                        //add marker
                        gMap.addMarker(markerOptions);

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

            //if permission is needed, get it
            if (!locationPermissionGranted) getPermissions();   //ensures that this only executes once after permission is granted
        }

    }


    public void getPermissions(){
        locationManager.removeUpdates(locationListener);    //ensure no duplicate update requests
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
                else{
                    locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
                    //locationManager.requestLocationUpdates("gps", interval, 0, locationListener);
                    locationPermissionGranted = true;
                }
            }
            else {
                locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
                //locationManager.requestLocationUpdates("gps", interval, 0, locationListener);
                locationPermissionGranted = true;
            }

        }   //else if below Marshmallow, we don't need to ask special permission
        else if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            assert locationManager != null;
            locationManager.requestLocationUpdates(interval, 0, locationCriteria, locationListener, null);
            //locationManager.requestLocationUpdates("gps", interval, 0, locationListener);
            locationPermissionGranted = true;
        }
        else{
            //if permission still not granted, tell user app will not work without it
            Toast.makeText(this, "Need GPS permissions for app to function", Toast.LENGTH_LONG).show();
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

}
