package io.legado.app.ui.rss.source.edit

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.RssSourceEditScreen
import io.legado.app.ui.compose.screens.rss.RssSourceEditViewModel
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.rss.source.debug.RssSourceDebugComposeActivity
import io.legado.app.utils.GSON
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.sendToClip
import io.legado.app.utils.share
import io.legado.app.utils.shareWithQr
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.toastOnUi
import io.legado.app.lib.dialogs.AlertBuilder
import io.legado.app.lib.dialogs.alert

class RssSourceEditComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<RssSourceEditViewModel>()

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        viewModel.importSource(it) { source ->
            toastOnUi("导入成功")
        }
    }

    private val selectDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.isContentScheme()) {
                viewModel.importSource(uri.toString()) { source ->
                    toastOnUi("导入成功")
                }
            } else {
                uri.path?.let { path ->
                    viewModel.importSource(path) { source ->
                        toastOnUi("导入成功")
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceUrl = intent.getStringExtra("sourceUrl")

        setContent {
            LegadoTheme(theme = themeMode) {
                RssSourceEditScreen(
                    viewModel = viewModel,
                    initialSourceUrl = sourceUrl,
                    onNavigateBack = {
                        val source = viewModel.rssSource.value
                        if (source != null) {
                            finish()
                        } else {
                            finish()
                        }
                    },
                    onSave = { source ->
                        viewModel.save(source) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    },
                    onDebug = { source ->
                        viewModel.save(source) {
                            startActivity(Intent(this, RssSourceDebugComposeActivity::class.java).apply {
                                putExtra("key", source.sourceUrl)
                            })
                        }
                    },
                    onLogin = { source ->
                        viewModel.save(source) {
                            startActivity(Intent(this, SourceLoginActivity::class.java).apply {
                                putExtra("type", "rssSource")
                                putExtra("key", source.sourceUrl)
                            })
                        }
                    },
                    onCopySource = { json ->
                        sendToClip(json)
                    },
                    onShareSource = { json ->
                        share(json)
                    },
                    onShareQr = { json, title ->
                        shareWithQr(json, title, ErrorCorrectionLevel.L)
                    },
                    onPasteSource = { source ->
                        viewModel.pasteSource { }
                    },
                    onImportQr = {
                        qrCodeResult.launch(null)
                    },
                    onImportFile = {
                        selectDoc.launch {
                            mode = HandleFileContract.FILE
                            allowExtensions = arrayOf("txt", "json")
                        }
                    },
                    onShowHelp = {
                        showHelp("rssRuleHelp")
                    },
                    onShowLog = {
                        showDialogFragment<AppLogDialog>()
                    },
                    onSetVariable = { source ->
                        viewModel.save(source) {
                            showSetVariableDialog(it)
                        }
                    },
                    onClearCookie = { url ->
                        viewModel.clearCookie(url)
                    }
                )
            }
        }
    }

    private fun showSetVariableDialog(source: RssSource) {
        (this as android.content.Context).alert(titleResource = R.string.set_source_variable) {
            val alertBinding = io.legado.app.databinding.DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "输入变量值"
                editView.setText(source.getVariable())
            }
            customView { alertBinding.root }
            okButton {
                val variable = alertBinding.editView.text?.toString()
                io.legado.app.help.CacheManager.put("sourceVariable_${source.sourceUrl}", variable ?: "")
            }
            cancelButton()
        }
    }
}
