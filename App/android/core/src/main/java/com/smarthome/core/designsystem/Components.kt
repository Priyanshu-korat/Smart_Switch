package com.smarthome.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val SmartHomeShapes = androidx.compose.material3.Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * Reusable Glassmorphism card surface.
 * Renders a frosted-glass panel with a gradient border and subtle inner glow.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Glass20, Glass10)
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.verticalGradient(
                    colors = listOf(Glass20, Glass05)
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Glowing radial gradient used behind active switch cards.
 */
fun Modifier.activeGlow(color: androidx.compose.ui.graphics.Color, radius: Float = 200f): Modifier =
    this.background(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.25f), color.copy(alpha = 0f)),
            radius = radius
        )
    )
