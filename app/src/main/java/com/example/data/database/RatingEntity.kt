package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ratings")
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val score: Int,
    val verdict: String,
    val emojis: String,
    val explanation: String,
    val versesJson: String, // Storing serialized JSON string of verses
    val timestamp: Long = System.currentTimeMillis()
)

data class BibleVerse(
    val reference: String,
    val text: String
)
