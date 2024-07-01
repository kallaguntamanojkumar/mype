package com.ms.mype.reports;

public class ChartData {
    private String label;
    private float value;

    public ChartData(String label, float value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public float getValue() {
        return value;
    }
}
