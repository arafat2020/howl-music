package com.howl.music.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howl.music.data.db.LibraryRow
import com.howl.music.ui.components.AlbumArt
import com.howl.music.ui.components.HowlButton
import com.howl.music.ui.theme.HowlSpacing

@Composable
internal fun LibraryScreen(
    onPlay: (LibraryRow) -> Unit,
    onPlayNext: (LibraryRow) -> Unit,
    onScan: () -> Unit,
    model: LibraryViewModel = viewModel(),
) {
    val query by model.query.collectAsStateWithLifecycle()
    val sectionName by model.section.collectAsStateWithLifecycle()
    val descending by model.descending.collectAsStateWithLifecycle()
    val group by model.groupTitle.collectAsStateWithLifecycle()
    val result by model.results.collectAsStateWithLifecycle()
    val section = LibrarySection.valueOf(sectionName)
    val showsTracks = section == LibrarySection.Tracks || group.isNotEmpty()
    BackHandler(enabled = group.isNotEmpty(), onBack = model::backToGroups)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.retry() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = HowlSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(top = HowlSpacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Library", style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() })
            HowlButton("Scan music", onScan, primary = false)
        }
        Row(
            Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
        ) {
            for (tab in LibrarySection.entries) {
                FilterChip(
                    selected = tab == section,
                    onClick = { model.select(tab) },
                    label = { Text(tab.label) },
                    modifier = Modifier.heightIn(min = HowlSpacing.TouchTarget),
                )
            }
        }
        if (group.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small)) {
                HowlButton("Back", model::backToGroups, primary = false)
                Text(group, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = model::search,
                label = { Text("Search ${if (showsTracks) "tracks" else section.label.lowercase()}") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            HowlButton(if (descending) "Z–A" else "A–Z", model::toggleSort, primary = false)
        }

        when (val data = result) {
            LibraryResults.Loading -> CircularProgressIndicator()
            LibraryResults.Error -> {
                Text("Could not load the library.")
                HowlButton("Retry", model::retry)
            }
            is LibraryResults.Loaded -> {
                if (data.rows.isEmpty()) {
                    Text(if (query.isBlank()) "No ${if (showsTracks) "tracks" else section.label.lowercase()} found." else "No results for “$query”.")
                } else {
                    key(section, group, query, descending) {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = HowlSpacing.Small),
                            verticalArrangement = Arrangement.spacedBy(HowlSpacing.ExtraSmall),
                        ) {
                            items(data.rows, key = { it.id }) { row ->
                                LibraryItem(row, if (showsTracks) onPlayNext else null, if (showsTracks) onPlay else model::openGroup)
                            }
                            if (data.hasMore) item { HowlButton("Load more", model::loadMore, primary = false) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItem(row: LibraryRow, onPlayNext: ((LibraryRow) -> Unit)?, onClick: (LibraryRow) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClickLabel = if (onPlayNext != null) "Play" else "Browse") { onClick(row) }
            .padding(vertical = HowlSpacing.ExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(HowlSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArt(row.uri, 48.dp)
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = listOfNotNull(row.artist, row.album).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (onPlayNext == null) {
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            HowlButton(
                "Play next", { onPlayNext(row) },
                modifier = Modifier.semantics { contentDescription = "Play ${row.title} next" },
                primary = false,
            )
        }
    }
}
