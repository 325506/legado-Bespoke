package io.legado.app.ui.replace.edit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.replacerule.ReplaceRuleEditScreen
import io.legado.app.utils.GSON
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

class ReplaceEditComposeActivity : ComposeActivity() {

    companion object {
        fun startIntent(
            context: Context,
            id: Long = -1,
            pattern: String? = null,
            isRegex: Boolean = false,
            scope: String? = null
        ): Intent {
            val intent = Intent(context, ReplaceEditComposeActivity::class.java)
            intent.putExtra("id", id)
            intent.putExtra("pattern", pattern)
            intent.putExtra("isRegex", isRegex)
            intent.putExtra("scope", scope)
            return intent
        }
    }

    override val themeMode: Theme = Theme.Auto
    
    private val viewModel by viewModels<ReplaceEditComposeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            ReplaceRuleEditScreen(
                ruleId = intent.getLongExtra("id", -1),
                initialPattern = intent.getStringExtra("pattern"),
                initialIsRegex = intent.getBooleanExtra("isRegex", false),
                initialScope = intent.getStringExtra("scope"),
                onBack = { finish() },
                onSaveSuccess = {
                    setResult(RESULT_OK)
                    finish()
                },
                onCopyRule = { ruleJson ->
                    sendToClip(ruleJson)
                    toastOnUi("已拷贝到剪贴板")
                },
                onPasteRule = { clipText ->
                    try {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.primaryClip?.let { cp ->
                            if (cp.itemCount > 0) {
                                val text = cp.getItemAt(0).text.toString()
                                if (text.isNotBlank()) {
                                    viewModel.pasteRule(text) { rule ->
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
