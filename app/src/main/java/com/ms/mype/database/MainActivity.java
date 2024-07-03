package com.ms.mype.database;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ms.mype.R;

public class MainActivity extends AppCompatActivity {

    private static final String SPREADSHEET_ID = "1gKMEWRBh3jeyqHesXWTBiM7-yfHaJpZF7JDEcTKj9BU";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_activity_main);

        // Initialize SheetsServiceHelper
        SheetsServiceHelper.initialize(getApplicationContext());

        Button writeButton = findViewById(R.id.btnWriteToSheets);
        Button readButton = findViewById(R.id.btnReadFromSheets);

        // Example usage in a background thread or method
        new Thread(() -> {
            try {
                Sheets sheets = SheetsServiceHelper.getSheetsService();
                if (sheets != null) {
                    // Perform operations using sheetsService
                    // For example:
                    Sheets.Spreadsheets spreadsheets = sheets.spreadsheets();
                    // Further operations...
                } else {
                    Log.e("MainActivity", "sheetsService is null");
                    // Handle null sheetsService case
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Example: Write data to Google Sheets
        List<List<Object>> data = new ArrayList<>();
        data.add(new ArrayList<Object>() {{
            add("Name");
            add("Age");
        }});
        data.add(new ArrayList<Object>() {{
            add("Alice");
            add(30);
        }});
        data.add(new ArrayList<Object>() {{
            add("Bob");
            add(25);
        }});
        writeToSheets(data);

        // Example: Read data from Google Sheets
        readFromSheets();


        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Write button clicked");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        data.add(new ArrayList<Object>() {{
                            add("Manoj");
                            add(30);
                        }});
                        writeToSheets(data);
                    }
                }).start();
            }
        });

        readButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Read button clicked");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        readFromSheets();
                    }
                }).start();
            }
        });
    }

    private void writeToSheets(List<List<Object>> data) {
        new Thread(() -> {
            try {
                Sheets sheetsService = SheetsServiceHelper.getSheetsService();
                ValueRange body = new ValueRange()
                        .setValues(data);
                sheetsService.spreadsheets().values()
                        .update(SPREADSHEET_ID, "Sheet1!A1", body)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void readFromSheets() {
        new Thread(() -> {
            try {
                Sheets sheetsService = SheetsServiceHelper.getSheetsService();
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(SPREADSHEET_ID, "Sheet1!A1:B10")
                        .execute();
                List<List<Object>> values = response.getValues();
                if (values != null) {
                    for (List<Object> row : values) {
                        // Process row data
                        String name = row.get(0).toString();
                        String age = row.get(1).toString();
                        Log.e("FromSheet",name+" "+age);
                        // Update UI or perform other operations
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
