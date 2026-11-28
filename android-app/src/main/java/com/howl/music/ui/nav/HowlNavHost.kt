package com.howl.music.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.howl.music.bridge.AnalysisSnapshot
import com.howl.music.media.PlaybackQueue.Action
import com.howl.music.media.PlaybackOutputDiagnostics
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.settings.DiagnosticsScreen
import com.howl.music.ui.visualizer.VisualizerScreen
import com.howl.music.ui.queue.QueueScreen
import com.howl.music.ui.queue.QueueUiState
import com.howl.music.data.db.LibraryRow
import com.howl.music.ui.library.LibraryScreen
import com.howl.music.ui.eq.EqualizerScreen
import com.howl.music.ui.eq.EqualizerViewModel
import com.howl.music.ui.player.MiniPlayer
import com.howl.music.ui.player.NowPlayingScreen
import com.howl.music.ui.theme.HowlMotion
import com.howl.music.ui.theme.LocalMotionMode
import com.howl.music.ui.theme.MotionMode
import com.howl.music.ui.player.PlaybackUiState
import com.howl.music.ui.theme.HowlSpacing

private enum class Tab(val route: String, val label: String) {
    Library("library", "Library"),
    Equalizer("eq", "EQ"),
    Settings("settings", "Settings"),
}

private const val NOW_PLAYING = "now-playing"
private const val QUEUE = "queue"
private const val VISUALIZER = "visualizer"
private const val DIAGNOSTICS = "diagnostics"

@UnstableApi
@Composable
internal fun HowlNavHost(
    playback: PlaybackUiState,
    readAnalysis: suspend () -> AnalysisSnapshot,
    readOutputDiagnostics: suspend () -> PlaybackOutputDiagnostics?,
    equalizer: EqualizerViewModel,
    onToggle: () -> Unit,
    onPlayTrack: (LibraryRow) -> Unit,
    onPlayNext: (LibraryRow) -> Unit,
    queue: QueueUiState,
    queueMessage: String?,
    onQueueMessageShown: () -> Unit,
    onQueueEdit: (Action, String, String) -> Unit,
    onScan: () -> Unit,
    onSeek: (String, Long) -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    reduceMotionPreference: Boolean,
    onReduceMotionChange: (Boolean) -> Unit,
    readPositionMs: () -> Long,
    debugTools: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Tab.Library.route
    val activeRoute by rememberUpdatedState(route)
    val motion by rememberUpdatedState(LocalMotionMode.current)
    val savedMotionPreference by rememberUpdatedState(reduceMotionPreference)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(queueMessage) {
        if (queueMessage != null) {
            snackbar.showSnackbar(queueMessage)
            onQueueMessageShown()
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (route != NOW_PLAYING && route != QUEUE && route != VISUALIZER && route != DIAGNOSTICS) {
                Column {
                    MiniPlayer(
                        playback,
                        onToggle,
                        { nav.navigate(NOW_PLAYING) { launchSingleTop = true } },
                        Modifier.padding(horizontal = HowlSpacing.Large, vertical = HowlSpacing.Small),
                    )
                    Tabs(route) { tab ->
                        if (route != tab.route) {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Library.route,
            modifier = Modifier.fillMaxSize().padding(padding),
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    motion == MotionMode.Off || !isPlayerExpansion(from, to) -> EnterTransition.None
                    motion == MotionMode.Reduced -> fadeIn(HowlMotion.reducedFade())
                    to == NOW_PLAYING -> fadeIn(HowlMotion.fade()) +
                        slideInVertically(HowlMotion.settle()) { it / 12 }
                    else -> fadeIn(HowlMotion.fade())
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    motion == MotionMode.Off || !isPlayerExpansion(from, to) -> ExitTransition.None
                    motion == MotionMode.Reduced -> fadeOut(HowlMotion.reducedFade())
                    from == NOW_PLAYING -> fadeOut(HowlMotion.fade()) +
                        slideOutVertically(HowlMotion.settle()) { it / 12 }
                    else -> fadeOut(HowlMotion.fade())
                }
            },
        ) {
            fun page(path: String, content: @Composable () -> Unit) {
                composable(path) {
                    val visibility = if (activeRoute == path) Modifier else
                        Modifier.clearAndSetSemantics { invisibleToUser() }
                    Box(Modifier.fillMaxSize().then(visibility)) { content() }
                }
            }
            page(Tab.Library.route) {
                LibraryScreen(onPlay = onPlayTrack, onPlayNext = onPlayNext, onScan = onScan)
            }
            page(Tab.Equalizer.route) {
                EqualizerScreen(equalizer)
            }
            page(Tab.Settings.route) {
                Destination("Settings") {
                    val reduceMotion = savedMotionPreference
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = HowlSpacing.TouchTarget)
                            .toggleable(reduceMotion, role = Role.Switch, onValueChange = onReduceMotionChange),
                        horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Reduce motion", modifier = Modifier.weight(1f))
                        Switch(checked = reduceMotion, onCheckedChange = null)
                    }
                    if (LocalMotionMode.current == MotionMode.Off) {
                        Text("Android animations are off", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HowlButton("Output diagnostics", { nav.navigate(DIAGNOSTICS) { launchSingleTop = true } }, primary = false)
                    debugTools?.invoke(this)
                }
            }
            page(NOW_PLAYING) {
                NowPlayingScreen(
                    playback = playback,
                    readPositionMs = readPositionMs,
                    onToggle = onToggle,
                    onSeek = onSeek,
                    onSkipBack = onSkipBack,
                    onSkipForward = onSkipForward,
                    onBack = { nav.popBackStack() },
                    onQueue = { nav.navigate(QUEUE) { launchSingleTop = true } },
                    onVisualizer = { nav.navigate(VISUALIZER) { launchSingleTop = true } },
                )
            }
            page(DIAGNOSTICS) {
                DiagnosticsScreen(playback.mediaId, readOutputDiagnostics, onBack = { nav.popBackStack() })
            }
            page(VISUALIZER) {
                VisualizerScreen(
                    playback, readAnalysis, onToggle,
                    onBack = { nav.popBackStack() },
                )
            }
            page(QUEUE) {
                QueueScreen(queue, onBack = { nav.popBackStack() }, onEdit = onQueueEdit)
            }
        }
    }
}

private fun isPlayerExpansion(from: String?, to: String?): Boolean =
    (from == NOW_PLAYING && Tab.entries.any { it.route == to }) ||
        (to == NOW_PLAYING && Tab.entries.any { it.route == from })

@Composable
private fun Tabs(route: String, onSelect: (Tab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainer) {
        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup().padding(HowlSpacing.Small),
            horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
        ) {
            for (tab in Tab.entries) {
                val active = route == tab.route
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                    color = if (active) colors.primaryContainer else colors.surfaceContainer,
                    contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.heightIn(min = HowlSpacing.TouchTarget)
                            .selectable(active, role = Role.Tab, onClick = { onSelect(tab) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(tab.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Destination(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(HowlSpacing.Screen),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
    ) {
        Text(title, style = MaterialTheme.typography.displayMedium, modifier = Modifier.semantics { heading() })
        content()
    }
}
