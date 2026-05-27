package com.example.mamzleintencje.monitor

sealed interface MonitorState {
    object Connecting : MonitorState
    object Active : MonitorState
    data class Error(val message: String) : MonitorState
}