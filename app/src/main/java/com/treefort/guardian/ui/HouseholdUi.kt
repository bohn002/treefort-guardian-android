package com.treefort.guardian.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.treefort.guardian.AppStore
import com.treefort.guardian.MessageKind
import com.treefort.guardian.api.HouseholdInvite
import com.treefort.guardian.api.MAX_CALLERS
import com.treefort.guardian.api.MAX_GUARDIANS
import com.treefort.guardian.api.Member
import com.treefort.guardian.api.ROLE_CALLER
import com.treefort.guardian.api.ROLE_GUARDIAN
import com.treefort.guardian.api.ROLE_OWNER
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

fun roleLabel(role: String) = when (role) {
    ROLE_OWNER -> "Owner"
    ROLE_GUARDIAN -> "Guardian"
    ROLE_CALLER -> "Caller"
    else -> role.replaceFirstChar { it.uppercase() }
}

/** Co-guardians and callers for the selected household (ADR 0002: 6 guardians, 50 callers). */
@Composable
fun MembersScreen() {
    val h = AppStore.selected ?: return
    val members = AppStore.detail?.members.orEmpty()
    val managers = members.filter { it.role != ROLE_CALLER }
    val callers = members.filter { it.role == ROLE_CALLER }
    var inviting by remember { mutableStateOf(false) }
    var acting by remember { mutableStateOf<Member?>(null) }

    ScreenColumn {
        SectionTitle(h.name)
        Text("Guardians run the household together. Callers are adults with the app but no phone, like a faraway relative: they can call and be called, and can't change anything.", style = Tf.body, color = MutedText)

        SectionTitle("Guardians (${managers.size} of $MAX_GUARDIANS)")
        managers.forEach { MemberRow(it) { acting = it } }
        if (h.isOwner) {
            TactileButton(
                "Invite a co-guardian", { inviting = true }, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.AddPerson,
                enabled = managers.size < MAX_GUARDIANS,
            )
            if (managers.size >= MAX_GUARDIANS) Text("This household has reached its limit of $MAX_GUARDIANS guardians.", style = Tf.bodySm, color = MutedText)
            PendingInvites()
        }

        SectionTitle("Callers (${callers.size} of $MAX_CALLERS)")
        if (callers.isEmpty()) Text("No callers yet. Invite one from Friends and switch on \"They don't have a TreeFort phone\".", style = Tf.body, color = MutedText)
        callers.forEach { MemberRow(it) { acting = it } }
    }
    if (inviting) InviteGuardianDialog { inviting = false }
    acting?.let { m -> MemberActionsDialog(m) { acting = null } }
}

@Composable
private fun MemberRow(m: Member, onClick: () -> Unit) {
    TactileCard(padding = S.Md, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            PhosphorAvatar(m.avatarIcon, m.avatarColor ?: AvatarCatalog.defaultPreset(m.userId.toString()), m.label)
            Column(Modifier.weight(1f)) {
                Text(m.label + if (m.isMe) " (you)" else "", style = Tf.titleSm, color = Ink)
                if (m.email.isNotBlank()) Text(m.email, style = Tf.bodySm, color = MutedText)
                m.appDialCode?.let { Text(it, style = Tf.numericSm, color = MutedText) }
            }
            RoleChip(roleLabel(m.role), if (m.role == ROLE_OWNER) ColorWarningYellow else PillBg)
        }
    }
}

@Composable
private fun PendingInvites() {
    val scope = rememberCoroutineScope()
    val pending = AppStore.invites
    if (pending.isEmpty()) return
    FieldLabel("Waiting to be accepted")
    pending.forEach { inv ->
        TactileCard(padding = S.Md) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                Column(Modifier.weight(1f)) {
                    Text(inv.displayName ?: inv.invitedEmail ?: "Co-guardian invite", style = Tf.titleSm, color = Ink)
                    Text(inv.inviteCode, style = Tf.numericSm, color = Ink)
                    Text(if (inv.invitedEmail != null) "Only ${inv.invitedEmail} can accept" else "Anyone with the code can accept", style = Tf.caption, color = MutedText)
                }
                TactileButton("Cancel", { scope.launch { AppStore.revokeHouseholdInvite(inv.id).onFailure { AppStore.say(it.errorText, MessageKind.Danger) } } }, backgroundColor = PillBg)
            }
        }
    }
}

