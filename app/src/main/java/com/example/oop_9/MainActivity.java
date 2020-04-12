package com.example.oop_9;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    Context context;
    EditText inputDate, inputTimeStart, inputTimeEnd, inputMovie;
    Spinner spinner;
    String dateString = "";
    final String spinnerHint = "Choose a theatre";
    TextView searchResults;
    TheatresFromXml tfXML = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        context = MainActivity.this;
        inputDate = findViewById(R.id.inputDate);
        inputTimeStart = findViewById(R.id.inputTimeStart);
        inputTimeEnd = findViewById(R.id.inputTimeEnd);
        inputMovie = findViewById(R.id.inputMovie);
        searchResults = findViewById(R.id.searchResults);
        spinner = findViewById(R.id.spinner);
        tfXML = new TheatresFromXml();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) {
                    return false;
                } else {
                    return true;
                }
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add(spinnerHint);
        adapter.add("All theatres");
        adapter.addAll(tfXML.getTheatreMap().keySet());
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                search();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                return;
            }
        });
    }

    public void refresh(View v) {
        search();
    }

    public void search() {
        try {
            String key = (String) spinner.getItemAtPosition(spinner.getSelectedItemPosition());
            String id = "ALL";

            /* even though this is disabled, OnItemSelected gets automatically called
               with the first item once when building the view */
            if (key.equals(spinnerHint)) {
                return;
            } else if (!key.equals("All theatres")) {
                id = tfXML.getTheatreMap().get(key).id;
            }

            String date = inputDate.getText().toString();
            String timeStart = inputTimeStart.getText().toString();
            String timeEnd = inputTimeEnd.getText().toString();
            String movie = inputMovie.getText().toString();
            String results = tfXML.getShows(id, date, timeStart, timeEnd, movie);
            searchResults.setText(results);
        } catch (Exception e) {
            new AlertDialog.Builder(context)
                    .setTitle("Error")
                    .setMessage("Couldn't perform search with those parameters")
                    .setPositiveButton(android.R.string.yes, null)
                    .show();
        }
    }
}
