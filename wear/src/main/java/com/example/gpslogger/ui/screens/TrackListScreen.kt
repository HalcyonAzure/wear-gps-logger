package com.example.gpslogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.ui.components.TrackItem
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import com.example.gpslogger.GpsLoggerApp

/**
 * Track list screen
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

    LaunchedEffect(Unit) {
        viewModel.loadTracks()
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Tracks",
                        style = MaterialTheme.typography.title3
                    )
                }
            }

            if (tracks.isEmpty()) {
                item {
                    Text(
                        text = "No tracks yet",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(tracks.size) { index ->
                    val track = tracks[index]
                    TrackItem(
                        track = track,
                        onClick = { onTrackClick(track.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                CompactButton(
                    onClick = onNavigateBack
                ) {
                    Text(text = "Back")
                }
            }
        }
    }
}
