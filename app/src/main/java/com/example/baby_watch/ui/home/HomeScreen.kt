package com.example.baby_watch.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.data.repository.DataBridge
import com.example.baby_watch.data.repository.Esp32CamConfig
import com.example.baby_watch.device.mic.AudioStatus
import com.example.baby_watch.device.mic.Esp32AudioManager
import com.example.baby_watch.ui.esp32cam_img.Esp32CamImage
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Baby_watchTheme
import com.example.baby_watch.ui.theme.Level1Yellow
import com.example.baby_watch.ui.theme.Level2Purple
import com.example.baby_watch.ui.theme.Level3Red
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.Secondary
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ResponseLevel { Safe, Notice, Soothe, Emergency }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val data by DataBridge.state.collectAsState()
    val context = LocalContext.current
    val camHost by Esp32CamConfig.host.collectAsState(initial = Esp32CamConfig.getSavedHost(context))
    val hasLiveData = data.lastUpdate > 0L
    val level = responseLevel(data.alertLevel)
    val levelColor = levelColor(level)

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (hasLiveData) SafeGreen else TextTertiary))
                            Spacer(Modifier.width(8.dp))
                            Text("宝宝守护中", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Text(
                            if (hasLiveData) "最近更新 ${formatTime(data.lastUpdate)}" else "一切正常，等待设备数据",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Umbrella, contentDescription = "守护", tint = Primary)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Add, contentDescription = "添加", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiveVideoCard(camHost = camHost, level = level)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Thermostat,
                    title = "温度",
                    value = if (hasLiveData) "%.1f".format(data.temperature) else "--",
                    unit = "°C",
                    color = Color(0xFFFF7A45),
                    range = "24.0-28.0°C",
                    note = comfortText(data.temperature, 22f, 28f, hasLiveData)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Opacity,
                    title = "湿度",
                    value = if (hasLiveData) data.humidity.toInt().toString() else "--",
                    unit = "%",
                    color = Color(0xFF4994E8),
                    range = "40%-60%",
                    note = comfortText(data.humidity, 40f, 70f, hasLiveData)
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Air,
                    title = "CO2",
                    value = if (hasLiveData) data.co2.toString() else "--",
                    unit = "ppm",
                    color = Color(0xFF2E5C7D),
                    range = "<1000ppm",
                    note = if (!hasLiveData) "未连接" else if (data.co2 <= 1000) "良好" else "需通风"
                )
            }

            GuardianCard(
                level = level,
                title = if (hasLiveData) data.alertTitle.ifBlank { levelTitle(level) } else "安全守护中",
                hint = if (hasLiveData) data.alertHint.ifBlank { "一切正常，宝宝睡得很安稳" } else "设备连接后将实时显示监护结果",
                color = levelColor
            )
        }
    }
}

@Composable
private fun LiveVideoCard(camHost: String, level: ResponseLevel) {
    val danger = level == ResponseLevel.Emergency
    val audioState by Esp32AudioManager.state.collectAsState()

    DisposableEffect(camHost) {
        Esp32AudioManager.connect(camHost)
        onDispose {
            Esp32AudioManager.disconnect()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF151827)),
            border = if (danger) BorderStroke(2.dp, Level3Red) else null,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.32f)) {
                Esp32CamImage(host = camHost, modifier = Modifier.fillMaxSize())

                Pill(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    text = "实时画面",
                    color = Color.Black.copy(alpha = 0.48f),
                    contentColor = Color.White,
                    leading = {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = Secondary, modifier = Modifier.size(8.dp))
                    }
                )

                Pill(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    text = audioLabel(audioState.status, audioState.lastPacketBytes, audioState.peakAmplitude, audioState.isMuted),
                    color = Color.Black.copy(alpha = 0.48f),
                    contentColor = Color.White,
                    leading = {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = audioColor(audioState.status, audioState.isMuted), modifier = Modifier.size(8.dp))
                    }
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.42f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = SafeGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("AI 监护已开启", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("信号强", color = Color.White.copy(alpha = 0.78f), fontSize = 12.sp)
                }
            }
        }

        AudioMuteSwitch(
            muted = audioState.isMuted,
            status = audioState.status,
            onMutedChange = Esp32AudioManager::setMuted
        )
    }
}

