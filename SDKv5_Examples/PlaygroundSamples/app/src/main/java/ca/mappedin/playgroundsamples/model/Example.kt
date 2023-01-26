package ca.mappedin.playgroundsamples.model

import androidx.appcompat.app.AppCompatActivity

data class Example(
    val title: String,
    val description: String,
    val example: Class<out AppCompatActivity>
)
