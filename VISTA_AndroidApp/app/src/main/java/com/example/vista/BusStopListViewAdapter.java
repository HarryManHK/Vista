package com.example.vista;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.List;

public class BusStopListViewAdapter extends ArrayAdapter<String> {
    private int selectedPosition = -1;

    public BusStopListViewAdapter(Context context, int resource, List<String> objects) {
        super(context, resource, objects);
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (position == selectedPosition) {
            view.setBackgroundColor(getContext().getResources().getColor(android.R.color.darker_gray));
        } else {
            view.setBackgroundColor(getContext().getResources().getColor(android.R.color.transparent));
        }
        return view;
    }
}