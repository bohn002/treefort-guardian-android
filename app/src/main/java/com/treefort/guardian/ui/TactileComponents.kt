package com.treefort.guardian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.treefort.guardian.R as AppR
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.treefort.guardian.theme.BorderInk
import com.treefort.guardian.theme.CanvasBg
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.ColorInk
import com.treefort.guardian.theme.ColorPrimaryBlue
import com.treefort.guardian.theme.ColorSafeGreen
import com.treefort.guardian.theme.ColorWarningYellow
import com.treefort.guardian.theme.ColorWhite
import com.treefort.guardian.theme.DangerFg
import com.treefort.guardian.theme.DangerPale
import com.treefort.guardian.theme.DisabledFill
import com.treefort.guardian.theme.DisabledText
import com.treefort.guardian.theme.DividerColor
import com.treefort.guardian.theme.InfoFg
import com.treefort.guardian.theme.InfoPale
import com.treefort.guardian.theme.Ink
import com.treefort.guardian.theme.MutedText
import com.treefort.guardian.theme.PillBg
import com.treefort.guardian.theme.SuccessFg
import com.treefort.guardian.theme.SuccessPale
import com.treefort.guardian.theme.Tf
import com.treefort.guardian.theme.TreeFortTokens
import com.treefort.guardian.theme.WarningFg
import com.treefort.guardian.theme.WarningPale
import com.treefort.guardian.theme.upper

private val S = TreeFortTokens.Space
private val R = TreeFortTokens.Radius
private val B = TreeFortTokens.Border
private val Sh = TreeFortTokens.Shadow

/** Readable text color for text drawn on [bg]: ink or white, whichever has more contrast. */
fun onColorFor(bg: Color): Color {
    fun contrast(a: Color, b: Color): Float {
        val l1 = a.luminance() + 0.05f
        val l2 = b.luminance() + 0.05f
        return if (l1 > l2) l1 / l2 else l2 / l1
    }
    return if (contrast(bg, ColorInk) >= contrast(bg, ColorWhite)) ColorInk else ColorWhite
}

/** A card: `thick` border, `lg` radius, `md` hard shadow (DESIGN.md section 5). */
@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBg,
    borderWidth: Dp = B.Thick,
    shadowOffset: Dp = Sh.Md,
    shape: Shape = RoundedCornerShape(R.Lg),
    padding: Dp = S.Lg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Box(modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(BorderInk, shape)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape)
                .border(borderWidth, BorderInk, shape)
                .then(clickMod)
                .padding(padding),
            content = content,
        )
    }
}

/**
 * The pressable control. Rests with a 4 dp hard shadow; on press it travels 4 dp and the shadow
 * goes to 0. Sentence-case label in the `button` style. Touch target is at least 48 dp.
 */
@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = ColorPrimaryBlue,
    textColor: Color = onColorFor(backgroundColor),
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    shape: Shape = RoundedCornerShape(R.Md),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val travel = TreeFortTokens.Press.Travel
    val shadow = TreeFortTokens.Press.ShadowOffset
    val fill = if (enabled) backgroundColor else DisabledFill
    val fg = if (enabled) textColor else DisabledText
    val border = if (enabled) BorderInk else DividerColor

    Box(modifier = modifier.padding(end = shadow, bottom = shadow), propagateMinConstraints = true) {
        if (enabled && !pressed) {
            Box(Modifier.matchParentSize().offset(x = shadow, y = shadow).background(BorderInk, shape))
        }
        Box(
            Modifier
                .offset(x = if (pressed && enabled) travel else 0.dp, y = if (pressed && enabled) travel else 0.dp)
                .background(fill, shape)
                .border(B.Std, border, shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = active,
                    role = Role.Button,
                    onClick = onClick,
                )
                .defaultMinSize(minHeight = TreeFortTokens.Touch.MinTarget)
                .padding(horizontal = S.Lg, vertical = S.Sm),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
                    Spacer(Modifier.width(S.Sm))
                } else if (icon != null) {
                    Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(S.Sm))
                }
                Text(text, style = Tf.button, color = fg, maxLines = 2, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Status pill or chip. Pressable chips get the `md` shadow and the same press travel. */
@Composable
fun TactilePill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    backgroundColor: Color = CardBg,
    textColor: Color = onColorFor(backgroundColor),
    shape: Shape = RoundedCornerShape(R.Sm),
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shadow = if (onClick != null) Sh.Md else Sh.Sm
    val travel = TreeFortTokens.Press.Travel
    Box(modifier = modifier.padding(end = shadow, bottom = shadow)) {
        if (!pressed || onClick == null) {
            Box(Modifier.matchParentSize().offset(x = shadow, y = shadow).background(BorderInk, shape))
        }
        Row(
            Modifier
                .offset(x = if (pressed && onClick != null) travel else 0.dp, y = if (pressed && onClick != null) travel else 0.dp)
                .background(backgroundColor, shape)
                .border(B.Std, BorderInk, shape)
                .then(
                    if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                    else Modifier
                )
                .padding(horizontal = S.Sm + 2.dp, vertical = S.Xs + 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(S.Xs))
            }
            Text(upper(text), style = Tf.labelSm, color = textColor, maxLines = 1)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = Tf.title, color = Ink, modifier = Modifier.weight(1f, fill = false))
        trailing?.invoke()
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(upper(text), style = Tf.label, color = MutedText)
}

/** Hidden by default; a password field only reveals its text when the user toggles it. */
fun passwordTransformation(password: Boolean, shown: Boolean): VisualTransformation =
    if (password && !shown) PasswordVisualTransformation() else VisualTransformation.None

