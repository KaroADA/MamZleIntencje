package com.example.mamzleintencje.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentRecordDao
import com.example.mamzleintencje.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.log10

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val maxScore by viewModel.maxCvssScore.collectAsState()
    val criticalCount by viewModel.criticalCount.collectAsState()
    val mediumCount by viewModel.mediumCount.collectAsState()
    val lowCount by viewModel.lowCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val suspiciousCount by viewModel.suspiciousCount.collectAsState()
    val topApps by viewModel.topDangerousPackages.collectAsState()
    val recentIntents by viewModel.recentSuspiciousIntents.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HeroStatusCard(maxScore ?: 0.0)
        
        ThreatMatrixCard(
            critical = criticalCount,
            medium = mediumCount,
            low = lowCount,
            total = totalCount,
            suspicious = suspiciousCount
        )
        
        TopDangerousAppsCard(topApps)
        
        LiveWatchdogCard(recentIntents)
    }
}

@Composable
fun HeroStatusCard(maxScore: Double) {
    val (statusText, statusColor, statusIcon) = when {
        maxScore >= 7.0 -> Triple("HIGH SEVERITY ANOMALIES", Color(0xFFE53935), Icons.Default.GppBad)
        maxScore >= 3.0 -> Triple("MEDIUM RISK DETECTED", Color(0xFFFB8C00), Icons.Default.GppMaybe)
        else -> Triple("NO THREATS DETECTED", Color(0xFF43A047), Icons.Default.GppGood)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.15f),
            contentColor = statusColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column() {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "MAX CVSS SCORE: $maxScore",
                    style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp)
                )
            }
        }
    }
}

@Composable
fun ThreatMatrixCard(critical: Int, medium: Int, low: Int, total: Int, suspicious: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "THREAT MATRIX BREAKDOWN",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
                ThreatPieChart(
                    critical = critical,
                    medium = medium,
                    low = low,
                    modifier = Modifier.size(120.dp)
                )
                
                Spacer(modifier = Modifier.width(24.dp))
                
                // Legend and Metrics
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricItem("Critical", critical, Color(0xFFD32F2F))
                    MetricItem("Medium", medium, Color(0xFFF57C00))
                    MetricItem("None/Low", low, Color(0xFF388E3C))
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(
                        "Total Processed: $total",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Suspicious Streams: $suspicious",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suspicious > 0) Color(0xFFD32F2F) else Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).padding(end = 4.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = color)
            }
        }
        Text(
            "$label: $count",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ThreatPieChart(critical: Int, medium: Int, low: Int, modifier: Modifier = Modifier) {
    if (critical == 0 && medium == 0 && low == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("NO DATA", style = MaterialTheme.typography.labelSmall)
        }
        return
    }

    val wCritical = log10(critical.toDouble() + 1.0)
    val wMedium = log10(medium.toDouble() + 1.0)
    val wLow = log10(low.toDouble() + 1.0)
    val wTotal = (wCritical + wMedium + wLow).toFloat()

    Canvas(modifier = modifier) {
        val criticalAngle = (wCritical.toFloat() / wTotal) * 360f
        val mediumAngle = (wMedium.toFloat() / wTotal) * 360f
        val lowAngle = (wLow.toFloat() / wTotal) * 360f

        drawArc(
            color = Color(0xFFD32F2F),
            startAngle = -90f,
            sweepAngle = criticalAngle,
            useCenter = true
        )
        drawArc(
            color = Color(0xFFF57C00),
            startAngle = -90f + criticalAngle,
            sweepAngle = mediumAngle,
            useCenter = true
        )
        drawArc(
            color = Color(0xFF388E3C),
            startAngle = -90f + criticalAngle + mediumAngle,
            sweepAngle = lowAngle,
            useCenter = true
        )
    }
}

@Composable
fun TopDangerousAppsCard(apps: List<IntentRecordDao.PackageRisk>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "TOP MALICIOUS PACKAGES",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (apps.isEmpty()) {
                Text("No malicious activity detected", style = MaterialTheme.typography.bodySmall)
            } else {
                apps.forEach { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = app.callerPackage ?: "unknown",
                            style = MaterialTheme.typography.bodyMedium,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row {
                            Badge(
                                containerColor = if (app.maxScore >= 7.0) Color(0xFFD32F2F) else Color(0xFFF57C00)
                            ) {
                                Text(app.maxScore.toString(), color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${app.intentCount} hits)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveWatchdogCard(intents: List<IntentRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LIVE INTENT WATCHDOG",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (intents.isEmpty()) {
                Text("Watching for suspicious traffic...", style = MaterialTheme.typography.bodySmall)
            } else {
                intents.forEach { intent ->
                    WatchdogItem(intent)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun WatchdogItem(intent: IntentRecord) {
    val time = remember(intent.timestamp) {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(intent.timestamp))
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                intent.action?.split('.')?.lastOrNull() ?: "UNKNOWN_ACTION",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "${intent.callerPackage} • $time",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
        Badge(
            containerColor = when {
                intent.cvssBaseScore >= 7.0 -> Color(0xFFD32F2F)
                intent.cvssBaseScore >= 3.0 -> Color(0xFFF57C00)
                else -> Color(0xFF388E3C)
            }
        ) {
            Text(intent.cvssBaseScore.toString(), color = Color.White)
        }
    }
}

