package io.legado.app.ui.compose.screens.dictrule

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.DictRule
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.dict.edit.DictRuleEditComposeViewModel
import io.legado.app.utils.GSON
import io.legado.app.utils.toastOnUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictRuleEditScreen(
    ruleName: String? = null,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onCopyRule: (String) -> Unit,
    onPasteRule: (String) -> Unit,
    viewModel: DictRuleEditComposeViewModel = viewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var urlRule by remember { mutableStateOf("") }
    var showRule by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(ruleName) {
        if (ruleName != null) {
            viewModel.loadRule(ruleName) { rule ->
                name = rule.name
                urlRule = rule.urlRule ?: ""
                showRule = rule.showRule ?: ""
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (ruleName != null) "编辑字典规则" else "添加字典规则") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val rule = DictRule(
                                name = name,
                                urlRule = urlRule,
                                showRule = showRule
                            )
                            onCopyRule(GSON.toJson(rule))
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "拷贝")
                        }
                        IconButton(onClick = {
                            onPasteRule("")
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "粘贴")
                        }
                        IconButton(onClick = {
                            if (name.isBlank()) {
                                context.toastOnUi("规则名称不能为空")
                                return@IconButton
                            }
                            val rule = DictRule(
                                name = name,
                                urlRule = urlRule,
                                showRule = showRule
                            )
                            viewModel.save(rule) {
                                onSaveSuccess()
                            }
                        }) {
                            Icon(Icons.Default.Info, contentDescription = "保存")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("名称") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = urlRule,
                        onValueChange = { urlRule = it },
                        label = { Text("URL规则") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = showRule,
                        onValueChange = { showRule = it },
                        label = { Text("显示规则") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 10,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }
            }
        }
    }
}
