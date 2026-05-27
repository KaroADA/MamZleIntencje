package com.example.mamzleintencje.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mamzleintencje.monitor.MonitorState
import com.example.mamzleintencje.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val monitorState by viewModel.monitorState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Monitor Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonitorStatusIcon(state = monitorState)
                    Text(
                        text = "Status: ${getMonitorStatusText(monitorState)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (monitorState is MonitorState.Error) {
                    Text(
                        text = (monitorState as MonitorState.Error).message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = { viewModel.restartMonitor() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Restart Shizuku Connection")
                }
            }
        }
    }
}

@Composable
fun MonitorStatusIcon(state: MonitorState) {
    when (state) {
        is MonitorState.Connecting -> {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        is MonitorState.Active -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Active",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
        is MonitorState.Error -> {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun getMonitorStatusText(state: MonitorState): String {
    return when (state) {
        is MonitorState.Connecting -> "Connecting to Shizuku"
        is MonitorState.Active -> "Active"
        is MonitorState.Error -> "Error"
    }
}
