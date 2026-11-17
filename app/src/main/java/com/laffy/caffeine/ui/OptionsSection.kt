package com.laffy.caffeine.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.laffy.caffeine.R

enum class OptionKey { ScreenOff, Unplug, Dim }

/** The three behaviour switches. Each whole row is one toggle (a Switch to the screen reader). */
@Composable
fun OptionsSection(
    stopOnScreenOff: Boolean,
    stopOnUnplug: Boolean,
    dimScreen: Boolean,
    onChange: (OptionKey, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.options),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp).semantics { heading() }
        )
        OptionRow(R.drawable.ic_screen_off, R.string.opt_screen_off, stopOnScreenOff) {
            onChange(OptionKey.ScreenOff, it)
        }
        OptionRow(R.drawable.ic_power, R.string.opt_unplug, stopOnUnplug) {
            onChange(OptionKey.Unplug, it)
        }
        OptionRow(R.drawable.ic_dim, R.string.opt_dim, dimScreen) {
            onChange(OptionKey.Dim, it)
        }
    }
}

@Composable
private fun OptionRow(
    @DrawableRes icon: Int,
    @StringRes label: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        content = { Text(stringResource(label)) },
    )
}
