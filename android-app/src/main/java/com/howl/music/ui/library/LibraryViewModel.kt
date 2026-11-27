package com.howl.music.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.howl.music.data.db.HowlDatabase
import com.howl.music.data.db.LibraryRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

internal enum class LibrarySection(val label: String) {
    Tracks("Tracks"), Albums("Albums"), Artists("Artists"),
}

internal sealed interface LibraryResults {
    data object Loading : LibraryResults
    data object Error : LibraryResults
    data class Loaded(val rows: List<LibraryRow>, val hasMore: Boolean) : LibraryResults
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class LibraryViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {
    private val db = HowlDatabase.open(application)
    private val dao = db.libraryDao()

    val query = state.getStateFlow("query", "")
    val section = state.getStateFlow("section", LibrarySection.Tracks.name)
    val descending = state.getStateFlow("descending", false)
    val groupTitle = state.getStateFlow("groupTitle", "")
    private val groupId = state.getStateFlow("groupId", -1L)
    private val limit = state.getStateFlow("limit", PAGE_SIZE)
    private val retry = state.getStateFlow("retry", 0)

    private data class Filter(
        val query: String,
        val section: LibrarySection,
        val descending: Boolean,
        val group: Long,
        val count: Int,
    )

    val results = combine(query, section, descending, groupId, limit) { q, tab, reverse, group, count ->
        Filter(q.trim(), LibrarySection.valueOf(tab), reverse, group, count)
    }.combine(retry) { filter, _ -> filter }
        .flatMapLatest { filter ->
            val (q, tab, reverse, group, count) = filter
            val rows = when {
                tab == LibrarySection.Albums && group < 0 -> dao.browseAlbums(q, reverse, count + 1)
                tab == LibrarySection.Artists && group < 0 -> dao.browseArtists(q, reverse, count + 1)
                else -> dao.browseTracks(
                    q, reverse,
                    albumId = if (tab == LibrarySection.Albums) group else -1,
                    artistId = if (tab == LibrarySection.Artists) group else -1,
                    limit = count + 1,
                )
            }
            rows.map<List<LibraryRow>, LibraryResults> {
                LibraryResults.Loaded(it.take(count), it.size > count)
            }.onStart { if (count == PAGE_SIZE) emit(LibraryResults.Loading) }.catch { emit(LibraryResults.Error) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryResults.Loading)

    fun search(value: String) {
        state["limit"] = PAGE_SIZE
        state["query"] = value
    }

    fun select(value: LibrarySection) {
        if (value.name == section.value && groupId.value < 0) return
        backToGroups()
        state["section"] = value.name
    }

    fun toggleSort() {
        state["descending"] = !descending.value
    }

    fun openGroup(row: LibraryRow) {
        search("")
        state["groupTitle"] = row.title
        state["groupId"] = row.id
    }

    fun backToGroups() {
        state["groupId"] = -1L
        state["groupTitle"] = ""
        search("")
    }

    fun loadMore() {
        state["limit"] = limit.value + PAGE_SIZE
    }

    fun retry() {
        state["retry"] = retry.value + 1
    }

    override fun onCleared() {
        db.close()
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
