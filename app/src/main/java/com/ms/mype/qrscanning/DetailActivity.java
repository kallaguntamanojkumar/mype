package com.ms.mype.qrscanning;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ms.mype.R;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qrscanning_activity_detail);

        TextView textViewDetail = findViewById(R.id.textViewDetail);

        // Get the item text from the intent
        String itemText = getIntent().getStringExtra("item_text");

        // Display the item text
        textViewDetail.setText(itemText);
    }
}
