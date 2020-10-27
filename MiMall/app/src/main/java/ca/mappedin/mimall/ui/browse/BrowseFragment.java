package ca.mappedin.mimall.ui.browse;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.mappedin.models.MiLocation;

import ca.mappedin.mimall.R;
import ca.mappedin.mimall.shared.Repository;

public class BrowseFragment extends Fragment {

    private BrowseViewModel browseViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        //TODO 'androidx.lifecycle.ViewModelProviders' is deprecated
        //TODO 'of(androidx.fragment.app.Fragment)' is deprecated
        browseViewModel =
                ViewModelProviders.of(this).get(BrowseViewModel.class);
        View root = inflater.inflate(R.layout.fragment_browse, container, false);
        final TextView textView = root.findViewById(R.id.text_notifications);
        browseViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        for (MiLocation location :
                Repository.getInstance().getLocations()) {
            Log.d("Browse Location", "Name: " + location.getName());
        }

        return root;
    }
}