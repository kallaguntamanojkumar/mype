package com.ms.mype.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.ms.mype.R;

public class ChartDataAdapter extends RecyclerView.Adapter<ChartDataAdapter.ViewHolder> {
    private List<ChartData> chartDataList;

    public ChartDataAdapter(List<ChartData> chartDataList) {
        this.chartDataList = chartDataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.reports_item_chart_data, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChartData chartData = chartDataList.get(position);
        holder.labelTextView.setText(chartData.getLabel());
        holder.valueTextView.setText(String.valueOf(chartData.getValue()));
    }

    @Override
    public int getItemCount() {
        return chartDataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView labelTextView;
        public TextView valueTextView;

        public ViewHolder(View view) {
            super(view);
            labelTextView = view.findViewById(R.id.labelTextView);
            valueTextView = view.findViewById(R.id.valueTextView);
        }
    }
    public void updateData(List<ChartData> newChartDataList) {
        this.chartDataList = newChartDataList;
        notifyDataSetChanged();
    }
}

