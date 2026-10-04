package com.example.personalfinances.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.personalfinances.ui.theme.wallet

/**
 * A circular progress ring: a muted full-circle track with a rounded arc drawn over it for
 * [progressFraction] (0 to 1), in the theme's savings colour.
 */
@Composable
fun CircularProgressArc(
    progressFraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    strokeWidth: Dp = 16.dp
) {
    val track = MaterialTheme.wallet.cardTonal
    val progress = MaterialTheme.wallet.saving

    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

        // Background track
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )

        // Progress arc
        if (progressFraction > 0f) {
            drawArc(
                color = progress,
                startAngle = -90f,
                sweepAngle = progressFraction * 360f,
                useCenter = false,
                style = stroke
            )
        }
    }
}
