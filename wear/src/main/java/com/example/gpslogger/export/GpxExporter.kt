package com.example.gpslogger.export

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.gpslogger.data.LocationEntity
import com.example.gpslogger.data.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GPX 1.1 format exporter
 *
 * Wear OS 导出策略:
 * - 文件写入 /sdcard/Documents/GPSLogger/ 目录
 * - 用户可通过以下方式获取:
 *   1. adb pull /sdcard/Documents/GPSLogger/xxx.gpx
 *   2. 手机文件管理器访问手表存储
 * - Wear OS 上的系统分享 Intent 几乎不可用，不依赖它
 */
object GpxExporter {

    private const val TAG = "GpxExporter"
    private const val EXPORT_DIR = "GPSLogger"

    sealed class ExportResult {
        data class Success(val file: File, val filePath: String) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    /**
     * 导出轨迹为 GPX 文件到外部存储
     * @return 导出结果，包含文件对象和可读路径
     */
    suspend fun exportToGpx(context: Context, track: Track, locations: List<LocationEntity>): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                if (locations.isEmpty()) {
                    return@withContext ExportResult.Error("No location points to export")
                }

                val gpxContent = generateGpxXml(track, locations)
                val fileName = generateFileName(track)

                // 写入外部存储 Documents/GPSLogger/ 目录
                // 这是手表上最容易通过 adb 访问的位置
                val exportDir = File(Environment.getExternalStorageDirectory(), "Documents/$EXPORT_DIR")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val file = File(exportDir, fileName)
                file.writeText(gpxContent)

                // 同时复制一份到 app 外部文件目录（备用）
                val appFile = File(context.getExternalFilesDir(null), fileName)
                appFile.writeText(gpxContent)

                Log.i(TAG, "GPX exported: ${file.absolutePath}, ${file.length()} bytes, ${locations.size} points")

                ExportResult.Success(file, file.absolutePath)
            } catch (e: IOException) {
                Log.e(TAG, "GPX export IO error", e)
                ExportResult.Error("IO error: ${e.localizedMessage}")
            } catch (e: SecurityException) {
                Log.e(TAG, "GPX export permission error", e)
                ExportResult.Error("Permission denied. Grant storage permission in Settings > Apps > GPS Logger > Permissions")
            } catch (e: Exception) {
                Log.e(TAG, "GPX export unknown error", e)
                ExportResult.Error("Error: ${e.localizedMessage}")
            }
        }

    /**
     * 获取已导出的 GPX 文件列表
     */
    fun getExportedFiles(): List<File> {
        val exportDir = File(Environment.getExternalStorageDirectory(), "Documents/$EXPORT_DIR")
        return if (exportDir.exists()) {
            exportDir.listFiles { f -> f.extension == "gpx" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * 删除已导出的文件
     */
    fun deleteExportedFile(fileName: String): Boolean {
        val file = File(Environment.getExternalStorageDirectory(), "Documents/$EXPORT_DIR/$fileName")
        return file.delete()
    }

    private fun generateGpxXml(track: Track, locations: List<LocationEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val avgSpeed = if (locations.size > 1) {
            val validSpeeds = locations.filter { it.speed > 0 }.map { it.speed.toDouble() }
            if (validSpeeds.isNotEmpty()) validSpeeds.average() else 0.0
        } else 0.0

        val durationSeconds = if (track.endTime != null) {
            (track.endTime - track.startTime) / 1000
        } else {
            (System.currentTimeMillis() - track.startTime) / 1000
        }

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<gpx version=\"1.1\" creator=\"WearOS GPS Logger\" xmlns=\"http://www.topografix.com/GPX/1/1\">")

        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <desc>${locations.size} points, ${String.format(Locale.US, "%.1f", track.distanceMeters)}m</desc>")
        sb.appendLine("    <time>${sdf.format(Date(track.startTime))}</time>")
        sb.appendLine("    <bounds minlat=\"${locations.minOf { it.latitude }}\" minlon=\"${locations.minOf { it.longitude }}\" maxlat=\"${locations.maxOf { it.latitude }}\" maxlon=\"${locations.maxOf { it.longitude }}\" />")
        sb.appendLine("  </metadata>")

        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <cmt>${String.format(Locale.US, "%.1f", track.distanceMeters)}m, ${String.format(Locale.US, "%.2f", avgSpeed)}m/s, ${durationSeconds}s</cmt>")
        sb.appendLine("    <trkseg>")

        locations.forEach { loc ->
            sb.appendLine("      <trkpt lat=\"${String.format(Locale.US, "%.7f", loc.latitude)}\" lon=\"${String.format(Locale.US, "%.7f", loc.longitude)}\">")
            sb.appendLine("        <ele>${String.format(Locale.US, "%.1f", loc.altitude)}</ele>")
            sb.appendLine("        <time>${sdf.format(Date(loc.timestamp))}</time>")
            if (loc.speed > 0) {
                sb.appendLine("        <speed>${String.format(Locale.US, "%.2f", loc.speed)}</speed>")
            }
            sb.appendLine("      </trkpt>")
        }

        sb.appendLine("    </trkseg>")
        sb.appendLine("  </trk>")
        sb.appendLine("</gpx>")

        return sb.toString()
    }

    private fun generateFileName(track: Track): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val dateStr = sdf.format(Date(track.startTime))
        return "track_${dateStr}.gpx"
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
