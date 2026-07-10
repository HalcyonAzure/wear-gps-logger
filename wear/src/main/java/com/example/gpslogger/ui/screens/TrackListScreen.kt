package com.example.gpslogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.GpsLoggerApp
import com.example.gpslogger.ui.components.TrackItem
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory

/**
 * TrackListScreen - 轨迹列表 (圆形屏幕优化版)
 */
@Composable
fun TrackListScreen(
    onNavigateBack: () -> Unit,
    onTrackClick: (Long) -> Unit
) {
    val viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(GpsLoggerApp.instance)
    )
    val tracks by viewModel.tracks.observeAsState(emptyList())
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Text(
                    text = "My Tracks",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (tracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDCCD",
                            style = MaterialTheme.typography.title2,
                            color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "No tracks yet",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                        Text(
                            text = "Tap record to start",
                            style = MaterialTheme.typography.caption3,
                            color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(tracks.size) { index ->
                    TrackItem(
                        track = tracks[index],
                        onClick = { onTrackClick(tracks[index].id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Chip(
                    onClick = onNavigateBack,
                    label = { Text("Back") },
                    modifier = Modifier.fillMaxWidth(0.75f),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
