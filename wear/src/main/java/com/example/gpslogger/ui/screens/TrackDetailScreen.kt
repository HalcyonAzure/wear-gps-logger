package com.example.gpslogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.data.Track
import com.example.gpslogger.ui.components.StatRow
import com.example.gpslogger.ui.components.formatDistance
import com.example.gpslogger.ui.components.formatElapsedTime
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import com.example.gpslogger.GpsLoggerApp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Track detail screen
 */
@Composable
fun TrackDetailScreen(
    trackId: Long,
    onNavigateBack: () -> Unit
) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(GpsLoggerApp.instance)
    )
    val tracks by viewModel.tracks.observeAsState(emptyList())
    val track = tracks.find { it.id == trackId }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (track != null && !showDeleteConfirm) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary
                )

                Card(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        StatRow(label = "Start", value = dateFormat.format(Date(track.startTime)))
                        track.endTime?.let {
                            StatRow(label = "End", value = dateFormat.format(Date(it)))
                        }
                        StatRow(label = "Points", value = "${track.pointCount}")
                        StatRow(label = "Distance", value = formatDistance(track.distanceMeters))
                        if (!track.isRecording && track.endTime != null) {
                            val duration = track.endTime - track.startTime
                            StatRow(label = "Duration", value = formatElapsedTime(duration))
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportTrack(track) },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.secondary
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Text("Share", style = MaterialTheme.typography.caption3)
                    }

                    Button(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error
                        ),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Text("Del", style = MaterialTheme.typography.caption3)
                    }
                }

                CompactButton(onClick = onNavigateBack) {
                    Text(text = "Back")
                }
            } else if (showDeleteConfirm && track != null) {
                // Simple delete confirmation
                Text(
                    text = "Delete?",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.error
                )
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.body2
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.deleteTrack(track.id)
                            showDeleteConfirm = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.error
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("Yes", style = MaterialTheme.typography.caption3)
                    }
                    Button(
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Text("No", style = MaterialTheme.typography.caption3)
                    }
                }
            } else {
                Text(
                    text = "Track not found",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error
                )
                CompactButton(onClick = onNavigateBack) {
                    Text(text = "Back")
                }
            }
        }
    }
}
