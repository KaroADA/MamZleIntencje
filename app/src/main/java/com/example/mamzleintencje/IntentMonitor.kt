package com.example.mamzleintencje

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            val rawOutput = shizukuClient.execute("dumpsys activity broadcasts history | tail -n 25")
            Log.d(TAG, rawOutput)
        }
    }
}