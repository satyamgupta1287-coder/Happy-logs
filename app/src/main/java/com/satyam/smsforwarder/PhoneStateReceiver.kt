package com.satyam.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            Log.d("PhoneStateReceiver", "Phone state changed: $state")
            
            if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // Call ended or rejected. Wait a few seconds for the call log to be written to DB, then fetch the latest
                Log.d("PhoneStateReceiver", "Call ended, waiting to sync latest log...")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    syncLatestCallLog(context)
                }, 3000)
            }
        }
    }

    private fun syncLatestCallLog(context: Context) {
        try {
            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                arrayOf(
                    android.provider.CallLog.Calls._ID,
                    android.provider.CallLog.Calls.NUMBER,
                    android.provider.CallLog.Calls.TYPE,
                    android.provider.CallLog.Calls.DURATION,
                    android.provider.CallLog.Calls.DATE
                ),
                null, null,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(android.provider.CallLog.Calls._ID))
                    
                    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val lastSavedId = prefs.getLong("last_call_id", -1L)
                    
                    if (id == lastSavedId) {
                        Log.d("PhoneStateReceiver", "Call log already processed, ignoring.")
                        return
                    }
                    
                    prefs.edit().putLong("last_call_id", id).apply()

                    val number = it.getString(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER)) ?: "Unknown"
                    val type = it.getInt(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE))
                    val dur = it.getLong(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.DURATION))
                    val date = it.getLong(it.getColumnIndexOrThrow(android.provider.CallLog.Calls.DATE))

                    val typeText = when (type) {
                        android.provider.CallLog.Calls.INCOMING_TYPE -> "📲 Incoming Call"
                        android.provider.CallLog.Calls.OUTGOING_TYPE -> "📞 Outgoing Call"
                        android.provider.CallLog.Calls.MISSED_TYPE -> "📵 Missed Call"
                        android.provider.CallLog.Calls.REJECTED_TYPE -> "🚫 Rejected Call"
                        else -> "📳 Unknown Call"
                    }

                    val mins = dur / 60
                    val secs = dur % 60
                    val durationText = if (dur > 0) "${mins}m ${secs}s" else "0s"

                    FirebaseForwarder.sendHistoricalCallLog(context, number, typeText, durationText, date)
                    Log.d("PhoneStateReceiver", "Forwarded call log from broadcast: $typeText from $number")
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneStateReceiver", "Error syncing call log: ${e.message}")
        }
    }
}
