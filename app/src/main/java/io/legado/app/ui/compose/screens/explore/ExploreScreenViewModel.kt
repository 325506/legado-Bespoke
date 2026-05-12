package io.legado.app.ui.compose.screens.explore

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.help.source.SourceHelp
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ExploreScreenViewModel(application: Application) : AndroidViewModel(application) {

    var exploreSources: List<BookSourcePart> by mutableStateOf(emptyList())
        private set
    var searchQuery: String by mutableStateOf("")
        private set
    var groups: Set<String> by mutableStateOf(emptySet())
        private set
    var expandedIndex: Int by mutableStateOf(-1)
        private set

    private var groupsFlowJob: Job? = null
    private var exploreFlowJob: Job? = null

    init {
        initGroupData()
        upExploreData()
    }

    private fun initGroupData() {
        groupsFlowJob?.cancel()
        groupsFlowJob = viewModelScope.launch {
            appDb.bookSourceDao.flowExploreGroups()
                .catch {
                    AppLog.put("发现界面获取分组数据失败\n${it.localizedMessage}", it)
                }
                .conflate()
                .distinctUntilChanged()
                .collect {
                    groups = it.toSet()
                }
        }
    }

    fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        searchQuery = searchKey ?: ""
        exploreFlowJob = viewModelScope.launch {
            val flow = when {
                searchKey.isNullOrEmpty() -> appDb.bookSourceDao.flowExplore()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.bookSourceDao.flowGroupExplore(key)
                }
                else -> appDb.bookSourceDao.flowExplore(searchKey)
            }
            flow
                .catch {
                    AppLog.put("发现界面更新数据出错", it)
                }
                .flowOn(IO)
                .conflate()
                .collect {
                    exploreSources = it
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        upExploreData(query)
    }

    fun onGroupFilter(group: String) {
        upExploreData(group)
    }

    fun toggleExpand(index: Int) {
        expandedIndex = if (expandedIndex == index) -1 else index
    }

    fun topSource(source: BookSourcePart) {
        execute {
            val minXh = appDb.bookSourceDao.minOrder
            source.customOrder = minXh - 1
            appDb.bookSourceDao.upOrder(source)
        }
    }

    fun deleteSource(source: BookSourcePart) {
        execute {
            SourceHelp.deleteBookSource(source.bookSourceUrl)
        }
    }

    private fun execute(block: suspend () -> Unit) {
        viewModelScope.launch(IO) {
            try {
                block()
            } catch (e: Exception) {
                AppLog.put("发现界面操作失败", e)
            }
        }
    }
}