@Composable
fun InviteGuardianDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctx = rememberContext()
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var made by remember { mutableStateOf<HouseholdInvite?>(null) }

    TfFormDialog("Invite a co-guardian", onDismiss) {
        val inv = made
        if (inv == null) {
            Text("A co-guardian can manage phones, the Safelist and trips with you. Owners can also invite and remove guardians.", style = Tf.body, color = Ink)
            TfTextField(name, { name = it }, "Their name", placeholder = "Grandpa Joe", enabled = !busy)
            TfTextField(
                email, { email = it }, "Their email (recommended)", keyboardType = KeyboardType.Email, enabled = !busy,
                supportingText = "With an email, only that account can accept the code. Leave it empty to let anyone with the code join.",
            )
            ErrorText(error)
            DialogButtons("Make code", {
                scope.launch {
                    busy = true; error = null
                    AppStore.createHouseholdInvite(email.ifBlank { null }, name.ifBlank { null }).onSuccess { made = it }.onFailure { error = it.errorText }
                    busy = false
                }
            }, onDismiss, loading = busy)
        } else {
            Text("Send this code to ${name.ifBlank { "your co-guardian" }}. It works once, for 72 hours.", style = Tf.body, color = Ink)
            TactileCard(padding = S.Md, backgroundColor = PillBg) { Text(inv.inviteCode, style = Tf.numericXl, color = Ink) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                TactileButton("Share", {
                    shareText(ctx, "TreeFort co-guardian invite", "Join my TreeFort household. In the TreeFort app choose \"I have an invite code\" and enter: ${inv.inviteCode}" + inv.shareUrl.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty())
                }, Modifier.weight(1f), backgroundColor = ColorSafeGreen, icon = Ph.Share)
                TactileButton("Copy", { copyText(ctx, "TreeFort invite", inv.inviteCode); AppStore.say("Code copied.") }, Modifier.weight(1f), backgroundColor = CardBg, icon = Ph.Copy)
            }
            TactileButton("Done", onDismiss, Modifier.fillMaxWidth(), backgroundColor = PillBg)
        }
    }
}

