package com.example.gpslogger.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.GpsLoggerApp
import com.example.gpslogger.data.Track
import com.example.gpslogger.ui.components.formatDistance
import com.example.gpslogger.ui.components.formatElapsedTime
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

enum class ExportStatus { Idle, Exporting, Success, Error }

/**
 * TrackDetailScreen - 轨迹详情 (圆形屏幕优化版)
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
    var exportStatus by remember { mutableStateOf(ExportStatus.Idle) }
    var exportPath by remember { mutableStateOf("") }

    val listState = rememberScalingLazyListState()
    val shortDateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            if (showDeleteConfirm && track != null) {
                // Delete confirmation - replaces Dialog (not in Wear OS)
                item {
                    Text(
                        text = "Delete?",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
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
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text("Del", style = MaterialTheme.typography.caption2)
                        }
                        Button(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary
                            ),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text("No", style = MaterialTheme.typography.caption2)
                        }
                    }
                }
            } else if (track != null) {
                item {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    TrackStatsCard(track = track, shortDateFormat = shortDateFormat)
                }

                if (exportStatus != ExportStatus.Idle) {
                    item {
                        ExportStatusIndicator(status = exportStatus, exportPath = exportPath)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                exportStatus = ExportStatus.Exporting
                                viewModel.exportTrack(track) { path, success ->
                                    exportPath = path
                                    exportStatus = if (success) ExportStatus.Success else ExportStatus.Error
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Text("Export", style = MaterialTheme.typography.caption2, maxLines = 1)
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.error
                            ),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Text("Del", style = MaterialTheme.typography.caption2, maxLines = 1)
                        }
                    }
                }

                item {
                    Chip(
                        onClick = onNavigateBack,
                        label = { Text("Back") },
                        modifier = Modifier.fillMaxWidth(0.75f),
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            } else {
                item {
                    Text(
                        text = "Track not found",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.error
                    )
                }
                item {
                    Chip(
                        onClick = onNavigateBack,
                        label = { Text("Back") },
                        modifier = Modifier.fillMaxWidth(0.75f),
                        colors = ChipDefaults.secondaryChipColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackStatsCard(
    track: Track,
    shortDateFormat: SimpleDateFormat,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { },
        modifier = modifier.fillMaxWidth(0.92f).wrapContentHeight(),
        backgroundPainter = CardDefaults.cardBackgroundPainter(
            startBackgroundColor = MaterialTheme.colors.surface,
            endBackgroundColor = MaterialTheme.colors.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${formatDistance(track.distanceMeters)} km",
                style = MaterialTheme.typography.title2,
                color = MaterialTheme.colors.primary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "${track.pointCount} points",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Box(
                modifier = Modifier.fillMaxWidth(0.8f).height(1.dp)
                    .background(MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.2f))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DateTimeItem(label = "Start", value = shortDateFormat.format(Date(track.startTime)))
                track.endTime?.let {
                    DateTimeItem(label = "End", value = shortDateFormat.format(Date(it)))
                }
            }
            track.endTime?.let {
                Text(
                    text = "Duration: ${formatElapsedTime(it - track.startTime)}",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } ?: run {
                Text(
                    text = "Ongoing: ${formatElapsedTime(System.currentTimeMillis() - track.startTime)}",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DateTimeItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.caption3, color = MaterialTheme.colors.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onSurface, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ExportStatusIndicator(status: ExportStatus, exportPath: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        when (status) {
            ExportStatus.Exporting -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, indicatorColor = MaterialTheme.colors.primary)
                    Text(text = "Exporting...", style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onSurfaceVariant)
                }
            }
            ExportStatus.Success -> {
                Text(text = "\u2713 Exported", style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.primary)
            }
            ExportStatus.Error -> {
                Text(
                    text = exportPath,
                    style = MaterialTheme.typography.caption3,
                    color = MaterialTheme.colors.error,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            else -> { }
        }
    }
}
