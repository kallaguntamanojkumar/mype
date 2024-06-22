package com.ms.mype.preference;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ms.mype.R;
import com.ms.mype.qrscanning.AmountEntryActivity;
import com.ms.mype.qrscanning.QRResultActivity;
import com.ms.mype.qrscanning.QRScanActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final int QR_SCAN_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    private static final String PREFS_NAME = "MyListPrefs";
    private static final String KEY_ITEMS = "Items";

    private List<String> itemList;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_main);

        ListView listView = findViewById(R.id.listView);
        FloatingActionButton fab = findViewById(R.id.fab);

        // Load items from SharedPreferences
        itemList = new ArrayList<>(loadItems());

        // Add default items if the list is empty
        if (itemList.isEmpty()) {
            itemList.addAll(Arrays.asList("D-Mart", "Groceries", "Amazon Shopping", "Restaurants and Cafes", "Utility Bill", "Mobile Recharges", "Travel Tickets", "Medical Shop"));
        }

        // Initialize the CustomAdapter
        adapter = new CustomAdapter(this, itemList);
        listView.setAdapter(adapter);

        // Handle FAB click to show dialog
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemDialog();
            }
        });
    }

    private void showAddItemDialog() {
        // Create the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.preference_dialog_add_item, null);
        builder.setView(dialogView);

        final EditText editTextNewItem = dialogView.findViewById(R.id.editTextNewItem);
        Button buttonAdd = dialogView.findViewById(R.id.buttonAdd);

        final AlertDialog dialog = builder.create();

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newItem = editTextNewItem.getText().toString().trim();
                if (!TextUtils.isEmpty(newItem)) {
                    itemList.add(newItem);
                    adapter.notifyDataSetChanged();
                    saveItems(itemList);
                    dialog.dismiss();
                }
            }
        });

        dialog.show();
    }

    private Set<String> loadItems() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getStringSet(KEY_ITEMS, new HashSet<String>());
    }

    public void saveItems(List<String> items) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> itemsSet = new HashSet<>(items);
        editor.putStringSet(KEY_ITEMS, itemsSet);
        editor.apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == QR_SCAN_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String qrCode = data.getStringExtra("SCAN_RESULT");
                if (!TextUtils.isEmpty(qrCode)) {
                    // Check if the scanned QR code is a UPI QR code
                    if (qrCode.startsWith("upi://")) {
                        // It's a UPI QR code, handle it accordingly
                        // For example, start a UPI payment activity
                        startUPIPaymentActivity(qrCode);
                    } else {
                        // Start QRResultActivity to display the QR code result
                        Intent intent = new Intent(MainActivity.this, QRResultActivity.class);
                        intent.putExtra("SCAN_RESULT", qrCode);
                        startActivity(intent);
                    }
                }
            }
        }
    }

    private void startUPIPaymentActivity(String upiQrData) {
        /*// Implement your UPI payment handling here
        // Example: Start an activity to handle UPI payment
        Toast.makeText(this, "UPI QR Code scanned: " + upiQrData, Toast.LENGTH_SHORT).show();
        // Replace this with actual UPI payment handling logic*/
        Intent intent = new Intent(MainActivity.this, AmountEntryActivity.class);
        intent.putExtra("UPI_QR_DATA", upiQrData);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
                startActivityForResult(intent, QR_SCAN_REQUEST_CODE);
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
