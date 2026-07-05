package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val seriesName: String?,
    val episodeName: String?,
    val seasonNumber: Int?,
    val episodeNumber: Int?,
    val uploader: String?,
    val quality: String?,
    val format: String?,
    val downloadUrl: String?,
    val watchUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)
