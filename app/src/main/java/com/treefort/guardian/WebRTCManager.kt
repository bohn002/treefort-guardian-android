package com.treefort.guardian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import com.treefort.guardian.api.Ipv4FirstDns
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

/** Safelist name and avatar for a dial code. */
data class PeerInfo(val name: String, val icon: String? = null, val color: String? = null)

sealed class CallState {
    object Idle : CallState()
    object Calling : CallState()
    data class Incoming(val callerDialCode: String, val callerName: String) : CallState()
    data class Connected(val peerDialCode: String) : CallState()
}

object WebRTCManager {
    private const val TAG = "TreeFort-WebRTC"
    
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    val rootEglBase: EglBase by lazy { EglBase.create() }
    
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(3, TimeUnit.SECONDS)
        // Broken IPv6 routes (common on home networks) otherwise cost ~10s per AAAA address.
        .dns(Ipv4FirstDns)
        .build()
        
    private var webSocket: WebSocket? = null
    
    /** This account's own TreeFort number from `POST /api/user/app-dial-code`. Empty until signed in. */
    var myDialCode: String = ""

    /** Supplies a fresh Supabase access token at every (re)connect; tokens expire hourly. */
    var tokenProvider: () -> String? = { SupabaseManager.accessToken() }

    /** Name shown to the other side; the backend still derives identity from the verified token. */
    var callerName: String = "TreeFort Guardian"

    /** Resolves a dial code to a Safelist name and avatar for incoming calls. */
    var peerResolver: (String) -> PeerInfo? = { null }

    var activeTargetDialCode: String? = null

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    @Volatile var isConnecting: Boolean = false
        private set
    
    val isMuted = MutableStateFlow(false)
    val isSpeakerOn = MutableStateFlow(false)
    val callDurationSeconds = MutableStateFlow(0)
    var activePeerName by androidx.compose.runtime.mutableStateOf("")
    var activePeerIcon by androidx.compose.runtime.mutableStateOf<String?>(null)
    var activePeerColor by androidx.compose.runtime.mutableStateOf<String?>(null)
    var activePeerDialCode by androidx.compose.runtime.mutableStateOf("")
    
    private var localAudioTrack: org.webrtc.AudioTrack? = null
    private var timerJob: kotlinx.coroutines.Job? = null
    private var appContext: Context? = null

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    fun toggleMute() {
        isMuted.value = !isMuted.value
        localAudioTrack?.setEnabled(!isMuted.value)
    }

