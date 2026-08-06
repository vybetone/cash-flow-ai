package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.CashFlowViewModel
import com.example.ui.components.LiveChartOverlayCanvas
import com.example.ui.components.LiveSignalCard

@Composable
fun ScreenAnalysisHudScreen(
    viewModel: CashFlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingChart.collectAsState()
    val isScreenCaptureActive by viewModel.isScreenCaptureActive.collectAsState()
    val latestSignal by viewModel.latestActiveSignal.collectAsState()
    val allSignals by viewModel.filteredSignals.collectAsState()

    val greenColor = Color(0xFF00E676)
    val redColor = Color(0xFFFF5252)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14))
            .padding(16.dp)
            .testTag("screen_analysis_hud_screen")
    ) {
        // Control Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131822)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isScreenCaptureActive) greenColor.copy(alpha = 0.5f) else Color(0xFF263238)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isScreenCaptureActive) greenColor else Color(0xFFFFB300))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isScreenCaptureActive) "SCREEN ANALYSIS ACTIVE" else "SCREEN MONITOR STANDBY",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (isScreenCaptureActive) "Capturing live trading chart frames..." else "Tap start to begin real-time analysis",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF90A4AE)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleScreenCaptureService(context) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScreenCaptureActive) redColor else greenColor,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("toggle_screen_capture_btn")
                ) {
                    Icon(
                        imageVector = if (isScreenCaptureActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isScreenCaptureActive) "STOP" else "START",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Live Chart Canvas Window
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF070A0F)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val candleWidth = w / 18f
                    var lastPrice = h * 0.5f

                    for (i in 0 until 18) {
                        val cx = i * candleWidth + (candleWidth / 2f)
                        val delta = (Math.sin(i * 0.6) * 28 + Math.cos(i * 0.4) * 18).toFloat()
                        val open = lastPrice
                        val close = open + delta
                        lastPrice = close

                        val isUp = close < open
                        val cColor = if (isUp) greenColor else redColor

                        val high = Math.min(open, close) - 12f
                        val low = Math.max(open, close) + 12f

                        drawLine(
                            color = cColor.copy(alpha = 0.7f),
                            start = androidx.compose.ui.geometry.Offset(cx, high),
                            end = androidx.compose.ui.geometry.Offset(cx, low),
                            strokeWidth = 2f
                        )

                        drawRect(
                            color = cColor,
                            topLeft = androidx.compose.ui.geometry.Offset(cx - candleWidth * 0.35f, Math.min(open, close)),
                            size = androidx.compose.ui.geometry.Size(candleWidth * 0.70f, Math.abs(close - open).coerceAtLeast(4f))
                        )
                    }
                }

                LiveChartOverlayCanvas(
                    signal = latestSignal,
                    isAnalyzing = isAnalyzing
                )

                Surface(
                    color = Color(0xCC0D1117),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE AI CHART HUD",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }

                Button(
                    onClick = {
                        val dummyBitmap = createSimulatedChartBitmap()
                        viewModel.processChartFrameBitmap(dummyBitmap, "SCREEN_ANALYSIS")
                    },
                    enabled = !isAnalyzing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00E676),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.BottomEnd)
                        .testTag("capture_and_analyze_screen_btn")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ANALYZING...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCAN CHART NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Real-Time AI Trading Signals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Surface(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${allSignals.size} Signals",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF82B1FF)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (allSignals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No trading signals generated yet.\nTap 'SCAN CHART NOW' to analyze your screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF78909C),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("signals_list")
            ) {
                items(allSignals) { signal ->
                    LiveSignalCard(
                        signal = signal,
                        onConvertToJournal = { viewModel.convertSignalToJournalEntry(it) },
                        onDelete = { viewModel.deleteSignal(it) }
                    )
                }
            }
        }
    }
}

private fun createSimulatedChartBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(800, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    paint.color = android.graphics.Color.parseColor("#070A0F")
    canvas.drawRect(0f, 0f, 800f, 500f, paint)

    val greenPaint = Paint().apply { color = android.graphics.Color.parseColor("#00E676") }
    val redPaint = Paint().apply { color = android.graphics.Color.parseColor("#FF5252") }

    var lastY = 250f
    for (i in 0 until 16) {
        val x = i * 48f + 20f
        val delta = (Math.sin(i.toDouble()) * 40 + Math.cos(i * 0.7) * 20).toFloat()
        val nextY = lastY + delta
        val isGreen = nextY < lastY

        val p = if (isGreen) greenPaint else redPaint
        canvas.drawRect(x, Math.min(lastY, nextY), x + 30f, Math.max(lastY, nextY) + 10f, p)
        lastY = nextY
    }

    return bitmap
}
