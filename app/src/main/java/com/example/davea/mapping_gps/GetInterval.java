package com.example.davea.mapping_gps;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class GetInterval extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_interval);

        //setup UI
        final EditText interval_input = findViewById(R.id.interval_input);
        final EditText lat_input = findViewById(R.id.lat_input);
        final EditText lng_input = findViewById(R.id.lng_input);
        Button done = findViewById(R.id.done);
        TextView TV = findViewById(R.id.instructions);

        //print instrucitons
        TV.setText(R.string.GetInterval_Instructions);

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //note: using an unsigned EditText for interval, so don't have to worry about negative numbers there
                if(interval_input.getText().toString().trim().length() > 0) {   //ensure interval_input is not empty
                    MapsActivity.interval = Integer.valueOf(interval_input.getText().toString()); //set interval to value specified in interval_input
                    MapsActivity.interval *= 1000;  //convert seconds into milliseconds
                    MapsActivity.setInterval = true;    //ensures that this activity only runs once
                    if (MapsActivity.locationListener != null) {    //if there is a location listener set up, remove it
                        MapsActivity.locationManager.removeUpdates(MapsActivity.locationListener);  //ensures we only have one location listener running at once. Don't want duplicate data.
                    }
                    //if user has entered both a true lat and long value, set them
                    //if entered only one, ignore it
                    if(lat_input.getText().toString().trim().length() > 0 && lng_input.getText().toString().trim().length() > 0){
                        MapsActivity.setTrueLatLng = true;
                        //set values in MapsActivity
                        MapsActivity.trueLat = Float.valueOf(lat_input.getText().toString());
                        MapsActivity.trueLng = Float.valueOf(lng_input.getText().toString());
                    }
                    else {  //else did not specify both true lat and long
                        MapsActivity.setTrueLatLng = false;
                    }
                    //go back to main activity
                    startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                }
            }
        });
    }
}
