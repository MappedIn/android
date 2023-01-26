package ca.mappedin.playgroundsamples

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import ca.mappedin.playgroundsamples.adapter.ExampleAdapter
import ca.mappedin.playgroundsamples.data.Datasource

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vaguely following https://developer.android.com/codelabs/basic-android-kotlin-training-recyclerview-scrollable-list#4

        val dataset = Datasource().loadExamples()

        fun onListItemClick(position: Int) {
            Log.d(javaClass.simpleName, "$position clicked")
            val intent = Intent(this, dataset[position].example)
            this.startActivity(intent)
        }

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = ExampleAdapter(dataset) { position -> onListItemClick(position) }
        recyclerView.setHasFixedSize(true)
    }
}
