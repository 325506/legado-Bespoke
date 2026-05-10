package io.legado.app.ui.compose.booksource

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.ui.compose.booksource.edit.BookSourceEditComposeActivity
import io.legado.app.ui.compose.booksource.debug.SourceDebugComposeActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.booksource.BookSourceScreen
import io.legado.app.ui.compose.screens.booksource.BookSourceViewModel
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.utils.startActivity
import java.io.File

class BookSourceComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<BookSourceViewModel>()
    private val qrResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            importBookSource(it)
        }
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            importBookSource(uri.toString())
        }
    }
    private val exportDir = registerForActivityResult(HandleFileContract()) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            BookSourceScreen(
                viewModel = viewModel,
                onNavigateBack = { finish() },
                onEditSource = { url ->
                    startActivity<BookSourceEditComposeActivity> {
                        if (url.isNotEmpty()) putExtra("sourceUrl", url)
                    }
                },
                onDebugSource = { url ->
                    startActivity<SourceDebugComposeActivity> {
                        putExtra("key", url)
                    }
                },
                onSearchBook = { source ->
                    SearchActivity.start(this@BookSourceComposeActivity, source)
                },
                onExportSelection = { sources ->
                    exportSources(sources)
                },
                onShareSelection = { sources ->
                    shareSources(sources)
                },
                onImportLocal = {
                    importDoc.launch {
                        mode = HandleFileContract.FILE
                        allowExtensions = arrayOf("txt", "json")
                    }
                },
                onImportOnline = {
                    showImportOnlineDialog()
                },
                onQrCodeImport = {
                    qrResult.launch(null)
                },
                onGroupManage = {
                },
                onCheckSource = { sources ->
                    checkSources(sources)
                }
            )
        }
    }

    private fun importBookSource(text: String) {
    }

    private fun exportSources(sources: List<BookSourcePart>) {
        viewModel.saveToFile(sources) { file, name ->
            exportDir.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    name,
                    file,
                    "application/json"
                )
            }
        }
    }

    private fun shareSources(sources: List<BookSourcePart>) {
        viewModel.saveToFile(sources) { file, _ ->
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.FileProvider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }
    }

    private fun showImportOnlineDialog() {
    }

    private fun checkSources(sources: List<BookSourcePart>) {
    }
}
