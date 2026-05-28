package com.example.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExfiltrateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("IntentGen", "ExfiltrateReceiver caught: ${intent.action}")
    }
}