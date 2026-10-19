package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.treefort.guardian.AppStore
import com.treefort.guardian.ScreenFixture
import com.treefort.guardian.MessageKind
import com.treefort.guardian.api.QuietRule
import com.treefort.guardian.api.Trip
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
import java.time.LocalDateTime
import java.time.ZoneId

private val S = TreeFortTokens.Space

/** Vacation mode: trips that move a phone's timezone, Quiet Hours and emergency address, then revert on their own. */
@Composable
fun TripsScreen() {
    var planning by remember { mutableStateOf(ScreenFixture.at("settings-travel-plan-trip")) }
    val live = AppStore.trips.filter { it.status == "active" || it.status == "upcoming" }
    ScreenColumn {
        SectionTitle("Vacation mode")
        Text(
            "Plan a trip and the phone follows it: the right time zone, optional Quiet Hours, and a temporary emergency address. When the trip ends the phone goes back to its home settings by itself.",
            style = Tf.body, color = MutedText,
        )
        TactileButton("Plan a trip", { planning = true }, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Travel, enabled = AppStore.isManager)
        if (live.isEmpty()) TactileCard { EmptyState(Ph.Travel, "No trips planned", "Nothing changes on the phones until a trip starts.") }
        live.forEach { TripCard(it) }
    }
    if (planning) PlanTripDialog { planning = false }
}

@Composable
private fun TripCard(t: Trip) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val zone = zoneOf(t.timezone)
    val phone = AppStore.devices.firstOrNull { it.id == t.deviceId }?.name ?: "All phones"
    val address = AppStore.addresses.firstOrNull { it.id == t.addressId }
    TactileCard {
        Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                Column(Modifier.weight(1f)) {
                    Text(t.name?.takeIf { it.isNotBlank() } ?: "Trip", style = Tf.title, color = Ink)
                    Text(phone, style = Tf.bodySm, color = MutedText)
                }
                RoleChip(if (t.status == "active") "On now" else "Upcoming", if (t.status == "active") ColorWarningYellow else PillBg)
            }
            Text("${formatWhen(t.startsAt, zone)} to ${formatWhen(t.endsAt, zone)} (${t.timezone})", style = Tf.body, color = Ink)
            if (address != null) Text("Emergency address: ${address.oneLine}", style = Tf.bodySm, color = MutedText)
            if (t.quietHoursOverride) Text("Uses its own Quiet Hours during the trip.", style = Tf.bodySm, color = MutedText)
            ErrorText(error)
            TactileButton(
                if (t.status == "active") "End trip now" else "Cancel trip", { confirm = true },
                Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Close,
            )
        }
    }
    if (confirm) {
        ConfirmDialog(
            if (t.status == "active") "End this trip now?" else "Cancel this trip?",
            if (t.status == "active") "The phone goes back to its home settings. If the trip had its own address you'll confirm where the phone is." else "Nothing will change on the phone.",
            if (t.status == "active") "End trip" else "Cancel trip", destructive = true,
            onConfirm = {
                scope.launch {
                    AppStore.cancelTrip(t.id).onSuccess { confirm = false; AppStore.say("Trip updated.", MessageKind.Success) }.onFailure { error = it.errorText; confirm = false }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}

/**
 * Plan a trip. A trip address needs one specific phone (a household-wide trip can't move a
 * phone that stayed home), so the address picker only appears once a phone is chosen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanTripDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var deviceId by remember { mutableStateOf<Int?>(null) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf(ZoneId.systemDefault().id) }
    var addressId by remember { mutableStateOf<Int?>(null) }
    var override by remember { mutableStateOf(false) }
    var ruleFrom by remember { mutableStateOf("20:00") }
    var ruleTo by remember { mutableStateOf("07:00") }
    var addingAddress by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val zoneOk = runCatching { ZoneId.of(zone.trim()) }.isSuccess
    val temp = AppStore.addresses.filter { it.isTemporary }

    TfFormDialog("Plan a trip", onDismiss) {
        TfTextField(name, { name = it }, "Trip name (optional)", placeholder = "Grandma's house", enabled = !busy)
        FieldLabel("Which phone is traveling?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
            TactilePill("All phones", onClick = { deviceId = null; addressId = null }, backgroundColor = if (deviceId == null) ColorSafeGreen else CardBg)
            AppStore.devices.forEach { d -> TactilePill(d.name, onClick = { deviceId = d.id }, backgroundColor = if (deviceId == d.id) ColorSafeGreen else CardBg) }
        }
        DateTimeField("Starts", start, { start = it })
        DateTimeField("Ends", end, { end = it })
        TfTextField(zone, { zone = it }, "Time zone at the destination", placeholder = "America/Chicago", enabled = !busy,
            error = if (!zoneOk && zone.isNotBlank()) "That isn't a time zone name. Use a name like America/Chicago." else null)

        if (deviceId != null) {
            FieldLabel("Temporary emergency address (for upcoming E911)")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
                TactilePill("None", onClick = { addressId = null }, backgroundColor = if (addressId == null) ColorSafeGreen else CardBg)
                temp.forEach { a -> TactilePill(a.oneLine, onClick = { addressId = a.id }, backgroundColor = if (addressId == a.id) ColorSafeGreen else CardBg) }
            }
            TactileButton("Add a trip address", { addingAddress = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Plus)
            Text("After a trip with an address, you'll confirm where the phone is when it comes home.", style = Tf.caption, color = MutedText)
        } else {
            Text("To give a trip its own emergency address, pick a single phone above.", style = Tf.caption, color = MutedText)
        }

        ToggleRow("Different Quiet Hours on this trip", "Replaces the phone's own rules until the trip ends.", override, { override = it }, enabled = !busy)
        if (override) {
            Row(horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                TimeField("From", ruleFrom, { ruleFrom = it }, Modifier.weight(1f))
                TimeField("Until", ruleTo, { ruleTo = it }, Modifier.weight(1f))
            }
        }
        ErrorText(error)
        DialogButtons("Save trip", {
            val s = runCatching { LocalDateTime.parse(start) }.getOrNull()
            val e = runCatching { LocalDateTime.parse(end) }.getOrNull()
            when {
                s == null || e == null -> error = "Choose when the trip starts and ends."
                !e.isAfter(s) -> error = "The trip has to end after it starts."
                else -> scope.launch {
                    busy = true; error = null
                    val rules = if (override) listOf(QuietRule(name = "Trip quiet hours", days = WEEKDAYS, startTime = ruleFrom, endTime = ruleTo)) else emptyList()
                    AppStore.createTrip(name.ifBlank { null }, deviceId, s.withSecond(0).toString() + ":00", e.withSecond(0).toString() + ":00", zone.trim(), addressId, override, rules)
                        .onSuccess { AppStore.say("Trip saved.", MessageKind.Success); onDismiss() }
                        .onFailure { error = it.errorText }
                    busy = false
                }
            }
        }, onDismiss, enabled = zoneOk && start.isNotBlank() && end.isNotBlank(), loading = busy)
    }
    if (addingAddress) AddressDialog(temporary = true, onDismiss = { addingAddress = false }, onSaved = { addressId = it.id })
}
