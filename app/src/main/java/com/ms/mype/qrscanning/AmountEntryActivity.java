package com.ms.mype.qrscanning;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ms.mype.R;

public class AmountEntryActivity extends AppCompatActivity {

    private static final int UPI_PAYMENT_REQUEST_CODE = 1;

    private EditText editTextAmount;
    private Button buttonPay;
    private String upiQrData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrscanning_activity_amount_entry);

        editTextAmount = findViewById(R.id.editTextAmount);
        buttonPay = findViewById(R.id.buttonPay);

        // Get the UPI QR data from the intent
        upiQrData = getIntent().getStringExtra("UPI_QR_DATA");

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
        } else if ("payment cancelled by user".equalsIgnoreCase(status)) {
            Toast.makeText(this, "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Transaction failed. Please try again", Toast.LENGTH_SHORT).show();
        }
    }
}
