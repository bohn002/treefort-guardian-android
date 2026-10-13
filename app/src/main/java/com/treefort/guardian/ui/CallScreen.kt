package com.treefort.guardian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.treefort.guardian.CallState
import com.treefort.guardian.WebRTCManager
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.CanvasBg
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import com.treefort.guardian.theme.upper

@Composable
private fun RoundControl(label: String, on: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TreeFortTokens.Space.Xs)) {
        Box(
            Modifier.size(64.dp)
                .background(if (on) TreeFortTokens.Palette.WarningYellow else PillBg, CircleShape)
                .border(TreeFortTokens.Border.Std, BorderInk, CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (on) TreeFortTokens.Palette.Ink else Ink, modifier = Modifier.size(28.dp)) }
        Text(upper(label), style = Tf.label, color = Ink)
    }
}

/** Full-screen call UI for ringing, calling and connected. Name and avatar come from the Safelist. */
@Composable
fun ActiveCallOverlay(callState: CallState) {
    val context = LocalContext.current
    val isMuted by WebRTCManager.isMuted.collectAsState()
    val isSpeakerOn by WebRTCManager.isSpeakerOn.collectAsState()
    val seconds by WebRTCManager.callDurationSeconds.collectAsState()

    val name = when (callState) {
        is CallState.Incoming -> WebRTCManager.activePeerName.ifBlank { callState.callerName }
        else -> WebRTCManager.activePeerName
    }
    val number = when (callState) {
        is CallState.Incoming -> callState.callerDialCode
        is CallState.Connected -> callState.peerDialCode.ifBlank { WebRTCManager.activePeerDialCode }
        else -> WebRTCManager.activePeerDialCode
    }
    val status = when (callState) {
        is CallState.Incoming -> "Incoming call"
        is CallState.Calling -> "Calling"
        is CallState.Connected -> WebRTCManager.formatDuration(seconds)
        else -> ""
    }

    Box(Modifier.fillMaxSize().background(CanvasBg).statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TreeFortTokens.Space.Md)) {
                PhosphorAvatar(WebRTCManager.activePeerIcon, WebRTCManager.activePeerColor, name.ifBlank { "Caller" }, size = 144.dp)
                Text(name.ifBlank { number }, style = Tf.display, color = Ink, textAlign = TextAlign.Center)
                if (number.isNotBlank()) Text(number, style = Tf.numeric, color = MutedText)
                Text(status, style = Tf.numericXl, color = Ink, modifier = Modifier.semantics { contentDescription = status })
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(TreeFortTokens.Space.Xl)) {
                if (callState is CallState.Connected) {
                    Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        RoundControl(if (isMuted) "Muted" else "Mute", isMuted, if (isMuted) Ph.MicOff else Ph.Mic) { WebRTCManager.toggleMute() }
                        RoundControl(if (isSpeakerOn) "Speaker on" else "Speaker", isSpeakerOn, if (isSpeakerOn) Ph.Speaker else Ph.SpeakerOff) { WebRTCManager.toggleSpeaker(context) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TreeFortTokens.Space.Md)) {
                    if (callState is CallState.Incoming) {
                        TactileButton("Answer", { WebRTCManager.acceptCall() }, Modifier.weight(1f), backgroundColor = ColorSafeGreen, icon = Ph.Call)
                    }
                    TactileButton(
                        if (callState is CallState.Incoming) "Decline" else "End call",
                        { WebRTCManager.hangup() }, Modifier.weight(1f),
                        backgroundColor = TreeFortTokens.Palette.DangerRed, icon = Ph.EndCall,
                    )
                }
            }
        }
    }
}
