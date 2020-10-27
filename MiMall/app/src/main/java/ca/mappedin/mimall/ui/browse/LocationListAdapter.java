package ca.mappedin.mimall.ui.browse;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.mappedin.models.MiLocation;

import java.util.List;

public class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.LocationViewHolder> {
    private List<MiLocation> dataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView textView;

        public LocationViewHolder(TextView v) {
            super(v);
            textView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationListAdapter(List<MiLocation> dataset) {
        this.dataset = dataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LocationListAdapter.LocationViewHolder onCreateViewHolder(ViewGroup parent,
                                                                     int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        LocationViewHolder vh = new LocationViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(LocationViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.setText(this.dataset.get(position).getName());

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.dataset.size();
    }

}
