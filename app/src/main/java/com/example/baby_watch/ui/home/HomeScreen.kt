package com.example.baby_watch.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.data.server.AudioPlayerSummary
import com.example.baby_watch.data.server.AudioStreamState
import com.example.baby_watch.data.server.AiAdvisory
import com.example.baby_watch.data.server.AiStory
import com.example.baby_watch.data.server.MobileServerRepository
import com.example.baby_watch.data.server.MonitorSnapshot
import com.example.baby_watch.data.server.ServerConnectionStatus
import com.example.baby_watch.data.server.VideoStreamState
import com.example.baby_watch.data.server.VisionOverlaySummary
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Level1Yellow
import com.example.baby_watch.ui.theme.Level2Purple
import com.example.baby_watch.ui.theme.Level3Red
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.SecondaryContainer
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val repositoryState by MobileServerRepository.state.collectAsState()
    val videoState by MobileServerRepository.videoState.collectAsState()
    val audioState by MobileServerRepository.audioState.collectAsState()
    val command by MobileServerRepository.lastCommand.collectAsState()
    val stories by MobileServerRepository.stories.collectAsState()
    val advisories by MobileServerRepository.advisories.collectAsState()
    val snapshot = repositoryState.snapshot
    val deviceOnline = repositoryState.connectionStatus ==
        ServerConnectionStatus.Connected && snapshot.device.online

    DisposableEffect(Unit) {
        onDispose { MobileServerRepository.stopMedia() }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            deviceOnline -> SafeGreen
                                            repositoryState.connectionStatus ==
                                                ServerConnectionStatus.Connected -> Level3Red
                                            else -> TextTertiary
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    deviceOnline -> "宝宝守护中"
                                    repositoryState.connectionStatus ==
                                        ServerConnectionStatus.Connecting -> "正在连接监护服务"
                                    else -> "监护设备离线"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                            )
                        }
                        Text(
                            connectionSubtitle(
                                repositoryState.connectionStatus,
                                snapshot,
                                repositoryState.errorMessage,
                            ),
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionTitle("实时画面")
            LiveVideoCard(
                videoState = videoState,
                audioState = audioState,
                deviceOnline = deviceOnline,
                videoAvailable = snapshot.videoAvailable,
                audioAvailable = snapshot.audioAvailable,
                visionOverlay = snapshot.visionOverlay,
                onVideoToggle = { enabled ->
                    if (enabled) MobileServerRepository.startVideo()
                    else MobileServerRepository.stopVideo()
                },
                onAudioToggle = { enabled ->
                    if (enabled) MobileServerRepository.startAudio()
                    else MobileServerRepository.stopAudio()
                },
            )

            SectionTitle("婴儿状态")
            GuardianCard(snapshot = snapshot, deviceOnline = deviceOnline)
            if (repositoryState.capabilities.aiEnabled) {
                advisories.firstOrNull { !it.acknowledged }?.let { advisory ->
                    AiAdvisoryCard(advisory)
                }
            }

            SectionTitle("音乐播放")
            MusicPlayerCard(player = snapshot.audioPlayer, deviceOnline = deviceOnline)
            command
                ?.takeIf { it.action.startsWith("audio.") }
                ?.let {
                    CommandStatusCard(
                        action = it.action,
                        status = it.status,
                    )
                }
            if (repositoryState.capabilities.aiEnabled) {
                SectionTitle("AI 故事")
                AiStoryCard(
                    latestStory = stories.firstOrNull(),
                    deviceOnline = deviceOnline,
                )
            }
        }
    }
}

@Composable
private fun AiAdvisoryCard(advisory: AiAdvisory) {
    val color = when (advisory.level) {
        "urgent" -> Level3Red
        "attention" -> Level1Yellow
        else -> SafeGreen
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AI 辅助建议 · ${advisoryLevelLabel(advisory.level)}",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    "置信度 ${(advisory.confidence * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
            }
            Text(advisory.title.ifBlank { "状态提示" }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(advisory.summary, fontSize = 13.sp, color = TextSecondary)
            advisory.actions.take(2).forEach { action ->
                Text("• $action", fontSize = 12.sp, color = TextPrimary)
            }
            Text(
                "仅供照护参考，不是医疗诊断；硬件告警与本地安全判断始终优先。",
                fontSize = 11.sp,
                color = TextTertiary,
            )
            Button(
                onClick = { MobileServerRepository.acknowledgeAdvisory(advisory.advisoryId) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("我已查看")
            }
        }
    }
}

@Composable
private fun AiStoryCard(latestStory: AiStory?, deviceOnline: Boolean) {
    var theme by remember { mutableStateOf("月亮和小云朵") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("生成安抚故事", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "由服务端生成并安全传输到 RA8 播放，生成过程不会阻塞实时遥测。",
                fontSize = 12.sp,
                color = TextSecondary,
            )
            OutlinedTextField(
                value = theme,
                onValueChange = { theme = it.take(40) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("故事主题") },
                singleLine = true,
                enabled = deviceOnline,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = { MobileServerRepository.createStory(theme = theme) },
                    enabled = deviceOnline && theme.isNotBlank(),
                ) {
                    Text("生成并播放")
                }
                if (latestStory?.canReplay == true) {
                    Button(
                        onClick = { MobileServerRepository.replayStory(latestStory.storyId) },
                        enabled = deviceOnline,
                    ) {
                        Text("重新播放")
                    }
                }
            }
            latestStory?.let { story ->
                Text(
                    "${story.title.ifBlank { "最近故事" }} · ${storyStatusLabel(story.status)}",
                    fontSize = 12.sp,
                    color = if (story.status == "failed") Level3Red else TextSecondary,
                )
                if (story.error.isNotBlank()) {
                    Text(story.error.take(160), fontSize = 11.sp, color = Level3Red)
                }
            }
        }
    }
}

