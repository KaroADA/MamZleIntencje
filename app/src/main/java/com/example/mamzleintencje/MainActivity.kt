package com.example.mamzleintencje

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.mamzleintencje.data.IntentDatabase
import com.example.mamzleintencje.monitor.IntentMonitor
import com.example.mamzleintencje.monitor.MonitorService
import com.example.mamzleintencje.monitor.MonitorState
import com.example.mamzleintencje.ui.screens.MainScreen
import com.example.mamzleintencje.ui.theme.MamZłeIntencjeTheme
import com.example.mamzleintencje.ui.viewmodel.MainViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var monitor: IntentMonitor
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dao = IntentDatabase.getDatabase(applicationContext).intentRecordDao()
                return MainViewModel(application, dao) as T
            }
        }
    }
    private val TAG = "UI_MAIN"
    private var localTimerJob: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupMonitor()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        lifecycleScope.launch {
            viewModel.monitorSettings.collect { settings ->
                manageScanningLifecycle(settings)
            }
        }
        lifecycleScope.launch {
            viewModel.restartSignal.collect {
                monitor.destroy()
                delay(500)
                setupMonitor()
            }
        }

        enableEdgeToEdge()
        setContent {
            MamZłeIntencjeTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        monitor.destroy()
    }
    private fun setupMonitor() {
        monitor = IntentMonitor(this, lifecycleScope) { state ->
            runOnUiThread {
                viewModel.updateMonitorState(state)
                handleMonitorState(state)
            }
        }
        monitor.start()
    }
    private fun manageScanningLifecycle(settings: MainViewModel.MonitorSettings) {
        val intent = Intent(this, MonitorService::class.java).apply {
            putExtra("PERIOD_SECONDS", settings.fetchPeriodSeconds)
        }

        if (settings.workInBackground) {
            localTimerJob?.cancel()
            startForegroundService(intent)
        } else {
            stopService(intent)
            startLocalTimerLoop(settings.fetchPeriodSeconds)
        }
    }

    private fun startLocalTimerLoop(periodSeconds: Int) {
        localTimerJob?.cancel()
        localTimerJob = lifecycleScope.launch {
            while (true) {
                Log.d("Local", "scan")
                monitor.triggerScan()
                delay(periodSeconds * 1000L)
            }
        }
    }

    // TODO: coś w UI zamiast logów
    private fun handleMonitorState(state: MonitorState) {
        when (state) {
            is MonitorState.Connecting -> {
                Log.d(TAG, "Stan: Łączenie z Shizuku...")
            }
            is MonitorState.Active -> {
                Log.d(TAG, "Stan: Aktywny!")
            }
            is MonitorState.Error -> {
                val errorMessage = state.message
                Log.e(TAG, "Stan: Błąd -> $errorMessage")
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}
