package com.laffy.caffeine.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.laffy.caffeine.R

private val HeroSize = 240.dp
private val ShapeSize = 176.dp
private val RingStrokeWidth = 8.dp

/**
 * The one primary action: a coffee cup on a shape that is a circle when off and a nine-sided
 * cookie (slowly rotating) when on, surrounded by a wavy progress ring.
 *
 * @param progress fraction remaining of a timed session, or null for ∞ (indeterminate ring).
 * @param reducedMotion no rotation, instant morph, plain static ring.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HeroToggle(
    active: Boolean,
    progress: Float?,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    val motion = MaterialTheme.motionScheme

    val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val morphProgress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (reducedMotion) snap() else motion.defaultSpatialSpec(),
        label = "morph"
    )
    val shape = remember(morph, morphProgress) { MorphShape(morph, morphProgress) }

    val container by animateColorAsState(
        targetValue = if (active) colors.primaryContainer else colors.surfaceContainerHighest,
        animationSpec = if (reducedMotion) snap() else motion.defaultEffectsSpec(),
        label = "container"
    )
    val content by animateColorAsState(
        targetValue = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = if (reducedMotion) snap() else motion.defaultEffectsSpec(),
        label = "content"
    )
    val ringProgress by animateFloatAsState(
        targetValue = (progress ?: 1f).coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) snap() else WavyProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "ring"
    )
    val angle = rememberSpinAngle(spinning = active && !reducedMotion)

    val appName = stringResource(R.string.app_name)
    val stateText = stringResource(if (active) R.string.hero_state_on else R.string.hero_state_off)

    Box(
        modifier = modifier
            .size(HeroSize)
            .clip(CircleShape)
            .toggleable(
                value = active,
                role = Role.Switch,
                onValueChange = {
                    haptic.performHapticFeedback(
                        if (active) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn
                    )
                    onToggle()
                }
            )
            .semantics {
                contentDescription = appName
                stateDescription = stateText
                role = Role.Switch
            },
        contentAlignment = Alignment.Center
    ) {
        // Decorative children: the toggle's own semantics are all a screen reader should see.
        Box(Modifier.fillMaxSize().clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
            Ring(
                active = active,
                progress = if (progress == null) null else ringProgress,
                reducedMotion = reducedMotion,
            )
            Box(
                Modifier
                    .size(ShapeSize)
                    .graphicsLayer { rotationZ = angle }
                    .clip(shape)
                    .background(container)
            )
            Icon(
                painter = painterResource(R.drawable.ic_coffee),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
private fun Ring(active: Boolean, progress: Float?, reducedMotion: Boolean) {
    val colors = MaterialTheme.colorScheme
    val strokeWidth = with(LocalDensity.current) { RingStrokeWidth.toPx() }
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    val fill = Modifier.fillMaxSize()

    when {
        !active -> if (reducedMotion) {
            CircularProgressIndicator(
                progress = { 1f }, modifier = fill, color = colors.outlineVariant,
                trackColor = colors.outlineVariant, strokeWidth = RingStrokeWidth
            )
        } else {
            CircularWavyProgressIndicator(
                progress = { 1f }, modifier = fill, color = colors.outlineVariant,
                trackColor = colors.outlineVariant, stroke = stroke, trackStroke = stroke,
                amplitude = { 0f }
            )
        }

        reducedMotion -> CircularProgressIndicator(
            progress = { progress ?: 1f }, modifier = fill, strokeWidth = RingStrokeWidth
        )

        progress == null -> CircularWavyProgressIndicator(
            modifier = fill, stroke = stroke, trackStroke = stroke
        )

        else -> CircularWavyProgressIndicator(
            progress = { progress }, modifier = fill, stroke = stroke, trackStroke = stroke
        )
    }
}

/** Slow continuous rotation; the infinite transition only exists while [spinning]. */
@Composable
private fun rememberSpinAngle(spinning: Boolean): Float {
    if (!spinning) return 0f
    val transition = rememberInfiniteTransition(label = "spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 30_000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    return angle
}

/** A [Shape] that draws [morph] at [progress], scaled to the layout size and centered. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class MorphShape(private val morph: Morph, private val progress: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        morph.toPath(progress, path)
        path.transform(Matrix().apply { scale(size.width, size.height) })
        path.translate(size.center - path.getBounds().center)
        return Outline.Generic(path)
    }
}
