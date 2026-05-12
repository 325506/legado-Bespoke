package io.legado.app.ui.compose.screens.rss

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssScreenViewModel(application: Application) : AndroidViewModel(application) {

    var rssSources: List<RssSource> by mutableStateOf(emptyList())
        private set
    var searchQuery: String by mutableStateOf("")
        private set
    var groups: Set<String> by mutableStateOf(emptySet())
        private set

    private var groupsFlowJob: Job? = null
    private var rssFlowJob: Job? = null

    init {
        initGroupData()
        upRssFlowJob()
    }

    private fun initGroupData() {
        groupsFlowJob?.cancel()
        groupsFlowJob = viewModelScope.launch {
            appDb.rssSourceDao.flowEnabledGroups()
                .catch {
                    AppLog.put("订阅界面获取分组数据失败\n${it.localizedMessage}", it)
                }
                .conflate()
                .collect {
                    groups = it.toSet()
                }
        }
    }

    fun upRssFlowJob(searchKey: String? = null) {
        rssFlowJob?.cancel()
        searchQuery = searchKey ?: ""
        rssFlowJob = viewModelScope.launch {
            val flow = when {
                searchKey.isNullOrEmpty() -> appDb.rssSourceDao.flowEnabled()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowEnabledByGroup(key)
                }
                else -> appDb.rssSourceDao.flowEnabled(searchKey)
            }
            flow
                .catch {
                    AppLog.put("订阅界面更新数据出错", it)
                }
                .flowOn(IO)
                .conflate()
                .collect {
                    rssSources = it
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        upRssFlowJob(query)
    }

    fun onGroupFilter(group: String) {
        upRssFlowJob(group)
    }
}
