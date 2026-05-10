package io.legado.app.ui.compose.screens.rss

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.DefaultData
import io.legado.app.help.source.SourceHelp
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.normalizeFileName
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.stackTraceStr
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RssSourceViewModel(application: Application) : BaseViewModel(application) {

    private val _searchKey = MutableStateFlow("")
    private val _selection = MutableStateFlow<List<RssSource>>(emptyList())
    val selection = _selection.asStateFlow()

    private val _rssSources = MutableStateFlow<List<RssSource>>(emptyList())
    val rssSources = _rssSources.asStateFlow()

    val groups: Flow<List<String>> = appDb.rssSourceDao.flowGroups()
        .flowOn(Dispatchers.IO)
        .conflate()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            _searchKey.collect { key ->
                when {
                    key.isBlank() -> appDb.rssSourceDao.flowAll()
                    key == context.getString(io.legado.app.R.string.enabled) -> appDb.rssSourceDao.flowEnabled()
                    key == context.getString(io.legado.app.R.string.disabled) -> appDb.rssSourceDao.flowDisabled()
                    key == context.getString(io.legado.app.R.string.need_login) -> appDb.rssSourceDao.flowLogin()
                    key == context.getString(io.legado.app.R.string.no_group) -> appDb.rssSourceDao.flowNoGroup()
                    key.startsWith("group:") -> {
                        val group = key.substringAfter("group:")
                        appDb.rssSourceDao.flowGroupSearch(group)
                    }
                    else -> appDb.rssSourceDao.flowSearch(key)
                }.catch {
                    // Handle error silently
                }.flowOn(Dispatchers.IO).conflate().collect { sources ->
                    _rssSources.value = sources
                }
            }
        }
    }

    fun search(key: String) {
        _searchKey.value = key
    }

    fun toggleSelection(source: RssSource) {
        val currentSelection = _selection.value.toMutableList()
        if (currentSelection.any { it.sourceUrl == source.sourceUrl }) {
            currentSelection.removeAll { it.sourceUrl == source.sourceUrl }
        } else {
            currentSelection.add(source)
        }
        _selection.value = currentSelection
    }

    fun selectAll() {
        _selection.value = _rssSources.value.toList()
    }

    fun invertSelection() {
        val currentSelection = _selection.value.map { it.sourceUrl }.toSet()
        _selection.value = _rssSources.value.filter { it.sourceUrl !in currentSelection }
    }

    fun clearSelection() {
        _selection.value = emptyList()
    }

    fun enableSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = _selection.value
            val array = Array(sources.size) {
                sources[it].copy(enabled = true)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun disableSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = _selection.value
            val array = Array(sources.size) {
                sources[it].copy(enabled = false)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun topSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = _selection.value.sortedBy { it.customOrder }
            val minOrder = appDb.rssSourceDao.minOrder - 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = minOrder - it)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun bottomSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            val sources = _selection.value.sortedBy { it.customOrder }
            val maxOrder = appDb.rssSourceDao.maxOrder + 1
            val array = Array(sources.size) {
                sources[it].copy(customOrder = maxOrder + it)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun topSource(source: RssSource) {
        viewModelScope.launch(Dispatchers.IO) {
            val minOrder = appDb.rssSourceDao.minOrder - 1
            appDb.rssSourceDao.update(source.copy(customOrder = minOrder))
        }
    }

    fun bottomSource(source: RssSource) {
        viewModelScope.launch(Dispatchers.IO) {
            val maxOrder = appDb.rssSourceDao.maxOrder + 1
            appDb.rssSourceDao.update(source.copy(customOrder = maxOrder))
        }
    }

    fun deleteSelection() {
        viewModelScope.launch(Dispatchers.IO) {
            SourceHelp.deleteRssSources(_selection.value)
            _selection.value = emptyList()
        }
    }

    fun delete(source: RssSource) {
        viewModelScope.launch(Dispatchers.IO) {
            SourceHelp.deleteRssSources(listOf(source))
        }
    }

    fun update(vararg sources: RssSource) {
        viewModelScope.launch(Dispatchers.IO) {
            appDb.rssSourceDao.update(*sources)
        }
    }

    fun selectionAddToGroups(sources: List<RssSource>, groups: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val array = Array(sources.size) {
                sources[it].copy().addGroup(groups)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<RssSource>, groups: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val array = Array(sources.size) {
                sources[it].copy().removeGroup(groups)
            }
            appDb.rssSourceDao.update(*array)
        }
    }

    fun filterByGroup(group: String) {
        _searchKey.value = group
    }

    fun sort(sortType: RssSourceSort) {
        viewModelScope.launch(Dispatchers.IO) {
            when (sortType) {
                RssSourceSort.Default -> {
                    val sources = appDb.rssSourceDao.all
                    for ((index, source) in sources.withIndex()) {
                        source.customOrder = index + 1
                    }
                    appDb.rssSourceDao.update(*sources.toTypedArray())
                }
                RssSourceSort.Name -> {
                    // Sort by name handled in UI
                }
                RssSourceSort.Url -> {
                    // Sort by URL handled in UI
                }
            }
        }
    }

    fun exportSelection(sources: List<RssSource>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val name = if (sources.size == 1) {
                    "rssSource_${sources.first().sourceName.normalizeFileName()}.json"
                } else {
                    val timestamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
                    "rssSource_$timestamp.json"
                }
                val path = "${context.filesDir}/shareRssSource.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                file.writeText(GSON.toJson(sources))
                // Callback to handle export
            } catch (e: Exception) {
                context.toastOnUi(e.stackTraceStr)
            }
        }
    }

    fun shareSelection(sources: List<RssSource>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = "${context.filesDir}/shareRssSource.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                file.writeText(GSON.toJson(sources))
                // Callback to handle share
            } catch (e: Exception) {
                context.toastOnUi(e.stackTraceStr)
            }
        }
    }

    fun importDefault() {
        viewModelScope.launch(Dispatchers.IO) {
            DefaultData.importDefaultRssSources()
        }
    }
}
