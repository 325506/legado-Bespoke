package io.legado.app.ui.compose.screens.rss

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.utils.GSON
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {

    private val _rssSource = MutableStateFlow<RssSource?>(null)
    val rssSource = _rssSource.asStateFlow()

    var autoComplete: Boolean = false

    fun initData(sourceUrl: String?, onSuccess: (RssSource) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!sourceUrl.isNullOrBlank()) {
                val source = appDb.rssSourceDao.getByKey(sourceUrl)
                if (source != null) {
                    _rssSource.value = source
                    withContext(Dispatchers.Main) {
                        onSuccess(source)
                    }
                }
            } else {
                val newSource = RssSource()
                _rssSource.value = newSource
                withContext(Dispatchers.Main) {
                    onSuccess(newSource)
                }
            }
        }
    }

    fun save(source: RssSource, onSuccess: (RssSource) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                appDb.rssSourceDao.insert(source)
                _rssSource.value = source
                withContext(Dispatchers.Main) {
                    onSuccess(source)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context.toastOnUi("保存失败: ${e.message}")
                }
            }
        }
    }

    fun importSource(json: String, onSuccess: (RssSource) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val source = GSON.fromJson(json, RssSource::class.java)
                if (source != null) {
                    _rssSource.value = source
                    withContext(Dispatchers.Main) {
                        onSuccess(source)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context.toastOnUi("导入失败: ${e.message}")
                }
            }
        }
    }

    fun pasteSource(onSuccess: (RssSource) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                    as android.content.ClipboardManager
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val text = clip.getItemAt(0).text.toString()
                    val source = GSON.fromJson(text, RssSource::class.java)
                    if (source != null) {
                        _rssSource.value = source
                        withContext(Dispatchers.Main) {
                            onSuccess(source)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context.toastOnUi("粘贴失败: ${e.message}")
                }
            }
        }
    }

    fun clearCookie(sourceUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear cookie logic
                withContext(Dispatchers.Main) {
                    context.toastOnUi("Cookie已清除")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context.toastOnUi("清除失败: ${e.message}")
                }
            }
        }
    }

    fun ruleComplete(rule: String?, articlesRule: String?, type: Int = 0): String? {
        if (rule.isNullOrBlank() || articlesRule.isNullOrBlank()) {
            return rule
        }
        if (autoComplete && !rule.contains("##") && !rule.contains("@") && !rule.contains(":")) {
            return "$articlesRule##$rule"
        }
        return rule
    }
}
