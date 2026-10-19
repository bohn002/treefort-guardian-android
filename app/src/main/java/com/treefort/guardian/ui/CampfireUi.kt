package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.treefort.guardian.AppStore
import com.treefort.guardian.CampfireManager
import com.treefort.guardian.MessageKind
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens

private val S = TreeFortTokens.Space

/** Entry card on the TreeFort tab. Only owners and guardians see it; callers never get the control. */
@Composable
fun CampfireCard(onOpen: () -> Unit) {
    val st by CampfireManager.state.collectAsState()
    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                PhosphorAvatar("campfire", "warning-yellow", "Campfire")
                Column(Modifier.weight(1f)) {
                    Text("Campfire", style = Tf.title, color = Ink)
                    Text(
                        if (st.inRoom) "You're in the room" else "A family party line for all your TreeFort phones. Anyone in the room hears everyone.",
                        style = Tf.body, color = MutedText,
                    )
                }
            }
            TactileButton(if (st.inRoom) "Open the Campfire" else "Join the Campfire", onOpen, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Campfire)
        }
    }
}

/** The Campfire room: join, who is here, mute, leave. Leaving the screen does not leave the room. */
@Composable
fun CampfireScreen() {
    val st by CampfireManager.state.collectAsState()
    val muted by CampfireManager.isMuted.collectAsState()
    val h = AppStore.selected
    val canJoin = h?.isManager == true

    // Surface a closed/refused room once as a banner, then clear it.
    DisposableEffect(st.notice) {
        st.notice?.let { AppStore.say(it, MessageKind.Danger); CampfireManager.clearNotice() }
        onDispose { }
    }

    ScreenColumn {
        SectionTitle("Campfire")
        Text(
            "A room for the whole household. Every TreeFort phone reaches it with speed dial #0, and guardians can join from here. Audio only; nothing is recorded.",
            style = Tf.body, color = Ink,
        )
        if (!canJoin) {
            Banner("Only guardians can join the Campfire from the app. Callers can still call a phone directly.")
            return@ScreenColumn
        }
        if (!st.inRoom) {
            TactileButton(
                if (st.joining) "Joining..." else "Join the Campfire",
                {
                    if (!CampfireManager.join(h?.id)) AppStore.say("Can't join right now. Wait until TreeFort is connected and no call is in progress.", MessageKind.Danger)
                },
                Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Campfire, enabled = !st.joining,
            )
            return@ScreenColumn
        }
        SectionTitle("Here now")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                RosterRow(AppStore.selected?.displayName ?: "You", AppStore.selected?.avatarIcon, AppStore.selected?.avatarColor, "You" + if (muted) " (muted)" else "")
                if (st.peers.isEmpty()) Text("No one else is here yet. Phones join when someone presses #0.", style = Tf.bodySm, color = MutedText)
                st.peers.forEach { code ->
                    val info = AppStore.peerFor(code)
                    RosterRow(info?.name ?: code, info?.icon ?: "phone-call", info?.color ?: "primary-blue", code)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            TactileButton(
                if (muted) "Unmute" else "Mute", { CampfireManager.toggleMute() }, Modifier.weight(1f),
                backgroundColor = if (muted) TreeFortTokens.Palette.WarningYellow else PillBg, icon = if (muted) Ph.MicOff else Ph.Mic,
            )
            TactileButton("Leave", { CampfireManager.leave() }, Modifier.weight(1f), backgroundColor = TreeFortTokens.Palette.DangerRed, icon = Ph.EndCall)
        }
    }
}

@Composable
private fun RosterRow(name: String, icon: String?, color: String?, sub: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
        PhosphorAvatar(icon, color, name)
        Column(Modifier.weight(1f)) {
            Text(name, style = Tf.title, color = Ink, maxLines = 1)
            Text(sub, style = Tf.numericSm, color = MutedText)
        }
    }
}
