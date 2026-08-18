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