private fun advisoryLevelLabel(level: String): String = when (level) {
    "urgent" -> "紧急关注"
    "attention" -> "需要留意"
    else -> "一般提示"
}

private fun storyStatusLabel(status: String): String = when (status) {
    "queued" -> "等待生成"
    "generating" -> "正在生成"
    "synthesizing" -> "正在合成语音"
    "ready" -> "已生成"
    "queued_for_device" -> "正在传输到设备"
    "queued_for_playback" -> "已加入播放队列"
    "failed" -> "生成失败"
    else -> status
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = TextPrimary,
    )
}

@Composable
private fun LiveVideoCard(
    videoState: VideoStreamState,
    audioState: AudioStreamState,
    deviceOnline: Boolean,
    videoAvailable: Boolean,
    audioAvailable: Boolean,
    visionOverlay: VisionOverlaySummary,
    onVideoToggle: (Boolean) -> Unit,
    onAudioToggle: (Boolean) -> Unit,
) {
    val bitmap = remember(videoState.sequence) {
        videoState.frameBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151827)),
            border = if (videoState.errorMessage != null) BorderStroke(1.dp, Level3Red) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color(0xFF10201C)),
                    contentAlignment = Alignment.Center,
                ) {
                    val showingLiveFrame = deviceOnline && bitmap != null
                    if (showingLiveFrame) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "服务端摄像头实时画面",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )

                        if (visionOverlay.enabled &&
                            !visionOverlay.expired &&
                            videoState.width > 0 &&
                            videoState.height > 0
                        ) {
                            val frameAspect = videoState.width.toFloat() / videoState.height
                            val stageAspect = maxWidth.value / maxHeight.value
                            val visibleWidth = if (frameAspect >= stageAspect) {
                                maxWidth
                            } else {
                                maxHeight * frameAspect
                            }
                            val visibleHeight = if (frameAspect >= stageAspect) {
                                maxWidth / frameAspect
                            } else {
                                maxHeight
                            }
                            val frameLeft = (maxWidth - visibleWidth) / 2f
                            val frameTop = (maxHeight - visibleHeight) / 2f

                            visionOverlay.boxes.forEach { detection ->
                                val boxColor = detectionBoxColor(detection.classId)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset(
                                            x = frameLeft + visibleWidth * detection.x,
                                            y = frameTop + visibleHeight * detection.y,
                                        )
                                        .size(
                                            width = visibleWidth * detection.width,
                                            height = visibleHeight * detection.height,
                                        )
                                        .border(2.dp, boxColor),
                                ) {
                                    Text(
                                        text = buildString {
                                            append(
                                                detection.label.ifBlank {
                                                    "类别 ${detection.classId}"
                                                }
                                            )
                                            append(" ")
                                            append("%.1f%%".format(detection.scoreX1000 / 10f))
                                        },
                                        modifier = Modifier
                                            .background(boxColor)
                                            .padding(horizontal = 5.dp, vertical = 2.dp),
                                        color = Color(0xFF101718),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.35f),
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                when {
                                    !deviceOnline -> "设备离线，请检查电源和网络"
                                    !videoState.wanted -> "点击下方开关查看实时画面"
                                    !videoAvailable -> "视频上传端暂不可用"
                                    videoState.connected -> "WSS 已连接，等待 S3 JPEG"
                                    else -> videoState.errorMessage ?: "正在连接服务端视频 WSS…"
                                },
                                color = Color.White.copy(alpha = 0.72f),
                                fontSize = 13.sp,
                            )
                        }
                    }

                    StatusPill(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                        text = when {
                            showingLiveFrame -> "服务端实时画面"
                            !deviceOnline -> "设备离线"
                            !videoState.wanted -> "手动开启"
                            videoState.connected && videoAvailable -> "等待画面"
                            else -> "视频未连接"
                        },
                        active = showingLiveFrame,
                    )

                    if (showingLiveFrame) {
                        Text(
                            text = when {
                                !visionOverlay.enabled -> "检测框已关闭"
                                visionOverlay.expired -> "检测框超过 30 秒，等待新推理"
                                visionOverlay.stale -> "显示上次检测框 · ${
                                    "%.1f".format(visionOverlay.sourceAgeMs / 1000f)
                                } 秒前"
                                visionOverlay.boxes.isEmpty() -> "本帧未检出目标"
                                else -> "${visionOverlay.boxes.size} 个检测框 · ${
                                    if (visionOverlay.calibrationStatus == "calibrated") {
                                        "已校准"
                                    } else {
                                        "同坐标"
                                    }
                                }"
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xD9172126))
                                .border(
                                    width = 1.dp,
                                    color = if (visionOverlay.stale) {
                                        Color(0xFF8F6D2C)
                                    } else {
                                        Color(0xFF46555B)
                                    },
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 7.dp, vertical = 4.dp),
                            color = if (visionOverlay.stale) {
                                Color(0xFFFFE1A3)
                            } else {
                                Color(0xFFDCE5E7)
                            },
                            fontSize = 10.sp,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111C20))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (videoState.frameBytes != null) {
                                "${videoState.width}×${videoState.height} · JPEG quality ${videoState.quality}"
                            } else {
                                "S3MD JPEG · 服务端 WSS"
                            },
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (videoState.frameBytes != null) {
                                "序列 ${videoState.sequence} · ${videoState.gapCount} 帧间断"
                            } else {
                                "最新帧优先"
                            },
                            color = Color.White.copy(alpha = 0.56f),
                            fontSize = 11.sp,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (videoState.wanted) "实时画面已开启" else "实时画面默认关闭",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "按需订阅，不影响后台遥测刷新",
                                color = Color.White.copy(alpha = 0.56f),
                                fontSize = 11.sp,
                            )
                        }
                        Switch(
                            checked = videoState.wanted,
                            onCheckedChange = onVideoToggle,
                            enabled = deviceOnline && videoAvailable,
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val audioEnabled = deviceOnline && audioAvailable
                val audioOn = audioState.wanted
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (audioOn) {
                            Icons.AutoMirrored.Filled.VolumeUp
                        } else {
                            Icons.AutoMirrored.Filled.VolumeOff
                        },
                        contentDescription = null,
                        tint = if (audioEnabled) Primary else TextTertiary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "实时声音",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Text(
                        when {
                            !deviceOnline -> "设备离线，暂无实时声音"
                            !audioState.wanted -> "点击开关后开始监听"
                            !audioAvailable -> "设备暂未上传声音"
                            !audioState.connected -> audioState.errorMessage ?: "正在连接音频…"
                            audioState.receiving -> "正在播放宝宝房间声音"
                            audioState.connected -> "已开启，等待声音"
                            else -> audioState.errorMessage ?: "正在连接音频…"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary,
                    )
                }
                Switch(
                    checked = audioOn,
                    onCheckedChange = onAudioToggle,
                    enabled = audioEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Primary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE7E4DF),
                        uncheckedBorderColor = Color.Transparent,
                        disabledUncheckedThumbColor = Color.White,
                        disabledUncheckedTrackColor = Color(0xFFE7E4DF),
                        disabledUncheckedBorderColor = Color.Transparent,
                    ),
                )
            }
        }
    }
}

