package io.legado.app.ui.compose.screens.rss

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.help.source.removeSortCache
import io.legado.app.help.source.sortUrls
import io.legado.app.model.rss.Rss
import io.legado.app.utils.GSON
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.fromJsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RssSortComposeViewModel(application: Application) : BaseViewModel(application) {

    var url: String? = null
    var sortUrl: String? = null
    var rssSource: RssSource? = null
    var order = System.currentTimeMillis()
    val articleStyle get() = rssSource?.articleStyle ?: 0
    var searchKey: String? = null
    var sourceName: String? = null

    private val _sortList = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val sortList = _sortList.asStateFlow()

    fun initData(intent: Intent) {
        execute {
            url = intent.getStringExtra("sourceUrl")
            url?.let { url ->
                rssSource = appDb.rssSourceDao.getByKey(url)
                rssSource?.let {
                    sourceName = it.sourceName
                } ?: let {
                    rssSource = RssSource(sourceUrl = url)
                }
            }
            sortUrl = intent.getStringExtra("sortUrl") ?: sortUrl
            searchKey = intent.getStringExtra("key")
        }.onFinally {
            updateSortList()
        }
    }

    fun updateSortList() {
        viewModelScope.launch(Dispatchers.IO) {
            val source = rssSource ?: return@launch
            val list = mutableListOf<Pair<String, String>>()
            if (searchKey != null) {
                val name = "搜索"
                val url = source.searchUrl ?: return@launch
                list.add(Pair(name, url))
            } else {
                sortUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    val urls: List<Pair<String, String>> = try {
                        if (url.isJsonObject()) {
                            GSON.fromJsonObject<Map<String, String>>(url)
                                .getOrThrow()
                                .map { (key, value) -> Pair(key, value) }
                        } else {
                            listOf(Pair("", url))
                        }
                    } catch (_: Exception) {
                        listOf(Pair("", url))
                    }
                    list.addAll(urls)
                } ?: run {
                    list.addAll(source.sortUrls())
                }
            }
            _sortList.value = list
        }
    }

    fun switchLayout() {
        rssSource?.let {
            if (it.articleStyle < 4) {
                it.articleStyle += 1
            } else {
                it.articleStyle = 0
            }
            execute {
                appDb.rssSourceDao.update(it)
            }
        }
    }

    fun clearArticles() {
        execute {
            url?.let {
                appDb.rssArticleDao.delete(it)
            }
            order = System.currentTimeMillis()
        }
    }

    fun clearSortCache(onFinally: () -> Unit) {
        execute {
            rssSource?.removeSortCache()
        }.onFinally {
            onFinally.invoke()
        }
    }
}
