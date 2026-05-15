package io.legado.app.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isLocalTxt
import io.legado.app.help.config.AppConfig
import io.legado.app.model.ReadBook
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.bookmark.BookmarkDialog
import io.legado.app.ui.book.toc.rule.TxtTocRuleDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.book.toc.TocScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.showDialogFragment

class TocComposeActivity : ComposeActivity(),
    TxtTocRuleDialog.CallBack {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<TocViewModel>()
    private val waitDialog by lazy { WaitDialog(this) }
    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                1 -> viewModel.saveBookmark(uri)
                2 -> viewModel.saveBookmarkMd(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent.getStringExtra("bookUrl")?.let {
            viewModel.initBook(it)
        }

        setContent {
            LegadoTheme(theme = themeMode) {
                TocScreen(
                    viewModel = viewModel,
                    onBack = { finish() },
                    onChapterSelected = { index, chapterPos, chapterChanged, durVolumeIndex, chapterInVolumeIndex ->
                        setResult(Activity.RESULT_OK, Intent().apply {
                            putExtra("index", index)
                            putExtra("chapterPos", chapterPos)
                            putExtra("chapterChanged", chapterChanged)
                            putExtra("durVolumeIndex", durVolumeIndex)
                            putExtra("chapterInVolumeIndex", chapterInVolumeIndex)
                        })
                        finish()
                    },
                    onShowTocRegexDialog = { tocUrl ->
                        showDialogFragment(TxtTocRuleDialog(tocUrl))
                    },
                    onShowAppLog = {
                        showDialogFragment<AppLogDialog>()
                    },
                    onShowBookmarkDialog = { bookmark, pos ->
                        showDialogFragment(BookmarkDialog(bookmark, pos))
                    },
                    onExportBookmark = {
                        exportDir.launch {
                            requestCode = 1
                        }
                    },
                    onExportBookmarkMd = {
                        exportDir.launch {
                            requestCode = 2
                        }
                    },
                    onReverseToc = {
                        viewModel.reverseToc {
                            setResult(Activity.RESULT_OK, Intent().apply {
                                putExtra("index", it.durChapterIndex)
                                putExtra("chapterPos", 0)
                            })
                        }
                    },
                    onSplitLongChapterChanged = { checked ->
                        viewModel.bookData.value?.let { book ->
                            book.setSplitLongChapter(checked)
                            upBookAndToc(book)
                        }
                    },
                    onUseReplaceChanged = { checked ->
                        AppConfig.tocUiUseReplace = checked
                        viewModel.chapterListCallBack?.clearDisplayTitle()
                        viewModel.chapterListCallBack?.upChapterList(null)
                    },
                    onLoadWordCountChanged = { checked ->
                        AppConfig.tocCountWords = checked
                        viewModel.upChapterListAdapter()
                    },
                    isSplitLongChapter = viewModel.bookData.value?.getSplitLongChapter() == true,
                    isUseReplace = AppConfig.tocUiUseReplace,
                    isLoadWordCount = AppConfig.tocCountWords,
                    isLocalTxt = viewModel.bookData.value?.isLocalTxt == true
                )
            }
        }
    }

    override fun onTocRegexDialogResult(tocRegex: String) {
        viewModel.bookData.value?.let { book ->
            book.tocUrl = tocRegex
            upBookAndToc(book)
        }
    }

    private fun upBookAndToc(book: Book) {
        waitDialog.show()
        viewModel.upBookTocRule(book) {
            waitDialog.dismiss()
            if (ReadBook.book == book) {
                if (it == null) {
                    ReadBook.upMsg(null)
                } else {
                    ReadBook.upMsg("LoadTocError:${it.localizedMessage}")
                }
            }
        }
    }
}
