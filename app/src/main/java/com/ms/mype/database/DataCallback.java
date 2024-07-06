package com.ms.mype.database;

import com.ms.mype.reports.ChartData;

import java.util.List;

public interface DataCallback {
    void onDataRetrieved(List<ChartData> chartDataList);
}
