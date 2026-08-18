package com.satyam.smsforwarder

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.provider.CallLog
import android.util.Log

class CallLogObserver(private val context: Context, handler: Handler) : ContentObserver(handler) {

    private var lastCallId = -1L
    private var lastDuration = -1L

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
                    val dur    = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                    
                    if (id == lastCallId && dur == lastDuration) return  // already processed exact state

                    lastCallId = id
                    lastDuration = dur

                    val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown"
                    val type   = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))

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

                    FirebaseForwarder.sendCallLog(context, number, typeText, durationText)
                    Log.d("CallLogObserver", "Call forwarded: $typeText from $number")
                }
            }
        } catch (e: Exception) {
            Log.e("CallLogObserver", "Failed to read call log: ${e.message}")
        }
    }
}
