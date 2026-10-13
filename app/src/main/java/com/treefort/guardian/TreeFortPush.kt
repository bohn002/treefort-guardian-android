package com.treefort.guardian

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

/** Tries IPv4 first: broken home IPv6 routes otherwise cost ~connectTimeout per AAAA address. */
object Ipv4FirstDns : okhttp3.Dns {
    override fun lookup(hostname: String): List<java.net.InetAddress> =
        okhttp3.Dns.SYSTEM.lookup(hostname).sortedBy { it is java.net.Inet6Address }
}

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
    private val http = OkHttpClient.Builder()
        .connectTimeout(3, java.util.concurrent.TimeUnit.SECONDS)
        .dns(Ipv4FirstDns)
        .build()

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
                    Log.d(TAG, "FCM token acquired: ${task.result}")
                    registerToken(dialCode, task.result)
                } else {
                    Log.w(TAG, "FCM token fetch failed: ${task.exception?.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed: ${e.message}")
        }
    }

    fun registerToken(dialCode: String, token: String) {
        val host = WebRTCManager.signalingHost
        val isPlain = host.startsWith("192.168.") || host.contains("localhost") || host.contains("10.0.2.2") || host.contains(":8000")
        val url = "${if (isPlain) "http" else "https"}://$host/api/push/voip-token"
        val body = JSONObject()
            .put("dial_code", dialCode)
            .put("voip_token", token)
            .put("platform", "android")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url(url).post(body).apply {
            WebRTCManager.authToken?.takeIf { it.isNotBlank() }?.let { header("Authorization", "Bearer $it") }
        }.build()
        http.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.w(TAG, "FCM token registration failed: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { Log.i(TAG, "FCM token registered for $dialCode (HTTP ${it.code})") }
            }
        })
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
        WebRTCManager.showIncomingCallNotification(callerName, from)
        val dialCode = TreeFortPush.savedDialCode(this) ?: WebRTCManager.myDialCode
        if (!WebRTCManager.isOnline.value) {
            // Backend delivers the queued call_offer as soon as this socket connects
            WebRTCManager.connectSignaling(dialCode = dialCode)
        }
    }
}
