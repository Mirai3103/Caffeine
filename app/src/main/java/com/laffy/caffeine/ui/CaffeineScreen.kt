package com.laffy.caffeine.ui

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.laffy.caffeine.CaffeineController
import com.laffy.caffeine.CaffeineState
import com.laffy.caffeine.CaffeineTileService
import com.laffy.caffeine.Duration
import com.laffy.caffeine.Prefs
import com.laffy.caffeine.Privacy
import com.laffy.caffeine.R
import com.laffy.caffeine.Session
import com.laffy.caffeine.isActive
import com.laffy.caffeine.toSession
import com.laffy.caffeine.ui.theme.CaffeineTheme
import kotlinx.coroutines.delay

private const val SECOND = 1_000L
private const val MINUTE = 60_000L

/** Material "medium" window width: from here up the screen fits two panes side by side. */
private val TwoPaneMinWidth = 600.dp

/** Keeps the panes from drifting apart on very wide windows (desktop, unfolded tablets). */
private val TwoPaneMaxWidth = 1000.dp

/** Reading width for the single-column layout on portrait tablets. */
private val SinglePaneMaxWidth = 560.dp

/** State wiring: collects the session, ticks once per second (visible screen only), writes prefs. */
@Composable
fun CaffeineScreen(lifecycle: Lifecycle) {
    val context = LocalContext.current
    val session by CaffeineState.session.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var selected by remember { mutableStateOf(Prefs.selected(context)) }
    var stopOnScreenOff by remember { mutableStateOf(Prefs.stopOnScreenOff(context)) }
    var stopOnUnplug by remember { mutableStateOf(Prefs.stopOnUnplug(context)) }
    var dimScreen by remember { mutableStateOf(Prefs.dimScreen(context)) }
    var tileAdded by remember { mutableStateOf(Prefs.tileAdded(context)) }

    // One tick per second, only while this screen is visible and a session is running.
    LaunchedEffect(session) {
        if (session == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val t = System.currentTimeMillis()
                now = t
                if (session?.isActive(t) != true) break
                delay(SECOND - t % SECOND)
            }
        }
    }

    CaffeineContent(
        session = session,
        now = now,
        selected = selected,
        stopOnScreenOff = stopOnScreenOff,
        stopOnUnplug = stopOnUnplug,
        dimScreen = dimScreen,
        showAddTile = Build.VERSION.SDK_INT >= 33 && !tileAdded,
        reducedMotion = rememberReducedMotion(),
        onToggle = { CaffeineController.toggle(context) },
        onExtend = { CaffeineController.extend(context) },
        onSelect = { duration ->
            selected = duration
            Prefs.setSelected(context, duration)
            // Changing the duration while running restarts the session from now.
            if (CaffeineController.isActive()) CaffeineController.start(context, duration)
        },
        onOption = { key, value ->
            when (key) {
                OptionKey.ScreenOff -> { stopOnScreenOff = value; Prefs.setStopOnScreenOff(context, value) }
                OptionKey.Unplug -> { stopOnUnplug = value; Prefs.setStopOnUnplug(context, value) }
                OptionKey.Dim -> {
                    dimScreen = value
                    Prefs.setDimScreen(context, value)
                    // Re-arm so the wake-lock level follows the pref, but keep the same session.
                    if (CaffeineController.isActive()) CaffeineController.rearm(context)
                }
            }
        },
        onAddTile = {
            if (Build.VERSION.SDK_INT >= 33) {
                requestAddTile(context) {
                    Prefs.setTileAdded(context, true)
                    tileAdded = true
                }
            }
        },
        onPrivacy = { Privacy.openPrivacyPolicy(context) },
    )
}

@Composable
internal fun CaffeineContent(
    session: Session?,
    now: Long,
    selected: Duration,
    stopOnScreenOff: Boolean,
    stopOnUnplug: Boolean,
    dimScreen: Boolean,
    showAddTile: Boolean,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
    onExtend: () -> Unit,
    onSelect: (Duration) -> Unit,
    onOption: (OptionKey, Boolean) -> Unit,
    onAddTile: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(innerPadding)) {
            // Medium/expanded landscape windows (tablets, unfolded foldables, desktop) get two
            // panes side by side. Portrait and compact windows keep the single centered column.
            val twoPane = maxWidth >= TwoPaneMinWidth && maxWidth > maxHeight
            val hero: @Composable () -> Unit = {
                HeroSection(session, now, reducedMotion, onToggle, onExtend)
            }
            val controls: @Composable () -> Unit = {
                ControlsSection(
                    selected, stopOnScreenOff, stopOnUnplug, dimScreen, showAddTile,
                    onSelect, onOption, onAddTile, onPrivacy
                )
            }
            Column(
                Modifier
                    .fillMaxSize()
                    // Order matters: the scroll modifier receives infinite height, so the
                    // minimum has to be applied inside it for vertical centering to work.
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(
                        horizontal = if (twoPane) 32.dp else 24.dp,
                        vertical = if (twoPane) 24.dp else 16.dp,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = if (twoPane) 48.dp else 24.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                if (twoPane) {
                    Row(
                        // widthIn first: after fillMaxWidth the incoming width is already
                        // pinned to the parent's, so the cap would have no effect.
                        Modifier.widthIn(max = TwoPaneMaxWidth).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) { hero() }
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) { controls() }
                    }
                } else {
                    // Groups, so spacedBy only separates hero from controls instead of
                    // adding a gap between every node HeroSection/ControlsSection emit.
                    Column(
                        Modifier.widthIn(max = SinglePaneMaxWidth).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { hero() }
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) { controls() }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSection(
    session: Session?,
    now: Long,
    reducedMotion: Boolean,
    onToggle: () -> Unit,
    onExtend: () -> Unit,
) {
    val active = session?.isActive(now) == true
    val startAt = session?.startAt ?: now
    // Non-null only for a running timed session; null for off and for ∞.
    val endAt = session?.endAt?.takeIf { active }
    val timed = endAt != null
    val remainingMs = if (endAt != null) (endAt - now).coerceAtLeast(0L) else 0L
    val progress = if (endAt != null) {
        (remainingMs.toFloat() / (endAt - startAt).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } else null

    val appName = stringResource(R.string.app_name)
    val clockText = when {
        !active -> appName
        endAt == null -> formatClock((now - startAt).coerceAtLeast(0L) / SECOND)
        else -> formatClock((remainingMs + SECOND - 1) / SECOND)
    }
    // Minute granularity so a screen reader isn't told the time again every second.
    val clockDescription = when {
        !active -> null
        endAt == null -> stringResource(R.string.infinite_cd)
        else -> {
            val minutes = ((remainingMs + MINUTE - 1) / MINUTE).toInt()
            pluralStringResource(R.plurals.remaining_cd, minutes, minutes)
        }
    }

    HeroToggle(active = active, progress = progress, reducedMotion = reducedMotion, onToggle = onToggle)

    val clockStyle = MaterialTheme.typography.displayLargeEmphasized
        .merge(TextStyle(fontFeatureSettings = "tnum"))
    Text(
        text = clockText,
        style = clockStyle,
        maxLines = 1,
        textAlign = TextAlign.Center,
        autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = clockStyle.fontSize),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .semantics { clockDescription?.let { contentDescription = it } }
    )
    Text(
        text = stringResource(if (active) R.string.status_on else R.string.status_off),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
    )
    if (timed) {
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = onExtend, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.notif_extend))
        }
    }
}

