package com.treefort.guardian.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.treefort.guardian.AppConfig
import com.treefort.guardian.AppPrefs
import com.treefort.guardian.AppStore
import com.treefort.guardian.BuildConfig
import com.treefort.guardian.GuardianLock
import com.treefort.guardian.MessageKind
import com.treefort.guardian.WebRTCManager
import com.treefort.guardian.api.Device
import com.treefort.guardian.errorText
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.ThemeConfig
import com.treefort.guardian.theme.ThemeMode
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.launch

private val S = TreeFortTokens.Space

enum class Sub { Help, Trips, Members, Campfire }

private fun openUrl(ctx: android.content.Context, url: String) {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Settings, in the canonical order shared with web and iOS: Account, Household, Phone, Safety,
 * Subscription, Display, Diagnostics, About.
 */
@Composable
fun SettingsTab(onOpen: (Sub) -> Unit) {
    val ctx = rememberContext()
    val scope = rememberCoroutineScope()
    val h = AppStore.selected
    var joining by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var addingAddress by remember { mutableStateOf(false) }
    var settingPin by remember { mutableStateOf(false) }
    var pinSet by remember { mutableStateOf(GuardianLock.isSet(ctx)) }
    var removePin by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var quietFor by remember { mutableStateOf<Device?>(null) }
    val online by WebRTCManager.isOnline.collectAsState()

    ScreenColumn {
        // 1. Account
        SectionTitle("Account")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                Text(AppStore.profile?.email.orEmpty(), style = Tf.bodyStrong, color = Ink)
                TactileButton("Sign out", { AppStore.signOut() }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.SignOut)
                TactileButton("Delete account", { deleting = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg, icon = Ph.Trash)
            }
        }

        // 2. Household
        SectionTitle("Household")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                if (h != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                        PhosphorAvatar(h.avatarIcon, h.avatarColor ?: AvatarCatalog.defaultPreset(h.id.toString()), h.displayName ?: h.name)
                        Column(Modifier.weight(1f)) {
                            Text(h.name, style = Tf.title, color = Ink)
                            Text("You are " + (if (h.isOwner) "an owner" else if (h.isManager) "a guardian" else "a caller"), style = Tf.bodySm, color = MutedText)
                        }
                        if (h.isManager) TactileButton("Rename", { renaming = true }, backgroundColor = CardBg, icon = Ph.Edit)
                    }
                    if (h.isManager) {
                        TactileButton("Guardians and callers", { onOpen(Sub.Members) }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.UsersThree)
                        TactileButton("Vacation mode", { onOpen(Sub.Trips) }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Travel)
                    }
                } else {
                    Text("You aren't in a household yet. Add a TreeFort phone, or join with an invite code.", style = Tf.body, color = Ink)
                }
                TactileButton("I have an invite code", { joining = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.AddPerson)
            }
        }
        if (h?.isManager == true) {
            TactileCard {
                Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                    Text("Emergency addresses (for upcoming E911)", style = Tf.titleSm, color = Ink)
                    Text(EMERGENCY_ADDRESS_NOTE, style = Tf.bodySm, color = MutedText)
                    val homes = AppStore.addresses.filter { !it.isTemporary }
                    if (homes.isEmpty()) Text("No addresses saved.", style = Tf.body, color = MutedText)
                    homes.forEach { Text(it.oneLine, style = Tf.body, color = Ink) }
                    TactileButton("Add an address", { addingAddress = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Plus)
                }
            }

            // 3. Phone
            SectionTitle("TreeFort phones")
            TactileCard {
                Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                    Text("Volume, Quiet Hours and the emergency address for each phone live on the TreeFort tab. Speed dials are set in Friends.", style = Tf.bodySm, color = MutedText)
                    AppStore.devices.forEach { d ->
                        TactileButton("Quiet Hours: ${d.name}", { quietFor = d }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.QuietHours)
                    }
                }
            }
        }

        // 4. Safety
        SectionTitle("Safety")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                Text("Guardian Lock", style = Tf.titleSm, color = Ink)
                Text(
                    if (pinSet) "A PIN is needed to open Settings. It keeps kids out when your phone is unlocked."
                    else "Off. Choose a PIN to keep kids out of Settings when your phone is unlocked. There is no default PIN.",
                    style = Tf.bodySm, color = MutedText,
                )
                TactileButton(if (pinSet) "Change PIN" else "Set a PIN", { settingPin = true }, Modifier.fillMaxWidth(), backgroundColor = ColorSafeGreen, icon = Ph.Lock)
                if (pinSet) TactileButton("Turn off", { removePin = true }, Modifier.fillMaxWidth(), backgroundColor = PillBg)
            }
        }

        // 5. Subscription
        SectionTitle("Plan")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                val p = AppStore.profile
                Text(
                    if (p?.subscriptionActive == true) "Active" + (p.subscriptionPlan.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "") else "No active plan",
                    style = Tf.titleSm, color = Ink,
                )
                Text("Plans are managed on the web. There are no in-app purchases.", style = Tf.bodySm, color = MutedText)
                TactileButton("Manage on web", { openUrl(ctx, AppConfig.WEB_APP_URL) }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Share)
            }
        }

        // 6. Display
        SectionTitle("Display")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                FieldLabel("Appearance")
                SegmentedControl(listOf("System", "Light", "Dark"), ThemeConfig.mode.ordinal, { AppPrefs.themeMode = ThemeMode.entries[it] })
                ToggleRow("Plain Text", "Uses one simple, easy-to-read typeface everywhere.", ThemeConfig.isBoringTextEnabled, { AppPrefs.plainText = it })
            }
        }

        // 7. Diagnostics
        SectionTitle("Diagnostics")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                    StatusDot(online)
                    Text(if (online) "Connected for calls" else "Not connected for calls", style = Tf.body, color = Ink)
                }
                FieldLabel("This app's TreeFort number")
                Text(AppStore.myDialCode.ifBlank { "Not assigned yet" }, style = Tf.numeric, color = Ink)
                Text("Friends and phones use this number to call you in the app.", style = Tf.bodySm, color = MutedText)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
                    TactileButton("Reconnect", { WebRTCManager.connectSignaling(AppStore.myDialCode) }, Modifier.weight(1f), backgroundColor = CardBg)
                    TactileButton("Refresh", { AppStore.refresh() }, Modifier.weight(1f), backgroundColor = CardBg)
                }
                DevToolsSection()
            }
        }

        // 8. About
        SectionTitle("About")
        TactileCard {
            Column(verticalArrangement = Arrangement.spacedBy(S.Md)) {
                Text("TreeFort Guardian ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = Tf.body, color = Ink)
                TactileButton("Help", { onOpen(Sub.Help) }, Modifier.fillMaxWidth(), backgroundColor = CardBg, icon = Ph.Help)
                TactileButton("Privacy policy", { openUrl(ctx, AppConfig.PRIVACY_URL) }, Modifier.fillMaxWidth(), backgroundColor = CardBg)
                TactileButton("Email support", { openUrl(ctx, "mailto:${AppConfig.SUPPORT_EMAIL}") }, Modifier.fillMaxWidth(), backgroundColor = CardBg)
            }
        }
    }

    if (joining) JoinDialog { joining = false }
    if (addingAddress) AddressDialog(temporary = false, onDismiss = { addingAddress = false })
    quietFor?.let { QuietHoursDialog(it) { quietFor = null } }
    if (renaming && h != null) RenameHouseholdDialog(h.name) { renaming = false }
    if (settingPin) SetPinDialog(onDone = { settingPin = false; pinSet = true; AppStore.say("Guardian Lock is on.", MessageKind.Success) }, onDismiss = { settingPin = false })
    if (removePin) ConfirmDialog("Turn off Guardian Lock?", "Anyone holding your unlocked phone will be able to open Settings.", "Turn off", destructive = true,
        onConfirm = { GuardianLock.clear(ctx); pinSet = false; removePin = false }, onDismiss = { removePin = false })
    if (deleting) DeleteAccountDialog { deleting = false }
}

