package com.laffy.caffeine.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laffy.caffeine.CUSTOM_RANGE
import com.laffy.caffeine.Duration
import com.laffy.caffeine.PRESET_MINUTES
import com.laffy.caffeine.R
import kotlin.math.roundToInt

private const val DEFAULT_CUSTOM_MINUTES = 30
private val MIN_TOUCH_HEIGHT = 48.dp

/** Slider covers 5–120 so every tick is a whole 5 minutes; ±1 reaches 1–4. */
private val SLIDER_RANGE = 5..CUSTOM_RANGE.last

private class PickerItem(
    val label: String,
    val description: String,
    val checked: Boolean,
    val onClick: () -> Unit,
)

/**
 * Connected button group: 10 · 15 · 20 · ∞ · custom. The last button opens a dialog and shows the
 * chosen value when [selected] is a non-preset number of minutes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPicker(
    selected: Duration,
    onSelect: (Duration) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val customMinutes = (selected as? Duration.Minutes)?.value?.takeIf { it !in PRESET_MINUTES }

    val items = buildList {
        PRESET_MINUTES.forEach { minutes ->
            add(
                PickerItem(
                    label = minutes.toString(),
                    description = stringResource(R.string.minutes_short, minutes),
                    checked = selected == Duration.Minutes(minutes),
                    onClick = { onSelect(Duration.Minutes(minutes)) }
                )
            )
        }
        add(
            PickerItem(
                label = stringResource(R.string.infinite),
                description = stringResource(R.string.infinite_cd),
                checked = selected == Duration.Infinite,
                onClick = { onSelect(Duration.Infinite) }
            )
        )
        val customLabel = if (customMinutes != null) stringResource(R.string.minutes_short, customMinutes)
        else stringResource(R.string.custom)
        add(
            PickerItem(
                label = customLabel,
                description = customLabel,
                checked = customMinutes != null,
                onClick = { showDialog = true }
            )
        )
    }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEachIndexed { index, item ->
            ToggleButton(
                checked = item.checked,
                onCheckedChange = { item.onClick() },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .heightIn(min = MIN_TOUCH_HEIGHT)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = item.description
                    },
            ) {
                Text(item.label)
            }
        }
    }

    if (showDialog) {
        CustomDurationDialog(
            initial = customMinutes ?: DEFAULT_CUSTOM_MINUTES,
            onConfirm = { minutes ->
                showDialog = false
                onSelect(Duration.Minutes(minutes))
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun CustomDurationDialog(initial: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(initial.coerceIn(CUSTOM_RANGE)) }
    // The slider is the coarse control (ticks every 5 min); the ± buttons are the fine one.
    val sliderState = rememberSliderState(
        value = minutes.coerceIn(SLIDER_RANGE).toFloat(),
        steps = (SLIDER_RANGE.last - SLIDER_RANGE.first) / 5 - 1,
        trackRange = SLIDER_RANGE.first.toFloat()..SLIDER_RANGE.last.toFloat(),
    )
    // Keep the thumb on the value after the ± buttons move it (the state setter snaps to a tick).
    SideEffect {
        if (minutes in SLIDER_RANGE) sliderState.value = minutes.toFloat()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_title)) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { minutes = (minutes - 1).coerceIn(CUSTOM_RANGE) },
                    enabled = minutes > CUSTOM_RANGE.first
                ) { Text("−1") }
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.minutes_short, minutes),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    Slider(
                        state = sliderState,
                        onValueChange = { minutes = it.roundToInt().coerceIn(CUSTOM_RANGE) },
                    )
                }
                IconButton(
                    onClick = { minutes = (minutes + 1).coerceIn(CUSTOM_RANGE) },
                    enabled = minutes < CUSTOM_RANGE.last
                ) { Text("+1") }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(minutes) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
