package com.ms.mype.qrscanning;

import static android.content.ContentValues.TAG;

import static com.ms.mype.constants.Constats.SPREADSHEET_ID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.ms.mype.R;
import com.ms.mype.database.SheetsServiceHelper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AmountEntryActivity extends AppCompatActivity {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final int UPI_PAYMENT_REQUEST_CODE = 1;

    private EditText editTextAmount;
    private Button buttonPay;
    private Button buttonAlreadyPay;
    private String upiQrData;
    private String optionSelected;
    String amount = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrscanning_activity_amount_entry);

        editTextAmount = findViewById(R.id.editTextAmount);
        buttonPay = findViewById(R.id.buttonPay);
        buttonAlreadyPay = findViewById(R.id.buttonAlreadyPaid);

        // Get the UPI QR data from the intent
        upiQrData = getIntent().getStringExtra("UPI_QR_DATA");
        optionSelected = getIntent().getStringExtra("OPTION_SELECTED");

        amount = editTextAmount.getText().toString().trim();

        buttonPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = editTextAmount.getText().toString().trim();
                if (!TextUtils.isEmpty(amount)) {
                    initiateUPIPayment(upiQrData, amount);
                } else {
                    Toast.makeText(AmountEntryActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonAlreadyPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = editTextAmount.getText().toString().trim();
                if (!TextUtils.isEmpty(amount)) {
                    Toast.makeText(AmountEntryActivity.this, "Transaction Added successfully." + optionSelected +" "+ amount, Toast.LENGTH_SHORT).show();
                    // add database logic here, for thi we need optionSelected and amount
                    addEntryToDatbase(optionSelected,amount);
                    finish();
                } else {
                    Toast.makeText(AmountEntryActivity.this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void initiateUPIPayment(String upiQrData, String amount) {
        // Append the amount to the UPI QR data
        Uri uri = Uri.parse(upiQrData + "&am=" + amount + "&cu=INR");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        Intent chooser = Intent.createChooser(intent, "Pay with");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(chooser);
        } else {
            Toast.makeText(this, "No UPI app found to handle the payment", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            if (data != null) {
                String response = data.getStringExtra("response");
                if (response == null) {
                    response = "discard";
                }
                handleUPIPaymentResponse(response);
            } else {
                handleUPIPaymentResponse("nothing");
            }
        }
    }

    private void handleUPIPaymentResponse(String response) {
        String status = "";
        String approvalRefNo = "";
        String[] responseParams = response.split("&");
        for (String param : responseParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length >= 2) {
                String key = keyValue[0].toLowerCase();
                String value = keyValue[1];
                if (key.equals("status")) {
                    status = value.toLowerCase();
                } else if (key.equals("approvalrefno") || key.equals("txnref")) {
                    approvalRefNo = value;
                }
            }
        }

        if (status.equals("success")) {
            Toast.makeText(this, "Transaction successful. Reference No: " + approvalRefNo, Toast.LENGTH_SHORT).show();
            // add database logic here, for thi we need optionSelected and amount
            addEntryToDatbase(optionSelected,amount);
        } else if ("payment cancelled by user".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Transaction failed. Please try again", Toast.LENGTH_SHORT).show();
        }
    }

    private void addEntryToDatbase(String iteam, String price){
        List<List<Object>> test_data = new ArrayList<>();
        test_data.add(new ArrayList<Object>() {{
            add(iteam);
            add(Float.parseFloat(price));
        }});

        SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String sheetName = sharedPreferences.getString("SHEETNAME", "");
        writeDataToSheet(sheetName, getCurrentMonth(),getCurrentYear(),test_data);
    }

    public String getCurrentMonth() {
        LocalDate currentDate = LocalDate.now();
        String month = currentDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault());
        return month;
    }

    public String getCurrentYear() {
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        return String.valueOf(year);
    }

    private void writeDataToSheet(String sheetName, String month, String year, List<List<Object>> data) {
        executorService.execute(() -> {
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
                    if (values.get(0).get(i).toString().equalsIgnoreCase(month) && values.get(0).get(i + 1).toString().equalsIgnoreCase(year)) {
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
        });
    }
    private String getColLetter(int col) {
        StringBuilder colLetter = new StringBuilder();
        while (col >= 0) {
            colLetter.insert(0, (char) ('A' + col % 26));
            col = col / 26 - 1;
        }
        return colLetter.toString();
    }

    //database stuff
    private void showLoadingDialogWithDuration(long secs) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(com.ms.mype.qrscanning.AmountEntryActivity.this);
            builder.setView(R.layout.loading_dialog);
            builder.setCancelable(false);
            AlertDialog dialog = builder.create();
            dialog.show();

            // Dismiss the dialog after 2 seconds
            new Handler().postDelayed(dialog::dismiss, 1000*secs);
        });
    }
}
