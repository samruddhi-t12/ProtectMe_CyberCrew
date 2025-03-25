package com.cybercrew.protectme


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.provider.Settings




class MainActivity : ComponentActivity() {
    // ✅ Fix: Properly defining permissions
    private val permissions = mutableListOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS) // ✅ Only for API 33+
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            add(Manifest.permission.FOREGROUND_SERVICE) // ✅ Only for API 28+
        }
    }.toTypedArray()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.all { it.value }) {
                Toast.makeText(this, "✅ All permissions granted!", Toast.LENGTH_SHORT).show()
                startSMSPollingService()  // ✅ Start background polling after permissions
            } else {
                Toast.makeText(
                    this,
                    "⚠ Permissions denied. App may not work properly!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionsIfNeeded()  // ✅ Ask for SMS permissions first
        requestAutoStartPermission(this)  // ✅ Ask for AutoStart
        startSMSPolling(this)  // ✅ Start polling service
        SMSUtils.initialize(this)
        SMSUtils.fetchLatestSMS(this)


        }
    private fun requestPermissionsIfNeeded() {
        if (!hasPermissions()) {
            requestPermissionLauncher.launch(permissions)
        } else {
            startSMSPollingService()  // ✅ If already granted, start polling service
        }
    }
    private fun hasPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }}
    private fun startSMSPollingService() {
        val serviceIntent = Intent(this, SMSPollingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent) // ✅ For Android 8+
        } else {
            startService(serviceIntent)
        }
    }

}

fun startSMSPolling(context: Context) {
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val runnable = object : Runnable {
        override fun run() {
            SMSUtils.fetchLatestSMS(context) // ✅ Fetch only NEW messages
            Log.d("SMS_DEBUG", "🔄 Polling for new SMS...")
            handler.postDelayed(this, 5000) // 🔄 Repeat every 5 sec
        }
    }
    handler.post(runnable) // Start polling
}
fun requestAutoStartPermission(context: Context) {
    val manufacturers = listOf("xiaomi", "oppo", "vivo", "realme", "oneplus", "Letv", "asus", "huawei", "samsung")
    val manufacturer = Build.MANUFACTURER.lowercase()  // ✅ Get phone brand

    if (manufacturers.contains(manufacturer)) {
        try {
            val intent = Intent()
            intent.setClassName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Log.w("PERMISSION_DEBUG", "⚠ Auto-start permission not supported on this device.")
        }
    }
}




    // ✅ Fix: Ensure AppScreen exists
    @Composable
    fun AppScreen(modifier: Modifier = Modifier) {
        Text("Hello, this is AppScreen!", modifier = modifier)
    }





@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}


