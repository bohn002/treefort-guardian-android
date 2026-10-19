package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.treefort.guardian.AppStore
import com.treefort.guardian.ScreenFixture
import com.treefort.guardian.MessageKind
import com.treefort.guardian.api.Address
import com.treefort.guardian.api.Device
import com.treefort.guardian.api.QuietRule
import com.treefort.guardian.errorText
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.launch

private val S = TreeFortTokens.Space

val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Formats a claim code as the phone shows it: NNN-NNN. */
fun formatClaimCode(raw: String): String {
    val d = raw.filter(Char::isDigit).take(6)
    return if (d.length > 3) d.substring(0, 3) + "-" + d.substring(3) else d
}

/**
 * Real claim flow (`POST /api/devices/claim`). The code is the 6-digit one the TreeFort phone
 * announces after it connects to Wi-Fi.
 */
@Composable
fun ClaimDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var kid by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TfFormDialog("Add a TreeFort phone", onDismiss) {
        Text("Plug in the TreeFort phone and connect it to Wi-Fi, wait for the phone's chime, then enter the code.", style = Tf.body, color = Ink)
        TfTextField(
            code, { code = formatClaimCode(it); error = null }, "Claim code", placeholder = "123-456",
            keyboardType = KeyboardType.NumberPassword, enabled = !busy,
            supportingText = "The 6-digit code expires after 15 minutes. If it has, restart setup on the phone for a new one.",
        )
        TfTextField(kid, { kid = it }, "Child's name", placeholder = "Maya", enabled = !busy)
        TfTextField(room, { room = it }, "Room", placeholder = "Bedroom", enabled = !busy)
        ErrorText(error)
        DialogButtons("Add phone", {
            scope.launch {
                busy = true
                AppStore.claimDevice(code, kid.ifBlank { "Child" }, room.ifBlank { "Bedroom" })
                    .onSuccess { AppStore.say("TreeFort phone added.", MessageKind.Success); onDismiss() }
                    .onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = code.filter(Char::isDigit).length == 6, loading = busy)
    }
}

/** One or more weekly quiet-hours rules for a phone: `GET/PUT /api/devices/{id}/quiet-hours`. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuietHoursDialog(device: Device, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val rules = remember { mutableStateListOf<QuietRule>() }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(device.id) {
        AppStore.loadQuietRules(device.id)
            .onSuccess { rules.clear(); rules.addAll(it) }
            .onFailure { error = it.errorText }
        if (ScreenFixture.at("modal-add-rule")) rules.add(QuietRule(name = "Bedtime", enabled = true, days = WEEKDAYS, startTime = "20:00", endTime = "07:00"))
        loading = false
    }

    TfFormDialog("Quiet Hours: ${device.name}", onDismiss) {
        Text("During Quiet Hours the phone won't ring. Calls from the Safelist wait in the Logbook.", style = Tf.body, color = Ink)
        if (loading) Text("Loading", style = Tf.body, color = MutedText)
        rules.forEachIndexed { i, rule ->
            TactileCard(padding = S.Md, shadowOffset = TreeFortTokens.Shadow.Sm) {
                Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                    TfTextField(rule.name.orEmpty(), { rules[i] = rule.copy(name = it) }, "Name", placeholder = "Bedtime")
                    FieldLabel("Days")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
                        WEEKDAYS.forEach { d ->
                            val on = d in rule.days
                            TactilePill(
                                d, onClick = { rules[i] = rule.copy(days = if (on) rule.days - d else (rule.days + d).sortedBy { WEEKDAYS.indexOf(it) }) },
                                backgroundColor = if (on) ColorSafeGreen else CardBg,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                        TimeField("From", rule.startTime, { rules[i] = rule.copy(startTime = it) }, Modifier.weight(1f))
                        TimeField("Until", rule.endTime, { rules[i] = rule.copy(endTime = it) }, Modifier.weight(1f))
                    }
                    ToggleRow("On", null, rule.enabled, { rules[i] = rule.copy(enabled = it) })
                    TactileButton("Remove rule", { rules.removeAt(i) }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Trash)
                }
            }
        }
        if (!loading && rules.isEmpty()) Text("No Quiet Hours yet.", style = Tf.body, color = MutedText)
        TactileButton("Add a rule", {
            rules.add(QuietRule(name = "Bedtime", enabled = true, days = WEEKDAYS, startTime = "20:00", endTime = "07:00"))
        }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Plus, enabled = !loading && rules.size < 10)
        ErrorText(error)
        DialogButtons("Save", {
            if (rules.any { it.days.isEmpty() }) { error = "Pick at least one day for every rule."; return@DialogButtons }
            scope.launch {
                busy = true; error = null
                AppStore.saveQuietRules(device.id, rules.toList())
                    .onSuccess { AppStore.say("Quiet Hours saved.", MessageKind.Success); onDismiss() }
                    .onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = !loading, loading = busy)
    }
}

/** Saves an address for later (no verification or dispatch claims). */
@Composable
fun AddressDialog(temporary: Boolean, onDismiss: () -> Unit, onSaved: (Address) -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var street1 by remember { mutableStateOf("") }
    var street2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TfFormDialog(if (temporary) "Add a trip address" else "Add an emergency address", onDismiss) {
        Text(EMERGENCY_ADDRESS_NOTE, style = Tf.bodySm, color = MutedText)
        TfTextField(street1, { street1 = it }, "Street address", enabled = !busy)
        TfTextField(street2, { street2 = it }, "Apartment, suite (optional)", enabled = !busy)
        TfTextField(city, { city = it }, "City", enabled = !busy)
        Row(horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            TfTextField(state, { state = it.take(2).uppercase() }, "State", Modifier.weight(1f), enabled = !busy)
            TfTextField(zip, { zip = it.take(10) }, "ZIP", Modifier.weight(1f), keyboardType = KeyboardType.Number, enabled = !busy)
        }
        ErrorText(error)
        DialogButtons("Save address", {
            scope.launch {
                busy = true; error = null
                AppStore.addAddress(street1, street2.ifBlank { null }, city, state, zip, temporary)
                    .onSuccess { onSaved(it); onDismiss() }
                    .onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = street1.isNotBlank() && city.isNotBlank() && state.length == 2 && zip.length >= 5, loading = busy)
    }
}

const val EMERGENCY_ADDRESS_NOTE =
    "Emergency address (for upcoming E911). TreeFort saves it so it's ready later. Today, dialing 911 on the TreeFort phone plays a prompt asking the child to use a grown-up's phone."

/** Pick which saved address a phone is at (`POST /api/devices/{id}/confirm-address`). */
@Composable
fun AddressPickerDialog(device: Device, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val homes = AppStore.addresses.filter { !it.isTemporary }

    fun pick(a: Address) {
        scope.launch {
            busy = true; error = null
            AppStore.confirmAddress(device.id, a.id)
                .onSuccess { AppStore.say("Emergency address saved for ${device.name}.", MessageKind.Success); onDismiss() }
                .onFailure { error = it.errorText }
            busy = false
        }
    }

    TfFormDialog("Where is ${device.name}?", onDismiss) {
        Text(EMERGENCY_ADDRESS_NOTE, style = Tf.bodySm, color = MutedText)
        if (homes.isEmpty()) Text("No saved addresses yet.", style = Tf.body, color = Ink)
        homes.forEach { a ->
            TactileButton(a.oneLine, { pick(a) }, Modifier.fillMaxWidth(), backgroundColor = if (a.id == device.e911AddressId) ColorSafeGreen else CardBg, enabled = !busy)
        }
        TactileButton("Add a new address", { adding = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Plus, enabled = !busy)
        ErrorText(error)
        TactileButton("Close", onDismiss, Modifier.fillMaxWidth(), backgroundColor = PillBg)
    }
    if (adding) AddressDialog(temporary = false, onDismiss = { adding = false }, onSaved = { pick(it) })
}
