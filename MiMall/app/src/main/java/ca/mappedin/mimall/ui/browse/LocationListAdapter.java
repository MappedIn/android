package ca.mappedin.mimall.ui.browse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.mappedin.models.MiLocation;

import java.util.List;

import ca.mappedin.mimall.R;

public class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.LocationViewHolder> {
    private List<MiLocation> dataset;
    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView locationName;
        public ImageView locationLogo;

        public LocationViewHolder(View v) {
            super(v);
            locationName = v.findViewById(R.id.locationName);
            locationLogo = v.findViewById(R.id.locationLogo);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public LocationListAdapter(List<MiLocation> dataset, Context context) {
        this.dataset = dataset;
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public LocationListAdapter.LocationViewHolder onCreateViewHolder(ViewGroup parent,
                                                                     int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_browse_list_item, parent, false);
        LocationViewHolder vh = new LocationViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(LocationViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.locationName.setText(this.dataset.get(position).getName());
        if (this.dataset.get(position).getLogo() != null) {
            Glide.with(context)
                    .load(this.dataset.get(position).getLogo().getSmall())
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.img_load_fail)
                    .into(holder.locationLogo);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.dataset.size();
    }

}