private fun detectionBoxColor(classId: Int): Color = when (classId) {
    0 -> Color(0xFF4DD0A8)
    1 -> Color(0xFF56A8FF)
    2 -> Color(0xFFFFCA5C)
    3 -> Color(0xFFE98BFF)
    4 -> Color(0xFFFF7C7C)
    else -> Color(0xFFF2F5F6)
}

@Composable
private fun StatusPill(modifier: Modifier, text: String, active: Boolean) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) SafeGreen else Level1Yellow)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GuardianCard(snapshot: MonitorSnapshot, deviceOnline: Boolean) {
    val severity = snapshot.inference.severity
    val color = if (deviceOnline) severityColor(severity) else TextTertiary
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryContainer, Color(0xFFFFF8EE))
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (deviceOnline) "服务端融合判断" else "实时监护已暂停",
                            fontSize = 12.sp,
                            color = if (deviceOnline) Primary else TextSecondary,
                        )
                        Text(
                            if (deviceOnline) {
                                snapshot.inference.stateLabel
                            } else {
                                "监护设备离线"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            if (deviceOnline) {
                                "风险等级 ${severityLabel(severity)} · 置信度 ${
                                    "%.1f".format(snapshot.inference.confidence)
                                }% · ${diagnosticsLabel(snapshot.diagnosticsOverall)}"
                            } else {
                                "请检查设备电源和网络，恢复在线后将自动继续监护"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = {
                        if (deviceOnline) severity.coerceIn(0, 5) / 5f else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
private fun MusicPlayerCard(player: AudioPlayerSummary, deviceOnline: Boolean) {
    var volumeDraft by remember(player.volume) {
        mutableFloatStateOf(player.volume.coerceIn(0, 100).toFloat())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PrimaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "摇篮曲",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        if (deviceOnline) {
                            playerSubtitle(player)
                        } else {
                            "设备离线，暂时无法控制播放"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(
                    onClick = { MobileServerRepository.sendCommand("audio.previous") },
                    enabled = deviceOnline,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(32.dp),
                        tint = TextSecondary,
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                IconButton(
                    onClick = {
                        when {
                            player.isPlaying -> MobileServerRepository.sendCommand("audio.pause")
                            player.isPaused -> MobileServerRepository.sendCommand("audio.resume")
                            else -> MobileServerRepository.sendCommand(
                                "audio.play_track",
                                mapOf("track" to player.track.coerceAtLeast(1)),
                            )
                        }
                    },
                    enabled = deviceOnline,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                !deviceOnline -> TextTertiary
                                player.isPlaying -> Primary
                                else -> SafeGreen
                            }
                        ),
                ) {
                    Icon(
                        if (player.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (player.isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                IconButton(
                    onClick = { MobileServerRepository.sendCommand("audio.next") },
                    enabled = deviceOnline,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(32.dp),
                        tint = TextSecondary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("音量", fontSize = 13.sp, color = TextSecondary)
                Slider(
                    value = volumeDraft,
                    onValueChange = { volumeDraft = it },
                    onValueChangeFinished = {
                        MobileServerRepository.sendCommand(
                            "audio.set_volume",
                            mapOf("percent" to volumeDraft.toInt()),
                        )
                    },
                    enabled = deviceOnline,
                    valueRange = 0f..100f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp),
                )
                Text(
                    "${volumeDraft.toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun CommandStatusCard(action: String, status: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            "最近操作：${commandActionLabel(action)} · ${commandStatusLabel(status)}",
            modifier = Modifier.padding(14.dp),
            fontSize = 13.sp,
            color = TextPrimary,
        )
    }
}

private fun playerSubtitle(player: AudioPlayerSummary): String {
    val state = when (player.status) {
        1 -> "等待 SD 卡"
        2 -> "文件打开失败"
        3 -> "WAV 文件异常"
        4 -> "音频接口异常"
        5 -> "正在播放"
        6 -> "播放完成"
        7 -> "已暂停"
        8 -> "已停止"
        else -> "空闲"
    }
    val track = player.file.ifBlank {
        if (player.track > 0) "曲目 ${player.track}" else "未选择曲目"
    }
    return "$state · $track · 共 ${player.trackCount} 首"
}

private fun commandStatusLabel(status: String): String = when (status) {
    "queued" -> "等待发送"
    "sent" -> "已转发"
    "acked" -> "网关已确认"
    "failed" -> "执行失败"
    else -> status
}

private fun commandActionLabel(action: String): String = when (action) {
    "audio.previous" -> "上一首"
    "audio.next" -> "下一首"
    "audio.pause" -> "暂停播放"
    "audio.resume" -> "继续播放"
    "audio.play_track" -> "播放曲目"
    "audio.set_volume" -> "调整音量"
    else -> "播放控制"
}

private fun severityLabel(severity: Int): String = when (severity) {
    0 -> "正常"
    1 -> "留意"
    2 -> "提醒"
    3 -> "警告"
    4 -> "高危"
    else -> "紧急"
}

private fun diagnosticsLabel(value: String): String = when (value.lowercase()) {
    "ok", "healthy", "normal" -> "传感器正常"
    "degraded", "warning" -> "部分传感器异常"
    "offline" -> "设备数据中断"
    "error", "fault" -> "传感器故障"
    else -> "诊断 ${value.ifBlank { "未知" }}"
}

private fun severityColor(severity: Int): Color = when (severity) {
    0 -> SafeGreen
    1, 2 -> Level1Yellow
    3, 4 -> Level2Purple
    else -> Level3Red
}

private fun connectionSubtitle(
    status: ServerConnectionStatus,
    snapshot: MonitorSnapshot,
    error: String?,
): String = when (status) {
    ServerConnectionStatus.NotConfigured -> "请先在设置中填写服务端账号"
    ServerConnectionStatus.Connecting -> "正在连接服务端…"
    ServerConnectionStatus.Error -> error ?: "服务端连接异常"
    ServerConnectionStatus.Connected -> {
        if (snapshot.device.online) {
            "设备在线 · ${snapshot.device.hardware.ifBlank { snapshot.device.id }}"
        } else {
            "服务端已连接，设备当前离线"
        }
    }
}
