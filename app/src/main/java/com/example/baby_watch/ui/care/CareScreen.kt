package com.example.baby_watch.ui.care

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.device.light.WarmLight
import com.example.baby_watch.device.music.Music
import com.example.baby_watch.device.pat.pat
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Baby_watchTheme
import com.example.baby_watch.ui.theme.Level3Red
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.Secondary
import com.example.baby_watch.ui.theme.SecondaryContainer
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary

data class CareUiState(
    val isPlaying: Boolean = false,
    val patEnabled: Boolean = false,
    val warmLightEnabled: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember {
        mutableStateOf(
            CareUiState(
                isPlaying = Music.isPlaying(context),
                patEnabled = pat.isEnabled(context),
                warmLightEnabled = WarmLight.isEnabled(context)
            )
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("安抚中心", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("远程控制助眠设备", fontSize = 13.sp, color = TextSecondary)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InsightCard()
            MusicCard(
                isPlaying = state.isPlaying,
                onPlayPause = {
                    val next = !state.isPlaying
                    state = state.copy(isPlaying = next)
                    Music.setPlaying(context, next)
                    Music.sendMusicCommand(next)
                },
                onPrev = { Music.sendTrackCommand("prev") },
                onNext = { Music.sendTrackCommand("next") }
            )
            DeviceToggleCard(
                icon = Icons.Default.TouchApp,
                title = "轻拍安抚",
                subtitle = "模拟稳定、轻柔的拍背节奏",
                enabled = state.patEnabled,
                activeText = "轻拍模块运行中",
                color = Primary,
                onToggle = {
                    val next = !state.patEnabled
                    state = state.copy(patEnabled = next)
                    pat.setEnabled(context, next)
                    pat.sendPatCommand(next)
                }
            )
            DeviceToggleCard(
                icon = Icons.Default.Lightbulb,
                title = "暖光灯",
                subtitle = "营造柔和光线，降低夜间刺激",
                enabled = state.warmLightEnabled,
                activeText = "暖光灯已开启",
                color = Secondary,
                onToggle = {
                    val next = !state.warmLightEnabled
                    state = state.copy(warmLightEnabled = next)
                    WarmLight.setEnabled(context, next)
                    WarmLight.sendLightCommand(next)
                }
            )
            SafetyCard()
        }
    }
}

@Composable
private fun InsightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI 状态评估", fontSize = 12.sp, color = TextSecondary)
                Text("情绪平稳", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("当前无需强制干预，可保持轻量监护。", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun MusicCard(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("摇篮曲", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(if (isPlaying) "正在播放舒缓音乐" else "准备播放舒缓音乐", fontSize = 13.sp, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                IconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "上一首", modifier = Modifier.size(32.dp), tint = TextSecondary)
                }
                Spacer(modifier = Modifier.width(18.dp))
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(68.dp).clip(CircleShape).background(if (isPlaying) Primary else SafeGreen)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "下一首", modifier = Modifier.size(32.dp), tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun DeviceToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    activeText: String,
    color: Color,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(23.dp))
            }
            Spacer(modifier = Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, fontSize = 13.sp, color = TextSecondary)
                Spacer(modifier = Modifier.height(5.dp))
                Text(if (enabled) activeText else "已关闭", fontSize = 12.sp, color = if (enabled) color else TextTertiary)
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = color,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = TextTertiary.copy(alpha = 0.45f)
                )
            )
        }
    }
}

@Composable
private fun SafetyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SecondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Text(
            "安全策略：三级告警触发时，请优先人工确认现场情况；机械安抚只作为辅助措施。",
            modifier = Modifier.padding(16.dp),
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = Level3Red
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareScreenPreview() {
    Baby_watchTheme { CareScreen() }
}
