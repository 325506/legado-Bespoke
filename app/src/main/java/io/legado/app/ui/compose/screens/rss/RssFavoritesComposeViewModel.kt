package io.legado.app.ui.compose.screens.rss

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssStar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RssFavoritesComposeViewModel(application: Application) : BaseViewModel(application) {

    private val _groupList = MutableStateFlow<List<String>>(emptyList())
    val groupList = _groupList.asStateFlow()

    private val _favoritesMap = MutableStateFlow<Map<String, List<RssStar>>>(emptyMap())
    val favoritesMap = _favoritesMap.asStateFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        viewModelScope.launch(Dispatchers.IO) {
            appDb.rssStarDao.flowGroups().catch {
                AppLog.put("订阅分组数据获取失败\n${it.localizedMessage}", it)
            }.collect { groups ->
                _groupList.value = groups
                groups.forEach { group ->
                    loadFavoritesByGroup(group)
                }
            }
        }
    }

    private fun loadFavoritesByGroup(group: String) {
        viewModelScope.launch(Dispatchers.IO) {
            appDb.rssStarDao.flowByGroup(group).catch {
                AppLog.put("收藏数据获取失败\n${it.localizedMessage}", it)
            }.collect { list ->
                val current = _favoritesMap.value.toMutableMap()
                current[group] = list
                _favoritesMap.value = current
            }
        }
    }

    fun deleteStar(rssStar: RssStar) {
        execute {
            appDb.rssStarDao.delete(rssStar.origin, rssStar.link)
        }
    }

    fun deleteGroup(group: String) {
        execute {
            appDb.rssStarDao.deleteByGroup(group)
        }
    }

    fun deleteAll() {
        execute {
            appDb.rssStarDao.deleteAll()
        }
    }
}
