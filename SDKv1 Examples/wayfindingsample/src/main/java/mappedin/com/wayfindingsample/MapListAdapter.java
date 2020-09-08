package mappedin.com.wayfindingsample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mappedin.sdk.Map;

/**
 * Created by Peter on 2018-02-23.
 */
class MapListAdapter extends ArrayAdapter<Map> {
    private MainActivity mainActivity;
    private Map[] maps;
    private int resource;
    private int selectedIndex;

    MapListAdapter(MainActivity mainActivity, Context context, Map[] maps) {
        super(context, R.layout.list_item_level, maps);
        this.mainActivity = mainActivity;
        this.maps = maps;
        this.resource = R.layout.list_item_level;
        selectedIndex = -1;
    }

    void setSelectedIndex(int index)
    {
        selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder mViewHolder;
        if (convertView == null) {
            mViewHolder = new ViewHolder();
            LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            if (layoutInflater != null) {
                convertView = layoutInflater.inflate(resource, null);
                mViewHolder.levelTextView = convertView.findViewById(R.id.level_text_view);
                convertView.setTag(mViewHolder);
            }
        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }
        if (selectedIndex != -1 && position == selectedIndex) {
            mViewHolder.levelTextView.setTextColor(getContext().getResources().getColor(R.color.colorSelectedText));
            mViewHolder.levelTextView.setBackground(getContext().getResources().getDrawable(R.drawable.level_picker_selected_bg));
        } else {
            mViewHolder.levelTextView.setTextColor(getContext().getResources().getColor(R.color.colorUnselectedText));
            mViewHolder.levelTextView.setBackground(getContext().getResources().getDrawable(R.drawable.level_picker_item));
        }
        Map map = maps[position];
        String name = map.getShortName();
        mViewHolder.levelTextView.setText(name);
        mViewHolder.levelTextView.setTypeface(mainActivity.robotoRegular);
        mViewHolder.position = position;
        return convertView;
    }

    static class ViewHolder {
        TextView levelTextView;
        int position;
    }
}
