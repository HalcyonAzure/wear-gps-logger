package com.example.gpslogger.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.gpslogger.data.Track
import java.text.SimpleDateFormat
import java.util.*

/**
 * Track list item
 */
@Composable
fun TrackItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface
                )
                Text(
                    text = "${track.pointCount} pts \u00B7 ${formatDistance(track.distanceMeters)}",
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
            if (track.isRecording) {
                Text(
                    text = "\u25CF",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.primary
                )
            }
        }
    }
}
