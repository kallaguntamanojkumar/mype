package com.ms.mype.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;

import com.ms.mype.R;

import java.util.List;

public class CustomAdapter extends BaseAdapter {

    private Context context;
    private List<String> items;
    private LayoutInflater inflater;

    public CustomAdapter(Context context, List<String> items) {
        this.context = context;
        this.items = items;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.preference_list_item, parent, false);
            holder = new ViewHolder();
            holder.buttonItem = convertView.findViewById(R.id.buttonItem);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.buttonItem.setText(items.get(position));

        // Set click listener to navigate to a new activity
        holder.buttonItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, com.ms.mype.qrscanning.DetailActivity.class);
                intent.putExtra("item_text", items.get(position));
                context.startActivity(intent);
            }
        });

        // Set long click listener to remove the item
        holder.buttonItem.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Show confirmation dialog before deleting
                new AlertDialog.Builder(context)
                        .setTitle("Delete Item")
                        .setMessage("Are you sure you want to delete this item?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                items.remove(position);
                                notifyDataSetChanged();
                                ((MainActivity) context).saveItems(items);
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
                return true;
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        Button buttonItem;
    }
}
