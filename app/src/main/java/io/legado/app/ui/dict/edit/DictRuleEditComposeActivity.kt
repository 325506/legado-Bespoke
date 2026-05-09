package io.legado.app.ui.dict.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.dictrule.DictRuleEditScreen
import io.legado.app.utils.GSON
import io.legado.app.utils.getClipText
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

class DictRuleEditComposeActivity : ComposeActivity() {
    companion object {
        fun startIntent(
            context: Context,
            name: String = ""
        ): Intent {
            val intent = Intent(context, DictRuleEditComposeActivity::class.java)
            intent.putExtra("name", name)
            return intent
        }
    }

    override val themeMode: Theme = Theme.Auto
    
    private val viewModel by viewModels<DictRuleEditComposeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            DictRuleEditScreen(
                ruleName = intent.getStringExtra("name"),
                onBack = { finish() },
                onSaveSuccess = {
                    setResult(RESULT_OK)
                    finish()
                },
                onCopyRule = { ruleJson ->
                    sendToClip(ruleJson)
                    toastOnUi("已拷贝到剪贴板")
                },
                onPasteRule = { _ ->
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.primaryClip?.let { cp ->
                            if (cp.itemCount > 0) {
                                val text = cp.getItemAt(0).text.toString()
                                if (text.isNotBlank()) {
                                    viewModel.pasteRule { rule ->
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        toastOnUi("粘贴失败")
                    }
                }
            )
        }
    }
}