@Composable
private fun AudioMuteSwitch(
    muted: Boolean,
    status: AudioStatus,
    onMutedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("麦克风静音", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(
                    if (muted) "已静音，实时画面不播放声音" else audioStatusText(status),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Switch(
                checked = muted,
                onCheckedChange = onMutedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE7E4DF),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}

private fun audioLabel(status: AudioStatus, lastPacketBytes: Int, peakAmplitude: Int, muted: Boolean): String {
    if (muted) return "麦克风静音"
    return when (status) {
        AudioStatus.Idle -> "音频待机"
        AudioStatus.Connecting -> "音频连接中"
        AudioStatus.Connected -> "音频已连接"
        AudioStatus.Receiving -> "收音 ${lastPacketBytes}B P$peakAmplitude"
        AudioStatus.Error -> "音频异常"
    }
}

private fun audioStatusText(status: AudioStatus): String {
    return when (status) {
        AudioStatus.Idle -> "等待麦克风连接"
        AudioStatus.Connecting -> "正在连接麦克风"
        AudioStatus.Connected -> "麦克风已连接"
        AudioStatus.Receiving -> "正在播放实时麦克风声音"
        AudioStatus.Error -> "麦克风连接异常"
    }
}

private fun audioColor(status: AudioStatus, muted: Boolean): Color {
    if (muted) return TextTertiary
    return when (status) {
        AudioStatus.Receiving -> SafeGreen
        AudioStatus.Connected,
        AudioStatus.Connecting -> Level1Yellow
        AudioStatus.Idle,
        AudioStatus.Error -> Level3Red
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    unit: String,
    color: Color,
    range: String,
    note: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(2.dp))
                Text(unit, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(note, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (note == "舒适" || note == "良好") SafeGreen else TextTertiary)
            Spacer(Modifier.height(2.dp))
            Text(range, fontSize = 10.sp, color = TextTertiary)
            Text(title, fontSize = 0.sp, color = Color.Transparent)
        }
    }
}

@Composable
private fun GuardianCard(level: ResponseLevel, title: String, hint: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                        modifier = Modifier.size(50.dp).clip(CircleShape).background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前状态：", fontSize = 12.sp, color = Primary)
                        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(hint, fontSize = 13.sp, color = TextSecondary)
                    }
                }
                LinearProgressIndicator(
                    progress = { levelProgress(level) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = color,
                    trackColor = Color.White.copy(alpha = 0.75f)
                )
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    MiniFeature("多模态监测")
                    MiniFeature("AI 智能分析")
                    MiniFeature("实时守护")
                }
            }
        }
    }
}

@Composable
private fun MiniFeature(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Primary,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.62f)).padding(horizontal = 8.dp, vertical = 5.dp)
    )
}

@Composable
private fun Pill(
    modifier: Modifier,
    text: String,
    color: Color,
    contentColor: Color,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}

private fun responseLevel(level: Int) = when (level) {
    1 -> ResponseLevel.Notice
    2 -> ResponseLevel.Soothe
    3 -> ResponseLevel.Emergency
    else -> ResponseLevel.Safe
}

private fun levelColor(level: ResponseLevel) = when (level) {
    ResponseLevel.Safe -> SafeGreen
    ResponseLevel.Notice -> Level1Yellow
    ResponseLevel.Soothe -> Level2Purple
    ResponseLevel.Emergency -> Level3Red
}

private fun levelTitle(level: ResponseLevel) = when (level) {
    ResponseLevel.Safe -> "安全守护中"
    ResponseLevel.Notice -> "轻度提醒"
    ResponseLevel.Soothe -> "自动安抚中"
    ResponseLevel.Emergency -> "紧急告警"
}

private fun levelProgress(level: ResponseLevel) = when (level) {
    ResponseLevel.Safe -> 0.18f
    ResponseLevel.Notice -> 0.42f
    ResponseLevel.Soothe -> 0.7f
    ResponseLevel.Emergency -> 1f
}

private fun comfortText(value: Float, min: Float, max: Float, hasLiveData: Boolean): String {
    if (!hasLiveData) return "未连接"
    return if (value in min..max) "舒适" else "需关注"
}

private fun formatTime(time: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    Baby_watchTheme { HomeScreen() }
}
