package com.laffy.caffeine

import android.Manifest
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.laffy.caffeine.ui.theme.CaffeineTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            CaffeineTheme {
                CaffeineScreen(lifecycle)
            }
        }
    }
}

@Composable
fun CaffeineScreen(lifecycle: Lifecycle) {
    val context = LocalContext.current
    // Temporary until Task 6 rewrites the screen around Session/Duration.
    val session by CaffeineState.session.collectAsState()
    val endAt = session?.endAt ?: 0L
    var minutes by remember {
        mutableIntStateOf((Prefs.selected(context) as? Duration.Minutes)?.value ?: PRESET_MINUTES.first())
    }
    var remainingMs by remember { mutableLongStateOf(0L) }
    val active = endAt > 0L && remainingMs > 0L

    // Tick once per second, only while the screen is visible and a session is running.
    LaunchedEffect(endAt) {
        if (endAt <= 0L) {
            remainingMs = 0L
            return@LaunchedEffect
        }
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val r = endAt - System.currentTimeMillis()
                remainingMs = r.coerceAtLeast(0L)
                if (r <= 0L) break
                delay(((r - 1) % 1000L) + 1L)
            }
        }
    }

    CaffeineContent(
        active = active,
        remainingSeconds = ((remainingMs + 999L) / 1000L).toInt(),
        minutes = minutes,
        onMinutes = {
            minutes = it
            Prefs.setSelected(context, Duration.Minutes(it))
        },
        onToggle = { CaffeineController.toggle(context) },
        onAddTile = if (Build.VERSION.SDK_INT >= 33) ({ requestAddTile(context) }) else null
    )
}

@Composable
private fun CaffeineContent(
    active: Boolean,
    remainingSeconds: Int,
    minutes: Int,
    onMinutes: (Int) -> Unit,
    onToggle: () -> Unit,
    onAddTile: (() -> Unit)?
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val container by animateColorAsState(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                label = "container"
            )
            val content by animateColorAsState(
                if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "content"
            )
            Box(
                modifier = Modifier.size(144.dp).background(container, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_coffee),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
            Text(
                text = if (active) "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
                else stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = stringResource(if (active) R.string.status_on else R.string.status_off),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))
            Text(
                text = stringResource(R.string.duration),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )
            val options = PRESET_MINUTES
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, m ->
                    SegmentedButton(
                        selected = m == minutes,
                        onClick = { onMinutes(m) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        label = { Text(stringResource(R.string.minutes_short, m)) }
                    )
                }
            }
            Text(
                text = stringResource(R.string.duration_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start).padding(top = 8.dp)
            )

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(if (active) R.string.turn_off else R.string.turn_on))
            }
            if (onAddTile != null) {
                TextButton(onClick = onAddTile, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.add_tile))
                }
            }
        }
    }
}

private fun requestAddTile(context: Context) {
    if (Build.VERSION.SDK_INT < 33) return
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, CaffeineTileService::class.java),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.ic_coffee),
        context.mainExecutor
    ) { }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOff() {
    CaffeineTheme { CaffeineContent(false, 0, 15, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun PreviewOn() {
    CaffeineTheme { CaffeineContent(true, 754, 15, {}, {}, null) }
}
