package com.example.gpslogger.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks ORDER BY startTime DESC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: Long): TrackEntity?

    @Query("SELECT * FROM tracks WHERE isRecording = 1 LIMIT 1")
    suspend fun getActiveTrack(): TrackEntity?

    @Query("UPDATE tracks SET isRecording = 0, endTime = :endTime WHERE id = :trackId")
    suspend fun markTrackAsStopped(trackId: Long, endTime: Long = System.currentTimeMillis())

    @Query("UPDATE tracks SET totalDistanceMeters = :distance, pointCount = :count WHERE id = :trackId")
    suspend fun updateTrackStats(trackId: Long, distance: Double, count: Int)

    @Query("DELETE FROM tracks WHERE id = :trackId")
    suspend fun deleteTrack(trackId: Long)

    @Transaction
    suspend fun deleteTrackWithLocations(trackId: Long, locationDao: LocationDao) {
        locationDao.deleteLocationsByTrack(trackId)
        deleteTrack(trackId)
    }
}
