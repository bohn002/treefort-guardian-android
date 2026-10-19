package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.treefort.guardian.theme.upper
import com.treefort.guardian.QuietSummary
import com.treefort.guardian.api.QuietRule
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.ColorPrimaryBlue
import com.treefort.guardian.theme.InfoPale
import com.treefort.guardian.theme.LinkColor
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
import com.treefort.guardian.ScreenFixture
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
fun TreeFortTab(onOpenTrips: () -> Unit, onOpenCampfire: () -> Unit, onOpenJoin: () -> Unit) {
    val h = AppStore.selected
    var claiming by remember { mutableStateOf(ScreenFixture.at("modal-pair-phone")) }

    ScreenColumn {
        when {
            h == null -> OnboardingCards(onClaim = { claiming = true }, onJoin = onOpenJoin)
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
                if (AppStore.devices.isEmpty()) {
                    OnboardingCards(onClaim = { claiming = true }, onJoin = onOpenJoin)
                } else {
                    AppStore.devices.forEach { d -> PhoneCard(d) }
                    CampfireCard(onOpenCampfire)
                    TactileButton("Add a TreeFort phone", { claiming = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Plus)
                }
            }
        }
    }
    if (claiming) ClaimDialog { claiming = false }
}

/** First-run help for an account with no household or no phone yet (same cards and copy as iOS). */
@Composable
private fun OnboardingCards(onClaim: () -> Unit, onJoin: () -> Unit) {
    var phrase by remember { mutableStateOf(false) }
    EmptyState(Ph.Phone, "Let's set up TreeFort", "Add a TreeFort phone for your child, or join a family that already uses TreeFort.")
    OnboardingCard(Ph.ClaimCode, "Add a TreeFort phone", "Plug in your TreeFort phone, wait for the phone's chime, then enter the code.") {
        TactileButton("Enter claim code", onClaim, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.ClaimCode)
    }
    OnboardingCard(Ph.UsersThree, "Join as a co-guardian", "Another guardian can invite you to share their household. Enter the invite code they send you.") {
        TactileButton("Enter invite code", onJoin, Modifier.fillMaxWidth(), icon = Ph.AddPerson)
    }
    OnboardingCard(Ph.Link, "I have a friend phrase", "Got three words and a number from another family, like cedar-bear-412? Enter it to connect with them.") {
        TactileButton("Enter phrase", { phrase = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Link)
    }
    if (phrase) AcceptInviteDialog { phrase = false }
}

@Composable
private fun OnboardingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String, action: @Composable () -> Unit) {
    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                Icon(icon, null, tint = Ink, modifier = Modifier.size(24.dp))
                Text(title, style = Tf.title, color = Ink)
            }
            Text(body, style = Tf.body, color = MutedText)
            action()
        }
    }
}

