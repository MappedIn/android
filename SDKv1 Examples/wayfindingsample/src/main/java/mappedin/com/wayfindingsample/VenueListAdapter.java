package mappedin.com.wayfindingsample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import mappedin.com.wayfindingsample.R;
import com.mappedin.sdk.Venue;

import java.util.List;


/**
 * Created by Peter on 2016-03-18.
 */
public class VenueListAdapter extends BaseAdapter {

    private LayoutInflater layoutInflater;
    private List<Venue> venues;

    public VenueListAdapter(Context context, List<Venue> venues) {
        layoutInflater = LayoutInflater.from(context);
        this.venues = venues;
    }
    @Override
    public int getCount() {
        return venues.size();
    }

    @Override
    public Venue getItem(int position) {
        return venues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_venue, null);
            viewHolder = new ViewHolder();
            viewHolder.venueName = convertView.findViewById(R.id.venue_name_textview);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.venueName.setText(venues.get(position).getName());
        return convertView;
    }

    public void setVenues(List<Venue> venues) {
        this.venues = venues;
    }

    public static class ViewHolder {
        TextView venueName;
    }
}
