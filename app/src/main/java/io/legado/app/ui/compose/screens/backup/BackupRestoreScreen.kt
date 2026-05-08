package io.legado.app.ui.compose.screens.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.ui.compose.components.LegadoListItem
import io.legado.app.ui.compose.components.LegadoProfileCard
import io.legado.app.ui.compose.components.LegadoSectionHeader
import io.legado.app.ui.compose.theme.LegadoTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit = {},
    onLog: () -> Unit = {},
    viewModel: BackupRestoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showRestoreIgnoreDialog by remember { mutableStateOf(false) }

    val selectBackupPathLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            val path = it.toString()
            viewModel.updateBackupPath(path)
        }
    }

    val restoreFromLocalLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.restoreFromLocal(it) }
    }

    val importOldLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                io.legado.app.help.storage.ImportOldData.importUri(context, it)
            }
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(R.string.backup_restore)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = context.getString(R.string.back)
                            )
                        }
                    },
                    actions = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("帮助") },
                                    onClick = {
                                        showMenu = false
                                        onHelp()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("日志") },
                                    onClick = {
                                        showMenu = false
                                        onLog()
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    LegadoSectionHeader(title = "WebDAV 设置")
                }

                item {
                    OutlinedTextField(
                        value = uiState.webDavUrl,
                        onValueChange = { viewModel.updateWebDavUrl(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.web_dav_url)) },
                        placeholder = { Text(context.getString(R.string.web_dav_url_s)) },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.webDavAccount,
                        onValueChange = { viewModel.updateWebDavAccount(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.web_dav_account)) },
                        placeholder = { Text(context.getString(R.string.web_dav_account_s)) },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.webDavPassword,
                        onValueChange = { viewModel.updateWebDavPassword(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.web_dav_pw)) },
                        placeholder = { Text(context.getString(R.string.web_dav_pw_s)) },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.webDavDir,
                        onValueChange = { viewModel.updateWebDavDir(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("子目录") },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = uiState.webDavDeviceName,
                        onValueChange = { viewModel.updateWebDavDeviceName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(context.getString(R.string.webdav_device_name)) },
                        leadingIcon = {
                            Icon(Icons.Default.Info, contentDescription = null)
                        }
                    )
                }

                item {
                    LegadoListItem(
                        title = "同步阅读进度",
                        subtitle = "同步阅读进度到 WebDAV",
                        icon = Icons.Default.Share,
                        trailingContent = {
                            Switch(
                                checked = uiState.syncBookProgress,
                                onCheckedChange = { viewModel.updateSyncBookProgress(it) }
                            )
                        }
                    )
                }

                item {
                    LegadoListItem(
                        title = "增强同步",
                        subtitle = "同步更多阅读数据",
                        icon = Icons.Default.Share,
                        trailingContent = {
                            Switch(
                                checked = uiState.syncBookProgressPlus,
                                onCheckedChange = { viewModel.updateSyncBookProgressPlus(it) },
                                enabled = uiState.syncBookProgress
                            )
                        }
                    )
                }

                item {
                    LegadoSectionHeader(title = "备份设置")
                }

                item {
                    LegadoListItem(
                        title = "备份路径",
                        subtitle = if (uiState.backupPath.isEmpty()) {
                            "选择备份存储位置"
                        } else {
                            uiState.backupPath
                        },
                        icon = Icons.Default.Info,
                        onClick = { selectBackupPathLauncher.launch(null) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.backup(uiState.backupPath) {
                                        selectBackupPathLauncher.launch(null)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isBackingUp
                        ) {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("备份中")
                            } else {
                                Icon(Icons.Default.Create, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("备份")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.restore {
                                    restoreFromLocalLauncher.launch(arrayOf("application/zip"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isRestoring
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("恢复中")
                            } else {
                                Icon(Icons.Default.Build, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("恢复")
                            }
                        }
                    }
                }

                item {
                    LegadoListItem(
                        title = "恢复忽略项",
                        subtitle = "设置恢复时忽略的内容",
                        icon = Icons.Default.Settings,
                        onClick = { showRestoreIgnoreDialog = true }
                    )
                }

                item {
                    LegadoListItem(
                        title = "导入旧版备份",
                        subtitle = "从旧版阅读备份导入",
                        icon = Icons.Default.Build,
                        onClick = {
                            importOldLauncher.launch(arrayOf("application/zip"))
                        }
                    )
                }

                item {
                    LegadoListItem(
                        title = "仅保留最新备份",
                        subtitle = "备份时删除旧备份",
                        icon = Icons.Default.Info,
                        trailingContent = {
                            Switch(
                                checked = uiState.onlyLatestBackup,
                                onCheckedChange = { viewModel.updateOnlyLatestBackup(it) }
                            )
                        }
                    )
                }

                item {
                    LegadoListItem(
                        title = "自动检查新备份",
                        subtitle = "启动时自动检查",
                        icon = Icons.Default.Share,
                        trailingContent = {
                            Switch(
                                checked = uiState.autoCheckNewBackup,
                                onCheckedChange = { viewModel.updateAutoCheckNewBackup(it) }
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (uiState.showRestoreDialog && uiState.webDavBackupNames.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreDialog() },
            title = { Text("选择恢复文件") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    uiState.webDavBackupNames.forEach { name ->
                        Text(
                            text = name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.restoreWebDav(name)
                                    viewModel.dismissRestoreDialog()
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRestoreDialog() }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }

    if (uiState.showRestoreError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestoreError() },
            title = { Text(context.getString(R.string.restore)) },
            text = { Text(uiState.restoreErrorMessage) },
            confirmButton = {
                Button(onClick = { viewModel.dismissRestoreError() }) {
                    Text(context.getString(R.string.ok))
                }
            }
        )
    }

    if (showRestoreIgnoreDialog) {
        RestoreIgnoreDialog(
            onDismiss = { showRestoreIgnoreDialog = false }
        )
    }
}

@Composable
fun RestoreIgnoreDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val ignoreKeys = io.legado.app.help.storage.BackupConfig.ignoreKeys
    val ignoreTitles = io.legado.app.help.storage.BackupConfig.ignoreTitle
    val ignoreConfig = io.legado.app.help.storage.BackupConfig.ignoreConfig

    val checkedStates = remember {
        mutableStateOf(
            BooleanArray(ignoreKeys.size) {
                ignoreConfig[ignoreKeys[it]] ?: false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("恢复忽略项") },
        text = {
            Column {
                ignoreKeys.forEachIndexed { index, _ ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                checkedStates.value[index] = !checkedStates.value[index]
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = checkedStates.value[index],
                            onCheckedChange = {
                                checkedStates.value[index] = it
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(ignoreTitles[index])
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    ignoreKeys.forEachIndexed { index, key ->
                        ignoreConfig[key] = checkedStates.value[index]
                    }
                    io.legado.app.help.storage.BackupConfig.saveIgnoreConfig()
                    onDismiss()
                }
            ) {
                Text(context.getString(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}
