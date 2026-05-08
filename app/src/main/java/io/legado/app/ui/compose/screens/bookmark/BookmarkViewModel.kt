package io.legado.app.ui.compose.screens.bookmark

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark
import io.legado.app.utils.FileDoc
import io.legado.app.utils.GSON
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.openOutputStream
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.writeToOutputStream
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookmarkViewModel(application: Application) : AndroidViewModel(application) {

    val bookmarks = appDb.bookmarkDao.flowAll()
        .catch {
            AppLog.put("所有书签界面获取数据失败\n${it.localizedMessage}", it)
        }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun exportBookmark(treeUri: Uri) {
        viewModelScope.launch {
            kotlin.runCatching {
                val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                val fileName = "bookmark-${dateFormat.format(Date())}.json"
                val dirDoc = FileDoc.fromUri(treeUri, true)
                dirDoc.createFileIfNotExist(fileName).openOutputStream().getOrThrow().use {
                    GSON.writeToOutputStream(it, appDb.bookmarkDao.all)
                }
                getApplication<Application>().toastOnUi("导出成功")
            }.onFailure {
                AppLog.put("导出失败\n${it.localizedMessage}", it, true)
            }
        }
    }

    fun exportBookmarkMd(treeUri: Uri) {
        viewModelScope.launch {
            kotlin.runCatching {
                val dateFormat = SimpleDateFormat("yyMMddHHmmss", Locale.getDefault())
                val fileName = "bookmark-${dateFormat.format(Date())}.md"
                val dirDoc = FileDoc.fromUri(treeUri, true)
                val fileDoc = dirDoc.createFileIfNotExist(fileName).openOutputStream().getOrThrow()
                fileDoc.use { outputStream ->
                    var name = ""
                    var author = ""
                    appDb.bookmarkDao.all.forEach {
                        if (it.bookName != name && it.bookAuthor != author) {
                            name = it.bookName
                            author = it.bookAuthor
                            outputStream.write("## ${it.bookName} ${it.bookAuthor}\n\n".toByteArray())
                        }
                        outputStream.write("#### ${it.chapterName}\n\n".toByteArray())
                        outputStream.write("###### 原文\n ${it.bookText}\n\n".toByteArray())
                        outputStream.write("###### 摘要\n ${it.content}\n\n".toByteArray())
                    }
                }
                getApplication<Application>().toastOnUi("导出成功")
            }.onFailure {
                AppLog.put("导出失败\n${it.localizedMessage}", it, true)
            }
        }
    }
}
