package com.example.gpslogger.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.*
import com.example.gpslogger.data.Track
import com.example.gpslogger.data.TrackRepository
import com.example.gpslogger.export.GpxExporter
import com.example.gpslogger.service.GpsTrackingService
import com.example.gpslogger.GpsLoggerApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Intent
import android.os.BatteryManager
import android.content.IntentFilter

/**
 * Main ViewModel
 * Manages GPS recording state and UI data
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrackRepository = GpsLoggerApp.instance.repository

    private var trackingService: GpsTrackingService? = null
    private var isBound = false

    private val _isRecording = MutableLiveData(false)
    val isRecording: LiveData<Boolean> = _isRecording

    private val _pointCount = MutableLiveData(0)
    val pointCount: LiveData<Int> = _pointCount

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> = _distance

    private val _elapsedTime = MutableLiveData(0L)
    val elapsedTime: LiveData<Long> = _elapsedTime

    private val _batteryLevel = MutableLiveData(100)
    val batteryLevel: LiveData<Int> = _batteryLevel

    private val _tracks = MutableLiveData<List<Track>>(emptyList())
    val tracks: LiveData<List<Track>> = _tracks

    private val _exportResult = MutableLiveData<String?>(null)
    val exportResult: LiveData<String?> = _exportResult

    // Store Observer references to prevent memory leaks
    private val serviceObservers = mutableListOf<Pair<LiveData<*>, Observer<*>>>()

    private inline fun <T> observeServiceLiveData(
        liveData: LiveData<T>,
        crossinline onChanged: (T) -> Unit
    ) {
        val observer = Observer<T> { onChanged(it) }
        serviceObservers.add(liveData to observer)
        liveData.observeForever(observer)
    }

    private fun clearServiceObservers() {
        serviceObservers.forEach { (liveData, observer) ->
            @Suppress("UNCHECKED_CAST")
            (liveData as LiveData<Any>).removeObserver(observer as Observer<Any>)
        }
        serviceObservers.clear()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as GpsTrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true

            observeServiceLiveData(trackingService!!.isRecording) { _isRecording.value = it }
            observeServiceLiveData(trackingService!!.locationCount) { _pointCount.value = it }
            observeServiceLiveData(trackingService!!.currentDistance) { _currentDistance.value = it }
            observeServiceLiveData(trackingService!!.elapsedTime) { _elapsedTime.value = it }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            clearServiceObservers()
            trackingService = null
            isBound = false
        }
    }

    init {
        val activeId = repository.getActiveTrackId()
        if (activeId != null) {
            bindService()
        }
        refreshBatteryLevel()
        loadTracks()
    }

    fun startTracking() {
        val context = getApplication<Application>()
        val intent = Intent(context, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_START
        }
        context.startForegroundService(intent)
        bindService()
        refreshBatteryLevel()
    }

    fun stopTracking() {
        val context = getApplication<Application>()
        val intent = Intent(context, GpsTrackingService::class.java).apply {
            action = GpsTrackingService.ACTION_STOP
        }
        context.startService(intent)

        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            unbindService()
            _isRecording.value = false
            loadTracks()
        }
    }

    fun loadTracks() {
        viewModelScope.launch {
            try {
                val trackList = repository.getAllTracks()
                _tracks.value = trackList
            } catch (e: Exception) {
                _tracks.value = emptyList()
            }
        }
    }

    fun exportTrack(track: Track) {
        viewModelScope.launch {
            try {
                val locations = repository.getTrackLocations(track.id)
                when (val result = GpxExporter.exportToGpx(getApplication(), track, locations)) {
                    is GpxExporter.ExportResult.Success -> {
                        _exportResult.value = "Exported: ${result.file.name}"
                        GpxExporter.shareGpxFile(getApplication(), result.file)
                    }
                    is GpxExporter.ExportResult.Error -> {
                        _exportResult.value = "Export failed: ${result.exception.localizedMessage}"
                    }
                }
            } catch (e: Exception) {
                _exportResult.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteTrack(trackId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteTrack(trackId)
                loadTracks()
            } catch (_: Exception) {}
        }
    }

    fun clearExportResult() {
        _exportResult.value = null
    }

    fun refreshBatteryLevel() {
        val context = getApplication<Application>()
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        _batteryLevel.value = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    private fun bindService() {
        if (isBound) return
        try {
            GpsTrackingService.bind(getApplication(), serviceConnection)
        } catch (_: Exception) {}
    }

    private fun unbindService() {
        if (!isBound) return
        try {
            clearServiceObservers()
            GpsTrackingService.unbind(getApplication(), serviceConnection)
            isBound = false
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            unbindService()
        }
    }
}

@Suppress("UNCHECKED_CAST")
class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
