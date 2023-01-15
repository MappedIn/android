package ca.mappedin.playgroundsamples

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.adapter.ItemAdapter
import ca.mappedin.playgroundsamples.data.Datasource
import ca.mappedin.playgroundsamples.examples.RenderMap

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vaguely following https://developer.android.com/codelabs/basic-android-kotlin-training-recyclerview-scrollable-list#4

        val dataset = Datasource().loadExamples();

        fun onListItemClick(position: Int): Unit {
            Log.d(javaClass.simpleName, "$position clicked")
            val intent = Intent(this, dataset[position].example)
            this.startActivity(intent)
        }

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = ItemAdapter(this, dataset) { position -> onListItemClick(position) }
        recyclerView.setHasFixedSize(true)

    }
}