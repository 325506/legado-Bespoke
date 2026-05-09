package io.legado.app.ui.compose.screens.replacerule

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.replace.edit.ReplaceEditComposeViewModel
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.toastOnUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplaceRuleEditScreen(
    ruleId: Long = -1,
    initialPattern: String? = null,
    initialIsRegex: Boolean = false,
    initialScope: String? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onCopyRule: (String) -> Unit,
    onPasteRule: (String) -> Unit,
    viewModel: ReplaceEditComposeViewModel = viewModel()
) {
    val context = LocalContext.current

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

    LaunchedEffect(ruleId) {
        if (ruleId > 0) {
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
        } else if (initialPattern != null) {
            name = initialPattern
            pattern = initialPattern
            isRegex = initialIsRegex
            scope = initialScope ?: ""
            isLoading = false
        } else {
            isLoading = false
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (ruleId > 0) "编辑替换规则" else "添加替换规则") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val replaceRule = ReplaceRule(
                                id = if (ruleId > 0) ruleId else System.currentTimeMillis(),
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
                            onCopyRule(GSON.toJson(replaceRule))
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "拷贝")
                        }
                        IconButton(onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.primaryClip?.let { cp ->
                                    if (cp.itemCount > 0) {
                                        val text = cp.getItemAt(0).text.toString()
                                        if (text.isNotBlank()) {
                                            onPasteRule(text)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                context.toastOnUi("粘贴失败")
                            }
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "粘贴")
                        }
                        IconButton(onClick = {
                            val replaceRule = if (ruleId > 0) {
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
                                }
                            } catch (e: Exception) {
                                context.toastOnUi(e.message ?: "规则无效")
                            }
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "保存")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
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
            }
        }
    }
}
