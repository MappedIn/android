package com.mappedin.examples.singlevenue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import com.mappedin.sdk.Location;
import com.mappedin.sdk.SearchDelegate;
import com.mappedin.sdk.SearchResult;
import com.mappedin.sdk.SearchResultType;
import com.mappedin.sdk.SmartSearch;

import static com.mappedin.examples.singlevenue.MainActivity.PICK_CONTACT_REQUEST;

/**
 * implement SearchDelegate, handle results for search and suggest
 */
class SearchCallback implements SearchDelegate {

    private Activity activity;
    private SmartSearch smartSearch;
    private EditText searchBar;
    private ListView suggestListView;
    private GridView searchGridView;

    public SearchCallback(Activity activity, SmartSearch smartSearch, EditText searchBar, ListView suggestListView, GridView searchGridView) {
        this.activity = activity;
        this.smartSearch = smartSearch;
        this.searchBar = searchBar;
        this.suggestListView = suggestListView;
        this.searchGridView = searchGridView;
    }

    @Override
    public void suggestion(String quary, final String[] suggests) {
        if (suggests.length > 1 || (suggests.length == 1 && !suggests[0].equals(quary))) {
            SuggestListAdapter adapter =
                    new SuggestListAdapter(activity, suggests);
            suggestListView.setAdapter(adapter);
            suggestListView.setVisibility(View.VISIBLE);
            suggestListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selected = suggests[position];
                    searchBar.setText(selected);
                    suggestListView.setVisibility(View.INVISIBLE);
                    // try get search result in 1000ms(1s)
                    smartSearch.search(selected, 1000);
                }
            });
        }
    }

    @Override
    public void search(String quary, int total, int pgSize, int pgNum, final SearchResult[] results) {
        SearchGridAdapter adapter =
                    new SearchGridAdapter(activity, results);
        searchGridView.setAdapter(adapter);
        searchGridView.setVisibility(View.VISIBLE);
        searchGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                SearchResult selectResult = results[position];
                if (selectResult != null) {
                    Location location = selectResult.getResultObject(Location.class);
                    ((ApplicationSingleton) activity.getApplication()).setActiveLocation(location);
                    locationDetail();
                    searchGridView.setVisibility(View.INVISIBLE);
                    searchBar.setText("");
                }
            }
        });
    }

    private void locationDetail() {
        Intent showLocationDetail = new Intent(activity, LocationActivity.class);
        activity.startActivityForResult(showLocationDetail, PICK_CONTACT_REQUEST);
    }

    class SuggestListAdapter extends BaseAdapter {
        Context context;
        String[] suggests;

        SuggestListAdapter(Context context, String[] suggests) {
            this.context = context;
            this.suggests = suggests;
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return suggests.length;
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return suggests[position];
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = new TextView(context);
                textView.setTextSize(16);
                textView.setPadding(8, 8, 8, 8);
            } else {
                textView = (TextView) convertView;
            }

            String suggest = suggests[position];
            if (suggest != null) {
                textView.setText(suggest);
            }
            return textView;
        }
    }

    class SearchGridAdapter extends BaseAdapter {
        Context context;
        SearchResult[] results;

        SearchGridAdapter(Context context, SearchResult[] results) {
            this.context = context;
            this.results = results;
        }

        /**
         * How many items are in the data set represented by this Adapter.
         *
         * @return Count of items.
         */
        @Override
        public int getCount() {
            return results.length;
        }

        /**
         * Get the data item associated with the specified position in the data set.
         *
         * @param position Position of the item whose data we want within the adapter's
         *                 data set.
         * @return The data at the specified position.
         */
        @Override
        public Object getItem(int position) {
            return results[position];
        }

        /**
         * Get the row id associated with the specified position in the list.
         *
         * @param position The position of the item within the adapter's data set whose row id we want.
         * @return The id of the item at the specified position.
         */
        @Override
        public long getItemId(int position) {
            return 0;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                textView = new TextView(context);
                textView.setGravity(Gravity.CENTER);
                textView.setLayoutParams(new GridView.LayoutParams(300, 300));
                textView.setPadding(8, 8, 8, 8);
            } else {
                textView = (TextView) convertView;
            }

            SearchResult result = results[position];
            if (result != null) {
                if (result.getResultType() == SearchResultType.LOCATION) {
                    Location location = result.getResultObject(Location.class);
                    if (location != null){
                        textView.setText(location.getName());
                    }
                }
            }
            return textView;
        }
    }
}
