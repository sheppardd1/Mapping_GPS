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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;



public class ViewData extends AppCompatActivity {

    FileInputStream inStream;
    BufferedReader reader;
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
                Toast.makeText(ViewData.this, "File Deleted", Toast.LENGTH_SHORT).show();   //alter user file is deleted
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", TV2.getText());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ViewData.this, "Copied to Clipboard", Toast.LENGTH_SHORT).show();    //tell user text is copied to clipboard
            }
        });

    }

    void setup(){
        TV2 = findViewById(R.id.TV2);
        TV2.setMovementMethod(new ScrollingMovementMethod());
        btnDelete = findViewById(R.id.btnDelete);
        btnExport = findViewById(R.id.export);
    }

    void readFile(){
        try {
            inStream = openFileInput(MapsActivity.filename); //open file and set as input stream
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
