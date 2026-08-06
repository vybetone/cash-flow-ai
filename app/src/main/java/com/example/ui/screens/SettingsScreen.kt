package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.BuildConfig
import com.example.ui.CashFlowViewModel

@Composable
fun SettingsScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val preferredModel by viewModel.preferredModel.collectAsState()
    val autoInterval by viewModel.autoCaptureIntervalSeconds.collectAsState()

    val greenColor = Color(0xFF00E676)

    val hasCustomApiKey = try {
        BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    } catch (e: Throwable) {
        false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263238))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = greenColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI API CREDENTIALS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }

                    Surface(
                        color = if (hasCustomApiKey) greenColor.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (hasCustomApiKey) "ACTIVE" else "SECRETS PANEL / LOCAL FALLBACK",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = if (hasCustomApiKey) greenColor else Color(0xFFFFB300)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Google AI Studio automatically injects your GEMINI_API_KEY from Secrets at build time. If offline or no key is set, Cash Flow AI uses local high-speed chart vision intelligence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF90A4AE)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI Model Engine Choice",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = preferredModel == "gemini-3.5-flash",
                onClick = { viewModel.setPreferredModel("gemini-3.5-flash") },
                label = { Text("gemini-3.5-flash (Fastest)") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor),
                modifier = Modifier.testTag("model_select_flash_chip")
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = preferredModel == "gemini-3.1-pro-preview",
                onClick = { viewModel.setPreferredModel("gemini-3.1-pro-preview") },
                label = { Text("gemini-3.1-pro-preview (Deep Reasoning)") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF82B1FF).copy(alpha = 0.2f), selectedLabelColor = Color(0xFF82B1FF)),
                modifier = Modifier.testTag("model_select_pro_chip")
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Live Screen Auto-Scan Interval",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf(3, 5, 10, 30).forEach { sec ->
                FilterChip(
                    selected = autoInterval == sec,
                    onClick = { viewModel.setAutoCaptureInterval(sec) },
                    label = { Text("${sec}s") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = greenColor.copy(alpha = 0.2f), selectedLabelColor = greenColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = greenColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Risk Management Rules",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Max Capital Risk Per Trade: 1.0%", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0BEC5))
                Text("Default Stop Loss Distance: 1.5 ATR", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0BEC5))
                Text("Minimum Risk/Reward Threshold: 1 : 2.0", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB0BEC5))
            }
        }
    }
}