/** Labeled text field with token borders. Label sits above the field (never only a placeholder). */
@Composable
fun TfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
    error: String? = null,
    enabled: Boolean = true,
    supportingText: String? = null,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    imeAction: androidx.compose.ui.text.input.ImeAction = androidx.compose.ui.text.input.ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
) {
    var passwordShown by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(S.Xs)) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().let { m -> if (focusRequester != null) m.focusRequester(focusRequester) else m }.semantics { contentDescription = label },
            singleLine = singleLine,
            enabled = enabled,
            isError = error != null,
            textStyle = Tf.body.copy(color = Ink),
            placeholder = placeholder?.let { { Text(it, style = Tf.body, color = MutedText) } },
            visualTransformation = passwordTransformation(password, passwordShown),
            trailingIcon = if (!password) null else {
                {
                    IconButton(onClick = { passwordShown = !passwordShown }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (passwordShown) Ph.EyeOff else Ph.Eye,
                            contentDescription = stringResource(if (passwordShown) AppR.string.hide_password else AppR.string.show_password),
                            tint = MutedText,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType, imeAction = imeAction),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onAny = { onImeAction?.invoke() }),
            shape = RoundedCornerShape(R.Md),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BorderInk,
                unfocusedBorderColor = BorderInk,
                disabledBorderColor = DividerColor,
                errorBorderColor = DangerFg,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg,
                disabledContainerColor = DisabledFill,
                cursorColor = Ink,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
            ),
        )
        val msg = error ?: supportingText
        if (msg != null) Text(msg, style = Tf.caption, color = if (error != null) DangerFg else MutedText)
    }
}

enum class BannerKind { Info, Success, Warning, Danger }

@Composable
fun Banner(text: String, modifier: Modifier = Modifier, kind: BannerKind = BannerKind.Info, action: (@Composable () -> Unit)? = null) {
    val (bg, fg) = when (kind) {
        BannerKind.Info -> InfoPale to InfoFg
        BannerKind.Success -> SuccessPale to SuccessFg
        BannerKind.Warning -> WarningPale to WarningFg
        BannerKind.Danger -> DangerPale to DangerFg
    }
    val shape = RoundedCornerShape(R.Md)
    Column(
        modifier
            .fillMaxWidth()
            .background(bg, shape)
            .border(B.Std, BorderInk, shape)
            .padding(S.Md),
        verticalArrangement = Arrangement.spacedBy(S.Sm),
    ) {
        Text(text, style = Tf.bodySm, color = fg)
        action?.invoke()
    }
}

@Composable
fun StatusDot(online: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .background(if (online) ColorSafeGreen else ColorWarningYellow, CircleShape)
            .border(B.Std, BorderInk, CircleShape)
            .semantics { contentDescription = if (online) "Online" else "Offline" }
    )
}

@Composable
fun SegmentedControl(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(R.Md)
    Row(
        modifier
            .fillMaxWidth()
            .background(PillBg, shape)
            .border(B.Std, BorderInk, shape)
            .padding(S.Xs),
        horizontalArrangement = Arrangement.spacedBy(S.Xs),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .background(if (on) ColorSafeGreen else Color.Transparent, RoundedCornerShape(R.Sm))
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = S.Sm, vertical = S.Sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = Tf.titleSm, color = if (on) ColorInk else Ink, maxLines = 1)
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = TreeFortTokens.Touch.MinTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.Md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Tf.bodyStrong, color = Ink)
            if (subtitle != null) Text(subtitle, style = Tf.bodySm, color = MutedText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ColorWhite,
                checkedTrackColor = SuccessFg,
                checkedBorderColor = BorderInk,
                uncheckedThumbColor = Ink,
                uncheckedTrackColor = PillBg,
                uncheckedBorderColor = BorderInk,
            ),
        )
    }
}

/** Modal dialog with the card chrome (`thick` border, `lg` shadow, `xl` radius). */
@Composable
fun TfDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(0.94f)) {
            TactileCard(
                modifier = modifier.fillMaxWidth(),
                shadowOffset = Sh.Lg,
                shape = RoundedCornerShape(R.Xl),
            ) {
                Text(title, style = Tf.titleLg, color = Ink)
                Spacer(Modifier.height(S.Md))
                Column(verticalArrangement = Arrangement.spacedBy(S.Md), content = content)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    TfDialog(title, onDismiss) {
        Text(message, style = Tf.body, color = Ink)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(S.Md)) {
            TactileButton("Cancel", onDismiss, Modifier.weight(1f), backgroundColor = PillBg)
            TactileButton(
                confirmText, onConfirm, Modifier.weight(1f),
                backgroundColor = if (destructive) TreeFortTokens.Palette.DangerRed else ColorSafeGreen,
            )
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().padding(vertical = S.Xl, horizontal = S.Lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(S.Sm),
    ) {
        Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(48.dp))
        Text(title, style = Tf.title, color = Ink, textAlign = TextAlign.Center)
        Text(body, style = Tf.body, color = MutedText, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(DividerColor.copy(alpha = 0.4f)))
}

/** A row in a list: title, optional subtitle, trailing slot. */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .defaultMinSize(minHeight = TreeFortTokens.Touch.MinTarget)
            .padding(vertical = S.Xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.Md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = Tf.titleSm, color = Ink)
            if (subtitle != null) Text(subtitle, style = Tf.bodySm, color = MutedText)
        }
        trailing?.invoke()
    }
}

