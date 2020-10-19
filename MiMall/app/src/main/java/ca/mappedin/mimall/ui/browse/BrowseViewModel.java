package ca.mappedin.mimall.ui.browse;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BrowseViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public BrowseViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is Browse fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}