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
import com.example.gpslogger.ui.components.StatRow
import com.example.gpslogger.ui.components.formatDistance
import com.example.gpslogger.ui.components.formatElapsedTime
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import com.example.gpslogger.GpsLoggerApp
import java.text.SimpleDateFormat
import java.util.*

/**
 * 轨迹详情界面
 *
 * 操作:
 * - Export: 导出 GPX 到 /sdcard/Documents/GPSLogger/
 * - Del: 删除轨迹
 * - Back: 返回列表
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

    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            when {
                // 删除确认界面
                showDeleteConfirm && track != null -> {
                    Text(
                        text = "Delete Track?",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.error
                    )
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            Text("Yes", style = MaterialTheme.typography.caption3)
                        }
                        Button(
                            onClick = { showDeleteConfirm = false },
                            modifier = Modifier.size(52.dp)
                        ) {
                            Text("No", style = MaterialTheme.typography.caption3)
                        }
                    }
                }

                // 正常详情界面
                track != null -> {
                    // 轨迹名称
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )

                    // 统计信息卡片
                    Card(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            StatRow(label = "Points", value = "${track.pointCount}")
                            StatRow(label = "Distance", value = formatDistance(track.distanceMeters))
                            StatRow(label = "Start", value = dateFormat.format(Date(track.startTime)))
                            track.endTime?.let {
                                StatRow(label = "End", value = dateFormat.format(Date(it)))
                                val duration = it - track.startTime
                                StatRow(label = "Duration", value = formatElapsedTime(duration))
                            }
                        }
                    }

                    // 导出状态显示
                    when (exportStatus) {
                        ExportStatus.Exporting -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Exporting...",
                                style = MaterialTheme.typography.caption3,
                                color = MaterialTheme.colors.secondary
                            )
                        }
                        ExportStatus.Success -> {
                            Text(
                                text = "Exported!",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.primary
                            )
                            Text(
                                text = exportPath,
                                style = MaterialTheme.typography.caption3,
                                color = MaterialTheme.colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        ExportStatus.Error -> {
                            Text(
                                text = exportPath, // 错误信息
                                style = MaterialTheme.typography.caption3,
                                color = MaterialTheme.colors.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> { /* Idle - 不显示 */ }
                    }

                    // 操作按钮行
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportStatus = ExportStatus.Exporting
                                viewModel.exportTrack(track) { resultPath, success ->
                                    if (success) {
                                        exportStatus = ExportStatus.Success
                                        exportPath = resultPath
                                    } else {
                                        exportStatus = ExportStatus.Error
                                        exportPath = resultPath
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialTheme.colors.secondary
                            ),
                            modifier = Modifier.size(52.dp),
                            enabled = exportStatus != ExportStatus.Exporting
                        ) {
                            Text("Export", style = MaterialTheme.typography.caption3)
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

                    // 返回按钮
                    CompactButton(onClick = onNavigateBack) {
                        Text(text = "Back")
                    }
                }

                // 轨迹不存在
                else -> {
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
}

enum class ExportStatus {
    Idle, Exporting, Success, Error
}
