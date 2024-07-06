package com.ms.mype.reports;
import static android.content.ContentValues.TAG;

import static com.ms.mype.constants.Constats.SPREADSHEET_ID;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.ms.mype.R;
import com.ms.mype.database.SheetsServiceHelper;

public class MainActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private ChartDataAdapter adapter;
    private List<ChartData> chartDataList;
    private ProgressDialog progressDialog;
    private Sheets sheetsService;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reports_activity_main);

        // Initialize SheetsServiceHelper
        SheetsServiceHelper.initialize(getApplicationContext());

        pieChart = findViewById(R.id.pieChart);
        recyclerView = findViewById(R.id.recyclerView);

        setupPieChart();
        loadPieChartData();
        setupRecyclerView();

        // Get the Login result from the intent
        String userName = getIntent().getStringExtra("UserName");
        String userEmail = getIntent().getStringExtra("UserEmail");

        // Generate the sheet name using email and year
        String sheetName = userEmail + "-"+ Year.now().getValue();

        //storing userName and email id, if google login is introduced values should be updated here
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString("USERNAME", userName);
        editor.putString("EMAIL", userEmail);
        editor.putString("SHEETNAME", sheetName);
        editor.apply();

        //Retrieving Data
        /*SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String userName = sharedPreferences.getString("USERNAME", "");
        String email = sharedPreferences.getString("EMAIL", "");*/

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle the FAB click event
                Intent intent = new Intent(MainActivity.this, com.ms.mype.preference.MainActivity.class);
                startActivity(intent);
            }
        });



        // Initialize Sheets service
        sheetsService = SheetsServiceHelper.getSheetsService();

        // Check if the sheet exists, and create if it doesn't
        new Thread(() -> {
            try {
                checkAndCreateSheet(sheetName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        //signout logic
        // Configure Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    //signout logic
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reports_menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        // Clear SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Google Sign-In logout
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Redirect to login activity after logout
            Intent intent = new Intent(MainActivity.this, com.ms.mype.ssologin.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    //create new spreadsheet
    private void checkAndCreateSheet(String sheetName) throws IOException {
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(SPREADSHEET_ID).execute();
        List<Sheet> sheets = spreadsheet.getSheets();

        boolean sheetExists = false;
        for (Sheet sheet : sheets) {
            if (sheet.getProperties().getTitle().equals(sheetName)) {
                sheetExists = true;
                break;
            }
        }

        if (!sheetExists) {
            addSheet(sheetName);
        }
    }

    private void addSheet(String sheetName) throws IOException {
        List<Request> requests = new ArrayList<>();
        requests.add(new Request()
                .setAddSheet(new AddSheetRequest()
                        .setProperties(new SheetProperties()
                                .setTitle(sheetName))));

        BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, body).execute();

        Log.d("ReportsActivity", "Sheet " + sheetName + " created.");
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);
    }

    private void loadPieChartData() {

        chartDataList = new ArrayList<>();

        showLoadingDialog();

        FutureTask<List<ChartData>> futureTask = readDataFromSheets(LocalDate.now().getMonth().toString(), String.valueOf(LocalDate.now().getYear()));

        new Thread(() -> {
            try {
                // This will block until the data is available
                List<ChartData> mychartDataList = futureTask.get();

                // Use a map to sum values for duplicate keys
                Map<String, Float> chartDataMap = new HashMap<>();
                for (ChartData data : mychartDataList) {
                    chartDataMap.put(data.getLabel(), chartDataMap.getOrDefault(data.getLabel(), 0f) + data.getValue());
                }

                // Convert the map back to a list
                List<ChartData> uniqueChartDataList = new ArrayList<>();
                for (Map.Entry<String, Float> entry : chartDataMap.entrySet()) {
                    uniqueChartDataList.add(new ChartData(entry.getKey(), entry.getValue()));
                }

                // Process the retrieved data here
                for (ChartData data : uniqueChartDataList) {
                    Log.d(TAG, "Item: " + data.getLabel() + ", Price: " + data.getValue());
                }

                // Assign to your variable or update UI
                chartDataList = uniqueChartDataList;

                // Example of updating the UI (ensure this runs on the UI thread)
                runOnUiThread(() -> {
                    // Update your UI here with the chartDataList
                    ArrayList<PieEntry> entries = new ArrayList<>();
                    for (ChartData data : chartDataList) {
                        entries.add(new PieEntry(data.getValue(), data.getLabel()));
                        //Log.d(TAG, "Month: " + data.getLabel() + ", Year: " + data.getValue());
                    }

                    PieDataSet dataSet = new PieDataSet(entries, "Expense Chart");
                    dataSet.setSliceSpace(3f);
                    dataSet.setSelectionShift(5f);
                    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

                    PieData data = new PieData(dataSet);
                    data.setValueTextSize(10f);
                    data.setValueTextColor(Color.YELLOW);

                    pieChart.setData(data);
                    pieChart.invalidate(); // refresh

                    // Update RecyclerView
                    adapter.updateData(chartDataList);

                    // Dismiss the loading dialog
                    dismissLoadingDialog();
                });

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }).start();

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        adapter = new ChartDataAdapter(chartDataList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void adjustPieChartHeight() {
        final ViewTreeObserver observer = pieChart.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                pieChart.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int totalHeight = pieChart.getHeight() + recyclerView.getHeight();
                int halfHeight = totalHeight / 2;

                pieChart.getLayoutParams().height = halfHeight;
                pieChart.requestLayout();
            }
        });
    }

    //database stuff
    private void showLoadingDialogWithDuration(long secs) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(com.ms.mype.reports.MainActivity.this);
            builder.setView(R.layout.loading_dialog);
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();

            // Dismiss the dialog after 2 seconds
            new Handler().postDelayed(dialog::dismiss, 1000*secs);
        });
    }

    private void showLoadingDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Loading...");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void dismissLoadingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private FutureTask<List<ChartData>> readDataFromSheets(String month, String year) {
        FutureTask<List<ChartData>> futureTask = new FutureTask<>(new Callable<List<ChartData>>() {
            @Override
            public List<ChartData> call() {
                List<ChartData> chartDataList = new ArrayList<>();
                try {
                    Sheets sheetsService = SheetsServiceHelper.getSheetsService();
                    SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
                    String sheetName = sharedPreferences.getString("SHEETNAME", "");
                    ValueRange response = sheetsService.spreadsheets().values()
                            .get(SPREADSHEET_ID, sheetName+"!A:Z")
                            .execute();
                    List<List<Object>> values = response.getValues();
                    if (values != null && !values.isEmpty()) {
                        int colIndex = -1;
                        for (int i = 0; i < values.get(0).size(); i+=2) {
                            Log.d("reports/MainActivity.java", "Index: "+i);
                            if (values.get(0).get(i).toString().equalsIgnoreCase(month) && values.get(0).get(i + 1).toString().equalsIgnoreCase(year)) {
                                colIndex = i;
                                Log.d("reports/MainActivity.java", "Matched colIndex: "+colIndex);
                                break;
                            }
                        }

                        if (colIndex != -1) {
                            // Column found, read the entire column
                            Log.d("reports/MainActivity.java", "readDataFromSheets: "+values);
                            for (int i = 1; i < values.size(); i++) {
                                if (i < values.size() && colIndex < values.get(i).size()) {
                                    String item = values.get(i).get(colIndex).toString();
                                    String price = values.get(i).get(colIndex + 1).toString();
                                    Log.d("reports/MainActivity.java", "filtereddata: "+item+Float.parseFloat(price));
                                    // Skip empty strings
                                    if (!item.isEmpty() && !price.isEmpty()) {
                                        try {
                                            float value = Float.parseFloat(price);
                                            chartDataList.add(new ChartData(item, value));
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Invalid number format for price: " + price);
                                        }
                                    }
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
                return chartDataList;
            }
        });
        new Thread(futureTask).start();
        return futureTask;
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
                showLoadingDialogWithDuration(2);
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
                showLoadingDialogWithDuration(2);
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
            showLoadingDialogWithDuration(1);
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
