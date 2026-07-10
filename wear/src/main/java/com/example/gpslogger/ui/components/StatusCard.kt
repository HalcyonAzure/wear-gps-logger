package com.example.gpslogger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Status display card
 */
@Composable
fun StatusCard(
    isRecording: Boolean,
    pointCount: Int,
    distance: Double,
    elapsedTime: Long,
    batteryLevel: Int,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { },
        enabled = false,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusText = when {
                isRecording -> "Recording \u25CF"
                pointCount > 0 -> "Stopped"
                else -> "Ready"
            }
            val statusColor = when {
                isRecording -> MaterialTheme.colors.primary
                pointCount > 0 -> MaterialTheme.colors.secondary
                else -> MaterialTheme.colors.onSurfaceVariant
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.title3,
                color = statusColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            StatRow(label = "Points", value = "$pointCount")
            StatRow(label = "Distance", value = formatDistance(distance))
            StatRow(label = "Time", value = formatElapsedTime(elapsedTime))
            StatRow(label = "Battery", value = "$batteryLevel%")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurface
        )
    }
}

fun formatDistance(meters: Double): String {
    return if (meters >= 1000) {
        String.format("%.2f km", meters / 1000)
    } else {
        String.format("%.0f m", meters)
    }
}

fun formatElapsedTime(ms: Long): String {
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
