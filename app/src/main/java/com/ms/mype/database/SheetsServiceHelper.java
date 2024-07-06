package com.ms.mype.database;

import static android.content.ContentValues.TAG;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.io.InputStream;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.ms.mype.R;
import com.ms.mype.reports.ChartData;

public class SheetsServiceHelper {

    private static Sheets sheetsService;
    private static final String SPREADSHEET_ID = "1gKMEWRBh3jeyqHesXWTBiM7-yfHaJpZF7JDEcTKj9BU";

    public static void initialize(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

            // Initialize HttpTransport
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            sheetsService = new Sheets.Builder(
                    httpTransport,
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("mype")
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            Log.e("SheetsServiceHelper", "Error initializing SheetsServiceHelper", e);
        }
    }

    public static Sheets getSheetsService() {
        return sheetsService;
    }
  
    /*private void showLoadingDialog(long secs) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setView(R.layout.loading_dialog);
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();

            // Dismiss the dialog after 2 seconds
            new Handler().postDelayed(dialog::dismiss, 1000*secs);
        });
    }*/


    public void readDataFromSheets(String month, String year, DataCallback callback) {
        new Thread(() -> {
            List<ChartData> chartDataList = new ArrayList<>();
            try {
                Sheets sheetsService = SheetsServiceHelper.getSheetsService();
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(SPREADSHEET_ID, "Sheet1!A:Z")
                        .execute();
                List<List<Object>> values = response.getValues();
                if (values != null && !values.isEmpty()) {
                    int colIndex = -1;
                    for (int i = 0; i < values.get(0).size(); i++) {
                        if (values.get(0).get(i).toString().equalsIgnoreCase(month) && values.get(0).get(i+1).toString().equalsIgnoreCase(year)) {
                            colIndex = i;
                            break;
                        }
                    }

                    if (colIndex != -1) {
                        // Column found, read the entire column
                        for (int i = 1; i < values.size(); i++) {
                            if (i < values.size() && colIndex < values.get(i).size()) {
                                String smonth = values.get(i).get(colIndex).toString();
                                String syear = values.get(i).get(colIndex+1).toString();
                                chartDataList.add(new ChartData(smonth, Float.parseFloat(syear)));
                                Log.d(TAG, smonth + ": " + syear);
                            }
                        }
                    } else {
                        Log.d(TAG, "Column for " + month + " " + year + " not found.");
                    }
                } else {
                    Log.d(TAG, "Sheet is empty.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Call the callback with the retrieved data
            if (callback != null) {
                callback.onDataRetrieved(chartDataList);
            }
        }).start();
        Log.d(TAG, "readFromSheets: Invoked");
    }

    private void writeDataToSheet(String sheetName, String month, String year, List<List<Object>> data) {
        Sheets sheetsService = SheetsServiceHelper.getSheetsService();
        if (sheetsService == null) {
            Log.e(TAG, "SheetsService is null");
            return;
        }

        String spreadsheetId = SPREADSHEET_ID; // Replace with your Spreadsheet ID

        try {
            // Read the entire sheet to find the header
            String range = sheetName + "!A:Z"; // Adjust the range as needed
            ValueRange response = sheetsService.spreadsheets().values()
                    .get(spreadsheetId, range)
                    .execute();
            List<List<Object>> values = response.getValues();

            int colIndex = -1;

            // Check if the sheet is empty
            if (values == null || values.isEmpty()) {
                // Sheet is empty, initialize with the header and the data
                List<List<Object>> newTableData = new ArrayList<>();
                newTableData.add(Arrays.asList(month, year)); // Add header as the first row
                newTableData.addAll(data);

                String newTableRange = sheetName + "!A1";
                ValueRange body = new ValueRange().setValues(newTableData);
                sheetsService.spreadsheets().values().update(spreadsheetId, newTableRange, body)
                        .setValueInputOption("RAW")
                        .execute();

                Log.d(TAG, "New table created and data appended in empty sheet: " + month + " " + year);
                //showLoadingDialog(2);
                return;
            }



            for (int i = 0; i < values.get(0).size(); i += 2) {
                if (values.get(0).get(i).toString().equalsIgnoreCase(month) && values.get(0).get(i+1).toString().equalsIgnoreCase(year)) {
                    colIndex = i;
                    break;
                }
            }

            if (colIndex == -1) {
                // Table header not found, create new table at the next available columns
                colIndex = values.get(0).size();
                String headerRange = sheetName + "!" + getColLetter(colIndex) + "1:" + getColLetter(colIndex + 1) + "1";
                List<List<Object>> headers = new ArrayList<>();
                headers.add(Arrays.asList(month, year));
                ValueRange headerBody = new ValueRange().setValues(headers);
                sheetsService.spreadsheets().values().update(spreadsheetId, headerRange, headerBody)
                        .setValueInputOption("RAW")
                        .execute();
                //showLoadingDialog(2);
            }

            // Find the first empty row in the specified columns
            int rowIndex = -1;
            for (int i = 1; i < values.size(); i++) {
                if (values.get(i).size() <= colIndex || values.get(i).get(colIndex) == null || values.get(i).get(colIndex).toString().isEmpty()) {
                    rowIndex = i + 1; // 1-based index
                    break;
                }
            }

            if (rowIndex == -1) {
                rowIndex = values.size() + 1; // If no empty row found, append at the end
            }

            String appendRange = sheetName + "!" + getColLetter(colIndex) + rowIndex + ":" + getColLetter(colIndex + 1) + rowIndex;
            ValueRange body = new ValueRange().setValues(data);
            sheetsService.spreadsheets().values().append(spreadsheetId, appendRange, body)
                    .setValueInputOption("RAW")
                    .execute();

            Log.d(TAG, "Data appended successfully to " + month + " " + year);
            //showLoadingDialog(1);
        } catch (IOException e) {
            Log.e(TAG, "Error appending data to Sheets", e);
        }
    }

    private String getColLetter(int col) {
        StringBuilder colLetter = new StringBuilder();
        while (col >= 0) {
            colLetter.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return colLetter.toString();
    }
}
