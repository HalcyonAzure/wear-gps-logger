package com.example.gpslogger.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*

/**
 * StatusCard - GPS 状态卡片 (圆形屏幕优化版)
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
        modifier = modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight(),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .padding(end = 4.dp)
                                .background(
                                    color = MaterialTheme.colors.error,
                                    shape = CircleShape
                                )
                        )
                    }
                    Text(
                        text = when {
                            isRecording -> "REC"
                            pointCount > 0 -> "PAUSED"
                            else -> "READY"
                        },
                        style = MaterialTheme.typography.caption2,
                        color = when {
                            isRecording -> MaterialTheme.colors.error
                            pointCount > 0 -> MaterialTheme.colors.secondary
                            else -> MaterialTheme.colors.onSurfaceVariant
                        },
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = formatElapsedTime(elapsedTime),
                    style = MaterialTheme.typography.title2,
                    color = if (isRecording) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatItem(value = formatDistance(distance), label = "km")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    StatItem(value = "$pointCount", label = "pts")
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    BatteryIndicator(level = batteryLevel)
                }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BatteryIndicator(level: Int, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(8.dp)
                .background(
                    color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(2.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width((14 * level / 100).dp.coerceAtLeast(2.dp))
                    .background(
                        color = when {
                            level > 50 -> MaterialTheme.colors.primary
                            level > 20 -> MaterialTheme.colors.secondary
                            else -> MaterialTheme.colors.error
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
        Text(
            text = "$level%",
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

fun formatDistance(meters: Double): String {
    return when {
        meters < 1000 -> "%.0f".format(meters)
        else -> "%.1f".format(meters / 1000)
    }
}

fun formatElapsedTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
