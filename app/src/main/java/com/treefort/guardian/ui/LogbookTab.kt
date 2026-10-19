package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.treefort.guardian.AppStore
import com.treefort.guardian.DisplayClock
import com.treefort.guardian.Fmt
import com.treefort.guardian.ScreenFixture
import com.treefort.guardian.api.AuditEntry
import com.treefort.guardian.api.CallLogEntry
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens

private val S = TreeFortTokens.Space

/** Logbook: real call history (`GET /api/calls`) and the household activity feed (`GET /api/audit-logs`). */
@Composable
fun LogbookTab() {
    var section by rememberSaveable { mutableIntStateOf(if (ScreenFixture.at("logbook-safety", "logbook-safety-empty")) 1 else 0) }
    ScreenColumn {
        if (!AppStore.isManager) {
            TactileCard { EmptyState(Ph.Logbook, "Guardians see the Logbook", "Calls and activity for the household are visible to its guardians.") }
            return@ScreenColumn
        }
        SegmentedControl(listOf("Calls", "Activity"), section, { section = it })
        if (section == 0) CallsList() else ActivityList()
    }
}

@Composable
private fun CallsList() {
    val calls = AppStore.calls
    if (calls.isEmpty()) {
        TactileCard { EmptyState(Ph.Call, "No calls yet", "Calls to and from your TreeFort phones appear here.") }
        return
    }
    calls.sortedByDescending { parseInstant(it.timestamp) }.forEach { CallRow(it) }
}

@Composable
private fun CallRow(c: CallLogEntry) {
    val contact = AppStore.contacts.firstOrNull { it.id == c.contactId }
    val device = AppStore.devices.firstOrNull { it.id == c.deviceId }
    TactileCard(padding = S.Md) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            PhosphorAvatar(contact?.avatarIcon, contact?.avatarColor ?: AvatarCatalog.defaultPreset((c.contactId ?: c.id).toString()), contact?.name ?: "Unknown caller")
            Column(Modifier.weight(1f)) {
                Text(contact?.name ?: "Unknown caller", style = Tf.titleSm, color = Ink)
                Text(Fmt.directionLine(c.direction, device?.name ?: "the phone"), style = Tf.bodySm, color = MutedText)
                Text(formatWhen(c.timestamp), style = Tf.caption, color = MutedText)
            }
            Text(Fmt.callDuration(c.direction, c.durationSeconds), style = Tf.numericSm, color = Ink)
        }
    }
}

@Composable
private fun ActivityList() {
    val rows = AppStore.audit
    if (rows.isEmpty()) {
        TactileCard { EmptyState(Ph.Logbook, "Nothing yet", "Changes to the Safelist, phones and household show up here, with who made them.") }
        return
    }
    val zone = DisplayClock.zone(); val now = DisplayClock.now()
    var lastDay: String? = null
    rows.sortedByDescending { parseInstant(it.timestamp) }.forEach { a ->
        val t = parseInstant(a.timestamp)
        val day = t?.let { Fmt.dayHeader(it, now, zone) }
        if (day != null && day != lastDay) { Text(day, style = Tf.bodyStrong, color = MutedText); lastDay = day }
        AuditRow(a, t?.let { Fmt.time(it, zone) } ?: a.timestamp)
    }
}

@Composable
private fun AuditRow(a: AuditEntry, time: String) {
    TactileCard(padding = S.Md) {
        Column(verticalArrangement = Arrangement.spacedBy(S.Xs)) {
            Text(a.summary, style = Tf.titleSm, color = Ink)
            a.details?.takeIf { it.isNotBlank() }?.let { Text(it, style = Tf.bodySm, color = MutedText) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(listOfNotNull(a.actorName, time).joinToString(" - "), style = Tf.caption, color = MutedText)
                a.actorRole?.takeIf { it.isNotBlank() }?.let { RoleChip(it.replaceFirstChar { c -> c.uppercase() }) }
            }
        }
    }
}
