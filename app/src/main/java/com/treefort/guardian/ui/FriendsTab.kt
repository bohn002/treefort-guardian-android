package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.treefort.guardian.AppStore
import com.treefort.guardian.MessageKind
import com.treefort.guardian.api.Contact
import com.treefort.guardian.api.FriendInvite
import com.treefort.guardian.api.ROLE_CALLER
import com.treefort.guardian.errorText
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorPrimaryBlue
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.ColorWarningYellow
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.launch

private val S = TreeFortTokens.Space

private fun Contact.displayIcon() = avatarIcon
private fun Contact.displayColor() = avatarColor ?: AvatarCatalog.defaultPreset(id.toString())

/** Friends: the Safelist. Only these TreeFort numbers can call the phones or be called by them. */
@Composable
fun FriendsTab() {
    var editing by remember { mutableStateOf<Contact?>(null) }
    var adding by remember { mutableStateOf(false) }
    var inviting by remember { mutableStateOf(false) }
    var accepting by remember { mutableStateOf(false) }

    ScreenColumn {
        if (!AppStore.isManager) {
            SectionTitle("Friends")
            TactileCard {
                EmptyState(
                    Ph.UsersThree, "Guardians manage Friends",
                    "A guardian in this household chooses who can call and be called. You can join another household with an invite code.",
                    action = { TactileButton("I have an invite code", { accepting = true }, backgroundColor = ColorSafeGreen, icon = Ph.AddPerson) },
                )
            }
        } else {
            SectionTitle("Friends")
            Text("The Safelist: only the TreeFort numbers here can call the phones or be called by them.", style = Tf.body, color = MutedText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                TactileButton("Invite", { inviting = true }, Modifier.weight(1f), backgroundColor = ColorSafeGreen, icon = Ph.Share)
                TactileButton("Add number", { adding = true }, Modifier.weight(1f), backgroundColor = ColorPrimaryBlue, icon = Ph.Plus)
            }
            TactileButton("I have an invite code", { accepting = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.AddPerson)

            SpeedDialSummary()

            if (AppStore.contacts.isEmpty()) {
                TactileCard {
                    EmptyState(
                        Ph.Safelist, "No friends yet",
                        "Invite a grown-up or add a TreeFort number. Nobody can call the phone until they're on the Safelist.",
                    )
                }
            }
            AppStore.contacts.forEach { c -> ContactRow(c, onEdit = { editing = c }) }
        }
    }

    if (adding) ContactEditorDialog(null) { adding = false }
    editing?.let { c -> ContactEditorDialog(c) { editing = null } }
    if (inviting) InviteFriendDialog { inviting = false }
    if (accepting) AcceptInviteDialog { accepting = false }
}

@Composable
private fun SpeedDialSummary() {
    TactileCard(padding = S.Md) {
        FieldLabel("Speed dials on the keypad")
        Spacer(Modifier.height(S.Xs))
        Text("#0  Campfire (always the household room)", style = Tf.bodySm, color = Ink)
        for (n in 1..9) {
            val c = AppStore.contacts.firstOrNull { it.speedDial == "#$n" }
            Text("#$n  " + (c?.name ?: "Not set"), style = Tf.bodySm, color = if (c == null) MutedText else Ink)
        }
    }
}

@Composable
private fun ContactRow(c: Contact, onEdit: () -> Unit) {
    val isCaller = AppStore.detail?.members?.any { it.role == ROLE_CALLER && it.appDialCode != null && it.appDialCode == c.treefortId } == true
    TactileCard(padding = S.Md, onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            PhosphorAvatar(c.displayIcon(), c.displayColor(), c.name)
            Column(Modifier.weight(1f)) {
                Text(c.name, style = Tf.title, color = Ink)
                Text(c.treefortId ?: "No TreeFort number", style = Tf.numericSm, color = MutedText)
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
                    if (isCaller) RoleChip("Caller", ColorWarningYellow)
                    c.speedDial?.let { RoleChip("Speed dial $it") }
                    if (!c.allowIncoming) RoleChip("Can't call in")
                    if (!c.allowOutgoing) RoleChip("Can't be called")
                }
            }
            if (c.treefortId != null && c.allowOutgoing) {
                TactileButton("Call", { AppStore.placeCall(c.treefortId, c.name, c.avatarIcon, c.displayColor()) }, backgroundColor = ColorSafeGreen, icon = Ph.Call)
            }
        }
    }
}

