package ca.mappedin.playgroundsamples.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.models.MPINavigatable

class LocationAdapter(private val dataset: List<MPINavigatable.MPILocation>) : RecyclerView.Adapter<LocationAdapter.ItemViewHolder>() {
    class ItemViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val titleTextView: TextView = view.findViewById(R.id.item_title)
        val descriptionTextView: TextView = view.findViewById(R.id.item_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.titleTextView.text = item.name
        holder.descriptionTextView.text = item.description
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}
