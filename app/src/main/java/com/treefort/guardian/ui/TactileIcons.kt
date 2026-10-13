package com.treefort.guardian.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.treefort.guardian.theme.Ink

@Composable
fun IconStation(
    modifier: Modifier = Modifier.size(20.dp),
    color: Color = Ink,
    strokeWidth: Float = 4.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // House roof
        val roof = Path().apply {
            moveTo(w * 0.15f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.45f)
        }
        drawPath(
            path = roof,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // House walls
        val walls = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.25f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.85f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(
            path = walls,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Tree trunk / chimney
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.85f),
            end = Offset(w * 0.5f, h * 0.55f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun IconSafelist(
    modifier: Modifier = Modifier.size(20.dp),
    color: Color = Ink,
    strokeWidth: Float = 4.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.25f)
            cubicTo(w * 0.85f, h * 0.65f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
            cubicTo(w * 0.5f, h * 0.90f, w * 0.15f, h * 0.65f, w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(
            path = shield,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Checkmark
        val check = Path().apply {
            moveTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.47f, h * 0.63f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(
            path = check,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun IconActivity(
    modifier: Modifier = Modifier.size(20.dp),
    color: Color = Ink,
    strokeWidth: Float = 4.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.38f

        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // Clock hands
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.55f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.45f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun IconSettings(
    modifier: Modifier = Modifier.size(20.dp),
    color: Color = Ink,
    strokeWidth: Float = 4.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Top slider line
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.3f),
            end = Offset(w * 0.85f, h * 0.3f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = 3.5f,
            center = Offset(w * 0.35f, h * 0.3f)
        )

        // Middle slider line
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.55f),
            end = Offset(w * 0.85f, h * 0.55f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = 3.5f,
            center = Offset(w * 0.65f, h * 0.55f)
        )

        // Bottom slider line
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.8f),
            end = Offset(w * 0.85f, h * 0.8f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = color,
            radius = 3.5f,
            center = Offset(w * 0.45f, h * 0.8f)
        )
    }
}

@Composable
fun IconCampfire(
    modifier: Modifier = Modifier.size(24.dp),
    color: Color = Ink,
    strokeWidth: Float = 4.5f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Crossed logs
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.85f),
            end = Offset(w * 0.85f, h * 0.75f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.75f),
            end = Offset(w * 0.85f, h * 0.85f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Flame
        val flame = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.65f, h * 0.35f, w * 0.75f, h * 0.55f, w * 0.5f, h * 0.70f)
            cubicTo(w * 0.25f, h * 0.55f, w * 0.35f, h * 0.35f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(
            path = flame,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun IconPhone(
    modifier: Modifier = Modifier.size(18.dp),
    color: Color = Ink,
    strokeWidth: Float = 4f
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val handset = Path().apply {
            moveTo(w * 0.2f, h * 0.35f)
            cubicTo(w * 0.2f, h * 0.15f, w * 0.8f, h * 0.15f, w * 0.8f, h * 0.35f)
            lineTo(w * 0.65f, h * 0.35f)
            cubicTo(w * 0.65f, h * 0.25f, w * 0.35f, h * 0.25f, w * 0.35f, h * 0.35f)
            close()
        }
        drawPath(
            path = handset,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Handset base
        drawLine(
            color = color,
            start = Offset(w * 0.25f, h * 0.45f),
            end = Offset(w * 0.75f, h * 0.45f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
