package com.example.mamzleintencje.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.ui.theme.MamZłeIntencjeTheme
import com.example.mamzleintencje.ui.viewmodel.MainViewModel

@Composable
fun LogListScreen(viewModel: MainViewModel) {
    val logs = viewModel.getMockIntents()
    LogListContent(logs = List(10) { logs }.flatten())
}
@Composable
fun LogListContent(logs: List<IntentRecord>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 120.dp)
    ) {
        items(logs) { log ->
            IntentLogCard(record = log)
        }
    }
}

@Composable
fun IntentLogCard(record: IntentRecord) {
    val (colorSchemeAccent, labelText) = when {
        record.cvssBaseScore >= 9.0 -> Color(0xFFB71C1C) to "CRITICAL"
        record.cvssBaseScore >= 7.0 -> Color(0xFFE53935) to "HIGH"
        record.cvssBaseScore >= 4.0 -> Color(0xFFFB8C00) to "MEDIUM"
        record.cvssBaseScore >= 0.1 -> Color(0xFFFBC02D) to "LOW"
        else -> Color(0xFF43A047) to "NONE"
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp, topEnd = 8.dp, bottomStart = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.action?.substringAfterLast(".") ?: "UNKNOWN_ACTION",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = record.callerPackage ?: "system_server",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .background(colorSchemeAccent, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format("%.1f", record.cvssBaseScore),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorSchemeAccent
                )
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Log List")
@Composable
fun LogListPreview() {
    val previewLogs = MainViewModel().getMockIntents()

    MamZłeIntencjeTheme {
        LogListContent(logs = List(10) { previewLogs }.flatten())
    }
}