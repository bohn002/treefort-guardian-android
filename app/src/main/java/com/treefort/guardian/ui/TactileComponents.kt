package com.treefort.guardian.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.treefort.guardian.theme.CardBg
import com.treefort.guardian.theme.Ink

@Composable
fun TactileCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardBg,
    borderColor: Color = Ink,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 3.5.dp,
    shape: Shape = RoundedCornerShape(14.dp),
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset)) {
        // Hard Drop Shadow layer
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(x = shadowOffset, y = shadowOffset)
                .background(borderColor, shape)
        )
        // Foreground Card layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape)
                .border(borderWidth, borderColor, shape)
        ) {
            content()
        }
    }
}

@Composable
fun TactileButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF3DCC79),
    textColor: Color = Ink,
    icon: (@Composable () -> Unit)? = null,
    shadowOffset: Dp = 3.dp,
    shape: Shape = RoundedCornerShape(12.dp),
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier.padding(end = shadowOffset, bottom = shadowOffset),
        propagateMinConstraints = true
    ) {
        if (!isPressed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(Ink, shape)
            )
        }
        Box(
            modifier = Modifier
                .offset(
                    x = if (isPressed) shadowOffset else 0.dp,
                    y = if (isPressed) shadowOffset else 0.dp
                )
                .background(backgroundColor, shape)
                .border(2.5.dp, Ink, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    icon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text.uppercase(),
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TactilePill(
    text: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: String? = null,
    backgroundColor: Color = CardBg,
    textColor: Color = Ink,
    shadowOffset: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(8.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val effectiveShadow = if (onClick == null) 0.dp else shadowOffset

    Box(modifier = modifier.padding(end = effectiveShadow, bottom = effectiveShadow)) {
        if (!isPressed && effectiveShadow > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = effectiveShadow, y = effectiveShadow)
                    .background(Ink, shape)
            )
        }
        Box(
            modifier = Modifier
                .offset(
                    x = if (isPressed && onClick != null) effectiveShadow else 0.dp,
                    y = if (isPressed && onClick != null) effectiveShadow else 0.dp
                )
                .background(backgroundColor, shape)
                .border(2.dp, Ink, shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                )
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Text(
                        text = icon,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = text,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
