package com.mappedin.examples.singlevenue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mappedin.sdk.Category;

public class CategoryListAdapter  extends ArrayAdapter<Category> {
    Category[] categories;
    TextView categoryNameTextView;
    int resource;

    public CategoryListAdapter(Context context, int resource, Category[] categories) {
        super(context, resource, categories);
        this.categories = categories;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        convertView = layoutInflater.inflate(resource, null);
        categoryNameTextView = (TextView) convertView.findViewById(R.id.category_name_text_view);
        categoryNameTextView.setText(categories[position].getName()+"   ("+categories[position].getLocations().length+")");
        return convertView;
    }
}
