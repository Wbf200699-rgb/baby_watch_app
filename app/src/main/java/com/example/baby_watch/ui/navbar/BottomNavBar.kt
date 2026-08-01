package com.example.baby_watch.ui.navbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.baby_watch.ui.theme.Baby_watchTheme
import com.example.baby_watch.ui.theme.Primary
import com.example.baby_watch.ui.theme.PrimaryContainer
import com.example.baby_watch.ui.theme.TextSecondary

enum class BottomNavTab(
    val label: String,
    val icon: ImageVector,
) {
    Home("首页", Icons.Default.Home),
    Sensors("传感器", Icons.Default.Sensors),
    Log("告警", Icons.AutoMirrored.Filled.MenuBook),
    Settings("设置", Icons.Default.Settings),
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            val color by animateColorAsState(
                targetValue = if (selected) Primary else TextSecondary,
                label = "bottomNavColor"
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) PrimaryContainer else Color.Transparent)
                        .padding(horizontal = 18.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = tab.label,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = color
                )
            }
        }
    }
}

@Preview
@Composable
private fun BottomNavBarPreview() {
    Baby_watchTheme {
        BottomNavBar(selectedTab = BottomNavTab.Home, onTabSelected = {})
    }
}
