package com.example.gpslogger.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.example.gpslogger.data.Track
import java.text.SimpleDateFormat
import java.util.*

/**
 * TrackItem - 轨迹列表项 (圆形屏幕优化版)
 */
@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shortDateFormat = remember { SimpleDateFormat("MM/dd", Locale.getDefault()) }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        ),
        enabled = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(formatDistance(track.distanceMeters))
                        append(" km \u00B7 ")
                        append(track.pointCount)
                        append(" pts \u00B7 ")
                        append(shortDateFormat.format(Date(track.startTime)))
                    },
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (track.isRecording) {
                RecordingIndicator()
            }
        }
    }
}

@Composable
private fun RecordingIndicator(modifier: Modifier = Modifier) {
    val pulseAlpha by animateFloatAsState(
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rec_pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    color = MaterialTheme.colors.error.copy(alpha = 0.6f + pulseAlpha * 0.4f),
                    shape = CircleShape
                )
        )
        Text(
            text = "REC",
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.error
        )
    }
}
