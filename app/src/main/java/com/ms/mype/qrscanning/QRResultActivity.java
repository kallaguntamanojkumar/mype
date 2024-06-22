package com.ms.mype.qrscanning;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ms.mype.*;

public class QRResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrscanning_activity_qrresult);

        TextView textViewResult = findViewById(R.id.textViewResult);

        // Get the QR code result from the intent
        String qrResult = getIntent().getStringExtra("SCAN_RESULT");
        textViewResult.setText(qrResult);
    }
}
