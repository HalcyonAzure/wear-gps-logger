package com.example.gpslogger.service

import android.app.*
import android.content.*
import android.location.Location
import android.os.*
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import com.example.gpslogger.R
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import com.example.gpslogger.data.TrackRepository
import com.example.gpslogger.GpsLoggerApp
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter

/**
 * GPS tracking background service
 *
 * Low-power strategies:
 * - PRIORITY_BALANCED_POWER_ACCURACY
 * - 10s interval / 15m distance (screen on), 30s/50m (screen off)
 * - Foreground service with low-priority notification
 * - PARTIAL_WAKE_LOCK to prevent Doze interruption
 */
class GpsTrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock
    private val repository: TrackRepository by lazy { GpsLoggerApp.instance.repository }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _locationCount = MutableLiveData(0)
    val locationCount: LiveData<Int> = _locationCount

    private val _currentDistance = MutableLiveData(0.0)
    val currentDistance: LiveData<Double> = _currentDistance

    private val _elapsedTime = MutableLiveData(0L)
    val elapsedTime: LiveData<Long> = _elapsedTime

    private var trackId: Long = 0L
    private var locationCountValue = 0
    private var distanceValue = 0.0
    private var startTime = 0L
    private var timerJob: Job? = null
    private var lastLocation: Location? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> setLowPowerMode(true)
                Intent.ACTION_SCREEN_ON -> setLowPowerMode(false)
            }
        }
    }
    private var isLowPowerMode = false

    companion object {
        const val ACTION_START = "com.example.gpslogger.ACTION_START"
        const val ACTION_STOP = "com.example.gpslogger.ACTION_STOP"
        const val CHANNEL_ID = "gps_tracking_channel"
        const val NOTIFICATION_ID = 1001
        private const val WAKE_LOCK_TIMEOUT_MS = 60 * 60 * 1000L

        fun bind(context: Context, connection: ServiceConnection) {
            val intent = Intent(context, GpsTrackingService::class.java)
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        fun unbind(context: Context, connection: ServiceConnection) {
            context.unbindService(connection)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): GpsTrackingService = this@GpsTrackingService
    }

    private var localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = createLocationCallback()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "GpsLogger::TrackingWakeLock"
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        createNotificationChannel()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return localBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        if (_isRecording.value == true) return

        trackId = System.currentTimeMillis()
        locationCountValue = 0
        distanceValue = 0.0
        startTime = System.currentTimeMillis()
        lastLocation = null

        _isRecording.postValue(true)
        _locationCount.postValue(0)
        _currentDistance.postValue(0.0)
        _elapsedTime.postValue(0L)

        repository.saveTrackId(trackId)

        startForeground(NOTIFICATION_ID, buildNotification())

        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        requestLocationUpdates()
        startTimer()
        updateNotification()
    }

    private fun stopTracking() {
        if (_isRecording.value != true) return

        _isRecording.postValue(false)
        fusedLocationClient.removeLocationUpdates(locationCallback)
        timerJob?.cancel()

        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        stopForeground(STOP_FOREGROUND_REMOVE)

        serviceScope.launch {
            repository.stopTrack(trackId)
        }
        stopSelf()
    }

    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onNewLocation(location)
            }
        }
    }

    private fun onNewLocation(location: Location) {
        if (location.accuracy > 100f) return

        lastLocation?.let { prev ->
            val results = FloatArray(1)
            Location.distanceBetween(
                prev.latitude, prev.longitude,
                location.latitude, location.longitude,
                results
            )
            distanceValue += results[0]
            _currentDistance.postValue(distanceValue)
        }
        lastLocation = location

        locationCountValue++
        _locationCount.postValue(locationCountValue)

        serviceScope.launch {
            try {
                val batteryLevel = getBatteryLevel()
                repository.saveLocation(trackId, location, batteryLevel)
            } catch (e: Exception) {
                // Silently handle database errors
            }
        }

        if (locationCountValue % 10 == 0) {
            updateNotification()
        }
    }

    private fun requestLocationUpdates() {
        val request = buildLocationRequest(isLowPower = false)
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            stopTracking()
        }
    }

    private fun buildLocationRequest(isLowPower: Boolean): LocationRequest {
        val interval = if (isLowPower) 30000L else 10000L
        val minDistance = if (isLowPower) 50f else 15f

        return LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            interval
        ).apply {
            setMinUpdateDistanceMeters(minDistance)
            setWaitForAccurateLocation(true)
            setMinUpdateIntervalMillis(interval / 2)
        }.build()
    }

    private fun setLowPowerMode(enabled: Boolean) {
        if (isLowPowerMode == enabled) return
        isLowPowerMode = enabled

        if (_isRecording.value == true) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            val request = buildLocationRequest(isLowPower = enabled)
            try {
                fusedLocationClient.requestLocationUpdates(
                    request,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) {}
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _isRecording.value == true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime
                _elapsedTime.postValue(elapsed)
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            -1
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background GPS track recording"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val contentText = if (_isRecording.value == true) {
            "${locationCountValue} pts | ${formatDistance(distanceValue)}"
        } else {
            "GPS Logger"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Logger")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {}
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
}
