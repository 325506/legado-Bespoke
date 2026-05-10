package io.legado.app.ui.rss.source.manage

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.ui.association.ImportRssSourceDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.RssSourceScreen
import io.legado.app.ui.compose.screens.rss.RssSourceViewModel
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.rss.source.edit.RssSourceEditComposeActivity
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.startActivity
import io.legado.app.lib.dialogs.alert

class RssSourceComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<RssSourceViewModel>()

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportRssSourceDialog(it))
    }

    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportRssSourceDialog(uri.toString()))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegadoTheme(theme = themeMode) {
                RssSourceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onEditSource = { sourceUrl ->
                        startActivity<RssSourceEditComposeActivity> {
                            if (sourceUrl.isNotEmpty()) {
                                putExtra("sourceUrl", sourceUrl)
                            }
                        }
                    },
                    onDelete = { source ->
                        viewModel.delete(source)
                    },
                    onUpdate = { sources ->
                        viewModel.update(*sources)
                    },
                    onImportLocal = {
                        importDoc.launch {
                            this.mode = HandleFileContract.FILE
                            this.allowExtensions = arrayOf("txt", "json")
                        }
                    },
                    onImportOnline = {
                        showImportDialog()
                    },
                    onQrCodeImport = {
                        qrCodeResult.launch(null)
                    },
                    onGroupManage = {
                        showDialogFragment<GroupManageDialog>()
                    },
                    onImportDefault = {
                        viewModel.importDefault()
                    },
                    onShowHelp = {
                        showHelp("SourceMRssHelp")
                    }
                )
            }
        }
    }

    private fun showImportDialog() {
        val aCache = io.legado.app.utils.ACache.get(cacheDir = false)
        val importRecordKey = "rssSourceRecordKey"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toMutableList() ?: mutableListOf()
        
        this.alert(titleResource = R.string.import_on_line) {
            val alertBinding = io.legado.app.databinding.DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importRecordKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (it.isAbsUrl() && !cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(
                        ImportRssSourceDialog(it)
                    )
                }
            }
            cancelButton()
        }
    }
}
