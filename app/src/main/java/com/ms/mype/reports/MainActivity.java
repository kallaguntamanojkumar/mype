package com.ms.mype.reports;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ms.mype.R;

public class MainActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private ChartDataAdapter adapter;
    private List<ChartData> chartDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reports_activity_main);

        pieChart = findViewById(R.id.pieChart);
        recyclerView = findViewById(R.id.recyclerView);

        setupPieChart();
        loadPieChartData();
        setupRecyclerView();

        //storing username and email id, if google login is introduced values should be updated here
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USERNAME", "Manojkumar Kallagunta");
        editor.putString("EMAIL", "manojkumarkallagunta12@gmail.com");
        editor.apply();

        //Retrieving Data
        /*SharedPreferences sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String username = sharedPreferences.getString("USERNAME", "");
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
        chartDataList.add(new ChartData("Label 1", 10));
        chartDataList.add(new ChartData("Label 2", 20));
        chartDataList.add(new ChartData("Label 3", 30));
        chartDataList.add(new ChartData("Label 4", 40));

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (ChartData data : chartDataList) {
            entries.add(new PieEntry(data.getValue(), data.getLabel()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Example Pie Chart");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.YELLOW);

        pieChart.setData(data);
        pieChart.invalidate(); // refresh


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
}
