package io.legado.app.ui.compose.screens.rss

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.help.source.sortUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RssSourceDebugViewModel(application: Application) : BaseViewModel(application) {

    private val _debugLogs = MutableStateFlow<List<String>>(emptyList())
    val debugLogs = _debugLogs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _showHelp = MutableStateFlow(true)
    val showHelp = _showHelp.asStateFlow()

    private val _categoryText = MutableStateFlow("")
    val categoryText = _categoryText.asStateFlow()

    var rssSource: RssSource? = null
        private set

    var listSrc: String = ""
        private set

    var contentSrc: String = ""
        private set

    fun initData(sourceKey: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!sourceKey.isNullOrBlank()) {
                val source = appDb.rssSourceDao.getByKey(sourceKey)
                if (source != null) {
                    rssSource = source
                    loadCategoryText(source)
                }
            }
            withContext(Dispatchers.Main) {
                _showHelp.value = true
            }
        }
    }

    private fun loadCategoryText(source: RssSource) {
        viewModelScope.launch(Dispatchers.IO) {
            val sortKinds = source.sortUrls()?.filter { it.second.isNotBlank() }
            sortKinds?.firstOrNull()?.let {
                _categoryText.value = "${it.first}::${it.second}"
                if (it.first.startsWith("ERROR:")) {
                    addLog("获取发现出错\n${it.second}")
                    _showHelp.value = false
                }
            }
        }
    }

    fun startDebug(key: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _showHelp.value = false
            _debugLogs.value = emptyList()

            try {
                addLog("开始调试: $key")
                val source = rssSource
                if (source == null) {
                    addLog("错误: 未获取到订阅源")
                    return@launch
                }

                addLog("源名称: ${source.sourceName}")
                addLog("源地址: ${source.sourceUrl}")
                
                if (key.contains("::")) {
                    addLog("调试分类: $key")
                } else {
                    addLog("调试搜索: $key")
                }

                addLog("调试完成")
            } catch (e: Exception) {
                addLog("调试出错: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun addLog(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            _debugLogs.value = _debugLogs.value + message
        }
    }

    fun setShowHelp(show: Boolean) {
        _showHelp.value = show
    }
}
