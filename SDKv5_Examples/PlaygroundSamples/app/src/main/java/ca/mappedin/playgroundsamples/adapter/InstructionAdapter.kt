package ca.mappedin.playgroundsamples.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.R
import com.mappedin.sdk.models.MPIDirections

class InstructionAdapter(private val dataset: List<MPIDirections.MPIInstruction>) : RecyclerView.Adapter<InstructionAdapter.ItemViewHolder>() {
    class ItemViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val instructionTextView: TextView = view.findViewById(R.id.instruction)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_instruction_item, parent, false)
        return ItemViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val item = dataset[position]
        holder.instructionTextView.text = item.instruction
    }

    override fun getItemCount(): Int {
        return dataset.size
    }
}
