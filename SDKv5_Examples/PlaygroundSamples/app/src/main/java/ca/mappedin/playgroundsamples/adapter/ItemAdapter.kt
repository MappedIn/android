package ca.mappedin.playgroundsamples.adapter

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import ca.mappedin.playgroundsamples.examples.RenderMap
import ca.mappedin.playgroundsamples.model.Example


class ItemAdapter(private val context: Context, private val dataset: List<Example>,
                  private val onListItemClicked: (position: Int) -> Unit
) :
    RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    private fun onListItemClick(position: Int): Unit {
        Log.d("TAG", "CLICK $position is titled ${dataset[position]}")

    }

    class ItemViewHolder(private val view: View, private val onItemClicked: (position: Int) -> Unit):
         RecyclerView.ViewHolder(view), View.OnClickListener {
        init {
           view.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            val position = adapterPosition
            onItemClicked(position)
        }

        val titleTextView: TextView = view.findViewById(R.id.item_title)
        val descriptionTextView: TextView = view.findViewById(R.id.item_description)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ItemViewHolder(adapterLayout, onListItemClicked)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.titleTextView.text = item.title
        holder.descriptionTextView.text = item.description
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}