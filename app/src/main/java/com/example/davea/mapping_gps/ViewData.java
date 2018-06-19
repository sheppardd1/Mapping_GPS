package com.example.davea.mapping_gps;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;



public class ViewData extends AppCompatActivity {

    FileInputStream inStream;   //input stream from the file
    BufferedReader reader;  //reader to make the data useful

    //UI:
    TextView TV2;
    Button btnDelete;
    Button btnExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);

        setup();

        readFile();

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFile(MapsActivity.filename);   //delete the file
                TV2.setText("");    //clear TV2
                MapsActivity.fileContents = null;   //clear contents of fileContents so that it is not rewritten next time new data is added to the file
                Toast.makeText(ViewData.this, "File Deleted", Toast.LENGTH_SHORT).show();   //after user file is deleted, display toast saying so
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);    //instantiate clipboard manager
                ClipData clip = ClipData.newPlainText("clip", TV2.getText());  //copy data from textview into clipboard
                clipboard.setPrimaryClip(clip); //set as a clip
                Toast.makeText(ViewData.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();    //tell user text is copied to clipboard
            }
        });

    }

    void setup(){
        //define UI stuff
        TV2 = findViewById(R.id.TV2);
        TV2.setMovementMethod(new ScrollingMovementMethod());
        btnDelete = findViewById(R.id.btnDelete);
        btnExport = findViewById(R.id.export);
    }

    void readFile(){
        try {
            // try to open and read the file, but catch exceptions
            inStream = openFileInput(MapsActivity.filename); //open file and set as input stream
            reader = new BufferedReader(new InputStreamReader(new DataInputStream(inStream)));  //set value of reader
            String line;    //declare string to read in one line at a time
            while((line = reader.readLine()) != null){
                TV2.setText(TV2.getText() + line + "\n");   //set textview to output the line
            }//keep outputting lines until end of file is reached
            inStream.close();   //close file once finished reading through the file
        } catch (FileNotFoundException e) { //catch exceptions
            e.printStackTrace();
            TV2.setText("");
            Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "error reading file\ncannot read in lines", Toast.LENGTH_LONG);
        }
    }
}
