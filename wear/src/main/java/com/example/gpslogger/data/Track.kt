package com.example.gpslogger.data

/**
 * Track data class (UI layer)
 */
data class Track(
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val pointCount: Int,
    val distanceMeters: Double,
    val isRecording: Boolean
)
