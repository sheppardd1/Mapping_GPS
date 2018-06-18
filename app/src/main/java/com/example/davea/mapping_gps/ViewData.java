package com.example.davea.mapping_gps;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ViewData extends AppCompatActivity {

    FileInputStream inStream;
    BufferedReader reader;
    final String filename = "GPS_data.txt"; //name of file to open, as assigned when written to
    TextView TV2;
    Button btnDelete;
    File dataFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        TV2 = findViewById(R.id.TV2);
        TV2.setMovementMethod(new ScrollingMovementMethod());
        btnDelete = findViewById(R.id.btnDelete);

        readFile();

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFile(filename);   //delete the file
                TV2.setText("File deleted");    //say so
                MapsActivity.fileContents = null;
            }
        });

        /*
        https://stackoverflow.com/questions/12910503/read-file-as-string
        https://developer.android.com/reference/java/io/File
        */

    }

    void readFile(){
        try {
            inStream = openFileInput(filename); //open file and set as input stream
            reader = new BufferedReader(new InputStreamReader(new DataInputStream(inStream)));  //set value of reader
            String line;    //declare string to read in one line at a time
            while((line = reader.readLine()) != null){
                TV2.setText(TV2.getText() + line + "\n");   //set textview to output the line
            }//keep outputting lines until end of file is reached
            inStream.close();   //close file
        } catch (FileNotFoundException e) { //catch exceptions
            e.printStackTrace();
            TV2.setText("File not found");
        } catch (IOException e) {
            e.printStackTrace();
            TV2.setText("error reading file - cannot read in lines");
        }
    }
}
