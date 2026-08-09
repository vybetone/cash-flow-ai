package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AiAnalyticsScreen
import com.example.ui.screens.CameraScannerScreen
import com.example.ui.screens.PortfolioCashFlowScreen
import com.example.ui.screens.ScreenAnalysisHudScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TradeJournalScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashFlowApp(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingChart.collectAsState()

    val greenColor = Color(0xFF00E676)
    val darkBackground = Color(0xFF0B0E14)
    val surfaceColor = Color(0xFF131822)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = greenColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = greenColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "CASH FLOW AI",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Surface(
                            color = if (isAnalyzing) Color(0xFFFFB300).copy(alpha = 0.2f) else greenColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isAnalyzing) Color(0xFFFFB300) else greenColor)
                        ) {
                            Text(
                                text = if (isAnalyzing) "ANALYZING CHART..." else "LIVE QUANT",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (isAnalyzing) Color(0xFFFFB300) else greenColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBackground,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = surfaceColor,
                contentColor = Color.White,
                modifier = Modifier.testTag("cashflow_bottom_nav")
            ) {
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = greenColor,
                    selectedTextColor = greenColor,
                    unselectedIconColor = Color(0xFF78909C),
                    unselectedTextColor = Color(0xFF78909C),
                    indicatorColor = greenColor.copy(alpha = 0.15f)
                )

                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = "Screen HUD") },
                    label = { Text("Screen HUD", fontFamily = FontFamily.Monospace) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_item_screen_hud")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Filled.CameraAlt, contentDescription = "Camera") },
                    label = { Text("Camera", fontFamily = FontFamily.Monospace) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_item_camera")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = { Icon(Icons.Filled.Book, contentDescription = "Journal") },
                    label = { Text("Journal", fontFamily = FontFamily.Monospace) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_item_journal")
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    icon = { Icon(Icons.Filled.Analytics, contentDescription = "AI Analytics") },
                    label = { Text("Analytics", fontFamily = FontFamily.Monospace) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_item_analytics")
                )

                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { viewModel.setSelectedTab(4) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontFamily = FontFamily.Monospace) },
                    colors = itemColors,
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(darkBackground)
        ) {
            when (selectedTab) {
                0 -> ScreenAnalysisHudScreen(viewModel = viewModel)
                1 -> CameraScannerScreen(viewModel = viewModel)
                2 -> TradeJournalScreen(viewModel = viewModel)
                3 -> AiAnalyticsScreen(viewModel = viewModel)
                4 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