/** A caller (app-only adult, e.g. a relative) sees only the phones they may call. */
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
    val first = ScreenFixture.active && d.id == AppStore.devices.firstOrNull()?.id
    var quiet by remember { mutableStateOf(first && ScreenFixture.at("station-quiet-hours-expanded", "modal-add-rule")) }
    var picking by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(first && ScreenFixture.at("station-device-menu")) }
    var rules by remember(d.id) { mutableStateOf<List<QuietRule>>(emptyList()) }
    var quietTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(d.id, quietTick) { AppStore.loadQuietRules(d.id).onSuccess { rules = it } }
    var releasing by remember { mutableStateOf(first && ScreenFixture.at("modal-remove-phone", "settings-modal-release-phone")) }
    var releaseError by remember { mutableStateOf<String?>(null) }
    var volume by remember(d.id, d.volumeLevel) { mutableFloatStateOf(d.volumeLevel.toFloat()) }
    val address = AppStore.addresses.firstOrNull { it.id == d.e911AddressId }
    val online = WebRTCManager.isOnline.collectAsState().value || ScreenFixture.active

    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                    Text(d.name, style = Tf.titleLg, color = Ink, modifier = Modifier.weight(1f, fill = false))
                    StatusDot(d.isOnline)
                    Text(upper(if (d.isOnline) "Online" else "Offline"), style = Tf.labelSm, color = MutedText)
                }
                Box {
                    IconButton({ menu = true }, Modifier.size(48.dp)) { Icon(Ph.More, "More for ${d.name}", tint = Ink) }
                    DropdownMenu(menu, { menu = false }) {
                        DropdownMenuItem(
                            text = { Column { Text("Emergency address", style = Tf.bodyStrong, color = Ink); Text(address?.oneLine ?: "No address saved for this phone.", style = Tf.bodySm, color = MutedText) } },
                            leadingIcon = { Icon(Ph.Pin, null, tint = Ink) },
                            onClick = { menu = false; picking = true },
                        )
                        if (!d.isOnline) d.lastSeen?.let {
                            DropdownMenuItem(text = { Text("Last seen " + formatWhen(it), style = Tf.bodySm, color = MutedText) }, onClick = {}, enabled = false)
                        }
                        if (AppStore.isOwner) DropdownMenuItem(
                            text = { Text("Remove this phone", style = Tf.bodyStrong, color = Ink) },
                            leadingIcon = { Icon(Ph.Trash, null, tint = Ink) },
                            onClick = { menu = false; releasing = true },
                        )
                    }
                }
            }
            if (d.appliedTripId != null) RoleChip("On vacation settings", ColorWarningYellow)

            d.peerDialCode?.let { number ->
                Column(
                    Modifier.fillMaxWidth().background(PillBg, RoundedCornerShape(TreeFortTokens.Radius.Md))
                        .border(TreeFortTokens.Border.Std, BorderInk, RoundedCornerShape(TreeFortTokens.Radius.Md)).padding(S.Md),
                    verticalArrangement = Arrangement.spacedBy(S.Xs),
                ) {
                    FieldLabel("TreeFort number")
                    Text(number, style = Tf.numericXl, color = LinkColor)
                }
                TactileButton(
                    "Call ${d.name}", { AppStore.placeCall(number, d.name, "phone-call", "primary-blue") },
                    Modifier.fillMaxWidth(), backgroundColor = if (d.isOnline && online) ColorSafeGreen else PillBg, icon = Ph.Call, enabled = d.isOnline && online,
                )
                if (!d.isOnline) Text("This phone is offline right now.", style = Tf.caption, color = MutedText)
            }

            Column(verticalArrangement = Arrangement.spacedBy(S.Xs)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                    Icon(Ph.Speaker, null, tint = Ink, modifier = Modifier.size(20.dp))
                    Text("Volume", style = Tf.bodyStrong, color = Ink, modifier = Modifier.weight(1f))
                    Text("${volume.toInt()}%", style = Tf.numericSm, color = MutedText)
                }
                Slider(
                    value = volume, onValueChange = { volume = it },
                    onValueChangeFinished = {
                        scope.launch { AppStore.setVolume(d.id, volume.toInt()).onFailure { AppStore.say(it.errorText, MessageKind.Danger); volume = d.volumeLevel.toFloat() } }
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = CardBg, activeTrackColor = ColorPrimaryBlue, inactiveTrackColor = PillBg),
                    modifier = Modifier.semantics { contentDescription = "Volume for ${d.name}, ${volume.toInt()} percent" },
                )
            }

            Divider()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                Icon(Ph.QuietHours, null, tint = Ink, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text("Quiet Hours", style = Tf.bodyStrong, color = Ink)
                    Text(QuietSummary.activeLabel(rules.count { it.enabled }), style = Tf.bodySm, color = MutedText)
                }
                TactilePill("Edit", onClick = { quiet = true }, icon = Ph.Edit, backgroundColor = InfoPale)
            }
            rules.take(3).forEach { r ->
                Row(
                    Modifier.fillMaxWidth().background(PillBg, RoundedCornerShape(TreeFortTokens.Radius.Md)).padding(S.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(QuietSummary.title(r), style = Tf.bodyStrong, color = if (r.enabled) Ink else MutedText)
                        Text(QuietSummary.line(r), style = Tf.bodySm, color = MutedText)
                    }
                    StatusDot(r.enabled)
                }
            }

            if (d.e911PendingReconfirmation) {
                Banner("This phone was away. Tell TreeFort where it is now (menu, Emergency address).", kind = BannerKind.Warning)
            }
        }
    }
    if (quiet) QuietHoursDialog(d) { quiet = false; quietTick++ }
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
