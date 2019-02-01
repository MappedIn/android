package mappedin.com.wayfindingsample;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.SearchResult;
import com.mappedin.sdk.SearchResultType;

import java.util.ArrayList;

/**
 * Created by Peter on 2018-02-27.
 */
class SearchResultAdapter extends ArrayAdapter<SearchResult> {
    ArrayList<SearchResult> results;
    TextView suggestTextView;
    Typeface typeface;
    int resource;

    public SearchResultAdapter(
            Context context, int resource, ArrayList<SearchResult> results, Typeface typeface) {
        super(context, resource, results);
        this.results = results;
        this.typeface = typeface;
        this.resource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder mViewHolder;
        if (convertView == null){
            mViewHolder = new ViewHolder();
        LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
        convertView = layoutInflater.inflate(resource, null);
                mViewHolder.suggestTextView = convertView.findViewById(R.id.search_result_text_view);
                mViewHolder.suggestTextView.setTypeface(typeface);
                convertView.setTag(mViewHolder);
            }
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        SearchResult result = results.get(position);
                Location location = result.getResultObject(Location.class);
                if (location != null) {
            mViewHolder.suggestTextView.setText(location.getName());
            mViewHolder.position = position;
        }
        return convertView;
    }

    public void updateResults(SearchResult[] newResults) {
        results.clear();
        for (SearchResult result : newResults) {
            if (result != null && result.getResultType() == SearchResultType.LOCATION)
            results.add(result);
        }
        this.notifyDataSetChanged();
    }

    public ArrayList<SearchResult> getResults() {
        return results;
    }
    static class ViewHolder {
        TextView suggestTextView;
        int position;
    }
}
