        // Let's also auto-sync call logs once permissions are granted for the first time
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_synced_logs", false)) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                syncPreviousCallLogs()
                prefs.edit().putBoolean("has_synced_logs", true).apply()
            }, 3000)
        }