/** What can be done to one member, following the ADR 0002 rules (the backend enforces them again). */
@Composable
private fun MemberActionsDialog(m: Member, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val h = AppStore.selected ?: return
    val members = AppStore.detail?.members.orEmpty()
    val managerCount = members.count { it.role != ROLE_CALLER }
    val otherOwner = members.any { it.role == ROLE_OWNER && it.userId != m.userId }
    var editing by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val canEditProfile = m.isMe || h.isOwner || (h.role == ROLE_GUARDIAN && m.role == ROLE_CALLER)
    val canRemove = !m.isMe && ((h.isOwner && m.role != ROLE_OWNER) || (h.role == ROLE_GUARDIAN && m.role == ROLE_CALLER))
    val promote = if (h.isOwner) when {
        m.role == ROLE_GUARDIAN -> ROLE_OWNER to "Make owner"
        m.role == ROLE_CALLER -> ROLE_GUARDIAN to "Make guardian"
        else -> null
    } else null
    val stepDown = h.isOwner && m.isMe && m.role == ROLE_OWNER && otherOwner

    fun act(block: suspend () -> kotlin.Result<*>) {
        scope.launch {
            busy = true; error = null
            block().onSuccess { onDismiss() }.onFailure { error = it.errorText }
            busy = false
        }
    }

    TfFormDialog(m.label, onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            PhosphorAvatar(m.avatarIcon, m.avatarColor ?: AvatarCatalog.defaultPreset(m.userId.toString()), m.label, size = TreeFortTokens.Avatar.SizeLg)
            Column {
                Text(roleLabel(m.role), style = Tf.titleSm, color = Ink)
                if (m.email.isNotBlank()) Text(m.email, style = Tf.bodySm, color = MutedText)
            }
        }
        if (canEditProfile) TactileButton("Edit name and avatar", { editing = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Edit, enabled = !busy)
        if (promote != null) {
            val blocked = promote.first == ROLE_GUARDIAN && managerCount >= MAX_GUARDIANS
            TactileButton(promote.second, { act { AppStore.updateMember(m, role = promote.first) } }, Modifier.fillMaxWidth(), backgroundColor = ColorPrimaryBlue, enabled = !busy && !blocked)
            if (blocked) Text("This household has reached its limit of $MAX_GUARDIANS guardians.", style = Tf.bodySm, color = MutedText)
        }
        if (stepDown) TactileButton("Step down to guardian", { act { AppStore.updateMember(m, role = ROLE_GUARDIAN) } }, Modifier.fillMaxWidth(), backgroundColor = CardBg, enabled = !busy)
        if (m.isMe && m.role == ROLE_OWNER && !otherOwner) Text("You're the only owner. Make someone else an owner before you step down or leave.", style = Tf.bodySm, color = MutedText)
        if (m.isMe) TactileButton("Leave this household", { confirmRemove = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.SignOut, enabled = !busy)
        else if (canRemove) TactileButton("Remove from household", { confirmRemove = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Trash, enabled = !busy)
        ErrorText(error)
        TactileButton("Close", onDismiss, Modifier.fillMaxWidth(), backgroundColor = PillBg)
    }

    if (editing) EditMemberDialog(m) { editing = false; onDismiss() }
    if (confirmRemove) {
        ConfirmDialog(
            if (m.isMe) "Leave ${h.name}?" else "Remove ${m.label}?",
            if (m.isMe) "You'll lose access to this household's phones and Safelist." else if (m.role == ROLE_CALLER) "They can't call or be called by this household anymore." else "They lose access to this household.",
            if (m.isMe) "Leave" else "Remove", destructive = true,
            onConfirm = { confirmRemove = false; act { AppStore.removeMember(m) } },
            onDismiss = { confirmRemove = false },
        )
    }
}

@Composable
fun EditMemberDialog(m: Member, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(m.displayName.orEmpty()) }
    var icon by remember { mutableStateOf(m.avatarIcon ?: TreeFortTokens.Avatar.DefaultIcon) }
    var color by remember { mutableStateOf(m.avatarColor ?: AvatarCatalog.defaultPreset(m.userId.toString())) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    TfFormDialog("Edit profile", onDismiss) {
        TfTextField(name, { name = it }, "Name", enabled = !busy)
        AvatarPicker(icon, color) { i, c -> icon = i; color = c }
        ErrorText(error)
        DialogButtons("Save", {
            scope.launch {
                busy = true; error = null
                AppStore.updateMember(m, displayName = name.ifBlank { null }, icon = icon, color = color).onSuccess { onDismiss() }.onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = name.isNotBlank(), loading = busy)
    }
}

/** "I have an invite code" from first run or Settings: a co-guardian code or a friend/caller code. */
@Composable
fun JoinDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var kind by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf(TreeFortTokens.Avatar.DefaultIcon) }
    var color by remember { mutableStateOf(AvatarCatalog.defaultPreset("join")) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (kind == 1) { AcceptInviteDialog(onDismiss); return }
    TfFormDialog("Enter an invite code", onDismiss) {
        SegmentedControl(listOf("Co-guardian", "Friend"), kind, { kind = it })
        Text("A co-guardian code makes you a guardian of someone else's TreeFort household.", style = Tf.body, color = Ink)
        TfTextField(code, { code = it.trim().lowercase() }, "Guardian code", placeholder = "word-word-word-1234", enabled = !busy)
        TfTextField(name, { name = it }, "Your name in this household", enabled = !busy)
        AvatarPicker(icon, color) { i, c -> icon = i; color = c }
        ErrorText(error)
        DialogButtons("Join household", {
            scope.launch {
                busy = true; error = null
                AppStore.acceptHouseholdInvite(code, name.ifBlank { null }, icon, color)
                    .onSuccess { AppStore.say("You joined ${it.name}.", MessageKind.Success); onDismiss() }
                    .onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = code.isNotBlank(), loading = busy)
    }
}
