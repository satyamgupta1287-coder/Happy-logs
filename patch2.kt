    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(applicationContext, BootReceiver::class.java).apply {
            action = "com.satyam.smsforwarder.RESTART"
        }
        
        val restartPendingIntent = android.app.PendingIntent.getBroadcast(
            this, 1, restartIntent,
            android.app.PendingIntent.FLAG_ONE_SHOT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmService = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmService.setAndAllowWhileIdle(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent
            )
        } else {
            alarmService.set(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                android.os.SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent
            )
        }
        
        super.onTaskRemoved(rootIntent)
    }
