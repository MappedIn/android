package com.mappedin.demo

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Disable edge-to-edge BEFORE calling super to ensure it takes effect
        WindowCompat.setDecorFitsSystemWindows(window, true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Mappedin SDK Samples"
        val listView = findViewById<ListView>(R.id.demo_list)

        val demos =
            listOf(
                "Areas & Shapes",
                "Building & Floor Selection",
                "Camera",
                "Display a Map",
                "Image3D",
                "Interactivity",
                "Labels",
                "Locations",
                "Markers",
                "Models",
                "Navigation",
                "Paths",
                "Query",
                "Search",
            )
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, demos)
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(this, AreaShapesDemoActivity::class.java))
                1 -> startActivity(Intent(this, BuildingFloorSelectionDemoActivity::class.java))
                2 -> startActivity(Intent(this, CameraDemoActivity::class.java))
                3 -> startActivity(Intent(this, DisplayMapDemoActivity::class.java))
                4 -> startActivity(Intent(this, Image3DDemoActivity::class.java))
                5 -> startActivity(Intent(this, InteractivityDemoActivity::class.java))
                6 -> startActivity(Intent(this, LabelsDemoActivity::class.java))
                7 -> startActivity(Intent(this, LocationsDemoActivity::class.java))
                8 -> startActivity(Intent(this, MarkersDemoActivity::class.java))
                9 -> startActivity(Intent(this, ModelsDemoActivity::class.java))
                10 -> startActivity(Intent(this, NavigationDemoActivity::class.java))
                11 -> startActivity(Intent(this, PathsDemoActivity::class.java))
                12 -> startActivity(Intent(this, QueryDemoActivity::class.java))
                13 -> startActivity(Intent(this, SearchDemoActivity::class.java))
            }
        }
    }
}
