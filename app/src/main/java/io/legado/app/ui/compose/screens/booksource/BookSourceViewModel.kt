package io.legado.app.ui.compose.screens.booksource

import android.app.Application
import android.text.TextUtils
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppPattern
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.toBookSource
import io.legado.app.help.source.SourceHelp
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.cnCompare
import io.legado.app.utils.normalizeFileName
import io.legado.app.utils.outputStream
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.stackTraceStr
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeToOutputStream
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookSourceViewModel(application: Application) : BaseViewModel(application) {

    private val _bookSources = MutableStateFlow<List<BookSourcePart>>(emptyList())
    val bookSources: StateFlow<List<BookSourcePart>> = _bookSources.asStateFlow()

    private val _selection = MutableStateFlow<Set<BookSourcePart>>(emptySet())
    val selection: StateFlow<Set<BookSourcePart>> = _selection.asStateFlow()

    private val _groups = MutableStateFlow<List<String>>(emptyList())
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private var searchKey: String? = null
    private var sortType: Int = BookSourceSort.Default
    private var sortAscending: Boolean = true
    private var groupByDomain: Boolean = false

    init {
        observeBookSources()
        observeGroups()
    }

    private fun observeBookSources() {
        viewModelScope.launch {
            appDb.bookSourceDao.flowAll()
                .catch { }
                .flowOn(IO)
                .conflate()
                .collect { sources ->
                    updateSourceList(sources)
                }
        }
    }

    private fun observeGroups() {
        viewModelScope.launch {
            appDb.bookSourceDao.flowGroups()
                .catch { }
                .flowOn(IO)
                .conflate()
                .collect { groups ->
                    _groups.value = groups
                }
        }
    }

    private fun updateSourceList(sources: List<BookSourcePart>) {
        val filtered = when {
            searchKey.isNullOrEmpty() -> sources

            searchKey == appCtx.getString(R.string.enabled) -> sources.filter { it.enabled }
            searchKey == appCtx.getString(R.string.disabled) -> sources.filter { !it.enabled }
            searchKey == appCtx.getString(R.string.need_login) -> sources.filter { it.hasLoginUrl == true }
            searchKey == appCtx.getString(R.string.no_group) -> sources.filter { it.bookSourceGroup.isNullOrBlank() }
            searchKey == appCtx.getString(R.string.enabled_explore) -> sources.filter { it.enabledExplore }
            searchKey == appCtx.getString(R.string.disabled_explore) -> sources.filter { !it.enabledExplore }
            searchKey?.startsWith("group:") == true -> {
                val group = searchKey!!.substringAfter("group:")
                sources.filter { source ->
                    source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.contains(group) == true
                }
            }

            else -> sources.filter { source ->
                source.bookSourceName.contains(searchKey!!, true) ||
                        source.bookSourceGroup?.contains(searchKey!!, true) == true ||
                        source.bookSourceUrl.contains(searchKey!!, true)
            }
        }

        val sorted = if (groupByDomain) {
            filtered.sortedWith(
                compareBy<BookSourcePart> { getSourceHost(it.bookSourceUrl) == "#" }
                    .thenBy { getSourceHost(it.bookSourceUrl) }
                    .thenByDescending { it.lastUpdateTime }
            )
        } else if (sortAscending) {
            when (sortType) {
                BookSourceSort.Weight -> filtered.sortedBy { it.weight }
                BookSourceSort.Name -> filtered.sortedWith { o1, o2 ->
                    o1.bookSourceName.cnCompare(o2.bookSourceName)
                }

                BookSourceSort.Url -> filtered.sortedBy { it.bookSourceUrl }
                BookSourceSort.Update -> filtered.sortedByDescending { it.lastUpdateTime }
                BookSourceSort.Respond -> filtered.sortedBy { it.respondTime }
                BookSourceSort.Enable -> filtered.sortedWith { o1, o2 ->
                    var sort = -o1.enabled.compareTo(o2.enabled)
                    if (sort == 0) {
                        sort = o1.bookSourceName.cnCompare(o2.bookSourceName)
                    }
                    sort
                }

                else -> filtered
            }
        } else {
            when (sortType) {
                BookSourceSort.Weight -> filtered.sortedByDescending { it.weight }
                BookSourceSort.Name -> filtered.sortedWith { o1, o2 ->
                    o2.bookSourceName.cnCompare(o1.bookSourceName)
                }

                BookSourceSort.Url -> filtered.sortedByDescending { it.bookSourceUrl }
                BookSourceSort.Update -> filtered.sortedBy { it.lastUpdateTime }
                BookSourceSort.Respond -> filtered.sortedByDescending { it.respondTime }
                BookSourceSort.Enable -> filtered.sortedWith { o1, o2 ->
                    var sort = o1.enabled.compareTo(o2.enabled)
                    if (sort == 0) {
                        sort = o1.bookSourceName.cnCompare(o2.bookSourceName)
                    }
                    sort
                }

                else -> filtered.reversed()
            }
        }

        _bookSources.value = sorted
    }

    private fun getSourceHost(origin: String): String {
        return try {
            origin.substringAfter("://").substringBefore("/").substringBefore(":")
        } catch (e: Exception) {
            "#"
        }
    }

    fun search(key: String?) {
        searchKey = key
        val currentSources = _bookSources.value
        updateSourceList(currentSources)
    }

    fun sort(type: Int, ascending: Boolean) {
        sortType = type
        sortAscending = ascending
        val currentSources = _bookSources.value
        updateSourceList(currentSources)
    }

    fun groupByDomain(enabled: Boolean) {
        groupByDomain = enabled
        val currentSources = _bookSources.value
        updateSourceList(currentSources)
    }

    fun filterByGroup(group: String) {
        searchKey = if (group.isEmpty()) null else group
        val currentSources = _bookSources.value
        updateSourceList(currentSources)
    }

    fun toggleSelection(source: BookSourcePart) {
        val current = _selection.value.toMutableSet()
        if (current.any { it.bookSourceUrl == source.bookSourceUrl }) {
            current.removeIf { it.bookSourceUrl == source.bookSourceUrl }
        } else {
            current.add(source)
        }
        _selection.value = current
    }

    fun selectAll() {
        _selection.value = _bookSources.value.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun invertSelection() {
        val current = _selection.value.toMutableSet()
        val all = _bookSources.value.toSet()
        _selection.value = all.filter { !current.any { s -> s.bookSourceUrl == it.bookSourceUrl } }.toSet()
    }

    fun topSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val minOrder = appDb.bookSourceDao.minOrder - 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = minOrder - index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun bottomSource(vararg sources: BookSourcePart) {
        execute {
            sources.sortBy { it.customOrder }
            val maxOrder = appDb.bookSourceDao.maxOrder + 1
            val array = sources.mapIndexed { index, it ->
                it.copy(customOrder = maxOrder + index)
            }
            appDb.bookSourceDao.upOrder(array)
        }
    }

    fun delete(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSourceParts(listOf(source))
        }
    }

    fun deleteSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            SourceHelp.deleteBookSourceParts(selected.toList())
            clearSelection()
        }
    }

    fun update(source: BookSourcePart) {
        execute {
            appDb.bookSourceDao.enable(source.bookSourceUrl, source.enabled)
        }
    }

    fun updateExplore(source: BookSourcePart) {
        execute {
            appDb.bookSourceDao.enableExplore(source.bookSourceUrl, source.enabledExplore)
        }
    }

    fun enableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.bookSourceDao.enable(true, selected.toList())
        }
    }

    fun disableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.bookSourceDao.enable(false, selected.toList())
        }
    }

    fun enableSelectExplore() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.bookSourceDao.enableExplore(true, selected.toList())
        }
    }

    fun disableSelectExplore() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.bookSourceDao.enableExplore(false, selected.toList())
        }
    }

    fun topSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val minOrder = appDb.bookSourceDao.minOrder - selected.size
            selected.sortedBy { it.customOrder }.forEachIndexed { index, source ->
                appDb.bookSourceDao.upOrder(source.bookSourceUrl, minOrder + index)
            }
        }
    }

    fun bottomSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val maxOrder = appDb.bookSourceDao.maxOrder
            selected.sortedBy { it.customOrder }.forEachIndexed { index, source ->
                appDb.bookSourceDao.upOrder(source.bookSourceUrl, maxOrder + index + 1)
            }
        }
    }

    fun selectionAddToGroups(sources: List<BookSourcePart>, group: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    addGroup(group)
                }
            }
            appDb.bookSourceDao.upGroup(array)
        }
    }

    fun selectionRemoveFromGroups(sources: List<BookSourcePart>, group: String) {
        execute {
            val array = sources.map {
                it.copy().apply {
                    removeGroup(group)
                }
            }
            appDb.bookSourceDao.upGroup(array)
        }
    }

    fun saveToFile(
        sources: List<BookSourcePart>,
        success: (file: File, name: String) -> Unit
    ) {
        execute {
            val name = if (sources.size == 1) {
                "bookSource_${sources.first().bookSourceName.normalizeFileName()}.json"
            } else {
                val timestamp = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault()).format(Date())
                "bookSource_$timestamp.json"
            }
            val path = "${context.filesDir}/shareBookSource.json"
            FileUtils.delete(path)
            val file = FileUtils.createFileWithReplace(path)
            file.outputStream().buffered().use {
                GSON.writeToOutputStream(it, sources.map { s -> appDb.bookSourceDao.getBookSource(s.bookSourceUrl) }.filterNotNull())
            }
            file to name
        }.onSuccess { (file, name) ->
            success.invoke(file, name)
        }.onError {
            context.toastOnUi(it.stackTraceStr)
        }
    }

    fun addGroup(group: String) {
        execute {
            val sources = appDb.bookSourceDao.noGroup
            sources.forEach { source ->
                source.bookSourceGroup = group
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun upGroup(oldGroup: String, newGroup: String?) {
        execute {
            val sources = appDb.bookSourceDao.getByGroup(oldGroup)
            sources.forEach { source ->
                source.bookSourceGroup?.splitNotBlank(",")?.toHashSet()?.let {
                    it.remove(oldGroup)
                    if (!newGroup.isNullOrEmpty())
                        it.add(newGroup)
                    source.bookSourceGroup = TextUtils.join(",", it)
                }
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun delGroup(group: String) {
        execute {
            val sources = appDb.bookSourceDao.getByGroup(group)
            sources.forEach { source ->
                source.removeGroup(group)
            }
            appDb.bookSourceDao.update(*sources.toTypedArray())
        }
    }

    fun importDefault() {
        execute {
        }
    }
}

object BookSourceSort {
    const val Default = 0
    const val Weight = 1
    const val Name = 2
    const val Url = 3
    const val Update = 4
    const val Respond = 5
    const val Enable = 6
}
