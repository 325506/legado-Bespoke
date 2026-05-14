package io.legado.app.ui.compose.screens.bookshelf

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.stream.JsonWriter
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.decompressed
import io.legado.app.help.http.newCallResponseBody
import io.legado.app.help.http.okHttpClient
import io.legado.app.help.http.text
import io.legado.app.model.analyzeRule.AnalyzeUrl
import io.legado.app.model.webBook.WebBook
import io.legado.app.utils.FileUtils
import io.legado.app.utils.GSON
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.cnCompare
import io.legado.app.utils.fromJsonArray
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.isJsonArray
import io.legado.app.utils.printOnDebug
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.math.max

data class LayoutConfig(
    val bookshelfLayout: Int,
    val bookGroupStyle: Int,
    val showUnread: Boolean,
    val showLastUpdateTime: Boolean,
    val showWaitUpCount: Boolean,
    val bookshelfMargin: Int,
    val showBookname: Int,
    val bookshelfSort: Int
)

class BookshelfViewModel(application: Application) : BaseViewModel(application) {

    private val _groupId = MutableStateFlow(BookGroup.IdRoot)
    val groupId: StateFlow<Long> = _groupId.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _layoutConfig = MutableStateFlow(
        LayoutConfig(
            bookshelfLayout = AppConfig.bookshelfLayout,
            bookGroupStyle = AppConfig.bookGroupStyle,
            showUnread = AppConfig.showUnread,
            showLastUpdateTime = AppConfig.showLastUpdateTime,
            showWaitUpCount = AppConfig.showWaitUpCount,
            bookshelfMargin = AppConfig.bookshelfMargin,
            showBookname = AppConfig.showBookname,
            bookshelfSort = AppConfig.bookshelfSort
        )
    )
    val layoutConfig: StateFlow<LayoutConfig> = _layoutConfig.asStateFlow()

    val bookshelfLayout: Int get() = _layoutConfig.value.bookshelfLayout
    val bookGroupStyle: Int get() = _layoutConfig.value.bookGroupStyle
    val showUnread: Boolean get() = _layoutConfig.value.showUnread
    val showLastUpdateTime: Boolean get() = _layoutConfig.value.showLastUpdateTime
    val showWaitUpCount: Boolean get() = _layoutConfig.value.showWaitUpCount
    val bookshelfMargin: Int get() = _layoutConfig.value.bookshelfMargin
    val showBookname: Int get() = _layoutConfig.value.showBookname
    val bookshelfSort: Int get() = _layoutConfig.value.bookshelfSort

    fun updateLayoutConfig() {
        _layoutConfig.value = LayoutConfig(
            bookshelfLayout = AppConfig.bookshelfLayout,
            bookGroupStyle = AppConfig.bookGroupStyle,
            showUnread = AppConfig.showUnread,
            showLastUpdateTime = AppConfig.showLastUpdateTime,
            showWaitUpCount = AppConfig.showWaitUpCount,
            bookshelfMargin = AppConfig.bookshelfMargin,
            showBookname = AppConfig.showBookname,
            bookshelfSort = AppConfig.bookshelfSort
        )
    }

    var addBookJob: Coroutine<*>? = null
    val addBookProgressLiveData = MutableLiveData(-1)

    val bookGroups = appDb.bookGroupDao.show
        .asFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val books: StateFlow<List<Book>> = _groupId
        .flatMapLatest { groupId ->
            appDb.bookDao.flowByGroup(groupId)
                .map { list ->
                    sortBooks(list, AppConfig.getBookSortByGroupId(groupId))
                }
        }
        .catch { }
        .conflate()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun sortBooks(list: List<Book>, sort: Int): List<Book> {
        return when (sort) {
            1 -> list.sortedByDescending { it.latestChapterTime }
            2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
            3 -> list.sortedBy { it.order }
            4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
            5 -> list.sortedWith { o1, o2 -> o1.author.cnCompare(o2.author) }
            else -> list.sortedByDescending { it.durChapterTime }
        }
    }

    fun setGroupId(groupId: Long) {
        _groupId.value = groupId
    }

    fun backToRoot(): Boolean {
        if (_groupId.value != BookGroup.IdRoot) {
            _groupId.value = BookGroup.IdRoot
            return true
        }
        return false
    }

    fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        addBookJob = execute {
            val hasBookUrlPattern: List<BookSourcePart> by lazy {
                appDb.bookSourceDao.hasBookUrlPattern
            }
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (appDb.bookDao.getBook(bookUrl) != null) {
                    successCount++
                    continue
                }
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source: BookSource? = null
                val urlMatcher = AnalyzeUrl.paramPattern.matcher(bookUrl)
                if (urlMatcher.find()) {
                    val origin = GSON.fromJsonObject<AnalyzeUrl.UrlOption>(
                        bookUrl.substring(urlMatcher.end())
                    ).getOrNull()?.getOrigin()
                    try {
                        origin?.let {
                            appDb.bookSourceDao.getBookSource(it)?.let { bs ->
                                if (bookUrl.matches(bs.bookUrlPattern!!.toRegex())) {
                                    source = bs
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
                if (source == null) {
                    source = appDb.bookSourceDao.getBookSourceAddBook(baseUrl)
                }
                if (source == null) {
                    for (bookSource in hasBookUrlPattern) {
                        try {
                            val bs = bookSource.getBookSource()!!
                            if (bookUrl.matches(bs.bookUrlPattern!!.toRegex())) {
                                source = bs
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                val bookSource = source ?: continue
                val book = Book(
                    bookUrl = bookUrl,
                    origin = bookSource.bookSourceUrl,
                    originName = bookSource.bookSourceName
                )
                kotlin.runCatching {
                    WebBook.getBookInfoAwait(bookSource, book)
                }.onSuccess {
                    val dbBook = appDb.bookDao.getBook(it.name, it.author)
                    if (dbBook != null) {
                        val toc = WebBook.getChapterListAwait(bookSource, it).getOrThrow()
                        dbBook.migrateTo(it, toc)
                        appDb.bookDao.insert(it)
                        appDb.bookChapterDao.insert(*toc.toTypedArray())
                    } else {
                        it.order = appDb.bookDao.minOrder - 1
                        it.save()
                    }
                    successCount++
                    addBookProgressLiveData.postValue(successCount)
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                context.toastOnUi(R.string.success)
            } else {
                context.toastOnUi("添加网址失败")
            }
        }.onError {
            AppLog.put("添加网址出错\n${it.localizedMessage}", it, true)
        }.onFinally {
            addBookProgressLiveData.postValue(-1)
        }
    }

    fun exportBookshelf(books: List<Book>?, success: (file: File) -> Unit) {
        execute {
            books?.let {
                val path = "${context.filesDir}/books.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                FileOutputStream(file).use { out ->
                    val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
                    writer.setIndent("  ")
                    writer.beginArray()
                    books.forEach {
                        val bookMap = hashMapOf<String, String?>()
                        bookMap["name"] = it.name
                        bookMap["author"] = it.author
                        bookMap["intro"] = it.getDisplayIntro()
                        GSON.toJson(bookMap, bookMap::class.java, writer)
                    }
                    writer.endArray()
                    writer.close()
                }
                file
            } ?: throw NoStackTraceException("书籍不能为空")
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi("导出书籍出错\n${it.localizedMessage}")
        }
    }

    fun importBookshelf(str: String, groupId: Long) {
        execute {
            val text = str.trim()
            when {
                text.isAbsUrl() -> {
                    okHttpClient.newCallResponseBody {
                        url(text)
                    }.decompressed().text().let {
                        importBookshelf(it, groupId)
                    }
                }

                text.isJsonArray() -> {
                    importBookshelfByJson(text, groupId)
                }

                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    private fun importBookshelfByJson(json: String, groupId: Long) {
        execute {
            val bookSourceParts = appDb.bookSourceDao.allEnabledPart
            val semaphore = Semaphore(AppConfig.threadCount)
            GSON.fromJsonArray<Map<String, String?>>(json).getOrThrow().forEach { bookInfo ->
                val name = bookInfo["name"] ?: ""
                val author = bookInfo["author"] ?: ""
                if (name.isEmpty() || appDb.bookDao.has(name, author)) {
                    return@forEach
                }
                semaphore.withPermit {
                    WebBook.preciseSearch(
                        this, bookSourceParts, name, author,
                        semaphore = semaphore
                    ).onSuccess {
                        val book = it.first
                        if (groupId > 0) {
                            book.group = groupId
                        }
                        book.save()
                    }.onError { e ->
                        context.toastOnUi(e.localizedMessage)
                    }
                }
            }
        }.onError {
            it.printOnDebug()
        }.onFinally {
            context.toastOnUi(R.string.success)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun <T> androidx.lifecycle.LiveData<T>.asFlow(): kotlinx.coroutines.flow.Flow<T> {
    return kotlinx.coroutines.flow.callbackFlow {
        val observer = androidx.lifecycle.Observer<T> { value ->
            value?.let { trySend(it) }
        }
        this@asFlow.observeForever(observer)
        awaitClose { this@asFlow.removeObserver(observer) }
    }
}
