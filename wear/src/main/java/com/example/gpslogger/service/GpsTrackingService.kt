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
 * GPS 轨迹记录后台服务
 *
 * 低功耗策略 — 纯时间间隔采样 (GPS Logger 风格):
 * - 每隔固定时间打开一次 GPS，打点完成后立刻关闭 GPS 硬件
 * - 亮屏: 60 秒间隔 (保证轨迹平滑)
 * - 息屏: 120 秒间隔 (最大化省电)
 * - 不使用最小位移过滤，纯定时采样让 GPS 硬件有最长的休眠时间
 * - PRIORITY_BALANCED_POWER_ACCURACY: 平衡精度与电量
 * - Foreground Service 保活，低优先级通知
 */
class GpsTrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var wakeLock: PowerManager.WakeLock
    private val repository: TrackRepository by lazy { GpsLoggerApp.instance.repository }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 状态 LiveData (供 UI 绑定观察)
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

    // 息屏广播接收器
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

        // 唤醒锁超时: 1 小时（合理值，避免长时间持有导致电量耗尽）
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

        // 注册息屏广播
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

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

    /**
     * 开始轨迹记录
     */
    private fun startTracking() {
        if (_isRecording.value == true) return

        locationCountValue = 0
        distanceValue = 0.0
        startTime = System.currentTimeMillis()
        lastLocation = null

        _isRecording.postValue(true)
        _locationCount.postValue(0)
        _currentDistance.postValue(0.0)
        _elapsedTime.postValue(0L)

        // 在数据库中创建轨迹记录
        serviceScope.launch {
            trackId = repository.startTrack()
        }

        // 启动前台服务
        startForeground(NOTIFICATION_ID, buildNotification())

        // 获取唤醒锁（1小时超时，防止意外持有导致电量耗尽）
        if (!wakeLock.isHeld) {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        }

        // 请求位置更新
        requestLocationUpdates()

        // 启动计时器
        startTimer()

        // 更新通知为记录中状态
        updateNotification()
    }

    /**
     * 停止轨迹记录
     */
    private fun stopTracking() {
        if (_isRecording.value != true) return

        _isRecording.postValue(false)

        // 停止位置更新
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // 停止计时器
        timerJob?.cancel()

        // 释放唤醒锁
        if (wakeLock.isHeld) {
            wakeLock.release()
        }

        // 停止前台服务并移除通知
        stopForeground(STOP_FOREGROUND_REMOVE)

        // 保存结束标志
        serviceScope.launch {
            repository.stopTrack(trackId)
        }

        // 停止服务自身
        stopSelf()
    }

    /**
     * 创建位置回调
     */
    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                onNewLocation(location)
            }
        }
    }

    /**
     * 处理新位置
     */
    private fun onNewLocation(location: Location) {
        // 过滤低精度位置 (>100m 精度丢弃)
        if (location.accuracy > 100f) return

        // 计算距离
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

        // 保存到数据库
        serviceScope.launch {
            try {
                val batteryLevel = getBatteryLevel()
                repository.saveLocation(trackId, location, batteryLevel)
            } catch (e: Exception) {
                // 静默处理数据库错误，不中断记录
            }
        }

        // 更新通知 (每 10 个点更新一次，减少电量消耗)
        if (locationCountValue % 10 == 0) {
            updateNotification()
        }
    }

    /**
     * 请求位置更新
     */
    private fun requestLocationUpdates() {
        val request = buildLocationRequest(isLowPower = false)
        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // 权限未授予
            stopTracking()
        }
    }

    /**
     * 构建位置请求 — 纯时间间隔采样 (GPS Logger 风格)
     *
     * 核心省电原理:
     * - 每隔固定时间才唤醒 GPS 硬件获取一次定位
     * - 获取完成后 GPS 硬件立刻进入休眠
     * - 不设最小位移限制，纯定时驱动
     * - 息屏时加倍间隔，最大化 GPS 休眠时间
     */
    private fun buildLocationRequest(isLowPower: Boolean): LocationRequest {
        // 纯时间间隔: 亮屏 60s / 息屏 120s
        // GPS 硬件只在采样时唤醒，其余时间完全关闭
        val interval = if (isLowPower) 120_000L else 60_000L

        return LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            interval
        ).apply {
            // 不设置最小位移，纯时间驱动
            // 这样每次定时只打一次点，GPS 休眠时间最大化
            setMinUpdateDistanceMeters(0f)
            setWaitForAccurateLocation(true)
            setMinUpdateIntervalMillis(interval)
        }.build()
    }

    /**
     * 切换低功耗模式 (息屏时调用)
     * 正确实现：移除旧的位置请求，用新的参数重新请求
     */
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

    /**
     * 启动计时器
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _isRecording.value == true) {
                delay(1000)
                val elapsed = System.currentTimeMillis() - startTime
                _elapsedTime.postValue(elapsed)
                // 每秒更新通知（包含计时信息），但限制实际通知更新频率
                if (locationCountValue % 10 == 0) {
                    updateNotification()
                }
            }
        }
    }

    /**
     * 获取当前电量百分比
     */
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

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS 轨迹记录",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于后台 GPS 轨迹记录"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 构建通知 — 丰富信息显示：点数、距离、时长、电量
     */
    private fun buildNotification(): Notification {
        val contentText = if (_isRecording.value == true) {
            val elapsed = System.currentTimeMillis() - startTime
            val battery = getBatteryLevel()
            val batteryStr = if (battery >= 0) "电量 ${battery}%" else ""
            "${locationCountValue} 点 · ${formatDistance(distanceValue)} · ${formatElapsedTime(elapsed)}${if (batteryStr.isNotEmpty()) " · $batteryStr" else ""}"
        } else {
            "GPS 轨迹记录器就绪"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GPS Logger")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * 更新通知
     */
    private fun updateNotification() {
        val notification = buildNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 格式化距离显示
     */
    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) {
            String.format("%.2f km", meters / 1000)
        } else {
            String.format("%.0f m", meters)
        }
    }

    /**
     * 格式化时长显示
     */
    private fun formatElapsedTime(ms: Long): String {
        val seconds = ms / 1000
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
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
