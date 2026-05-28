package com.example.mamzleintencje.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mamzleintencje.MainActivity
import com.example.mamzleintencje.data.IntentDatabase
import com.example.mamzleintencje.data.IntentRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

class IntentMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStateChanged: (MonitorState) -> Unit
) {
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
            try {
                val outputFile = File(context.getExternalFilesDir(null), "broadcasts_dump.txt")

                val command = "dumpsys activity broadcasts history | sed -n '/Historical broadcasts/,/Historical broadcasts summary/p' > \"${outputFile.absolutePath}\""
                shizukuClient.execute(command)
                Log.d(TAG, "Shell successfully wrote dump to: ${outputFile.absolutePath}")

                //outputFile = File(context.getExternalFilesDir(null), "dumpsys.log")

                if (!outputFile.exists()) {
                    Log.e(TAG, "Dump file not found at: ${outputFile.absolutePath}")
                    return@launch
                }
                val lines = outputFile.useLines { sequence ->
                    sequence.map { it.trim() }.toList()
                }
                Log.d(TAG, "${lines.size} lines to process.")
                val records = parseLines(lines)
                var triggeredAlert = false

                for (record in records) {
                    val exists = intentDatabase.intentRecordDao().getRecordById(record.id) != null
                    if (!exists) {
                        if (record.cvssBaseScore >= 7.0) {
                            triggeredAlert = true
                        }
                    }
                }
                if (records.isNotEmpty()) {
                    intentDatabase.intentRecordDao().insertAll(records)
                    Log.d(TAG, "Processed ${records.size} records.")
                }
                if (triggeredAlert) {
                    Log.d(TAG, "SUS")
                    sendNotification()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing dumpsys scan", e)
            }
        }
    }

    private fun extractValue(line: String, prefix: String): String? {
        val index = line.indexOf(prefix)
        if (index == -1) return null
        val start = index + prefix.length
        var end = start
        while (end < line.length) {
            val char = line[end]
            if (char == ' ' || char == '}') break
            end++
        }
        val value = line.substring(start, end).trim()
        return if (value.isEmpty() || value == "null") null else value
    }

    private fun extractDateValue(line: String, prefix: String): Long? {
        val index = line.indexOf(prefix)
        if (index == -1) return null
        val start = index + prefix.length
        return if (start + 23 <= line.length) {
            val dateStr = line.substring(start, start + 23)
            try {
                val time = DATE_FORMAT.get()?.parse(dateStr)?.time
                // Ignore placeholder timestamps
                if (time != null && time > 86400000L) time else null
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    private fun generateIntentHash(
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

    private fun parseLines(lines: List<String>): List<IntentRecord> {
        var i = 0
        val res = ArrayList<IntentRecord>()

        while (i < lines.size) {
            val line = lines[i]

            if (!line.startsWith("Historical Broadcast")) {
                i++
                continue
            }

            var action: String? = null
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
            var currentSkippedTarget: String? = null
            var currentBlockInlineReason: String? = null

            val deliveredSet = LinkedHashSet<String>()
            val skippedMap = LinkedHashMap<String, String>()

            var hasDelivered = false
            var hasSkipped = false
            var hasDeferred = false

            // Extract inline package target on the header line if present
            val headerFilterMatch = RECEIVER_LIST_REGEX.find(line)
            if (headerFilterMatch != null) {
                deliveredSet.add(headerFilterMatch.groupValues[1])
                hasDelivered = true
            }

            i++
            while (i < lines.size && !lines[i].startsWith("Historical Broadcast") && !lines[i].startsWith("Historical broadcasts summary")) {
                val subLine = lines[i]

                if (subLine.startsWith("Intent {")) {
                    action = extractValue(subLine, "act=")
                } else if (subLine.contains("enqueueClockTime=")) {
                    enqTime = extractDateValue(subLine, "enqueueClockTime=")
                    dispTime = extractDateValue(subLine, "dispatchClockTime=")
                    finTime = dispTime
                } else if (subLine.contains("requiredPermissions=[")) {
                    requiredPermissions = subLine.substringAfter("requiredPermissions=[").substringBefore("]")
                } else if (subLine.startsWith("extras:")) {
                    extrasDump = subLine.substringAfter("extras:").trim()
                    val inside = extrasDump.substringAfter("{", "").substringBeforeLast("}", "")
                    extrasSize = if (inside.isNotEmpty()) inside.split(", ").size else 0
                } else if (subLine.startsWith("name=")) {
                    val activityName = subLine.substringAfter("name=").trim()
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(activityName)
                    if (currentReceiverStatus == "SKIPPED") {
                        currentSkippedTarget = activityName
                        skippedMap[activityName] = currentBlockInlineReason ?: "unknown"
                    }
                } else if (subLine.startsWith("packageName=")) {
                    val activityPkg = subLine.substringAfter("packageName=").trim()
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(activityPkg)
                    if (currentReceiverStatus == "SKIPPED") {
                        currentSkippedTarget = activityPkg
                        skippedMap[activityPkg] = currentBlockInlineReason ?: "unknown"
                    }
                } else if (subLine.contains("act=")) {
                    if (action == null) {
                        action = extractValue(subLine, "act=")
                    }
                } else if (subLine.contains("caller=")) {
                    callerPackage = extractValue(subLine, "caller=")
                    callerUid = extractValue(subLine, "uid=")?.toIntOrNull()
                } else if (subLine.contains("terminalCount=")) {
                    totalReceiverCount = extractValue(subLine, "terminalCount=")?.toIntOrNull() ?: 0
                } else if (subLine.startsWith("reason:") && currentReceiverStatus == "SKIPPED") {
                    if (currentSkippedTarget != null) {
                        skippedMap[currentSkippedTarget] = subLine.substringAfter("reason:").trim()
                    }
                }

                if (subLine.startsWith("DELIVERED")) {
                    currentReceiverStatus = "DELIVERED"
                    hasDelivered = true

                    val sTime = extractDateValue(subLine, "s:")
                    if (sTime != null && dispTime == null) {
                        dispTime = sTime
                    }
                    val eTime = extractDateValue(subLine, "e:")
                    if (eTime != null) {
                        finTime = eTime
                    }
                } else if (subLine.startsWith("SKIPPED")) {
                    currentReceiverStatus = "SKIPPED"
                    hasSkipped = true

                    currentSkippedTarget = null
                    val inlineReason = INLINE_REASON_REGEX.find(subLine)?.groupValues?.get(1)
                    currentBlockInlineReason = inlineReason?.trim()
                } else if (subLine.startsWith("DEFERRED")) {
                    currentReceiverStatus = "DEFERRED"
                    hasDeferred = true
                }

                val filterMatch = RECEIVER_LIST_REGEX.find(subLine)
                if (filterMatch != null) {
                    val pkgFromFilter = filterMatch.groupValues[1]
                    if (currentReceiverStatus == "DELIVERED") deliveredSet.add(pkgFromFilter)
                    if (currentReceiverStatus == "SKIPPED") {
                        currentSkippedTarget = pkgFromFilter
                        skippedMap[pkgFromFilter] = currentBlockInlineReason ?: "unknown"
                    }
                }

                i++
            }

            if (totalReceiverCount == 0) {
                totalReceiverCount = deliveredSet.size + skippedMap.size
            }

            val deliveredReceivers = if (deliveredSet.isNotEmpty()) deliveredSet.joinToString(", ") else null
            val skippedReceivers = if (skippedMap.isNotEmpty()) skippedMap.keys.joinToString(", ") else null
            val skipReasons = if (skippedMap.isNotEmpty()) skippedMap.values.joinToString("; ") else null

            val deliveryStatus = when {
                hasSkipped && hasDelivered -> "PARTIALLY_SKIPPED"
                hasSkipped -> "SKIPPED"
                hasDeferred && hasDelivered -> "PARTIALLY_DEFERRED"
                hasDeferred -> "DEFERRED"
                else -> "DELIVERED"
            }

            if (enqTime != null) {
                val finalAction = action ?: "BROADCAST_DELIVERY"

                val cvssResult = CvssCalculator.calculate(
                    action = finalAction,
                    callerPackage = callerPackage,
                    callerUid = callerUid,
                    requiredPermissions = requiredPermissions,
                    extrasSize = extrasSize,
                    deliveryStatus = deliveryStatus,
                    deliveredReceivers = deliveredReceivers
                )

                res.add(IntentRecord(
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
                    skippedReceivers = skippedReceivers,
                    cvssVector = cvssResult.vector,
                    cvssBaseScore = cvssResult.score
                ))
            }
        }
        return res
    }
    private fun sendNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "security_alerts_channel"

        val channel = NotificationChannel(
            channelId,
            "Security Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Vague alerts for suspicious background activity"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Security Notification")
            .setContentText("Suspicious system activity detected. Tap to review.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(3003, notification)
    }

    companion object {
        private const val TAG = "IntentMonitor"

        private val RECEIVER_LIST_REGEX = "ReceiverList\\{[^ ]+\\s+[^ ]+\\s+([^/\\s]+)".toRegex()
        private val INLINE_REASON_REGEX = "reason:([^/]+)".toRegex()

        private val DATE_FORMAT = object : ThreadLocal<SimpleDateFormat>() {
            override fun initialValue() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        }
    }
}