/** Add (contact == null) or edit one Safelist entry. The TreeFort number is read-only once added. */
@Composable
fun ContactEditorDialog(contact: Contact?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(contact?.name.orEmpty()) }
    var number by remember { mutableStateOf(contact?.treefortId.orEmpty()) }
    var incoming by remember { mutableStateOf(contact?.allowIncoming ?: true) }
    var outgoing by remember { mutableStateOf(contact?.allowOutgoing ?: true) }
    var speed by remember { mutableStateOf(contact?.speedDial) }
    var icon by remember { mutableStateOf(contact?.avatarIcon ?: TreeFortTokens.Avatar.DefaultIcon) }
    var color by remember { mutableStateOf(contact?.avatarColor ?: AvatarCatalog.defaultPreset(contact?.id?.toString() ?: "new")) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val taken = AppStore.contacts.filter { it.id != contact?.id && it.speedDial != null }.associate { it.speedDial!! to it.name }
    val numberOk = Regex("""\*\d{5}""").matches(number.trim())

    TfFormDialog(if (contact == null) "Add a friend" else "Edit ${contact.name}", onDismiss) {
        TfTextField(name, { name = it }, "Name", enabled = !busy)
        if (contact == null) {
            TfTextField(number, { number = it.filter { ch -> ch.isDigit() || ch == '*' }.take(6) }, "TreeFort number", placeholder = "*12345", keyboardType = KeyboardType.Phone, enabled = !busy,
                supportingText = "TreeFort numbers look like *12345. Regular phone numbers can't be added.")
        } else {
            FieldLabel("TreeFort number")
            Text(contact.treefortId ?: "None", style = Tf.numeric, color = Ink)
        }
        ToggleRow("Can call the phone", "They can ring the TreeFort phone.", incoming, { incoming = it }, enabled = !busy)
        ToggleRow("Phone can call them", "Kids can dial them.", outgoing, { outgoing = it }, enabled = !busy)
        FieldLabel("Speed dial")
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
            TactilePill("None", onClick = { speed = null }, backgroundColor = if (speed == null) ColorSafeGreen else CardBg)
            for (n in 1..9) {
                val key = "#$n"
                val other = taken[key]
                TactilePill(
                    if (other != null) "$key $other" else key,
                    onClick = if (other == null) ({ speed = key }) else null,
                    backgroundColor = if (speed == key) ColorSafeGreen else if (other != null) PillBg else CardBg,
                )
            }
        }
        Text("#0 is always the Campfire.", style = Tf.caption, color = MutedText)
        AvatarPicker(icon, color) { i, c -> icon = i; color = c }
        ErrorText(error)
        DialogButtons(if (contact == null) "Add friend" else "Save", {
            scope.launch {
                busy = true; error = null
                val r = if (contact == null) AppStore.addContact(name, number, incoming, outgoing, speed, icon, color)
                else AppStore.updateContact(contact.id, name, incoming, outgoing, speed, icon, color)
                r.onSuccess { onDismiss() }.onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = name.isNotBlank() && (contact != null || numberOk), loading = busy)
        if (contact != null) {
            TactileButton("Remove from Safelist", { confirmDelete = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Trash, enabled = !busy)
        }
    }

    if (confirmDelete && contact != null) {
        val isCaller = AppStore.detail?.members?.any { it.role == ROLE_CALLER && it.appDialCode == contact.treefortId } == true
        ConfirmDialog(
            "Remove ${contact.name}?",
            (deleteError?.let { "$it\n\n" } ?: "") +
                "They won't be able to call the phones, and the phones can't call them." +
                if (isCaller) " They will also lose their caller access to this household." else "",
            "Remove", destructive = true,
            onConfirm = {
                // HH-SEC-05: DELETE /api/contacts/{id}. The row stays until the backend confirms.
                scope.launch {
                    AppStore.deleteContact(contact.id)
                        .onSuccess { confirmDelete = false; AppStore.say("${contact.name} removed from the Safelist.", MessageKind.Success); onDismiss() }
                        .onFailure { deleteError = it.errorText }
                }
            },
            onDismiss = { confirmDelete = false; deleteError = null },
        )
    }
}

/** Create a friend invite: a three-word phrase with digits, valid 48 hours. */
@Composable
fun InviteFriendDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = rememberContext()
    var alias by remember { mutableStateOf("") }
    var asCaller by remember { mutableStateOf(false) }
    var deviceId by remember { mutableStateOf<Int?>(AppStore.devices.firstOrNull()?.id) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var invite by remember { mutableStateOf<FriendInvite?>(null) }

    TfFormDialog("Invite a friend", onDismiss) {
        val made = invite
        if (made == null) {
            Text("Make a code to send to a friend. When they accept it, you can call each other.", style = Tf.body, color = Ink)
            TfTextField(alias, { alias = it }, "What do you call them?", placeholder = "Grandpa Joe", enabled = !busy)
            ToggleRow(
                "They don't have a TreeFort phone", "Let them join with just the app, as a caller. They can call and be called by your phones, and can't change anything.",
                asCaller, { asCaller = it }, enabled = !busy,
            )
            if (AppStore.devices.size > 1) {
                FieldLabel("Link to which phone?")
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Xs), verticalArrangement = Arrangement.spacedBy(S.Xs)) {
                    AppStore.devices.forEach { d -> TactilePill(d.name, onClick = { deviceId = d.id }, backgroundColor = if (deviceId == d.id) ColorSafeGreen else CardBg) }
                }
            }
            if (AppStore.devices.isEmpty()) Banner("Add a TreeFort phone first, then invite friends.", kind = BannerKind.Warning)
            ErrorText(error)
            DialogButtons("Make invite", {
                scope.launch {
                    busy = true; error = null
                    AppStore.createFriendInvite(alias, asCaller, deviceId).onSuccess { invite = it }.onFailure { error = it.errorText }
                    busy = false
                }
            }, onDismiss, enabled = alias.isNotBlank() && AppStore.devices.isNotEmpty(), loading = busy)
        } else {
            Text("Share this code with $alias. It works for ${made.expiresInHours} hours, once.", style = Tf.body, color = Ink)
            TactileCard(padding = S.Md, backgroundColor = PillBg) { Text(made.inviteCode, style = Tf.numericXl, color = Ink) }
            if (made.allowCaller) Text("They can accept it as a caller if they don't have a TreeFort phone.", style = Tf.bodySm, color = MutedText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                TactileButton("Share", {
                    shareText(ctx, "TreeFort invite", "Join me on TreeFort. Open the TreeFort app and enter this invite code: ${made.inviteCode}" + made.shareUrl.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty())
                }, Modifier.weight(1f), backgroundColor = ColorSafeGreen, icon = Ph.Share)
                TactileButton("Copy", { copyText(ctx, "TreeFort invite", made.inviteCode); AppStore.say("Code copied.") }, Modifier.weight(1f), backgroundColor = CardBg, icon = Ph.Copy)
            }
            TactileButton("Done", onDismiss, Modifier.fillMaxWidth(), backgroundColor = PillBg)
        }
    }
}

