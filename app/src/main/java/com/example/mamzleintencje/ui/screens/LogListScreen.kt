package com.example.mamzleintencje.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentType
import com.example.mamzleintencje.ui.theme.MamZłeIntencjeTheme
import com.example.mamzleintencje.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogListScreen(viewModel: MainViewModel) {
    val logs by viewModel.intentRecords.collectAsState(initial = emptyList())
    LogListContent(logs = logs)
}
@Composable
fun LogListContent(logs: List<IntentRecord>, modifier: Modifier = Modifier) {
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 120.dp)
    ) {
        items(logs, key = { it.id }) { log ->
            IntentLogCard(
                record = log,
                expanded = expandedId == log.id,
                onExpandToggle = {
                    expandedId = if (expandedId == log.id) null else log.id
                }
            )
        }
    }
}

@Composable
fun IntentLogCard(
    record: IntentRecord,
    expanded: Boolean,
    onExpandToggle: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = timeFormatter.format(Date(record.timestamp))

    val containerColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surfaceColorAtElevation(if (expanded) 8.dp else 2.dp),
        label = "CardColor"
    )

    val (colorSchemeAccent, labelText) = when {
        record.cvssBaseScore >= 9.0 -> Color(0xFFB71C1C) to "CRITICAL"
        record.cvssBaseScore >= 7.0 -> Color(0xFFE53935) to "HIGH"
        record.cvssBaseScore >= 4.0 -> Color(0xFFFB8C00) to "MEDIUM"
        record.cvssBaseScore >= 0.1 -> Color(0xFFFBC02D) to "LOW"
        else -> Color(0xFF43A047) to "NONE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .animateContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onExpandToggle()
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column {
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
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.callerPackage ?: "system_server",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " • $timeString",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .background(colorSchemeAccent, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", record.cvssBaseScore),
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

            if (expanded) {
                ExpandedContent(record)
            }
        }
    }
}

@Composable
fun ExpandedContent(record: IntentRecord) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 1. Caller
        SectionLabel("Caller")
        Text(
            text = "${record.callerPackage ?: "system_server"} (${record.callerUid ?: "N/A"})",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!record.requiredPermissions.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            SectionLabel("Required Permissions")
            Text(
                text = record.requiredPermissions,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // 2. Delivery
        SectionLabel("Delivery Details")
        Row(verticalAlignment = Alignment.CenterVertically) {
            val statusColor = when (record.deliveryStatus) {
                "DELIVERED" -> Color(0xFF43A047)
                "SKIPPED" -> Color(0xFFE53935)
                "PARTIALLY_SKIPPED" -> Color(0xFFFB8C00)
                else -> MaterialTheme.colorScheme.outline
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = record.deliveryStatus ?: "UNKNOWN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Total Receivers: ${record.totalReceiverCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!record.deliveredReceivers.isNullOrBlank()) {
            DetailItem("Delivered to", record.deliveredReceivers)
        }
        if (!record.skippedReceivers.isNullOrBlank()) {
            DetailItem("Skipped for", record.skippedReceivers)
        }
        if (!record.skipReasons.isNullOrBlank()) {
            DetailItem("Skip Reasons", record.skipReasons)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))

        // 3. CVSS
        SectionLabel("CVSS Vector")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        ) {
            Text(
                text = record.cvssVector,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.outline
            )
        }

        val tags = parseCvssTags(record.cvssVector)
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row {
                tags.forEach { tag ->
                    Badge(text = tag)
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }

        val processingTime = if (record.dispatchTime != null && record.finishTime != null) {
            record.finishTime - record.dispatchTime
        } else null

        if (record.extrasSize > 0 || record.extrasDump != null || processingTime != null) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel("Payload & Performance")

            if (record.extrasSize > 0) {
                Text(
                    text = "Payload: ${record.extrasSize} items",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            record.extrasDump?.let { dump ->
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = dump,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFD4D4D4)
                    )
                }
            }

            processingTime?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Processing Time: $it ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun Badge(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun parseCvssTags(vector: String): List<String> {
    val tags = mutableListOf<String>()
    val parts = vector.split("/")
    for (part in parts) {
        val kv = part.split(":")
        if (kv.size == 2) {
            when (kv[0]) {
                "AV" -> if (kv[1] == "N") tags.add("Network")
                "UI" -> if (kv[1] == "N") tags.add("No Interaction")
                "PR" -> if (kv[1] == "N") tags.add("No Privileges")
            }
        }
    }
    return tags.take(3)
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true, name = "Log List")
@Composable
fun LogListPreview() {
    val previewLogs = MainViewModel.getMockIntents()
    val uniquePreviewLogs = List(2) { iteration ->
        previewLogs.map { it.copy(id = "${it.id}_$iteration") }
    }.flatten()

    MamZłeIntencjeTheme {
        LogListContent(logs = uniquePreviewLogs)
    }
}