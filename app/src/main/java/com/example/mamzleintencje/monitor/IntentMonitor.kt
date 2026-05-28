package com.example.mamzleintencje.monitor

import android.content.Context
import android.util.Log
import com.example.mamzleintencje.data.IntentDatabase
import com.example.mamzleintencje.data.IntentRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Vector

class IntentMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStateChanged: (MonitorState) -> Unit
) {
    private val TAG = "IntentMonitor"
    private val intentDatabase = IntentDatabase.getDatabase(context)

    private val shizukuClient = ShizukuClient(
        context = context,
        onReady = {
            onStateChanged(MonitorState.Active)
            triggerScan()
        },
        onError = { errorMsg ->
            onStateChanged(MonitorState.Error(errorMsg))
        }
    )

    fun start() {
        onStateChanged(MonitorState.Connecting)
        shizukuClient.start()
    }

    fun destroy() {
        shizukuClient.destroy()
    }

    fun triggerScan() {
        scope.launch(Dispatchers.IO) {
            val rawOutput = shizukuClient.execute(
                "dumpsys activity broadcasts history | sed -n '/Historical broadcasts/,/Historical broadcasts summary/p'"
            )
            saveDumpToFile(rawOutput)
            val lines = rawOutput.lines().map { it.trim() }
            val count = lines.count()
            Log.d(TAG, "$count")
            Log.d(TAG, "$lines")

            val records = parseLines(lines)
            for (record in records) {
                Log.d(TAG, "$record")
            }
            if (records.isNotEmpty()) {
                intentDatabase.intentRecordDao().insertAll(records)
                Log.d(TAG, "Processed ${records.size} records.")
            }
        }
    }

    private fun saveDumpToFile(rawOutput: String) {
        try {
            val file = File(context.getExternalFilesDir(null), "broadcasts_dump.txt")
            file.writeText(rawOutput)
            Log.d(TAG, "Full raw dump successfully saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write raw dump to file", e)
        }
    }

    fun extractValue(line: String, prefix: String): String? {
        val index = line.indexOf(prefix)
        if (index == -1) return null
        val start = index + prefix.length
        val end = line.indexOf(' ', start).let { if (it == -1) line.length else it }
        return line.substring(start, end).trim()
    }
    fun extractDateValue(line: String, prefix: String): Long? {
        val index = line.indexOf(prefix)
        if (index == -1) return null
        val start = index + prefix.length
        return if (start + 23 <= line.length) {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            val dateStr = line.substring(start, start + 23)
            sdf.parse(dateStr)?.time
        } else {
            null
        }
    }
    fun generateIntentHash(
        action: String?,
        timestamp: Long,
        dispatchTime: Long?,
        extrasDump: String?
    ): String {
        val rawSignature = "$action|$timestamp|$dispatchTime|$extrasDump"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(rawSignature.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun parseLines(lines: List<String>): Vector<IntentRecord> {
        var i = 0
        val res = Vector<IntentRecord>()

        while (i < lines.size) {
            val line = lines[i]

            // New broadcast transaction always starts with "Historical Broadcast"
            if (!line.startsWith("Historical Broadcast")) {
                i++
                continue
            }

            // --- Initialize Transaction State ---
            var action: String? = null
            var pkg: String? = null
            var cmp: String? = null

            var enqTime: Long? = null
            var dispTime: Long? = null
            var finTime: Long? = null

            var requiredPermissions: String? = null
            var extrasDump: String? = null
            var extrasSize = 0
            var callerPackage: String? = null
            var callerUid: Int? = null

            var totalReceiverCount = 0
            var currentReceiverStatus: String? = null

            val deliveredSet = LinkedHashSet<String>()
            val skippedSet = LinkedHashSet<String>()
            val skipReasonsSet = LinkedHashSet<String>()

            var hasDelivered = false
            var hasSkipped = false
            var hasDeferred = false

            // Extract inline package target on the header line if present (Samsung dynamic filters)
            val headerFilterMatch = "ReceiverList\\{[^ ]+\\s+[^ ]+\\s+([^/\\s]+)".toRegex().find(line)
            if (headerFilterMatch != null) {
                deliveredSet.add(headerFilterMatch.groupValues[1])
                hasDelivered = true
            }

            // --- Sub-block Scanning ---
            i++
            while (i < lines.size && !lines[i].startsWith("Historical Broadcast") && !lines[i].startsWith("Historical broadcasts summary")) {
                val subLine = lines[i]

                if (subLine.startsWith("Intent {")) {
                    action = extractValue(subLine, "act=") ?: "act=([^ }]+)".toRegex().find(subLine)?.groupValues?.get(1)
                    pkg = extractValue(subLine, "pkg=")
                    cmp = extractValue(subLine, "cmp=")
                } else if (subLine.contains("enqueueClockTime=")) {
                    // Clock times (Parent block fallback in AOSP Emulator)
                    enqTime = extractDateValue(subLine, "enqueueClockTime=")
                    dispTime = extractDateValue(subLine, "dispatchClockTime=")
                    finTime = dispTime
                } else if (subLine.contains("requiredPermissions=[")) {
                    // Extract global required permissions list
                    requiredPermissions = subLine.substringAfter("requiredPermissions=[").substringBefore("]")
                } else if (subLine.startsWith("extras:")) {
                    extrasDump = subLine.substringAfter("extras:").trim()
                    val inside = extrasDump.substringAfter("{", "").substringBeforeLast("}", "")
                    extrasSize = if (inside.isNotEmpty()) inside.split(", ").size else 0
                } else if (subLine.startsWith("name=")) {
                    // Add receiver details based on the current parsed state context
                    val activityName = subLine.substringAfter("name=").trim()
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(activityName)
                    if (currentReceiverStatus == "SKIPPED") skippedSet.add(activityName)
                } else if (subLine.startsWith("packageName=")) {
                    val activityPkg = subLine.substringAfter("packageName=").trim()
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(activityPkg)
                    if (currentReceiverStatus == "SKIPPED") skippedSet.add(activityPkg)
                } else if (subLine.contains("act=")) {
                    val match = "act=([^ }]+)".toRegex().find(subLine)
                    if (action == null) {
                        action = match?.groupValues?.get(1)
                    }
                } else if (subLine.contains("caller=")) {
                    callerPackage = extractValue(subLine, "caller=")
                    val uidMatch = "uid=(\\d+)".toRegex().find(subLine)
                    callerUid = uidMatch?.groupValues?.get(1)?.toIntOrNull()
                } else if (subLine.contains("terminalCount=")) {
                    totalReceiverCount = extractValue(subLine, "terminalCount=")?.toIntOrNull() ?: 0
                } else if (subLine.startsWith("reason:") && currentReceiverStatus == "SKIPPED") {
                    // Extract multiline drop reason (AOSP Emulator layout)
                    skipReasonsSet.add(subLine.substringAfter("reason:").trim())
                }

                // Check Individual Receiver Delivery Lines (Sets the context for subsequent lines)
                if (subLine.startsWith("DELIVERED")) {
                    currentReceiverStatus = "DELIVERED"
                    hasDelivered = true

                    // Parse modern Samsung receiver clock times (s: and e:)
                    val sTime = extractDateValue(subLine, "s:")
                    if (sTime != null && dispTime == null) {
                        dispTime = sTime // Set to first dispatch timestamp
                    }
                    val eTime = extractDateValue(subLine, "e:")
                    if (eTime != null) {
                        finTime = eTime  // Updates to the latest complete finish timestamp
                    }
                } else if (subLine.startsWith("SKIPPED")) {
                    currentReceiverStatus = "SKIPPED"
                    hasSkipped = true

                    // Extract inline skip reason if present (Samsung layout)
                    val inlineReason = "reason:([^/]+)".toRegex().find(subLine)?.groupValues?.get(1)
                    if (inlineReason != null) {
                        skipReasonsSet.add(inlineReason.trim())
                    }
                } else if (subLine.startsWith("DEFERRED")) {
                    currentReceiverStatus = "DEFERRED"
                    hasDeferred = true
                }

                // Extract dynamic packages inside active receiver blocks
                val filterMatch = "ReceiverList\\{[^ ]+\\s+[^ ]+\\s+([^/\\s]+)".toRegex().find(subLine)
                if (filterMatch != null) {
                    val pkgFromFilter = filterMatch.groupValues[1]
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(pkgFromFilter)
                    if (currentReceiverStatus == "SKIPPED") skippedSet.add(pkgFromFilter)
                }

                i++
            }

            // Estimate total count if terminalCount was absent/zero
            if (totalReceiverCount == 0) {
                totalReceiverCount = deliveredSet.size + skippedSet.size
            }

            val deliveredReceivers = if (deliveredSet.isNotEmpty()) deliveredSet.joinToString(", ") else null
            val skippedReceivers = if (skippedSet.isNotEmpty()) skippedSet.joinToString(", ") else null
            val skipReasons = if (skipReasonsSet.isNotEmpty()) skipReasonsSet.joinToString("; ") else null

            // Determine unified operational status of the entire transaction
            val deliveryStatus = when {
                hasSkipped && hasDelivered -> "PARTIALLY_SKIPPED"
                hasSkipped -> "SKIPPED"
                hasDeferred -> "DEFERRED"
                else -> "DELIVERED"
            }

            // Commit transaction
            if (enqTime != null) {
                val finalAction = action ?: "BROADCAST_DELIVERY"
                res.addElement(IntentRecord(
                    id = generateIntentHash(finalAction, enqTime, dispTime, extrasDump),
                    timestamp = enqTime,
                    action = finalAction,
                    callerPackage = callerPackage,
                    callerUid = callerUid,
                    requiredPermissions = requiredPermissions,
                    extrasSize = extrasSize,
                    extrasDump = extrasDump,
                    dispatchTime = dispTime,
                    finishTime = finTime,
                    deliveryStatus = deliveryStatus,
                    skipReasons = skipReasons,
                    totalReceiverCount = totalReceiverCount,
                    deliveredReceivers = deliveredReceivers,
                    skippedReceivers = skippedReceivers
                ))
            }
        }
        return res
    }
}