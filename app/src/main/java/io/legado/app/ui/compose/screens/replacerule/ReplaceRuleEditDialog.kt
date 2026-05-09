package io.legado.app.ui.compose.screens.replacerule

import android.content.ClipData
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.toastOnUi

@Composable
fun ReplaceRuleEditDialog(
    ruleId: Long? = null,
    initialRule: ReplaceRule? = null,
    onDismiss: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: ReplaceRuleEditViewModel = viewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf("") }
    var excludeScope by remember { mutableStateOf("") }
    var timeout by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(true) }
    var scopeTitle by remember { mutableStateOf(false) }
    var scopeContent by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(ruleId, initialRule) {
        if (initialRule != null) {
            name = initialRule.name
            group = initialRule.group ?: ""
            pattern = initialRule.pattern
            replacement = initialRule.replacement
            scope = initialRule.scope ?: ""
            excludeScope = initialRule.excludeScope ?: ""
            timeout = initialRule.timeoutMillisecond.toString()
            isRegex = initialRule.isRegex
            scopeTitle = initialRule.scopeTitle
            scopeContent = initialRule.scopeContent
            isLoading = false
        } else if (ruleId != null && ruleId > 0) {
            viewModel.loadRule(ruleId) { rule ->
                name = rule.name
                group = rule.group ?: ""
                pattern = rule.pattern
                replacement = rule.replacement
                scope = rule.scope ?: ""
                excludeScope = rule.excludeScope ?: ""
                timeout = rule.timeoutMillisecond.toString()
                isRegex = rule.isRegex
                scopeTitle = rule.scopeTitle
                scopeContent = rule.scopeContent
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (ruleId != null && ruleId > 0) "编辑替换规则" else "添加替换规则",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("规则名称") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("分组") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("替换规则") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isRegex,
                        onCheckedChange = { isRegex = it }
                    )
                    Text("使用正则")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = replacement,
                    onValueChange = { replacement = it },
                    label = { Text("替换为") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = scopeTitle,
                        onCheckedChange = { scopeTitle = it }
                    )
                    Text("作用于标题")

                    Spacer(modifier = Modifier.width(16.dp))

                    Checkbox(
                        checked = scopeContent,
                        onCheckedChange = { scopeContent = it }
                    )
                    Text("作用于正文")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = scope,
                    onValueChange = { scope = it },
                    label = { Text("作用范围") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = excludeScope,
                    onValueChange = { excludeScope = it },
                    label = { Text("排除范围") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = timeout,
                    onValueChange = { timeout = it },
                    label = { Text("超时时间(毫秒)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        val jsonRule = GSON.toJson(ReplaceRule().apply {
                            this.name = name
                            this.group = group
                            this.pattern = pattern
                            this.replacement = replacement
                            this.scope = scope
                            this.excludeScope = excludeScope
                            this.timeoutMillisecond = timeout.toLongOrNull() ?: 3000
                            this.isRegex = isRegex
                            this.scopeTitle = scopeTitle
                            this.scopeContent = scopeContent
                        })
                        val clip = ClipData.newPlainText("replace_rule", jsonRule)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(clip)
                        context.toastOnUi("已拷贝到剪贴板")
                    }) {
                        Text("拷贝")
                    }

                    TextButton(onClick = {
                        try {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = clipboard.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0).text.toString()
                                GSON.fromJsonObject<ReplaceRule>(text).getOrNull()?.let { pastedRule ->
                                    name = pastedRule.name
                                    group = pastedRule.group ?: ""
                                    pattern = pastedRule.pattern
                                    replacement = pastedRule.replacement
                                    scope = pastedRule.scope ?: ""
                                    excludeScope = pastedRule.excludeScope ?: ""
                                    timeout = pastedRule.timeoutMillisecond.toString()
                                    isRegex = pastedRule.isRegex
                                    scopeTitle = pastedRule.scopeTitle
                                    scopeContent = pastedRule.scopeContent
                                    context.toastOnUi("已粘贴规则")
                                }
                            }
                        } catch (e: Exception) {
                            context.toastOnUi("粘贴失败")
                        }
                    }) {
                        Text("粘贴")
                    }

                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }

                    Button(
                        onClick = {
                            val replaceRule = if (ruleId != null && ruleId > 0) {
                                ReplaceRule(
                                    id = ruleId,
                                    name = name,
                                    group = group,
                                    pattern = pattern,
                                    replacement = replacement,
                                    scope = scope,
                                    excludeScope = excludeScope,
                                    timeoutMillisecond = timeout.toLongOrNull() ?: 3000,
                                    isRegex = isRegex,
                                    scopeTitle = scopeTitle,
                                    scopeContent = scopeContent
                                )
                            } else {
                                ReplaceRule(
                                    name = name,
                                    group = group,
                                    pattern = pattern,
                                    replacement = replacement,
                                    scope = scope,
                                    excludeScope = excludeScope,
                                    timeoutMillisecond = timeout.toLongOrNull() ?: 3000,
                                    isRegex = isRegex,
                                    scopeTitle = scopeTitle,
                                    scopeContent = scopeContent
                                )
                            }

                            try {
                                replaceRule.checkValid()
                                viewModel.save(replaceRule) {
                                    context.toastOnUi("保存成功")
                                    onSaveSuccess()
                                    onDismiss()
                                }
                            } catch (e: Exception) {
                                context.toastOnUi(e.message ?: "规则无效")
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}
