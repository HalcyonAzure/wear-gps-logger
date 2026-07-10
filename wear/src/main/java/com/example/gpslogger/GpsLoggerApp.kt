package com.example.gpslogger

import android.app.Application
import androidx.room.Room
import com.example.gpslogger.data.AppDatabase
import com.example.gpslogger.data.TrackRepository

class GpsLoggerApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TrackRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "gps_logger_db"
        ).fallbackToDestructiveMigration().build()

        repository = TrackRepository.getInstance(this)
    }

    companion object {
        lateinit var instance: GpsLoggerApp
            private set
    }
}