@Composable
private fun ControlsSection(
    selected: Duration,
    stopOnScreenOff: Boolean,
    stopOnUnplug: Boolean,
    dimScreen: Boolean,
    showAddTile: Boolean,
    onSelect: (Duration) -> Unit,
    onOption: (OptionKey, Boolean) -> Unit,
    onAddTile: () -> Unit,
    onPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.duration),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
        )
        DurationPicker(selected = selected, onSelect = onSelect)
    }
    OptionsSection(
        stopOnScreenOff = stopOnScreenOff,
        stopOnUnplug = stopOnUnplug,
        dimScreen = dimScreen,
        onChange = onOption,
    )
    if (showAddTile) {
        TextButton(onClick = onAddTile, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.add_tile))
        }
    }
    TextButton(onClick = onPrivacy, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(R.string.privacy))
    }
}

/** m:ss, or h:mm:ss from one hour up. */
private fun formatClock(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = totalSeconds % 3600 / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@RequiresApi(33)
private fun requestAddTile(context: Context, onAdded: () -> Unit) {
    context.getSystemService(StatusBarManager::class.java).requestAddTileService(
        ComponentName(context, CaffeineTileService::class.java),
        context.getString(R.string.app_name),
        Icon.createWithResource(context, R.drawable.ic_coffee),
        context.mainExecutor
    ) { result ->
        if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
            result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED
        ) onAdded()
    }
}

// ---- Previews (fixed clock so they are deterministic) ----

private const val PREVIEW_NOW = 10_000_000L

private fun previewTimed(): Session {
    val s = Duration.Minutes(15).toSession(PREVIEW_NOW - 2 * MINUTE - 26 * SECOND)
    return s
}

private fun previewInfinite(): Session = Duration.Infinite.toSession(PREVIEW_NOW - 65 * MINUTE - 7 * SECOND)

@Composable
private fun PreviewScreen(
    session: Session?,
    selected: Duration = Duration.Minutes(15),
    showAddTile: Boolean = false,
) {
    CaffeineContent(
        session = session, now = PREVIEW_NOW, selected = selected,
        stopOnScreenOff = true, stopOnUnplug = false, dimScreen = false,
        showAddTile = showAddTile, reducedMotion = true,
        onToggle = {}, onExtend = {}, onSelect = {}, onOption = { _, _ -> }, onAddTile = {},
        onPrivacy = {},
    )
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewOff() = CaffeineTheme { PreviewScreen(null, showAddTile = true) }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewTimed() = CaffeineTheme { PreviewScreen(previewTimed()) }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewInfinite() = CaffeineTheme { PreviewScreen(previewInfinite(), Duration.Infinite) }

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
private fun PreviewLandscape() = CaffeineTheme { PreviewScreen(previewTimed(), Duration.Minutes(35)) }

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun PreviewTabletLandscape() = CaffeineTheme { PreviewScreen(previewTimed(), Duration.Minutes(35)) }

@Preview(showBackground = true, widthDp = 800, heightDp = 1280)
@Composable
private fun PreviewTabletPortrait() = CaffeineTheme { PreviewScreen(previewTimed(), showAddTile = true) }

@Preview(showBackground = true, heightDp = 900, fontScale = 2f)
@Composable
private fun PreviewFontScale() = CaffeineTheme { PreviewScreen(previewTimed(), showAddTile = true) }

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PreviewCoffeeLight() = CaffeineTheme(darkTheme = false, dynamicColor = false) {
    PreviewScreen(previewTimed(), showAddTile = true)
}

@Preview(showBackground = true, heightDp = 900, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCoffeeDark() = CaffeineTheme(darkTheme = true, dynamicColor = false) {
    PreviewScreen(previewTimed(), showAddTile = true)
}
