package io.legado.app.ui.book.info

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.constant.BookType
import io.legado.app.constant.EventBus
import io.legado.app.constant.Theme
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSource
import io.legado.app.help.book.isAudio
import io.legado.app.help.book.isImage
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isVideo
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.model.SourceCallBack
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.audio.AudioPlayActivity
import io.legado.app.ui.book.changecover.ChangeCoverDialog
import io.legado.app.ui.book.changesource.ChangeBookSourceDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.book.info.edit.BookInfoEditActivity
import io.legado.app.ui.book.manga.ReadMangaActivity
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.read.ReadBookActivity.Companion.RESULT_DELETED
import io.legado.app.ui.book.search.SearchComposeActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.toc.TocComposeActivityResult
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.book.detail.BookInfoScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.video.VideoPlayerActivity
import io.legado.app.ui.widget.dialog.PhotoDialog
import io.legado.app.ui.widget.dialog.VariableDialog
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.GSON
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookInfoComposeActivity : ComposeActivity(),
    GroupSelectDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeCoverDialog.CallBack,
    VariableDialog.Callback {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<BookInfoViewModel>()
    private var chapterChanged = false
    private val waitDialog by lazy { WaitDialog(this) }

    private val tocActivityResult = registerForActivityResult(TocComposeActivityResult()) {
        it?.let {
            viewModel.getBook()?.let { book ->
                val durChapterIndex = it[0] as Int
                val durChapterPos = it[1] as Int
                val durVolumeIndex = it[3] as Int
                val chapterInVolumeIndex = it[4] as Int
                chapterChanged = it[2] as Boolean
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        book.durChapterIndex = durChapterIndex
                        book.durChapterPos = durChapterPos
                        book.durVolumeIndex = durVolumeIndex
                        book.chapterInVolumeIndex = chapterInVolumeIndex
                        appDb.bookDao.update(book)
                    }
                    startReadActivity(book)
                }
            }
        } ?: let {
            if (!viewModel.inBookshelf) {
                viewModel.delBook()
            }
        }
    }

    private val readBookResult = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.upBook(intent)
        when (it.resultCode) {
            RESULT_OK -> {
                viewModel.inBookshelf = true
            }
            RESULT_DELETED -> {
                setResult(RESULT_OK)
                finish()
            }
        }
    }

    private val infoEditResult = registerForActivityResult(
        StartActivityContract(BookInfoEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.upEditBook()
        }
    }

    private val editSourceResult = registerForActivityResult(
        StartActivityContract(BookSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_CANCELED) return@registerForActivityResult
        viewModel.bookData.value?.let { book ->
            viewModel.bookSource = appDb.bookSourceDao.getBookSource(book.origin)?.also { source ->
                viewModel.hasCustomBtn = source.customButton
            }
            viewModel.refreshBook(book)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(intent)

        setContent {
            LegadoTheme(theme = themeMode) {
                BookInfoScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onReadBook = { book, _, _ -> startReadActivity(book) },
                    onShowChangeSource = { name, author ->
                        showDialogFragment(ChangeBookSourceDialog(name, author))
                    },
                    onShowChangeCover = { name, author ->
                        showDialogFragment(ChangeCoverDialog(name, author))
                    },
                    onShowGroupSelect = { groupId ->
                        showDialogFragment(GroupSelectDialog(groupId))
                    },
                    onShowPhoto = { path, isBook ->
                        showDialogFragment(PhotoDialog(path, isBook = isBook))
                    },
                    onShowVariableDialog = { key, variable, comment ->
                        showDialogFragment(
                            VariableDialog(
                                getString(R.string.set_source_variable),
                                key,
                                variable ?: "",
                                comment ?: ""
                            )
                        )
                    },
                    onShowAppLog = {
                        showDialogFragment<AppLogDialog>()
                    },
                    onShowWaitDialog = { show ->
                        if (show) {
                            waitDialog.setText("Loading.....")
                            waitDialog.show()
                        } else {
                            waitDialog.dismiss()
                        }
                    },
                    onBookDeleted = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    onOpenToc = { bookUrl ->
                        tocActivityResult.launch(bookUrl)
                    },
                    onOpenInfoEdit = { bookUrl ->
                        infoEditResult.launch {
                            putExtra("bookUrl", bookUrl)
                        }
                    },
                    onOpenSourceEdit = { sourceUrl ->
                        editSourceResult.launch {
                            putExtra("sourceUrl", sourceUrl)
                        }
                    },
                    onOpenSourceLogin = { key, bookUrl ->
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "bookSource")
                            putExtra("key", key)
                            putExtra("bookUrl", bookUrl)
                        }
                    },
                    onShareBook = { book ->
                        val bookJson = GSON.toJson(book)
                        val shareStr = "${book.bookUrl}#$bookJson"
                        SourceCallBack.callBackBtn(
                            this,
                            SourceCallBack.CLICK_SHARE_BOOK,
                            viewModel.bookSource,
                            book,
                            null,
                            result = shareStr
                        ) {
                            val intent = Intent(Intent.ACTION_SEND)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            intent.putExtra(Intent.EXTRA_TEXT, shareStr)
                            intent.type = "text/plain"
                            startActivity(Intent.createChooser(intent, book.name))
                        }
                    },
                    onSearchAuthor = { author ->
                        SourceCallBack.callBackBtn(
                            this,
                            SourceCallBack.CLICK_AUTHOR,
                            viewModel.bookSource,
                            viewModel.getBook(false) ?: return@BookInfoScreen,
                            null,
                            result = author
                        ) {
                            SearchComposeActivity.start(this, author)
                        }
                    },
                    onSearchBookName = { name ->
                        SourceCallBack.callBackBtn(
                            this,
                            SourceCallBack.CLICK_BOOK_NAME,
                            viewModel.bookSource,
                            viewModel.getBook(false) ?: return@BookInfoScreen,
                            null,
                            result = name
                        ) {
                            SearchComposeActivity.start(this, name)
                        }
                    },
                    onSearchKind = { kind ->
                        val source = viewModel.bookSource
                        if (source != null) {
                            SourceCallBack.callBackBtn(
                                this,
                                SourceCallBack.CLICK_BOOK_LABEL,
                                source,
                                viewModel.getBook(false) ?: return@BookInfoScreen,
                                null,
                                result = kind
                            ) {
                                SearchComposeActivity.start(this, source, kind)
                            }
                        } else {
                            SearchComposeActivity.start(this, kind)
                        }
                    }
                )
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when {
            book.isAudio -> readBookResult.launch(
                Intent(this, AudioPlayActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )
            book.isVideo -> readBookResult.launch(
                Intent(this, VideoPlayerActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )
            else -> readBookResult.launch(
                Intent(
                    this,
                    if (!book.isLocal && book.isImage && AppConfig.showMangaUi) ReadMangaActivity::class.java
                    else ReadBookActivity::class.java
                )
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
                    .putExtra("chapterChanged", chapterChanged)
            )
        }
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(source: BookSource, book: Book, toc: List<io.legado.app.data.entities.BookChapter>) {
        viewModel.changeTo(source, book, toc)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let { book ->
            book.customCoverUrl = coverUrl
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            }
        }
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        viewModel.getBook()?.let { book ->
            book.group = groupId
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            } else if (groupId > 0) {
                viewModel.addToBookshelf {}
            }
        }
    }

    override fun setVariable(key: String, variable: String?) {
        when (key) {
            viewModel.bookSource?.getKey() -> viewModel.bookSource?.setVariable(variable)
            viewModel.bookData.value?.bookUrl -> viewModel.bookData.value?.let {
                it.putCustomVariable(variable)
                if (viewModel.inBookshelf) {
                    viewModel.saveBook(it)
                }
            }
        }
    }
}
