package com.smarthome.app.ui.scenes

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.core.designsystem.*
import com.smarthome.core.model.Scene

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesScreen(
    state: ScenesUiState,
    onActivateScene: (Scene) -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Neutral900, Brand800.copy(alpha = 0.4f)))
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text("Scenes", style = MaterialTheme.typography.titleLarge, color = Neutral100)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Neutral300)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (state.scenes.isEmpty()) {
                EmptyScenesState()
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.scenes, key = { it.id }) { scene ->
                        SceneCard(
                            scene = scene,
                            isActivating = state.activatingSceneId == scene.id,
                            onActivate = { onActivateScene(scene) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SceneCard(
    scene: Scene,
    isActivating: Boolean,
    onActivate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isActivating) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "card_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        0.3f, 0.7f,
        infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    GlassCard(
        modifier = Modifier
            .height(140.dp)
            .scale(scale)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onActivate()
            }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isActivating) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Accent500.copy(alpha = glowAlpha * 0.2f))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(scene.icon, style = MaterialTheme.typography.headlineMedium)
                    if (isActivating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Accent500,
                            strokeWidth = 2.dp
                        )
                    }
                }
                Column {
                    Text(
                        scene.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Neutral100,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${scene.targets.size} switches",
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral400
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyScenesState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LottieEmptyState(
            title = "No Scenes Yet",
            message = "Create scenes in settings to control multiple devices at once."
        )
    }
}
