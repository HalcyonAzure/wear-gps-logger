package com.example.gpslogger.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Location record entity
 * Each record represents one GPS sample point
 */
@Entity(
    tableName = "locations",
    indices = [
        Index(value = ["trackId"]),
        Index(value = ["timestamp"])
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trackId: Long,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Float,
    val speed: Float,
    val timestamp: Long,
    val batteryLevel: Int
)
