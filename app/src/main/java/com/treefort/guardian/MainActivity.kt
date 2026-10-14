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

    AppPrefs.init(applicationContext)
    WebRTCManager.initialize(applicationContext)
    // Signed-in state, our own TreeFort number and signaling all come from the Supabase session
    // (AppStore). Nothing is connected until an account is signed in.
    AppStore.start(applicationContext)
    applyIncomingCallWindowFlags(intent)
    if (savedInstanceState == null) handleOAuthCallback(intent)

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
    handleOAuthCallback(intent)
  }

  /** `com.treefort.guardian://login-callback#access_token=...` from web-based Sign in with Apple. */
  private fun handleOAuthCallback(intent: android.content.Intent?) {
    if (intent?.action != android.content.Intent.ACTION_VIEW) return
    val uri = intent.data ?: return
    AppStore.handleOAuthIntent(uri.scheme, uri.host, uri.encodedFragment, uri.encodedQuery)
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
    AppStore.onResume()
  }
}
