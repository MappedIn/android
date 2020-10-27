package ca.mappedin.mimall.ui.browse;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ca.mappedin.mimall.R;
import ca.mappedin.mimall.shared.Repository;

public class BrowseFragment extends Fragment {

    private BrowseViewModel browseViewModel;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //TODO 'androidx.lifecycle.ViewModelProviders' is deprecated
        //TODO 'of(androidx.fragment.app.Fragment)' is deprecated
        browseViewModel =
                ViewModelProviders.of(this).get(BrowseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_browse, container, false);
//        final TextView textView = root.findViewById(R.id.text_notifications);
//        browseViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
//            @Override
//            public void onChanged(@Nullable String s) {
//                textView.setText(s);
//            }
//        });

//        for (MiLocation location :
//                Repository.getInstance().getLocations()) {
//            Log.d("Browse Location", "Name: " + location.getName());
//        }

        recyclerView = root.findViewById(R.id.locationList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        layoutManager = new LinearLayoutManager(this.getContext());
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        mAdapter = new LocationListAdapter(Repository.getInstance().getLocations());
        recyclerView.setAdapter(mAdapter);


        return root;
    }
}