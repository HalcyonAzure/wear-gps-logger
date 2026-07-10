package com.example.gpslogger.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.*
import com.example.gpslogger.ui.components.StatusCard
import com.example.gpslogger.ui.viewmodel.MainViewModel
import com.example.gpslogger.ui.viewmodel.MainViewModelFactory
import com.example.gpslogger.GpsLoggerApp
import kotlinx.coroutines.delay

/**
 * MainScreen - GPS 控制面板主界面 (圆形屏幕优化版)
 *
 * 设计要点：
 * 1. ScalingLazyColumn 支持旋转表冠滚动 + 自动居中
 * 2. 18dp 水平 padding 确保内容在圆形安全区域内
 * 3. 64dp 大录制按钮 + 脉冲动画光环
 * 4. CurvedText 弧形标题利用圆形屏幕特性
 * 5. Chip 替代 CompactButton，更大点击区域
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

    val listState = rememberScalingLazyListState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            viewModel.refreshBatteryLevel()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                CurvedText(
                    text = "GPS Logger",
                    style = CurvedTextStyle(
                        fontSize = MaterialTheme.typography.title3.fontSize,
                        color = MaterialTheme.colors.primary
                    ),
                    angularDirection = CurvedText.AngularDirection.CLOCKWISE
                )
            }

            item {
                StatusCard(
                    isRecording = isRecording,
                    pointCount = pointCount,
                    distance = distance,
                    elapsedTime = elapsedTime,
                    batteryLevel = batteryLevel
                )
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RecordButton(
                        isRecording = isRecording,
                        onToggle = {
                            if (isRecording) viewModel.stopTracking()
                            else viewModel.startTracking()
                        }
                    )
                }
            }

            item {
                Chip(
                    onClick = onNavigateToTracks,
                    label = { Text("My Tracks") },
                    modifier = Modifier.fillMaxWidth(0.85f),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RecordButton(
    isRecording: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isRecording) 0.25f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.size(72.dp)) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.error.copy(alpha = pulseAlpha))
            )
        }
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = if (isRecording) MaterialTheme.colors.error
                else MaterialTheme.colors.primary
            ),
            modifier = Modifier.size(64.dp)
        ) {
            Text(
                text = if (isRecording) "\u25A0" else "\u25B6",
                style = MaterialTheme.typography.display1,
                textAlign = TextAlign.Center
            )
        }
    }
}
