package com.treefort.guardian

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.treefort.guardian.api.TreeFortApi
import kotlinx.coroutines.launch

/**
 * FCM wake-up for incoming calls while the app is backgrounded, frozen, or the phone is locked.
 *
 * Flow: backend can't reach our signaling socket -> sends a high-priority FCM data message ->
 * [TreeFortMessagingService] rings immediately and reconnects signaling, at which point the
 * backend delivers the queued call_offer (+ ICE candidates).
 */
object TreeFortPush {
    private const val TAG = "TreeFort-Push"
    private const val PREFS = "treefort_prefs"
    private const val KEY_DIAL_CODE = "push_dial_code"
    // Public client identifiers from the Firebase console (same values google-services.json carries).
    private val options = FirebaseOptions.Builder()
        .setApplicationId("1:605781285981:android:708295f33e0b19f60741b4")
        .setApiKey("AIzaSyBw9MJ01V-Z3A6yemjNcTCS9LlNNNJ32wg")
        .setProjectId("treefort-f64a4")
        .setGcmSenderId("605781285981")
        .build()

    fun ensureFirebase(context: Context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context.applicationContext, options)
        }
    }

    fun savedDialCode(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DIAL_CODE, null)

    /** Initializes Firebase and registers this device's FCM token for [dialCode]. */
    fun init(context: Context, dialCode: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DIAL_CODE, dialCode).apply()
        try {
            ensureFirebase(context)
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    Log.d(TAG, "FCM token acquired")
                    registerToken(dialCode, task.result)
                } else {
                    Log.w(TAG, "FCM token fetch failed: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed: ${e.message}")
        }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    /** Registers the token for this account's own dial code only, with the Supabase bearer. Never for another number. */
    fun registerToken(dialCode: String, token: String) {
        if (dialCode.isBlank()) return
        scope.launch {
            try {
                TreeFortApi.registerPushToken(dialCode, token)
                Log.i(TAG, "FCM token registered for $dialCode")
            } catch (e: Exception) {
                Log.w(TAG, "FCM token registration failed: ${e.message}")
            }
        }
    }

    /** Signed out: forget the number so a late token refresh cannot register for the previous account. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_DIAL_CODE).apply()
    }
}

class TreeFortMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        TreeFortPush.savedDialCode(this)?.let { TreeFortPush.registerToken(it, token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "call_offer") return
        val from = data["from_dial_code"] ?: return
        val callerName = data["caller_name"] ?: from
        Log.i("TreeFort-Push", "Wake-up push: incoming call from $callerName ($from)")

        WebRTCManager.initialize(applicationContext)
        WebRTCManager.reportIncomingFromPush(callerName, from)
        val dialCode = TreeFortPush.savedDialCode(this) ?: WebRTCManager.myDialCode
        if (!WebRTCManager.isOnline.value && dialCode.isNotBlank()) {
            // Backend delivers the queued call_offer as soon as this socket connects
            WebRTCManager.connectSignaling(dialCode)
        }
    }
}