/** Accept a friend invite. If the account has no phone and the invite allows it, offer to join as a caller. */
@Composable
fun AcceptInviteDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var offerCaller by remember { mutableStateOf(false) }

    fun accept(asCaller: Boolean) {
        scope.launch {
            busy = true; error = null
            AppStore.acceptFriendInvite(code, alias, asCaller)
                .onSuccess {
                    AppStore.say(if (it.mode == "caller") "You can now call ${it.householdName ?: "this household"}'s phones." else "${alias.trim()} is on your Safelist.", MessageKind.Success)
                    onDismiss()
                }
                .onFailure {
                    error = it.errorText
                    // The backend's 400 tells a phoneless account that this invite allows joining as a caller.
                    offerCaller = !asCaller && it.errorText.contains("app caller", ignoreCase = true)
                }
            busy = false
        }
    }

    TfFormDialog("Enter an invite code", onDismiss) {
        Text("Type the code your friend sent you.", style = Tf.body, color = Ink)
        TfTextField(code, { code = it.trim().lowercase(); offerCaller = false }, "Invite code", placeholder = "word-word-word-1234", enabled = !busy)
        TfTextField(alias, { alias = it }, "What do you call them?", placeholder = "Grandma Ruth", enabled = !busy)
        ErrorText(error)
        if (offerCaller) {
            Banner("This account has no TreeFort phone. You can join as a caller instead: you'll be able to call and be called by their phones.")
            TactileButton("Join as a caller", { accept(true) }, Modifier.fillMaxWidth(), backgroundColor = ColorWarningYellow, enabled = !busy)
        }
        DialogButtons("Accept", { accept(false) }, onDismiss, enabled = code.isNotBlank() && alias.isNotBlank(), loading = busy)
    }
}
