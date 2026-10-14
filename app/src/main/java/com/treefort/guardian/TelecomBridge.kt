package com.treefort.guardian

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log
import androidx.annotation.RequiresApi

/**
 * Self-managed Telecom integration (DRIFT-39). TreeFort keeps owning the audio and the UI; Telecom
 * is told about each 1:1 call so Android can coordinate it with other calls (hold or reject a clash),
 * keep the process alive on the lock screen, and route Bluetooth and wearables. Self-managed calls
 * never appear in the system dialer; the incoming-call screen is ours, shown from
 * [TreeFortConnection.onShowIncomingCallUi]. Anything that fails falls back to the plain
 * notification path, so a Telecom problem can never make a call unreachable.
 */
object TelecomBridge {
    private const val TAG = "TreeFort-Telecom"
    private const val ACCOUNT_ID = "treefort_calls"
    const val SCHEME = "treefort"
    const val EXTRA_NAME = "com.treefort.guardian.CALLER_NAME"

    @Volatile internal var connection: TreeFortConnection? = null
    @Volatile private var pending: String? = null
    @Volatile private var registered = false

    private val supported get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handle(ctx: Context) =
        PhoneAccountHandle(ComponentName(ctx, TreeFortConnectionService::class.java), ACCOUNT_ID)

    fun register(ctx: Context) {
        if (!supported || registered) return
        runCatching {
            val tm = ctx.getSystemService(TelecomManager::class.java) ?: return
            val account = PhoneAccount.builder(handle(ctx), "TreeFort")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .addSupportedUriScheme(SCHEME)
                .build()
            tm.registerPhoneAccount(account)
            registered = true
        }.onFailure { Log.w(TAG, "Phone account not registered: ${it.message}") }
    }

    /** Tells Telecom a call is ringing; [fallback] posts the plain notification if that is impossible. */
    fun reportIncoming(ctx: Context, name: String, dialCode: String, fallback: () -> Unit) {
        if (connection?.dialCode == dialCode || pending == dialCode) return // push + socket both announce
        if (!supported || !registered) return fallback()
        pending = dialCode
        runCatching {
            val extras = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, Uri.fromParts(SCHEME, dialCode, null))
                putString(EXTRA_NAME, name)
            }
            ctx.getSystemService(TelecomManager::class.java).addNewIncomingCall(handle(ctx), extras)
        }.onFailure {
            Log.w(TAG, "Incoming call not reported to Telecom: ${it.message}")
            pending = null
            fallback()
        }
    }

    @android.annotation.SuppressLint("MissingPermission") // MANAGE_OWN_CALLS (declared) covers self-managed placeCall; failures are caught below
    fun reportOutgoing(ctx: Context, name: String, dialCode: String) {
        if (!supported || !registered || connection != null) return
        runCatching {
            val extras = Bundle().apply {
                putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle(ctx))
                putString(EXTRA_NAME, name)
            }
            ctx.getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts(SCHEME, dialCode, null), extras)
        }.onFailure { Log.w(TAG, "Outgoing call not reported to Telecom: ${it.message}") }
    }

    fun setActive() { connection?.setActive() }

    /** Call over (either side): release the Telecom connection. Idempotent. */
    fun end() {
        pending = null
        val c = connection ?: return
        connection = null
        c.setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
        c.destroy()
    }

    internal fun attach(c: TreeFortConnection) { pending = null; connection = c }
    internal fun clearPending() { pending = null }
}

@RequiresApi(Build.VERSION_CODES.O)
class TreeFortConnectionService : ConnectionService() {
    override fun onCreateIncomingConnection(account: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        val dial = request?.address?.schemeSpecificPart ?: ""
        val name = request?.extras?.getString(TelecomBridge.EXTRA_NAME) ?: dial
        return TreeFortConnection(dial, name, incoming = true).also { TelecomBridge.attach(it); it.setRinging() }
    }

    override fun onCreateOutgoingConnection(account: PhoneAccountHandle?, request: ConnectionRequest?): Connection {
        val dial = request?.address?.schemeSpecificPart ?: ""
        val name = request?.extras?.getString(TelecomBridge.EXTRA_NAME) ?: dial
        return TreeFortConnection(dial, name, incoming = false).also { TelecomBridge.attach(it); it.setDialing() }
    }

    override fun onCreateIncomingConnectionFailed(account: PhoneAccountHandle?, request: ConnectionRequest?) {
        TelecomBridge.clearPending()
        val dial = request?.address?.schemeSpecificPart ?: return
        // Telecom refused (e.g. another call owns the line): still ring the old way.
        WebRTCManager.showIncomingCallNotification(request.extras?.getString(TelecomBridge.EXTRA_NAME) ?: dial, dial)
    }

    override fun onCreateOutgoingConnectionFailed(account: PhoneAccountHandle?, request: ConnectionRequest?) {
        TelecomBridge.clearPending() // the WebRTC call itself already started; Telecom just isn't tracking it
    }
}

@RequiresApi(Build.VERSION_CODES.O)
class TreeFortConnection(
    val dialCode: String,
    private val name: String,
    incoming: Boolean,
) : Connection() {
    init {
        connectionProperties = PROPERTY_SELF_MANAGED
        audioModeIsVoip = true
        connectionCapabilities = CAPABILITY_MUTE
        setAddress(Uri.fromParts(TelecomBridge.SCHEME, dialCode, null), TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED)
        if (!incoming) setInitializing()
    }

    override fun onShowIncomingCallUi() {
        WebRTCManager.showIncomingCallNotification(name, dialCode)
    }

    override fun onAnswer() {
        WebRTCManager.acceptCall()
        setActive()
    }

    override fun onAnswer(videoState: Int) = onAnswer()

    override fun onReject() { WebRTCManager.hangup() }
    override fun onDisconnect() { WebRTCManager.hangup() }
    override fun onAbort() { WebRTCManager.hangup() }
}
