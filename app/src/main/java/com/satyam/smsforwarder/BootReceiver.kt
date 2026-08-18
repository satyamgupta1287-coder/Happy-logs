package com.satyam.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "com.satyam.smsforwarder.RESTART") {
            val serviceIntent = Intent(context, ForwarderForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
