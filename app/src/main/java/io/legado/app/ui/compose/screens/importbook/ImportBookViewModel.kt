package io.legado.app.ui.compose.screens.importbook

import android.app.Application
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.constant.AppPattern.archiveFileRegex
import io.legado.app.constant.AppPattern.bookFileRegex
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.help.config.AppConfig
import io.legado.app.model.localBook.LocalBook
import io.legado.app.ui.book.import.local.ImportBook
import io.legado.app.utils.AlphanumComparator
import io.legado.app.utils.ArchiveUtils
import io.legado.app.utils.FileDoc
import io.legado.app.utils.delete
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.isUri
import io.legado.app.utils.list
import io.legado.app.utils.mapParallel
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class ImportBookViewModel(application: Application) : BaseViewModel(application) {
    var rootDoc: FileDoc? = null
    val subDocs = arrayListOf<FileDoc>()
    var sort = context.getPrefInt(PreferKey.localBookImportSort)
    var dataCallback: DataCallback? = null
    var dataFlowStart: (() -> Unit)? = null
    var filterKey: String? = null

    val dataFlow = callbackFlow<List<ImportBook>> {
        val list = Collections.synchronizedList(ArrayList<ImportBook>())

        dataCallback = object : DataCallback {
            override fun setItems(fileDocs: List<FileDoc>) {
                list.clear()
                fileDocs.mapTo(list) { ImportBook(it) }
                trySend(list)
            }

            override fun addItems(fileDocs: List<FileDoc>) {
                fileDocs.mapTo(list) { ImportBook(it) }
                trySend(list)
            }

            override fun clear() {
                list.clear()
                trySend(emptyList())
            }

            override fun upAdapter() {
                trySend(list)
            }
        }

        withContext(Main) {
            dataFlowStart?.invoke()
        }

        awaitClose {
            dataCallback = null
        }
    }.map { docList ->
        val filterKey = filterKey
        val skipFilter = filterKey.isNullOrBlank()
        val comparator = when (sort) {
            2 -> compareBy<ImportBook>({ !it.isDir }, { -it.lastModified })
            1 -> compareBy({ !it.isDir }, { -it.size })
            else -> compareBy { !it.isDir }
        } then compareBy(AlphanumComparator) { it.name }
        docList.asSequence().filter {
            skipFilter || it.name.contains(filterKey)
        }.sortedWith(comparator).toList()
    }.flowOn(IO)

    fun initRootDoc(changedFolder: Boolean = false) {
        if (rootDoc != null && !changedFolder) {
            dataCallback?.upAdapter()
        } else {
            val lastPath = AppConfig.importBookPath
            if (lastPath.isNullOrBlank()) {
                // Need to select folder
            } else {
                val rootUri = if (lastPath.isUri()) {
                    lastPath.toUri()
                } else {
                    Uri.fromFile(File(lastPath))
                }
                if (rootUri.isContentScheme()) {
                    initRootPathUri(rootUri)
                } else {
                    initRootPath(lastPath)
                }
            }
        }
    }

    private fun initRootPathUri(rootUri: Uri) {
        kotlin.runCatching {
            val doc = DocumentFile.fromTreeUri(context, rootUri)
            if (doc == null || doc.name.isNullOrEmpty() || !doc.isDirectory) {
                // Need to select folder
            } else {
                subDocs.clear()
                rootDoc = FileDoc.fromDocumentFile(doc)
                dataCallback?.upAdapter()
            }
        }.onFailure {
            // Need to select folder
        }
    }

    private fun initRootPath(path: String) {
        kotlin.runCatching {
            val file = File(path)
            if (!file.isDirectory) {
                // Need to select folder
            } else {
                subDocs.clear()
                rootDoc = FileDoc.fromFile(file)
                dataCallback?.upAdapter()
            }
        }.onFailure {
            // Need to select folder
        }
    }

    fun selectFolder(uri: Uri) {
        uri.let {
            AppConfig.importBookPath = uri.toString()
            subDocs.clear()
            if (uri.isContentScheme()) {
                initRootPathUri(uri)
            } else {
                initRootPath(uri.path ?: "")
            }
        }
    }

    fun nextDoc(fileDoc: FileDoc) {
        subDocs.add(fileDoc)
        rootDoc?.let { loadDoc(it) }
    }

    fun goBackDir(): Boolean {
        return if (subDocs.isNotEmpty()) {
            subDocs.removeAt(subDocs.lastIndex)
            rootDoc?.let { loadDoc(it) }
            true
        } else {
            false
        }
    }

    fun loadDoc(fileDoc: FileDoc) {
        execute {
            val docList = fileDoc.list { item ->
                when {
                    item.name.startsWith(".") -> false
                    item.isDir -> true
                    else -> item.name.matches(bookFileRegex) || item.name.matches(archiveFileRegex)
                }
            }
            dataCallback?.setItems(docList!!)
        }.onError {
            context.toastOnUi("获取文件列表出错\n${it.localizedMessage}")
        }
    }

    fun scanFolder() {
        rootDoc?.let { doc ->
            val lastDoc = subDocs.lastOrNull() ?: doc
            execute {
                scanDoc(lastDoc)
            }
        }
    }

    suspend fun scanDoc(fileDoc: FileDoc) {
        dataCallback?.clear()
        val channel = Channel<FileDoc>(Channel.UNLIMITED)
        var n = 1
        channel.trySend(fileDoc)
        val list = arrayListOf<FileDoc>()
        channel.consumeAsFlow()
            .mapParallel(16) { fileDoc ->
                fileDoc.list()!!
            }.onEach { fileDocs ->
                n--
                list.clear()
                fileDocs.forEach {
                    if (it.isDir) {
                        n++
                        channel.trySend(it)
                    } else if (it.name.matches(bookFileRegex)
                        || it.name.matches(archiveFileRegex)
                    ) {
                        list.add(it)
                    }
                }
                dataCallback?.addItems(list)
            }.takeWhile {
                n > 0
            }.catch {
                context.toastOnUi("扫描文件夹出错\n${it.localizedMessage}")
            }.collect()
    }

    fun updateFilter(filterKey: String?) {
        this.filterKey = filterKey
        dataCallback?.upAdapter()
    }

    fun upSort(sort: Int) {
        this.sort = sort
        context.putPrefInt(PreferKey.localBookImportSort, sort)
        dataCallback?.upAdapter()
    }

    fun addToBookshelf(bookList: HashSet<ImportBook>, finally: () -> Unit) {
        execute {
            val fileUris = bookList.map { it.file.uri }
            LocalBook.importFiles(fileUris)
        }.onError {
            context.toastOnUi("添加书架失败，请尝试重新选择文件夹")
            AppLog.put("添加书架失败\n${it.localizedMessage}", it)
        }.onSuccess {
            context.toastOnUi("添加书架成功")
        }.onFinally {
            finally.invoke()
        }
    }

    fun deleteDoc(bookList: HashSet<ImportBook>, finally: () -> Unit) {
        execute {
            bookList.forEach { it.file.delete() }
        }.onFinally {
            finally.invoke()
        }
    }

    fun startRead(fileDoc: FileDoc, onRead: (io.legado.app.data.entities.Book) -> Unit) {
        if (!ArchiveUtils.isArchive(fileDoc.name)) {
            appDb.bookDao.getBookByFileName(fileDoc.name)?.let {
                val filePath = fileDoc.toString()
                if (it.bookUrl != filePath) {
                    it.bookUrl = filePath
                    appDb.bookDao.insert(it)
                }
                onRead(it)
            }
        }
    }

    fun getArchiveFiles(fileDoc: FileDoc, callback: (List<String>) -> Unit) {
        viewModelScope.launch(IO) {
            kotlin.runCatching {
                val files = ArchiveUtils.getArchiveFilesName(fileDoc) {
                    it.matches(bookFileRegex)
                }
                callback(files)
            }.onFailure {
                context.toastOnUi("读取压缩包失败")
                callback(emptyList())
            }
        }
    }

    fun importArchiveFile(fileDoc: FileDoc, fileName: String, onSuccess: (io.legado.app.data.entities.Book) -> Unit) {
        viewModelScope.launch(IO) {
            kotlin.runCatching {
                LocalBook.importArchiveFile(fileDoc.uri, fileName) {
                    it.contains(fileName)
                }.firstOrNull()
            }.onSuccess { book ->
                book?.let { onSuccess(it) }
            }.onFailure {
                context.toastOnUi("导入失败")
            }
        }
    }

    interface DataCallback {
        fun setItems(fileDocs: List<FileDoc>)
        fun addItems(fileDocs: List<FileDoc>)
        fun clear()
        fun upAdapter()
    }
}
