package com.example.mamzleintencje

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mamzleintencje.monitor.IntentMonitor
import com.example.mamzleintencje.monitor.MonitorState
import com.example.mamzleintencje.ui.screens.MainScreen
import com.example.mamzleintencje.ui.theme.MamZłeIntencjeTheme
import com.example.mamzleintencje.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var monitor: IntentMonitor
    private val viewModel: MainViewModel by viewModels()
    private val TAG = "UI_MAIN"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupMonitor()

        lifecycleScope.launch {
            viewModel.restartSignal.collect {
                monitor.destroy()
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
