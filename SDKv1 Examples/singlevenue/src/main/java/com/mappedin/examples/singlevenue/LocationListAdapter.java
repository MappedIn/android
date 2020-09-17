package com.mappedin.examples.singlevenue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mappedin.sdk.Location;

public class LocationListAdapter extends ArrayAdapter<Location> {
    Location[] locations;
    TextView locationNameTextView;
    TextView locationTypeTextView;
    int resource;

    public LocationListAdapter(Context context, int resource, Location[] locations) {
        super(context, resource, locations);
        this.locations = locations;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        convertView = layoutInflater.inflate(resource, null);
        Location location = locations[position];
        String name = location.getName();
        String type = location.getType();
        locationNameTextView = (TextView) convertView.findViewById(R.id.location_name_text_view);
        locationNameTextView.setText(name);
        locationTypeTextView = (TextView) convertView.findViewById(R.id.location_type_text_view);
        locationTypeTextView.setText(type);
        return convertView;
    }
}