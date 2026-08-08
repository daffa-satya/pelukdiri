package com.makhp.pelukdiri.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.makhp.pelukdiri.features.dashboard.DashboardTokens

@Composable
fun FreedomRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = DashboardTokens.RingStroke.toPx()
            val inset = stroke / 2f
            drawArc(
                color = colorScheme.outlineVariant,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        colorScheme.primary,
                        colorScheme.tertiary,
                        colorScheme.secondary,
                        colorScheme.primary
                    )
                ),
                startAngle = -90f,
                sweepAngle = (progress * 360f).coerceIn(0f, 360f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "of limit",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
