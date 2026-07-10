package com.example.gpslogger.data

import android.content.Context
import android.location.Location
import com.example.gpslogger.GpsLoggerApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class TrackRepository private constructor(context: Context) {

    private val db: AppDatabase = GpsLoggerApp.instance.database
    private val locationDao: LocationDao = db.locationDao()
    private val trackDao: TrackDao = db.trackDao()

    companion object {
        @Volatile
        private var instance: TrackRepository? = null

        fun getInstance(context: Context): TrackRepository {
            return instance ?: synchronized(this) {
                instance ?: TrackRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    suspend fun startTrack(): Long = withContext(Dispatchers.IO) {
        val trackId = System.currentTimeMillis()
        val track = TrackEntity(
            id = trackId,
            name = formatTrackName(trackId),
            startTime = trackId,
            isRecording = true
        )
        trackDao.insertTrack(track)
        trackId
    }

    suspend fun stopTrack(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.markTrackAsStopped(trackId)
    }

    suspend fun saveLocation(trackId: Long, location: Location, batteryLevel: Int) = withContext(Dispatchers.IO) {
        val entity = LocationEntity(
            trackId = trackId,
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            accuracy = location.accuracy,
            speed = location.speed,
            timestamp = System.currentTimeMillis(),
            batteryLevel = batteryLevel
        )
        locationDao.insertLocationAndUpdateStats(entity, trackDao)
    }

    suspend fun getAllTracks(): List<Track> = withContext(Dispatchers.IO) {
        val trackEntities = trackDao.getAllTracks()
        trackEntities.map { entity ->
            Track(
                id = entity.id,
                name = entity.name,
                startTime = entity.startTime,
                endTime = entity.endTime,
                pointCount = entity.pointCount,
                distanceMeters = entity.totalDistanceMeters,
                isRecording = entity.isRecording
            )
        }
    }

    suspend fun getTrackLocations(trackId: Long): List<LocationEntity> = withContext(Dispatchers.IO) {
        locationDao.getLocationsByTrack(trackId)
    }

    suspend fun deleteTrack(trackId: Long) = withContext(Dispatchers.IO) {
        trackDao.deleteTrackWithLocations(trackId, locationDao)
    }

    fun getActiveTrackId(): Long? {
        return null
    }

    fun saveTrackId(trackId: Long) {
        // Using TrackEntity table
    }

    private fun formatTrackName(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp)) + " Track"
    }
}
