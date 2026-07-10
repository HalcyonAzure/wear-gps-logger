package com.example.gpslogger.export

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
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
 * Compliant with http://www.topografix.com/GPX/1/1/gpx.xsd
 */
object GpxExporter {

    private const val TAG = "GpxExporter"

    sealed class ExportResult {
        data class Success(val file: File) : ExportResult()
        data class Error(val exception: Throwable) : ExportResult()
    }

    suspend fun exportToGpx(context: Context, track: Track, locations: List<LocationEntity>): ExportResult =
        withContext(Dispatchers.IO) {
            try {
                if (locations.isEmpty()) {
                    return@withContext ExportResult.Error(IllegalArgumentException("No location points to export"))
                }
                val gpxContent = generateGpxXml(track, locations)
                val fileName = generateFileName(track)
                val file = File(context.getExternalFilesDir(null), fileName)
                file.writeText(gpxContent)
                Log.i(TAG, "GPX exported: ${file.absolutePath}, size: ${file.length()} bytes")
                ExportResult.Success(file)
            } catch (e: IOException) {
                Log.e(TAG, "GPX export IO error", e)
                ExportResult.Error(e)
            } catch (e: SecurityException) {
                Log.e(TAG, "GPX export permission error", e)
                ExportResult.Error(e)
            } catch (e: Exception) {
                Log.e(TAG, "GPX export unknown error", e)
                ExportResult.Error(e)
            }
        }

    private fun generateGpxXml(track: Track, locations: List<LocationEntity>): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")

        val avgSpeed = if (locations.size > 1) {
            val totalSpeed = locations.filter { it.speed > 0 }.sumOf { it.speed.toDouble() }
            val count = locations.count { it.speed > 0 }
            if (count > 0) totalSpeed / count else 0.0
        } else 0.0

        val durationSeconds = if (track.endTime != null) {
            (track.endTime - track.startTime) / 1000
        } else {
            (System.currentTimeMillis() - track.startTime) / 1000
        }

        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<gpx version=\"1.1\" creator=\"WearOS GPS Logger\" xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">")

        sb.appendLine("  <metadata>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <desc>WearOS GPS Logger track, ${locations.size} points, total distance ${String.format(Locale.US, "%.2f", track.distanceMeters)} meters</desc>")
        sb.appendLine("    <author>")
        sb.appendLine("      <name>WearOS GPS Logger</name>")
        sb.appendLine("    </author>")
        sb.appendLine("    <time>${sdf.format(Date(track.startTime))}</time>")
        sb.appendLine("    <bounds minlat=\"${locations.minOf { it.latitude }}\" minlon=\"${locations.minOf { it.longitude }}\" maxlat=\"${locations.maxOf { it.latitude }}\" maxlon=\"${locations.maxOf { it.longitude }}\" />")
        sb.appendLine("  </metadata>")

        sb.appendLine("  <trk>")
        sb.appendLine("    <name>${escapeXml(track.name)}</name>")
        sb.appendLine("    <cmt>Distance: ${String.format(Locale.US, "%.2f", track.distanceMeters)}m, avg speed: ${String.format(Locale.US, "%.2f", avgSpeed)}m/s, duration: ${durationSeconds}s</cmt>")
        sb.appendLine("    <desc>${track.name} - ${locations.size} track points</desc>")
        sb.appendLine("    <trkseg>")

        locations.forEach { loc ->
            sb.appendLine("      <trkpt lat=\"${String.format(Locale.US, "%.7f", loc.latitude)}\" lon=\"${String.format(Locale.US, "%.7f", loc.longitude)}\">")
            sb.appendLine("        <ele>${String.format(Locale.US, "%.2f", loc.altitude)}</ele>")
            sb.appendLine("        <time>${sdf.format(Date(loc.timestamp))}</time>")
            if (loc.speed > 0) {
                sb.appendLine("        <speed>${String.format(Locale.US, "%.3f", loc.speed)}</speed>")
            }
            if (loc.accuracy > 0) {
                sb.appendLine("        <sat>${String.format(Locale.US, "%.1f", loc.accuracy)}</sat>")
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
        val sanitizedName = track.name.replace("[^a-zA-Z0-9\\u4e00-\\u9fa5_-]".toRegex(), "_")
        return "track_${dateStr}_${sanitizedName}.gpx"
    }

    fun shareGpxFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/gpx+xml"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Share GPX track")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share GPX file")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share GPX file", e)
        }
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
