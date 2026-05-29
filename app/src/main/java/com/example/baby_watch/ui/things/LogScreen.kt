package com.example.baby_watch.ui.things

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.data.repository.DataBridge
import com.example.baby_watch.service.log.LogManager
import com.example.baby_watch.ui.theme.Background
import com.example.baby_watch.ui.theme.Baby_watchTheme
import com.example.baby_watch.ui.theme.Level3Red
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.SafeGreen
import com.example.baby_watch.ui.theme.TextPrimary
import com.example.baby_watch.ui.theme.TextSecondary
import com.example.baby_watch.ui.theme.TextTertiary

private enum class LogFilter(val label: String) {
    All("全部"),
    Alert("告警"),
    System("系统"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(modifier: Modifier = Modifier) {
    val logs by DataBridge.logs.collectAsState()
    var filter by remember { mutableStateOf(LogFilter.All) }
    val filtered = logs.filter {
        when (filter) {
            LogFilter.All -> true
            LogFilter.Alert -> it.type == LogManager.LogType.ALERT
            LogFilter.System -> it.type == LogManager.LogType.SYSTEM
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("安全日志", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("记录设备、通知和告警事件", fontSize = 13.sp, color = TextSecondary)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FilterTabs(selected = filter, onSelect = { filter = it })
            if (filtered.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    itemsIndexed(filtered) { index, item ->
                        TimelineItem(
                            time = item.time,
                            type = item.type,
                            title = item.title,
                            detail = item.detail,
                            isFirst = index == 0,
                            isLast = index == filtered.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterTabs(selected: LogFilter, onSelect: (LogFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PrimaryContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LogFilter.entries.forEach { tab ->
            val active = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tab.label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) Primary else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    time: String,
    type: LogManager.LogType,
    title: String,
    detail: String,
    isFirst: Boolean,
    isLast: Boolean
) {
    val color = typeColor(type)
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(if (isFirst) 10.dp else 0.dp))
            if (!isFirst) Box(modifier = Modifier.width(2.dp).height(10.dp).background(PrimaryContainer))
            Box(modifier = Modifier.size(11.dp).clip(CircleShape).background(color))
            if (!isLast) Box(modifier = Modifier.width(2.dp).height(76.dp).background(PrimaryContainer))
        }
        Card(
            modifier = Modifier.weight(1f).padding(bottom = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (type == LogManager.LogType.ALERT) Color(0xFFFFF4F4) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(time, fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    TypeBadge(type)
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(title.ifBlank { typeLabel(type) }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (type == LogManager.LogType.ALERT) Level3Red else TextPrimary)
                if (detail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(detail, fontSize = 13.sp, color = TextSecondary, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: LogManager.LogType) {
    val color = typeColor(type)
    Text(
        typeLabel(type),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)
    )
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 42.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("暂无记录", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("收到设备数据或告警后，会自动出现在这里。", fontSize = 13.sp, color = TextSecondary)
        }
    }
}

private fun typeColor(type: LogManager.LogType) = when (type) {
    LogManager.LogType.ALERT -> Level3Red
    LogManager.LogType.SYSTEM -> SafeGreen
    LogManager.LogType.NOTIFICATION -> Primary
}

private fun typeLabel(type: LogManager.LogType) = when (type) {
    LogManager.LogType.ALERT -> "告警"
    LogManager.LogType.SYSTEM -> "系统"
    LogManager.LogType.NOTIFICATION -> "通知"
}

@Preview(showBackground = true)
@Composable
private fun LogScreenPreview() {
    Baby_watchTheme { LogScreen() }
}
