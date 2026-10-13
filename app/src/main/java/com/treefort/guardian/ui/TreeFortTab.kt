package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.treefort.guardian.AppStore
import com.treefort.guardian.MessageKind
import com.treefort.guardian.WebRTCManager
import com.treefort.guardian.api.Device
import com.treefort.guardian.errorText
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.ColorWarningYellow
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

private val S = TreeFortTokens.Space

/** The TreeFort tab: the household's phones, calling, volume, Quiet Hours, emergency address. */
@Composable
fun TreeFortTab(onOpenTrips: () -> Unit, onOpenJoin: () -> Unit) {
    val h = AppStore.selected
    var claiming by remember { mutableStateOf(false) }

    ScreenColumn {
        when {
            h == null -> FirstRunCard(onClaim = { claiming = true }, onJoin = onOpenJoin)
            !h.isManager -> CallerHome()
            else -> {
                val active = AppStore.trips.firstOrNull { it.status == "active" }
                if (active != null) {
                    Banner(
                        "Vacation mode is on" + (active.name?.let { ": $it" } ?: "") + ". Ends " + formatWhen(active.endsAt, zoneOf(active.timezone)) + ", then the phone goes back to its home settings by itself.",
                        kind = BannerKind.Info,
                        action = { TactileButton("Trips", onOpenTrips, backgroundColor = CardBg) },
                    )
                }
                SectionTitle("TreeFort phones", trailing = {
                    TactileButton("Add", { claiming = true }, backgroundColor = ColorSafeGreen, icon = Ph.Plus)
                })
                if (AppStore.devices.isEmpty()) {
                    TactileCard {
                        EmptyState(
                            Ph.Phone, "No TreeFort phone yet",
                            "Plug in your TreeFort phone, wait for the phone's chime, then enter the code it gives you.",
                            action = { TactileButton("Add a TreeFort phone", { claiming = true }, backgroundColor = ColorSafeGreen) },
                        )
                    }
                }
                AppStore.devices.forEach { d -> PhoneCard(d) }
            }
        }
    }
    if (claiming) ClaimDialog { claiming = false }
}

@Composable
private fun FirstRunCard(onClaim: () -> Unit, onJoin: () -> Unit) {
    SectionTitle("Welcome to TreeFort")
    TactileCard {
        EmptyState(
            Ph.Phone, "Set up your first TreeFort phone",
            "Plug it in, wait for the phone's chime, then enter the code it gives you.",
            action = {
                Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                    TactileButton("Add a TreeFort phone", onClaim, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Plus)
                    TactileButton("I have an invite code", onJoin, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.AddPerson)
                }
            },
        )
    }
}

/** A caller (app-only adult, e.g. Uncle Bob) sees only the phones they may call. */
@Composable
private fun CallerHome() {
    val h = AppStore.selected ?: return
    SectionTitle("Call a TreeFort phone")
    Text("You're a caller in ${h.name}. You can call these TreeFort phones, and they can call you.", style = Tf.body, color = Ink)
    if (h.phones.isEmpty()) {
        TactileCard { EmptyState(Ph.Phone, "No phones to call yet", "When a guardian adds a TreeFort phone you can reach, it shows up here.") }
    }
    h.phones.forEach { p ->
        TactileCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                PhosphorAvatar("phone-call", "primary-blue", p.name)
                Column(Modifier.weight(1f)) {
                    Text(p.name, style = Tf.title, color = Ink)
                    Text(p.peerDialCode ?: "No TreeFort number yet", style = Tf.numericSm, color = MutedText)
                }
                StatusDot(p.isOnline)
            }
            if (p.canCall && p.peerDialCode != null) {
                Spacer(Modifier.height(S.Md))
                TactileButton("Call ${p.name}", { AppStore.placeCall(p.peerDialCode, p.name, "phone-call", "primary-blue") }, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Call, enabled = p.isOnline)
                if (!p.isOnline) Text("This phone is offline right now.", style = Tf.bodySm, color = MutedText)
            }
        }
    }
}

@Composable
private fun PhoneCard(d: Device) {
    val scope = rememberCoroutineScope()
    var quiet by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var releasing by remember { mutableStateOf(false) }
    var releaseError by remember { mutableStateOf<String?>(null) }
    var volume by remember(d.id, d.volumeLevel) { mutableFloatStateOf(d.volumeLevel.toFloat()) }
    val address = AppStore.addresses.firstOrNull { it.id == d.e911AddressId }
    val online by WebRTCManager.isOnline.collectAsState()

    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                PhosphorAvatar("phone-call", "primary-blue", d.name)
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = Tf.title, color = Ink)
                    Text(d.peerDialCode ?: "Setting up", style = Tf.numericSm, color = MutedText)
                    Text(
                        if (d.isOnline) "Online" else d.lastSeen?.let { "Last seen " + formatWhen(it) } ?: "Offline",
                        style = Tf.bodySm, color = MutedText,
                    )
                }
                StatusDot(d.isOnline)
            }
            if (d.appliedTripId != null) RoleChip("On vacation settings", ColorWarningYellow)

            if (d.peerDialCode != null) {
                TactileButton(
                    "Call ${d.name}", { AppStore.placeCall(d.peerDialCode, d.name, "phone-call", "primary-blue") },
                    Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Call, enabled = d.isOnline && online,
                )
            }

            Divider()
            FieldLabel("Phone volume")
            Slider(
                value = volume, onValueChange = { volume = it },
                onValueChangeFinished = {
                    scope.launch { AppStore.setVolume(d.id, volume.toInt()).onFailure { AppStore.say(it.errorText, MessageKind.Danger); volume = d.volumeLevel.toFloat() } }
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = ColorSafeGreen, inactiveTrackColor = PillBg),
                modifier = Modifier.semantics { contentDescription = "Phone volume ${volume.toInt()} percent" },
            )

            Divider()
            FieldLabel("Emergency address (for upcoming E911)")
            Text(address?.oneLine ?: "No address saved for this phone.", style = Tf.body, color = Ink)
            if (d.e911PendingReconfirmation) {
                Banner(
                    "This phone was away. Tell TreeFort where it is now.",
                    kind = BannerKind.Warning,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(S.Md), modifier = Modifier.fillMaxWidth()) {
                TactileButton(if (address == null) "Choose address" else "Change", { picking = true }, Modifier.weight(1f), backgroundColor = CardBg, icon = Ph.Pin)
                TactileButton("Quiet Hours", { quiet = true }, Modifier.weight(1f), backgroundColor = CardBg, icon = Ph.QuietHours)
            }
            if (AppStore.isOwner) {
                TactileButton("Remove this phone", { releasing = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Trash)
            }
        }
    }
    if (quiet) QuietHoursDialog(d) { quiet = false }
    if (picking) AddressPickerDialog(d) { picking = false }
    if (releasing) {
        ConfirmDialog(
            "Remove ${d.name}?",
            (releaseError?.let { "$it\n\n" } ?: "") + "The phone stops working with this household and you'll need a new claim code to add it again.",
            "Remove", destructive = true,
            onConfirm = {
                scope.launch {
                    AppStore.releaseDevice(d.id)
                        .onSuccess { releasing = false; releaseError = null; AppStore.say("Phone removed.", MessageKind.Success) }
                        .onFailure { releaseError = it.errorText }
                }
            },
            onDismiss = { releasing = false; releaseError = null },
        )
    }
}

fun zoneOf(id: String): java.time.ZoneId = runCatching { java.time.ZoneId.of(id) }.getOrDefault(java.time.ZoneId.systemDefault())