@Composable
private fun RenameHouseholdDialog(current: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(current) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    TfDialog("Rename household", onDismiss) {
        TfTextField(name, { name = it }, "Household name", enabled = !busy)
        ErrorText(error)
        DialogButtons("Save", {
            scope.launch {
                busy = true; error = null
                AppStore.renameHousehold(name, null).onSuccess { onDismiss() }.onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = name.isNotBlank(), loading = busy)
    }
}

/** Two acknowledgements, then `POST /api/user/schedule-deletion`. 409 means the last owner of a shared household. */
@Composable
private fun DeleteAccountDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var ack1 by remember { mutableStateOf(false) }
    var ack2 by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    TfFormDialog("Delete your account?", onDismiss) {
        Text("Your account is scheduled for deletion and its data is erased after 30 days. A household where you are the only member is deleted with everything in it.", style = Tf.body, color = Ink)
        ToggleRow("I understand my account will be deleted", null, ack1, { ack1 = it }, enabled = !busy)
        ToggleRow("I understand this removes my access to every household", null, ack2, { ack2 = it }, enabled = !busy)
        ErrorText(error)
        DialogButtons("Delete account", {
            scope.launch {
                busy = true; error = null
                AppStore.deleteAccount().onSuccess { AppStore.say("Account scheduled for deletion.", MessageKind.Success) }.onFailure { error = it.errorText }
                busy = false
            }
        }, onDismiss, enabled = ack1 && ack2, loading = busy, destructive = true)
    }
}
