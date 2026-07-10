package com.example.gpslogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface LocationDao {

    @Insert
    suspend fun insert(location: LocationEntity): Long

    @Insert
    suspend fun insertAll(locations: List<LocationEntity>)

    @Query("SELECT * FROM locations WHERE trackId = :trackId ORDER BY timestamp ASC")
    suspend fun getLocationsByTrack(trackId: Long): List<LocationEntity>

    @Query("SELECT COUNT(*) FROM locations WHERE trackId = :trackId")
    suspend fun getLocationCount(trackId: Long): Int

    @Query("DELETE FROM locations WHERE trackId = :trackId")
    suspend fun deleteLocationsByTrack(trackId: Long)

    @Query("SELECT * FROM locations WHERE trackId = :trackId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastLocation(trackId: Long): LocationEntity?

    @Query("SELECT DISTINCT trackId FROM locations")
    suspend fun getAllTrackIds(): List<Long>

    @Query("SELECT * FROM locations WHERE trackId = :trackId ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstLocation(trackId: Long): LocationEntity?

    @Transaction
    suspend fun insertLocationAndUpdateStats(
        location: LocationEntity,
        trackDao: TrackDao
    ) {
        insert(location)
        val count = getLocationCount(location.trackId)
        val locations = getLocationsByTrack(location.trackId)
        val distance = calculateDistance(locations)
        trackDao.updateTrackStats(location.trackId, distance, count)
    }

    private fun calculateDistance(locations: List<LocationEntity>): Double {
        if (locations.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until locations.size) {
            val prev = locations[i - 1]
            val curr = locations[i]
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                prev.latitude, prev.longitude,
                curr.latitude, curr.longitude,
                results
            )
            total += results[0]
        }
        return total
    }
}
