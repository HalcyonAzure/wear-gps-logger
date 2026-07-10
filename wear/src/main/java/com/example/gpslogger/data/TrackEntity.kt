package com.example.gpslogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Track metadata entity
 * Stores track info to avoid inferring from location points
 */
@Entity(
    tableName = "tracks",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["isRecording"])
    ]
)
data class TrackEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val isRecording: Boolean = true,
    val totalDistanceMeters: Double = 0.0,
    val pointCount: Int = 0
)
