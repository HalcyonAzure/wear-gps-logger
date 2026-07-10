package com.example.gpslogger.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.ui.components.StatusCard
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import com.example.gpslogger.GpsLoggerApp

/**
 * Main screen - GPS recording control panel
 */
@Composable
fun MainScreen(
    onNavigateToTracks: () -> Unit,
    viewModel: MainViewModel = viewModel(
        factory = MainViewModelFactory(GpsLoggerApp.instance)
    )
) {
    val isRecording by viewModel.isRecording.observeAsState(false)
    val pointCount by viewModel.pointCount.observeAsState(0)
    val distance by viewModel.distance.observeAsState(0.0)
    val elapsedTime by viewModel.elapsedTime.observeAsState(0L)
    val batteryLevel by viewModel.batteryLevel.observeAsState(100)
    val exportResult by viewModel.exportResult.observeAsState(null)

    val snackbarState = remember { SnackbarHostState() }

    // Show export result as snackbar
    LaunchedEffect(exportResult) {
        exportResult?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearExportResult()
        }
    }

    // Refresh battery every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            viewModel.refreshBatteryLevel()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        snackbar = { SnackbarHost(snackbarState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "GPS Logger",
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.primary
            )

            StatusCard(
                isRecording = isRecording,
                pointCount = pointCount,
                distance = distance,
                elapsedTime = elapsedTime,
                batteryLevel = batteryLevel
            )

            Button(
                onClick = {
                    if (isRecording) {
                        viewModel.stopTracking()
                    } else {
                        viewModel.startTracking()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isRecording) {
                        MaterialTheme.colors.error
                    } else {
                        MaterialTheme.colors.primary
                    }
                ),
                modifier = Modifier.size(60.dp)
            ) {
                Text(
                    text = if (isRecording) "\u25A0" else "\u25B6",
                    style = MaterialTheme.typography.title2
                )
            }

            CompactButton(
                onClick = onNavigateToTracks,
                colors = ButtonDefaults.compactButtonColors(
                    backgroundColor = MaterialTheme.colors.surface
                )
            ) {
                Text(
                    text = "Tracks",
                    style = MaterialTheme.typography.caption2
                )
            }
        }
    }
}
