package io.legado.app.ui.compose.screens.rss

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssArticle
import io.legado.app.data.entities.RssSource
import io.legado.app.model.rss.Rss
import io.legado.app.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RssArticlesComposeViewModel(application: Application) : BaseViewModel(application) {

    var order = System.currentTimeMillis()
    private var nextPageUrl: String? = null
    var sortName: String = ""
    var sortUrl: String = ""
    var searchKey: String? = null
    var page = 1

    private val _articles = MutableStateFlow<List<RssArticle>>(emptyList())
    val articles = _articles.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore = _hasMore.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    val loadFinallyLiveData = MutableLiveData<Boolean>()
    val loadErrorLiveData = MutableLiveData<String>()

    private var articlesFlowJob: Job? = null

    fun init(sortName: String, sortUrl: String, searchKey: String?) {
        this.sortName = sortName
        this.sortUrl = sortUrl
        this.searchKey = searchKey
    }

    fun loadArticles(rssSource: RssSource) {
        _loading.value = true
        page = 1
        order = System.currentTimeMillis()
        collectArticles(rssSource.sourceUrl)
        Rss.getArticles(viewModelScope, sortName, sortUrl, rssSource, page, searchKey).onSuccess(IO) {
            nextPageUrl = it.second
            val articles = it.first
            articles.forEach { rssArticle ->
                rssArticle.order = order--
            }
            appDb.rssArticleDao.insert(*articles.toTypedArray())
            if (!rssSource.ruleNextPage.isNullOrEmpty()) {
                appDb.rssArticleDao.clearOld(rssSource.sourceUrl, sortName, order)
            }
            val hasMore = articles.isNotEmpty() && !rssSource.ruleNextPage.isNullOrEmpty()
            _hasMore.value = hasMore
            _loading.value = false
        }.onError {
            _hasMore.value = false
            _loading.value = false
            AppLog.put("rss获取内容失败", it)
            loadErrorLiveData.postValue(it.stackTraceStr)
        }
    }

    fun loadMore(rssSource: RssSource) {
        if (_loading.value) return
        _loading.value = true
        page++
        val pageUrl = nextPageUrl
        if (pageUrl.isNullOrEmpty()) {
            _hasMore.value = false
            return
        }
        Rss.getArticles(viewModelScope, sortName, pageUrl, rssSource, page, searchKey).onSuccess(IO) {
            nextPageUrl = it.second
            loadMoreSuccess(it.first)
            _loading.value = false
        }.onError {
            _hasMore.value = false
            _loading.value = false
            AppLog.put("rss获取内容失败", it)
            loadErrorLiveData.postValue(it.stackTraceStr)
        }
    }

    private fun loadMoreSuccess(articles: MutableList<RssArticle>) {
        if (articles.isEmpty()) {
            _hasMore.value = false
            return
        }
        val firstArticle = articles.first()
        val dbFirstArticle = appDb.rssArticleDao.get(firstArticle.origin, firstArticle.link, firstArticle.sort)
        val lastArticle = articles.last()
        val dbLastArticle = appDb.rssArticleDao.get(lastArticle.origin, lastArticle.link, firstArticle.sort)
        if (dbFirstArticle != null && dbLastArticle != null) {
            _hasMore.value = false
        } else {
            articles.forEach {
                it.order = order--
            }
            appDb.rssArticleDao.append(*articles.toTypedArray())
        }
    }

    private fun collectArticles(origin: String) {
        articlesFlowJob?.cancel()
        articlesFlowJob = viewModelScope.launch(IO) {
            appDb.rssArticleDao.flowByOriginSort(origin, sortName).collect { list ->
                _articles.value = list
            }
        }
    }
}
