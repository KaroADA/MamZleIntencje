package com.example.mamzleintencje

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mamzleintencje.ui.screens.MainScreen
import com.example.mamzleintencje.ui.theme.MamZłeIntencjeTheme
import com.example.mamzleintencje.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private lateinit var monitor: IntentMonitor
    private val viewModel: MainViewModel by viewModels()
    private val TAG = "UI_MAIN"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        monitor = IntentMonitor(this, lifecycleScope) { state ->
            runOnUiThread {
                handleMonitorState(state)
            }
        }
        monitor.start()
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

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MamZłeIntencjeTheme {
        Greeting("Android")
    }
}