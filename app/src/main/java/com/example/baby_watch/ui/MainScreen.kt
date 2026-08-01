package com.example.baby_watch.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.baby_watch.ui.home.HomeScreen
import com.example.baby_watch.ui.navbar.BottomNavBar
import com.example.baby_watch.ui.navbar.BottomNavTab
import com.example.baby_watch.ui.sensors.SensorScreen
import com.example.baby_watch.ui.settings.SettingsScreen
import com.example.baby_watch.ui.things.LogScreen

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var selectedTab by rememberSaveable { mutableStateOf(BottomNavTab.Home) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Crossfade(targetState = selectedTab, label = "pageTransition") { tab ->
                when (tab) {
                    BottomNavTab.Home -> HomeScreen()
                    BottomNavTab.Sensors -> SensorScreen()
                    BottomNavTab.Log -> LogScreen()
                    BottomNavTab.Settings -> SettingsScreen()
                }
            }
        }
    }
}
