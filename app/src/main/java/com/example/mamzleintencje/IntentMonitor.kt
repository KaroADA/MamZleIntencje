package com.example.mamzleintencje

import android.content.Context
import android.util.Log
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Vector

class IntentMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onStateChanged: (MonitorState) -> Unit
) {
    private val TAG = "IntentMonitor"
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
            val rawOutput =
                shizukuClient.execute("dumpsys activity broadcasts history | tail -n 500")
            val lines = rawOutput.lines().map { it.trim() }
            val records = parseLines(lines)
            for (record in records) {
                Log.d(TAG, "$record")
            }
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

    fun parseLines(lines: List<String>): Vector<IntentRecord> {
        var i = 0
        val res = Vector<IntentRecord>()
        while (i < lines.size) {
            val line = lines[i]
            if (!line.startsWith("#")) {
                i++
                continue
            }

            val action = extractValue(line, "act=")
            val pkg = extractValue(line, "pkg=")
            val cmp = extractValue(line, "cmp=")

            var enqTime: Long? = null
            var dispTime: Long? = null
            var finTime: Long? = null
            var extrasDump: String? = null
            var extrasSize = 0

            i++
            while (i < lines.size && !lines[i].startsWith("#")) {
                val subLine = lines[i]
                if (subLine.contains("enq=")) {
                    enqTime = extractDateValue(subLine, "enq=")
                    dispTime = extractDateValue(subLine, "disp=")
                    finTime = extractDateValue(subLine, "fin=")
                } else if (subLine.startsWith("extras:")) {
                    extrasDump = subLine.substringAfter("extras:").trim()
                    val inside = extrasDump.substringAfter("{", "").substringBeforeLast("}", "")
                    extrasSize = if (inside.isNotEmpty()) inside.split(", ").size else 0
                }
                i++
            }

            if (action != null && enqTime != null) {
                res.addElement( IntentRecord(
                    timestamp = enqTime,
                    action = action,
                    callerPackage = null,
                    targetComponent = cmp ?: pkg,
                    intentType = IntentType.BROADCAST,
                    extrasSize = extrasSize,
                    extrasDump = extrasDump,
                    dispatchTime = dispTime,
                    finishTime = finTime
                ))
            }
        }
        return res
    }
}