package com.howl.music.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.player.MiniPlayer
import com.howl.music.ui.player.PlaybackUiState
import com.howl.music.ui.theme.HowlSpacing
import kotlinx.coroutines.delay

private enum class Tab(val route: String, val label: String) {
    Library("library", "Library"),
    Equalizer("eq", "EQ"),
    Settings("settings", "Settings"),
}

private const val NOW_PLAYING = "now-playing"

@Composable
internal fun HowlNavHost(
    playback: PlaybackUiState,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSeekStart: () -> Unit,
    readPositionMs: () -> Long,
    debugTools: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Tab.Library.route
    Scaffold(
        bottomBar = {
            if (route != NOW_PLAYING) {
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
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
        ) {
            composable(Tab.Library.route) {
                Destination("Library") { Text("Your music will appear here.") }
            }
            composable(Tab.Equalizer.route) {
                Destination("Equalizer") { Text("Equalizer controls are coming here.") }
            }
            composable(Tab.Settings.route) {
                Destination("Settings") {
                    Text("Playback preferences will appear here.")
                    debugTools?.invoke(this)
                }
            }
            composable(NOW_PLAYING) {
                Destination("Now Playing") {
                    HowlButton("Back", { nav.popBackStack() }, primary = false)
                    AlbumArt(playback.artworkTrackUri, 128.dp)
                    Text(playback.title ?: "Nothing playing", style = MaterialTheme.typography.titleLarge)
                    Text(playback.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val position by produceState(playback.positionMs, playback) {
                        do {
                            value = readPositionMs()
                            if (playback.playRequested) delay(250)
                        } while (playback.playRequested)
                    }
                    Text("${position} ms", style = MaterialTheme.typography.bodySmall)
                    HowlButton(if (playback.playRequested) "Pause" else "Play", onToggle, enabled = playback.canToggle)
                    HowlButton("Seek to start", onSeekStart, enabled = playback.canSeek, primary = false)
                    HowlButton("Stop", onStop, enabled = playback.canSeek, primary = false)
                }
            }
        }
    }
}

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
        Text(title, style = MaterialTheme.typography.displayMedium)
        content()
    }
}
