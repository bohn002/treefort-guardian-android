package com.treefort.guardian.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorInk
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val S = TreeFortTokens.Space

/** The standard scrolling page for a tab or sub-screen. */
@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = S.Lg, vertical = S.Md),
        verticalArrangement = Arrangement.spacedBy(S.Lg),
        content = content,
    )
}

/** A dialog whose body scrolls when it is taller than the screen (forms, long lists). */
@Composable
fun TfFormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.68f).dp
    TfDialog(title, onDismiss) {
        Column(
            Modifier.heightIn(max = maxH).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(S.Md),
            content = content,
        )
    }
}

@Composable
fun ErrorText(text: String?) {
    if (text != null) Banner(text, kind = BannerKind.Danger)
}

/** Cancel / confirm button pair used at the foot of dialogs. */
@Composable
fun DialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    destructive: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
        TactileButton("Cancel", onCancel, Modifier.weight(1f), backgroundColor = PillBg)
        TactileButton(
            confirmText, onConfirm, Modifier.weight(1f), enabled = enabled, loading = loading,
            backgroundColor = if (destructive) TreeFortTokens.Palette.DangerRed else ColorSafeGreen,
        )
    }
}

/** A small tinted label ("Owner", "You") that is not clickable. */
@Composable
fun RoleChip(text: String, color: androidx.compose.ui.graphics.Color = PillBg) {
    TactilePill(text, backgroundColor = color)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AvatarPicker(icon: String?, color: String?, onChange: (icon: String, color: String) -> Unit) {
    val curIcon = icon ?: TreeFortTokens.Avatar.DefaultIcon
    val curColor = color ?: TreeFortTokens.Avatar.DefaultPreset
    Column(verticalArrangement = Arrangement.spacedBy(S.Sm)) {
        FieldLabel("Avatar")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            PhosphorAvatar(curIcon, curColor, "Selected avatar", size = TreeFortTokens.Avatar.SizeLg)
            Text(AvatarCatalog.label(curIcon), style = Tf.bodyStrong, color = Ink)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Sm), verticalArrangement = Arrangement.spacedBy(S.Sm)) {
            AvatarCatalog.presetIds.forEach { id ->
                val p = AvatarCatalog.preset(id)
                val on = id == curColor
                Box(
                    Modifier
                        .size(TreeFortTokens.Touch.MinTarget)
                        .background(p.background, CircleShape)
                        .border(if (on) TreeFortTokens.Border.Thick else TreeFortTokens.Border.Std, if (on) Ink else BorderInk, CircleShape)
                        .clickable(role = Role.RadioButton) { onChange(curIcon, id) }
                        .semantics { contentDescription = "Color " + id.replace('-', ' ') + if (on) ", selected" else "" },
                )
            }
        }
        AvatarCatalog.groups.forEach { group ->
            Text(group.title, style = Tf.bodySm, color = MutedText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(S.Sm), verticalArrangement = Arrangement.spacedBy(S.Sm)) {
                group.icons.forEach { (id, label) ->
                    val on = id == curIcon
                    Box(
                        Modifier
                            .size(TreeFortTokens.Touch.MinTarget)
                            .background(if (on) AvatarCatalog.preset(curColor).background else CardBg, RoundedCornerShape(TreeFortTokens.Radius.Md))
                            .border(if (on) TreeFortTokens.Border.Thick else TreeFortTokens.Border.Std, if (on) Ink else BorderInk, RoundedCornerShape(TreeFortTokens.Radius.Md))
                            .clickable(role = Role.RadioButton) { onChange(id, curColor) }
                            .semantics { contentDescription = label + if (on) ", selected" else "" },
                        contentAlignment = Alignment.Center,
                    ) {
                        DuotoneGlyph(AvatarCatalog.vector(id), outline = ColorInk, fill = AvatarCatalog.preset(curColor).fill, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

// ---- date and time pickers ----------------------------------------------------------------

/** "HH:mm" 24 hour value in a labelled button; opens a time picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(S.Xs)) {
        FieldLabel(label)
        TactileButton(formatClock(value), { open = true }, Modifier.fillMaxWidth(), backgroundColor = CardBg)
    }
    if (open) {
        val t = runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.of(20, 0))
        val state = rememberTimePickerState(initialHour = t.hour, initialMinute = t.minute, is24Hour = false)
        TfDialog(label, { open = false }) {
            TimePicker(state)
            DialogButtons("Set time", { onChange("%02d:%02d".format(state.hour, state.minute)); open = false }, { open = false })
        }
    }
}

/** A local date and time chosen in two steps; value is "yyyy-MM-ddTHH:mm" (blank until chosen). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var stage by remember { mutableIntStateOf(0) }
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val current = runCatching { LocalDateTime.parse(value) }.getOrNull()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(S.Xs)) {
        FieldLabel(label)
        TactileButton(
            current?.let { it.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)) } ?: "Choose date and time",
            { stage = 1 }, Modifier.fillMaxWidth(), backgroundColor = CardBg,
        )
    }
    if (stage == 1) {
        val ds = rememberDatePickerState(
            initialSelectedDateMillis = (current?.toLocalDate() ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
        )
        TfFormDialog(label, { stage = 0 }) {
            DatePicker(ds, showModeToggle = false)
            DialogButtons("Next", {
                ds.selectedDateMillis?.let { pickedDate = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate(); stage = 2 }
            }, { stage = 0 }, enabled = ds.selectedDateMillis != null)
        }
    }
    if (stage == 2) {
        val t = current?.toLocalTime() ?: LocalTime.of(9, 0)
        val ts = rememberTimePickerState(initialHour = t.hour, initialMinute = t.minute, is24Hour = false)
        TfDialog(label, { stage = 0 }) {
            TimePicker(ts)
            DialogButtons("Set", {
                pickedDate?.let { d -> onChange(LocalDateTime.of(d, LocalTime.of(ts.hour, ts.minute)).toString()) }
                stage = 0
            }, { stage = 0 })
        }
    }
}

// ---- formatting ----------------------------------------------------------------------------

fun formatClock(hhmm: String): String = runCatching {
    LocalTime.parse(hhmm).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
}.getOrDefault(hhmm)

/** Backend timestamps are UTC; naive ones (no offset) are treated as UTC. */
fun parseInstant(iso: String): Instant? = runCatching {
    if (iso.endsWith("Z") || iso.contains('+') || iso.lastIndexOf('-') > 9) Instant.parse(iso.replace("+00:00", "Z"))
    else LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC)
}.recoverCatching { ZonedDateTime.parse(iso).toInstant() }.getOrNull()

fun formatWhen(iso: String, zone: ZoneId = ZoneId.systemDefault()): String =
    parseInstant(iso)?.atZone(zone)?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault())) ?: iso

fun formatDuration(seconds: Int): String = if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"

fun shareText(context: android.content.Context, subject: String, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

fun copyText(context: android.content.Context, label: String, text: String) {
    val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    cm.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

@Composable
fun SpacerH(w: Int = 8) = Spacer(Modifier.width(w.dp))

@Composable
fun rememberContext() = LocalContext.current