    @Suppress("DEPRECATION")
    fun toggleSpeaker(context: Context) {
        isSpeakerOn.value = !isSpeakerOn.value
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val speakerDevice = audioManager.availableCommunicationDevices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            }
            if (isSpeakerOn.value && speakerDevice != null) {
                audioManager.setCommunicationDevice(speakerDevice)
            } else {
                audioManager.clearCommunicationDevice()
            }
        } else {
            audioManager.isSpeakerphoneOn = isSpeakerOn.value
        }
    }

    fun startTimer() {
        stopTimer()
        timerJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            callDurationSeconds.value = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                callDurationSeconds.value += 1
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        callDurationSeconds.value = 0
        isMuted.value = false
        isSpeakerOn.value = false
    }

    fun initialize(context: Context) {
        appContext = context.applicationContext
        createNotificationChannel(context)
        if (peerConnectionFactory != null) return
        
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val options = PeerConnectionFactory.Options()
        val encoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()
            
        Log.i(TAG, "WebRTC PeerConnectionFactory initialized.")
    }

    private var currentDialCode: String = ""

    /**
     * Connects the signaling socket for this account's own TreeFort number. The Supabase access
     * token (fresh at every connect) rides as `?ticket=` and as a Bearer header; the backend binds
     * the socket to the account that owns [dialCode].
     */
    fun connectSignaling(dialCode: String = myDialCode) {
        if (dialCode.isBlank()) return
        val token = tokenProvider()
        if (token.isNullOrBlank()) {
            Log.w(TAG, "No access token yet; not connecting signaling")
            return
        }
        if (isConnecting && dialCode == currentDialCode) {
            Log.i(TAG, "Signaling handshake already in progress for $dialCode; skipping duplicate connect")
            return
        }
        myDialCode = dialCode
        currentDialCode = dialCode
        fetchIceServers()

        webSocket?.close(1000, "Reconnecting")
        isConnecting = true

        val wsUrl = "${AppConfig.wsBase}/ws/signal/$dialCode?ticket=${java.net.URLEncoder.encode(token, "UTF-8")}"
        Log.i(TAG, "Connecting to signaling server for $dialCode")

        val request = Request.Builder().url(wsUrl).addHeader("Authorization", "Bearer $token").build()
        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            private fun isCurrent(ws: WebSocket) = ws === this@WebRTCManager.webSocket

            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isCurrent(webSocket)) return
                isConnecting = false
                Log.i(TAG, "Signaling WebSocket connected for $myDialCode")
                _isOnline.value = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                // Also accept from a socket that is mid-close so in-flight signaling isn't lost
                handleIncomingSignaling(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isCurrent(webSocket)) return
                isConnecting = false
                Log.e(TAG, "Signaling WebSocket failure: ${t.message}")
                _isOnline.value = false
                if (response?.code == 401 || response?.code == 403) return // signed out or not ours: do not hammer
                reconnectSignalingWithDelay()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isCurrent(webSocket)) return
                isConnecting = false
                Log.i(TAG, "Signaling WebSocket closed ($code): $reason")
                _isOnline.value = false
                if (code != 1000) {
                    reconnectSignalingWithDelay()
                }
            }
        })
        armHandshakeWatchdog(webSocket)
    }

    /** Signed out: drop the socket and forget the account's number. */
    fun disconnectSignaling() {
        handshakeWatchdog.removeCallbacksAndMessages(null)
        reconnectHandler?.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Signed out")
        webSocket = null
        isConnecting = false
        _isOnline.value = false
        disconnect()
        myDialCode = ""
        currentDialCode = ""
    }

    private val handshakeWatchdog = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * When woken by an FCM push, the first connect can start before Android lifts the
     * background network block; that socket then stalls for ~20s. Cancel and retry fast.
     */
    private fun armHandshakeWatchdog(attempt: WebSocket?) {
        handshakeWatchdog.removeCallbacksAndMessages(null)
        handshakeWatchdog.postDelayed({
            if (attempt != null && attempt === webSocket && isConnecting && !_isOnline.value) {
                Log.w(TAG, "Signaling handshake stalled for $myDialCode; retrying")
                webSocket = null
                attempt.cancel()
                isConnecting = false
                connectSignaling(myDialCode)
            }
        }, 8000)
    }

    private var reconnectHandler: android.os.Handler? = null

    private fun reconnectSignalingWithDelay() {
        if (myDialCode.isBlank()) return
        if (reconnectHandler == null) {
            reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())
        }
        reconnectHandler?.removeCallbacksAndMessages(null)
        reconnectHandler?.postDelayed({
            if (!_isOnline.value) {
                Log.i(TAG, "Attempting signaling reconnection for $myDialCode...")
                connectSignaling(myDialCode)
            }
        }, 3000)
    }

    private fun sendSignaling(json: JSONObject) {
        val str = json.toString()
        webSocket?.send(str)
    }

    private fun handleIncomingSignaling(rawJson: String) {
        try {
            val json = JSONObject(rawJson)
            when (json.optString("type")) {
                "call_offer" -> {
                    val from = json.optString("from_dial_code", "Unknown")
                    // The backend already decided this caller may reach us; the Safelist only supplies a friendly name.
                    val known = peerResolver(from)
                    val callerName = known?.name ?: json.optString("caller_name", from)
                    val sdp = json.optString("sdp")
                    activeTargetDialCode = from
                    activePeerName = callerName
                    activePeerDialCode = from
                    activePeerIcon = known?.icon
                    activePeerColor = known?.color
                    _callState.value = CallState.Incoming(from, callerName)
                    showIncomingCallNotification(callerName, from)
                    setupPeerConnection()
                    peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            Log.i(TAG, "Set remote offer SDP success")
                        }
                    }, SessionDescription(SessionDescription.Type.OFFER, sdp))
                }
                "call_answer" -> {
                    val sdp = json.optString("sdp")
                    peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            Log.i(TAG, "Set remote answer SDP success, call active!")
                            _callState.value = CallState.Connected(activeTargetDialCode ?: "")
                            startTimer()
                        }
                    }, SessionDescription(SessionDescription.Type.ANSWER, sdp))
                }
                "ice_candidate" -> {
                    val candObj = json.optJSONObject("candidate")
                    if (candObj != null) {
                        val candidate = IceCandidate(
                            candObj.optString("sdpMid"),
                            candObj.optInt("sdpMLineIndex"),
                            candObj.optString("candidate")
                        )
                        peerConnection?.addIceCandidate(candidate)
                    }
                }
                "call_hangup", "call_rejected" -> {
                    hangup()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming signaling message: ${e.message}", e)
        }
    }

    private var cachedIceServers: List<PeerConnection.IceServer> = listOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer()
    )

    fun fetchIceServers() {
        scope.launch {
            try {
                val servers = com.treefort.guardian.api.TreeFortApi.iceServers().mapNotNull { item ->
                    if (item.urls.isEmpty()) return@mapNotNull null
                    val builder = PeerConnection.IceServer.builder(item.urls)
                    if (!item.username.isNullOrBlank() && !item.credential.isNullOrBlank()) {
                        builder.setUsername(item.username)
                        builder.setPassword(item.credential)
                    }
                    builder.createIceServer()
                }
                if (servers.isNotEmpty()) {
                    cachedIceServers = servers
                    Log.i(TAG, "Loaded ${servers.size} dynamic ICE servers including TURN relay.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch dynamic ICE servers: ${e.message}")
            }
        }
    }

    private fun getWiFiIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback && (iface.name.contains("wlan") || iface.name.contains("eth"))) {
                    val addrs = iface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                            return addr.hostAddress
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun setupPeerConnection() {
        if (peerConnection != null) return
        
        val rtcConfig = PeerConnection.RTCConfiguration(cachedIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                if (candidate != null && activeTargetDialCode != null) {
                    var sdp = candidate.sdp
                    val wifiIp = getWiFiIpAddress()
                    if (sdp.contains(".local") && wifiIp != null) {
                        sdp = sdp.replace(Regex("[a-zA-Z0-9-]+\\.local"), wifiIp)
                    }
                    val candObj = JSONObject().apply {
                        put("candidate", sdp)
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                    }
                    val msg = JSONObject().apply {
                        put("type", "ice_candidate")
                        put("target_dial_code", activeTargetDialCode)
                        put("from_dial_code", myDialCode)
                        put("candidate", candObj)
                    }
                    sendSignaling(msg)
                }
            }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {}
        })
        
        // Add Audio Track
        val audioSource = peerConnectionFactory?.createAudioSource(MediaConstraints())
        val audioTrack = peerConnectionFactory?.createAudioTrack("audio_track_0", audioSource)
        localAudioTrack = audioTrack
        if (audioTrack != null) {
            peerConnection?.addTrack(audioTrack, listOf("media_stream_0"))
        }
    }

    fun startCall(
        targetDialCode: String,
        targetName: String,
        targetIcon: String? = null,
        targetColor: String? = null,
    ) {
        if (targetDialCode.isBlank() || myDialCode.isBlank()) return
        activeTargetDialCode = targetDialCode
        activePeerName = targetName
        activePeerDialCode = targetDialCode
        activePeerIcon = targetIcon
        activePeerColor = targetColor
        _callState.value = CallState.Calling
        setupPeerConnection()

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            val msg = JSONObject().apply {
                                put("type", "call_offer")
                                put("target_dial_code", targetDialCode)
                                put("from_dial_code", myDialCode)
                                put("caller_name", callerName)
                                put("sdp", sdp.description)
                            }
                            sendSignaling(msg)
                            Log.i(TAG, "Sent call_offer to $targetDialCode")
                        }
                    }, sdp)
                }
            }
        }, constraints)
    }

    fun acceptCall() {
        cancelIncomingCallNotification()
        val target = activeTargetDialCode ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                desc?.let { sdp ->
                    peerConnection?.setLocalDescription(object : SimpleSdpObserver() {
                        override fun onSetSuccess() {
                            val msg = JSONObject().apply {
                                put("type", "call_answer")
                                put("target_dial_code", target)
                                put("from_dial_code", myDialCode)
                                put("sdp", sdp.description)
                            }
                            sendSignaling(msg)
                            _callState.value = CallState.Connected(target)
                            startTimer()
                            Log.i(TAG, "Accepted call from $target, sent answer")
                        }
                    }, sdp)
                }
            }
        }, constraints)
    }

    fun hangup() {
        activeTargetDialCode?.let { target ->
            val msg = JSONObject().apply {
                put("type", "call_hangup")
                put("target_dial_code", target)
                put("from_dial_code", myDialCode)
            }
            sendSignaling(msg)
        }
        disconnect()
    }

    fun disconnect() {
        stopTimer()
        cancelIncomingCallNotification()
        peerConnection?.close()
        peerConnection = null
        localAudioTrack = null
        activeTargetDialCode = null
        _callState.value = CallState.Idle
    }

    // v2: ringtone sound on the channel (channel sound is immutable once created, hence the new id)
    private const val CALL_NOTIFICATION_CHANNEL_ID = "treefort_incoming_calls_v2"
    private const val CALL_NOTIFICATION_ID = 4040
    const val EXTRA_INCOMING_CALL = "com.treefort.guardian.INCOMING_CALL"

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ringtone = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            val attrs = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CALL_NOTIFICATION_CHANNEL_ID,
                "TreeFort Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for incoming TreeFort audio calls"
                enableVibration(true)
                setSound(ringtone, attrs)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.deleteNotificationChannel("treefort_incoming_calls")
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showIncomingCallNotification(callerName: String, dialCode: String) {
        val ctx = appContext ?: return
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_INCOMING_CALL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(ctx, CALL_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming TreeFort Call")
            .setContentText("$callerName ($dialCode) is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setTimeoutAfter(45_000)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)

        val notification = builder.build().apply {
            flags = flags or android.app.Notification.FLAG_INSISTENT // loop ringtone until answered/cancelled
        }
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun cancelIncomingCallNotification() {
        val ctx = appContext ?: return
        val notificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(CALL_NOTIFICATION_ID)
    }
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(desc: SessionDescription?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
