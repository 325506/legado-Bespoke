package io.legado.app.ui.compose.screens.txttocrule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getClipText
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

@Composable
fun TxtTocRuleEditDialog(
    ruleId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (TxtTocRule) -> Unit,
    viewModel: TxtTocRuleEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val tocRule by viewModel.tocRule.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var example by remember { mutableStateOf("") }
    var showExitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        viewModel.initData(ruleId)
    }

    LaunchedEffect(tocRule) {
        tocRule?.let {
            name = it.name
            rule = it.rule
            replacement = it.replacement ?: ""
            example = it.example ?: ""
        }
    }

    val isSame = tocRule?.let {
        it.name == name && it.rule == rule && 
        it.replacement == replacement && it.example == example
    } ?: name.isEmpty() && rule.isEmpty() && replacement.isEmpty() && example.isEmpty()

    AlertDialog(
        onDismissRequest = {
            if (!isSame) {
                showExitConfirm = true
            } else {
                onDismiss()
            }
        },
        title = { Text(context.getString(R.string.txt_toc_rule)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(context.getString(R.string.name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rule,
                    onValueChange = { rule = it },
                    label = { Text(context.getString(R.string.regex)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text(context.getString(R.string.replace_to_js)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = example,
                    onValueChange = { example = it },
                    label = { Text(context.getString(R.string.example)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isEmpty()) {
                    context.toastOnUi("名称不能为空")
                    return@TextButton
                }
                
                try {
                    Pattern.compile(rule, Pattern.MULTILINE)
                } catch (ex: Exception) {
                    context.toastOnUi("正则语法错误或不支持")
                    return@TextButton
                }
                
                val newRule = tocRule ?: TxtTocRule()
                newRule.name = name
                newRule.rule = rule
                newRule.replacement = replacement
                newRule.example = example
                
                onSave(newRule)
                onDismiss()
            }) {
                Text("保存")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = {
                    val text = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                    if (text.isNullOrBlank()) {
                        context.toastOnUi("剪贴板为空")
                        return@TextButton
                    }
                    try {
                        val pastedRule = GSON.fromJsonObject<TxtTocRule>(text).getOrNull()
                        if (pastedRule != null) {
                            name = pastedRule.name
                            rule = pastedRule.rule
                            replacement = pastedRule.replacement ?: ""
                            example = pastedRule.example ?: ""
                        } else {
                            context.toastOnUi("格式不对")
                        }
                    } catch (e: Exception) {
                        context.toastOnUi("格式不对")
                    }
                }) {
                    Text("粘贴")
                }
                TextButton(onClick = {
                    val jsonRule = GSON.toJson(tocRule ?: TxtTocRule().apply {
                        this.name = name
                        this.rule = rule
                        this.replacement = replacement
                        this.example = example
                    })
                    val clip = ClipData.newPlainText("txt_toc_rule", jsonRule)
                    clipboardManager.setPrimaryClip(clip)
                    context.toastOnUi("已拷贝到剪贴板")
                }) {
                    Text("拷贝")
                }
                TextButton(onClick = {
                    if (!isSame) {
                        showExitConfirm = true
                    } else {
                        onDismiss()
                    }
                }) {
                    Text("取消")
                }
            }
        }
    )

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("退出") },
            text = { Text("是否保存更改？") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    if (name.isEmpty()) {
                        context.toastOnUi("名称不能为空")
                        return@TextButton
                    }
                    
                    try {
                        Pattern.compile(rule, Pattern.MULTILINE)
                    } catch (ex: Exception) {
                        context.toastOnUi("正则语法错误或不支持")
                        return@TextButton
                    }
                    
                    val newRule = tocRule ?: TxtTocRule()
                    newRule.name = name
                    newRule.rule = rule
                    newRule.replacement = replacement
                    newRule.example = example
                    onSave(newRule)
                    onDismiss()
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onDismiss()
                }) {
                    Text("不保存")
                }
            }
        )
    }
}

class TxtTocRuleEditViewModel(application: android.app.Application) : BaseViewModel(application) {

    private val _tocRule = MutableStateFlow<TxtTocRule?>(null)
    val tocRule: StateFlow<TxtTocRule?> = _tocRule.asStateFlow()

    fun initData(id: Long?) {
        execute {
            if (id != null) {
                _tocRule.value = appDb.txtTocRuleDao.get(id)
            } else {
                _tocRule.value = null
            }
        }
    }
}
