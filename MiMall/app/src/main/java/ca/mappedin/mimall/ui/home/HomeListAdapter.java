package ca.mappedin.mimall.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import ca.mappedin.mimall.R;

public class HomeListAdapter extends RecyclerView.Adapter<HomeListAdapter.LocationViewHolder> {
    //    private List<MiLocation> dataset;
    private String[] title, subtitle, imgurls;
    Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView titleView;
        public TextView subtitleView;
        public ImageView imageView;

        public LocationViewHolder(View v) {
            super(v);
            titleView = v.findViewById(R.id.titleView);
            subtitleView = v.findViewById(R.id.subtitleView);
            imageView = v.findViewById(R.id.imageView);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public HomeListAdapter(String[] title, String[] subtitle, String[] imgurls, Context context) {
//        this.dataset = dataset;
        this.title = title;
        this.subtitle = subtitle;
        this.imgurls = imgurls;
        this.context = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public HomeListAdapter.LocationViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_home_list_item, parent, false);
        LocationViewHolder vh = new LocationViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(LocationViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.titleView.setText(this.title[position]);
        holder.subtitleView.setText(this.subtitle[position]);
        Glide.with(context)
                .load(imgurls[position])
                .placeholder(R.drawable.placeholder)
                .error(R.drawable.img_load_fail)
                .into(holder.imageView);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.title.length;
    }

}
