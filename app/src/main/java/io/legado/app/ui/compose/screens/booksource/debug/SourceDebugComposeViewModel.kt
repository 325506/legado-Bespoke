package io.legado.app.ui.compose.screens.booksource.debug

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.exploreKinds
import io.legado.app.model.Debug
import io.legado.app.utils.toastOnUi

class SourceDebugComposeViewModel(application: Application) : BaseViewModel(application),
    Debug.Callback {

    var bookSource: BookSource? = null
    var searchSrc: String? = null
    var bookSrc: String? = null
    var tocSrc: String? = null
    var contentSrc: String? = null

    private var debugCallback: ((Int, String) -> Unit)? = null
    var exploreKindsList: List<Pair<String?, String?>> = emptyList()
    var selectedExploreIndex: Int = 0

    fun init(key: String?, onFinally: () -> Unit) {
        execute {
            key?.let {
                bookSource = appDb.bookSourceDao.getBookSource(it)
                exploreKindsList = bookSource?.exploreKinds()?.filter { kind ->
                    !kind.url.isNullOrBlank()
                }?.map { kind -> kind.title to kind.url } ?: emptyList()
            }
        }.onFinally {
            onFinally()
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        debugCallback = callback
    }

    fun startDebug(key: String, start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        execute<Unit> {
            Debug.callback = this@SourceDebugComposeViewModel
            Debug.startDebug(this, bookSource!!, key)
        }.onStart {
            start?.invoke()
        }.onError {
            error?.invoke()
            context.toastOnUi(it.localizedMessage)
        }
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> searchSrc = msg
            20 -> bookSrc = msg
            30 -> tocSrc = msg
            40 -> contentSrc = msg
            else -> debugCallback?.invoke(state, msg)
        }
    }

    fun selectExploreKind(index: Int) {
        selectedExploreIndex = index
    }

    fun getSelectedExploreKind(): Pair<String?, String?>? {
        return exploreKindsList.getOrNull(selectedExploreIndex)
    }

    suspend fun clearExploreCache() {
        execute {
            bookSource?.clearExploreKindsCache()
            exploreKindsList = bookSource?.exploreKinds()?.filter { kind ->
                !kind.url.isNullOrBlank()
            }?.map { kind -> kind.title to kind.url } ?: emptyList()
            selectedExploreIndex = 0
        }
    }
}
