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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalLocale

@Composable
fun LogListScreen(viewModel: MainViewModel) {
    val logs by viewModel.intentRecords.collectAsState(initial = emptyList())
    LogListContent(
        logs = logs,
        onItemClick = { id -> viewModel.markAsSeen(id) },
        getThreatActors = { id -> viewModel.getThreatActorsForIntent(id) }
    )
}

@Composable
fun LogListContent(
    logs: List<IntentRecord>,
    modifier: Modifier = Modifier,
    onItemClick: ((String) -> Unit)? = null,
    getThreatActors: (String) -> kotlinx.coroutines.flow.Flow<List<String>>
) {
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
                    onItemClick?.invoke(log.id)
                },
                getThreatActors = getThreatActors
            )
        }
    }
}

@Composable
fun IntentLogCard(
    record: IntentRecord,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    getThreatActors: (String) -> kotlinx.coroutines.flow.Flow<List<String>>
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
        // Wrap the inner Column in a Box to allow absolute positioning of the dot
        Box(modifier = Modifier.fillMaxWidth()) {
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
                                text = String.format(LocalLocale.current.platformLocale, "%.1f", record.cvssBaseScore),
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
                    val threatActors by getThreatActors(record.id).collectAsState(initial = emptyList())
                    ExpandedContent(record, threatActors)
                }
            }

            if (!record.wasSeen && record.cvssBaseScore > 7.0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 8.dp)
                        .size(6.dp)
                        .background(
                            color = colorSchemeAccent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
@Composable
fun ExpandedContent(record: IntentRecord, threatActors: List<String> = emptyList()) {
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${record.callerPackage ?: "system_server"} (${record.callerUid ?: "N/A"})",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (threatActors.contains(record.callerPackage)) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (threatActors.contains(record.callerPackage)) FontWeight.Bold else FontWeight.Normal
            )
        }

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
            DetailItemWithHighlights("Delivered to", record.deliveredReceivers, threatActors)
        }
        if (!record.skippedReceivers.isNullOrBlank()) {
            DetailItem("Skipped for", record.skippedReceivers)
        }
        if (!record.skipReasons.isNullOrBlank()) {
            DetailItem("Skip Reasons", record.skipReasons)
        }

        if (!record.riskReasons.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))
            SectionLabel("Risk Analysis")
            
            val reasons = record.riskReasons.split("; ").filter { it.isNotEmpty() }
            reasons.forEach { reason ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

        val tags = generateThreatTags(record)
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { (tag, color) ->
                        Badge(text = tag, color = color)
                    }
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
private fun DetailItemWithHighlights(label: String, value: String, highlights: List<String>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )
        
        val items = value.split(", ").map { it.trim() }
        Column {
            items.forEach { item ->
                val isThreat = highlights.contains(item)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isThreat) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isThreat) FontWeight.Bold else FontWeight.Normal
                    )
                }
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
private fun Badge(text: String, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        val textColor = if (color == MaterialTheme.colorScheme.secondaryContainer) 
            MaterialTheme.colorScheme.onSecondaryContainer 
        else Color.White
        
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun generateThreatTags(record: IntentRecord): List<Pair<String, Color>> {
    val tags = mutableListOf<Pair<String, Color>>()
    val reasons = record.riskReasons?.split(";")?.map { it.trim() } ?: emptyList()

    if (reasons.any { it.contains("shell/root", ignoreCase = true) }) {
        tags.add("Shell/Root Abuse" to Color(0xFFB71C1C))
    }
    if (reasons.any { it.contains("Critical system action", ignoreCase = true) }) {
        tags.add("Critical Action" to Color(0xFFD32F2F))
    }
    if (reasons.any { it.contains("Persistence", ignoreCase = true) }) {
        tags.add("Persistence Attempt" to Color(0xFF512DA8))
    }

    val map = record.cvssVector.split("/").associate {
        val kv = it.split(":")
        if (kv.size == 2) kv[0] to kv[1] else "" to ""
    }

    val isSandboxEscape = map["S"] == "C"

    if (isSandboxEscape) {
        tags.add("Sandbox Escape" to Color(0xFFC2185B))
    } else {
        if (reasons.any { it.contains("Organization boundary", ignoreCase = true) }) {
            tags.add("Cross-Boundary IPC" to Color(0xFFF57C00))
        }
        if (reasons.any { it.contains("Suspicious receivers", ignoreCase = true) }) {
            tags.add("Suspicious Observer" to Color(0xFFE64A19))
        }
    }
// 3. METADATA TAGS
    if (reasons.any { it.contains("Sensitive permissions", ignoreCase = true) }) {
        tags.add("Privileged Scope" to Color(0xFF7B1FA2))
    }

    if (record.extrasSize > 10 || reasons.any { it.contains("Large data payload", ignoreCase = true) }) {
        tags.add("Large Payload" to Color(0xFF00897B))
    }

    val isBlocked = reasons.any { it.contains("blocked by OS", ignoreCase = true) }
    val hasHiddenExtras = record.skipReasons?.contains("(has extras)") == true
    val isReadPermission = record.requiredPermissions?.contains("READ", ignoreCase = true) == true

    if (isBlocked && hasHiddenExtras) {
        if (isReadPermission) {
            tags.add("Data Exfiltration Attempt" to Color(0xFFD32F2F))
        } else {
            tags.add("Hidden Payload" to Color(0xFF00897B))
        }
    }

    return tags.distinctBy { it.first }
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
        LogListContent(
            logs = uniquePreviewLogs,
            getThreatActors = { kotlinx.coroutines.flow.flowOf(emptyList()) }
        )
    }
}