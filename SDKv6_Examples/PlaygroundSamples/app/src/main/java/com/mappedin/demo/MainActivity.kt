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
                "Cache Map Data",
                "Camera",
                "Colors & Textures",
                "Display a Map",
                "Image3D",
                "Interactivity",
                "Labels",
                "Locations",
                "Markers",
                "Models",
                "Multi-Floor View",
                "Navigation",
                "Offline Mode",
                "Paths",
                "Query",
                "Search",
                "Stacked Maps",
                "Text3D",
                "Turn by Turn Directions",
            )
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, demos)
        listView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> startActivity(Intent(this, AreaShapesDemoActivity::class.java))
                1 -> startActivity(Intent(this, BuildingFloorSelectionDemoActivity::class.java))
                2 -> startActivity(Intent(this, CacheMapDataDemoActivity::class.java))
                3 -> startActivity(Intent(this, CameraDemoActivity::class.java))
                4 -> startActivity(Intent(this, ColorsAndTexturesDemoActivity::class.java))
                5 -> startActivity(Intent(this, DisplayMapDemoActivity::class.java))
                6 -> startActivity(Intent(this, Image3DDemoActivity::class.java))
                7 -> startActivity(Intent(this, InteractivityDemoActivity::class.java))
                8 -> startActivity(Intent(this, LabelsDemoActivity::class.java))
                9 -> startActivity(Intent(this, LocationsDemoActivity::class.java))
                10 -> startActivity(Intent(this, MarkersDemoActivity::class.java))
                11 -> startActivity(Intent(this, ModelsDemoActivity::class.java))
                12 -> startActivity(Intent(this, MultiFloorViewDemoActivity::class.java))
                13 -> startActivity(Intent(this, NavigationDemoActivity::class.java))
                14 -> startActivity(Intent(this, OfflineModeDemoActivity::class.java))
                15 -> startActivity(Intent(this, PathsDemoActivity::class.java))
                16 -> startActivity(Intent(this, QueryDemoActivity::class.java))
                17 -> startActivity(Intent(this, SearchDemoActivity::class.java))
                18 -> startActivity(Intent(this, StackedMapsDemoActivity::class.java))
                19 -> startActivity(Intent(this, Text3DDemoActivity::class.java))
                20 -> startActivity(Intent(this, TurnByTurnDemoActivity::class.java))
            }
        }
    }
}
