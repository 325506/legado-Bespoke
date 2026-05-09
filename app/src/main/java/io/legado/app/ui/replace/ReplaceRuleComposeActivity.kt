package io.legado.app.ui.replace

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportReplaceRuleDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.replacerule.ReplaceRuleScreen
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.replace.edit.ReplaceEditComposeActivity
import io.legado.app.utils.ACache
import io.legado.app.utils.GSON
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.splitNotBlank

class ReplaceRuleComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto
    
    private val viewModel by viewModels<ReplaceRuleViewModel>()
    private val importRecordKey = "replaceRuleRecordKey"
    
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportReplaceRuleDialog(it))
    }
    
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportReplaceRuleDialog(uri.toString()))
        }
    }
    
    private val exportResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage("导出成功")
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    sendToClip(uri.toString())
                }
            }
        }
    }

    private val editActivity = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            ReplaceRuleScreen(
                onBack = { finish() },
                onEditRule = { ruleId ->
                    editActivity.launch(ReplaceEditComposeActivity.startIntent(this@ReplaceRuleComposeActivity, ruleId ?: -1))
                },
                onImportLocal = {
                    importDoc.launch {
                        mode = HandleFileContract.FILE
                        allowExtensions = arrayOf("txt", "json")
                    }
                },
                onImportOnline = {
                    showImportDialog()
                },
                onImportQr = {
                    qrCodeResult.launch(null)
                },
                onExportSelection = { selection ->
                    exportResult.launch {
                        mode = HandleFileContract.EXPORT
                        fileData = HandleFileContract.FileData(
                            "exportReplaceRule.json",
                            GSON.toJson(selection).toByteArray(),
                            "application/json"
                        )
                    }
                }
            )
        }
    }

    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
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
                        ImportReplaceRuleDialog(it)
                    )
                }
            }
            cancelButton()
        }
    }
}
