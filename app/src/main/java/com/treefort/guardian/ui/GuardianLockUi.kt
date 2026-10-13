package com.treefort.guardian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.treefort.guardian.GuardianLock
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import kotlinx.coroutines.delay

private val S = TreeFortTokens.Space

/** Dots plus a 3x4 keypad. The value never leaves this composable except through [onChange]. */
@Composable
fun PinPad(value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(S.Md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(S.Sm), modifier = Modifier.semantics { contentDescription = "${value.length} digits entered" }) {
            repeat(GuardianLock.MAX_DIGITS) { i ->
                Box(
                    Modifier.size(20.dp)
                        .background(if (i < value.length) Ink else PillBg, CircleShape)
                        .border(TreeFortTokens.Border.Std, BorderInk, CircleShape),
                )
            }
        }
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("Clear", "0", "Back")).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Sm)) {
                row.forEach { key ->
                    val shape = RoundedCornerShape(TreeFortTokens.Radius.Md)
                    Box(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = TreeFortTokens.Touch.MinTarget + 4.dp)
                            .background(if (key.length > 1) PillBg else CardBg, shape)
                            .border(TreeFortTokens.Border.Std, BorderInk, shape)
                            .clickable(role = Role.Button) {
                                when (key) {
                                    "Clear" -> onChange("")
                                    "Back" -> onChange(value.dropLast(1))
                                    else -> if (value.length < GuardianLock.MAX_DIGITS) onChange(value + key)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(key, style = if (key.length > 1) Tf.titleSm else Tf.numeric, color = Ink)
                    }
                }
            }
        }
    }
}

/** Shown in place of Settings while the Guardian Lock is set and Settings has not been unlocked. */
@Composable
fun UnlockPrompt(onForgot: () -> Unit) {
    val ctx = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var lockedFor by remember { mutableIntStateOf(0) }

    LaunchedEffect(lockedFor) {
        if (lockedFor > 0) { delay(1000); lockedFor-- ; if (lockedFor == 0) error = null }
    }

    fun submit() {
        if (!GuardianLock.validPin(pin)) { error = "Enter your ${GuardianLock.MIN_DIGITS} to ${GuardianLock.MAX_DIGITS} digit PIN."; return }
        when (val r = GuardianLock.verify(ctx, pin)) {
            GuardianLock.Result.Ok -> Unit
            is GuardianLock.Result.Wrong -> { error = "That PIN isn't right. ${r.triesLeft} tries left before a short wait."; pin = "" }
            is GuardianLock.Result.LockedOut -> { lockedFor = r.seconds; error = "Too many tries. Wait ${r.seconds} seconds."; pin = "" }
        }
    }

    ScreenColumn {
        SectionTitle("Guardian Lock")
        TactileCard {
            Text("Enter your PIN to open Settings.", style = Tf.body, color = Ink)
            androidx.compose.foundation.layout.Spacer(Modifier.size(S.Md))
            PinPad(pin) { pin = it; if (lockedFor == 0) error = null }
            if (error != null) {
                androidx.compose.foundation.layout.Spacer(Modifier.size(S.Md))
                Text(if (lockedFor > 0) "Too many tries. Wait $lockedFor seconds." else error!!, style = Tf.bodySm, color = com.treefort.guardian.theme.DangerFg, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            androidx.compose.foundation.layout.Spacer(Modifier.size(S.Md))
            TactileButton("Unlock", { submit() }, Modifier.fillMaxWidth(), enabled = pin.length >= GuardianLock.MIN_DIGITS && lockedFor == 0)
        }
        Text(
            "Forgot your PIN? Signing out removes the lock. Sign back in with your password, then choose a new PIN.",
            style = Tf.bodySm, color = MutedText,
        )
        TactileButton("Sign out", onForgot, Modifier.fillMaxWidth(), backgroundColor = PillBg)
    }
}

/** Choose (or change) the PIN: enter it twice. There is no default. */
@Composable
fun SetPinDialog(onDone: () -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    TfDialog("Guardian Lock", onDismiss) {
        Text(
            if (confirming) "Enter the same PIN again." else "Choose a ${GuardianLock.MIN_DIGITS} to ${GuardianLock.MAX_DIGITS} digit PIN. It keeps kids out of Settings on your phone.",
            style = Tf.body, color = Ink,
        )
        PinPad(if (confirming) second else first) { if (confirming) second = it else first = it; error = null }
        ErrorText(error)
        if (!confirming) {
            DialogButtons("Next", {
                if (GuardianLock.validPin(first)) confirming = true
                else error = "Use ${GuardianLock.MIN_DIGITS} to ${GuardianLock.MAX_DIGITS} digits."
            }, onDismiss, enabled = first.length >= GuardianLock.MIN_DIGITS)
        } else {
            DialogButtons("Save PIN", {
                if (first == second) { GuardianLock.set(ctx, first); onDone() }
                else { error = "Those PINs don't match. Try again."; second = "" }
            }, { confirming = false; second = "" }, enabled = second.length >= GuardianLock.MIN_DIGITS)
        }
    }
}
