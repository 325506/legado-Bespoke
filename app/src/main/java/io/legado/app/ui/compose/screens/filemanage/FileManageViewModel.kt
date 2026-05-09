package io.legado.app.ui.compose.screens.filemanage

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class FileManageViewModel(application: Application) : BaseViewModel(application) {

    val rootDoc = context.getExternalFilesDir(null)?.parentFile
    private val _subDocs = mutableListOf<File>()
    val subDocs: List<File> get() = _subDocs

    private val _files = MutableStateFlow<List<File>>(emptyList())
    val files: StateFlow<List<File>> = _files.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val lastDir: File? get() = _subDocs.lastOrNull() ?: rootDoc

    init {
        upFiles(rootDoc)
    }

    fun upFiles(parentFile: File?) {
        execute {
            parentFile ?: return@execute emptyList()
            if (parentFile == rootDoc) {
                parentFile.listFiles()?.sortedWith(
                    compareBy({ it.isFile }, { it.name })
                )
            } else {
                val list = arrayListOf(parentFile)
                parentFile.listFiles()?.sortedWith(
                    compareBy({ it.isFile }, { it.name })
                )?.let {
                    list.addAll(it)
                }
                list
            }
        }.onStart {
            _files.value = emptyList()
        }.onSuccess {
            _files.value = it ?: emptyList()
            _searchQuery.value = ""
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    fun delFile(file: File) {
        execute {
            file.delete()
        }.onSuccess {
            upFiles(lastDir)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

    fun navigateToDir(dir: File) {
        _subDocs.add(dir)
        upFiles(dir)
    }

    fun navigateToPath(index: Int) {
        _subDocs.clear()
        _subDocs.addAll(subDocs.subList(0, index))
        val targetDir = _subDocs.lastOrNull() ?: rootDoc
        upFiles(targetDir)
    }

    fun navigateToRoot() {
        _subDocs.clear()
        upFiles(rootDoc)
    }

    fun goBackDir(): Boolean {
        return if (_subDocs.isNotEmpty()) {
            _subDocs.removeAt(_subDocs.lastIndex)
            upFiles(lastDir)
            true
        } else {
            false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredFiles(): List<File> {
        val query = _searchQuery.value
        return if (query.isNotEmpty()) {
            _files.value.filter {
                it == lastDir || it.name.contains(query, ignoreCase = true)
            }
        } else {
            _files.value
        }
    }
}
