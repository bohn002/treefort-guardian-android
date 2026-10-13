package com.treefort.guardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.treefort.guardian.theme.TreeFortGuardianTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val prefs = getSharedPreferences("treefort_prefs", MODE_PRIVATE)
    com.treefort.guardian.theme.ThemeConfig.isBoringTextEnabled = prefs.getBoolean("boring_text_enabled", false)
    val savedRole = prefs.getString("active_role", ProfileRole.FAMILY_CALLER_ONLY.name)
    val role = try {
        ProfileRole.valueOf(savedRole ?: ProfileRole.FAMILY_CALLER_ONLY.name)
    } catch (_: Exception) {
        ProfileRole.FAMILY_CALLER_ONLY
    }
    AndroidProfileManager.activeRole = role
    AndroidProfileManager.phoneTreeFortNumber = if (role == ProfileRole.FAMILY_CALLER_ONLY) "*38912" else "*10001"

    WebRTCManager.initialize(applicationContext)
    val startDialCode = if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) "*38912" else AndroidProfileManager.phoneTreeFortNumber
    WebRTCManager.connectSignaling(dialCode = startDialCode)
    TreeFortPush.init(applicationContext, startDialCode)
    applyIncomingCallWindowFlags(intent)

    val permissionsToRequest = mutableListOf(android.Manifest.permission.RECORD_AUDIO)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    val missingPermissions = permissionsToRequest.filter {
        androidx.core.content.ContextCompat.checkSelfPermission(this, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    if (missingPermissions.isNotEmpty()) {
        androidx.core.app.ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1001)
    }

    enableEdgeToEdge()
    setContent {
      TreeFortGuardianTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    applyIncomingCallWindowFlags(intent)
  }

  /** Only an incoming-call launch may appear over the keyguard; normal launches stay behind the lock. */
  private fun applyIncomingCallWindowFlags(intent: android.content.Intent?) {
    val incoming = intent?.getBooleanExtra(WebRTCManager.EXTRA_INCOMING_CALL, false) == true
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(incoming)
      setTurnScreenOn(incoming)
    }
  }

  override fun onResume() {
    super.onResume()
    val activeDialCode = if (AndroidProfileManager.activeRole == ProfileRole.FAMILY_CALLER_ONLY) "*38912" else AndroidProfileManager.phoneTreeFortNumber
    if (!WebRTCManager.isOnline.value) {
      WebRTCManager.connectSignaling(dialCode = activeDialCode)
    }
  }
}
