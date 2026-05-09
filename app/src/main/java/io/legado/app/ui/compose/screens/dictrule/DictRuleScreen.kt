package io.legado.app.ui.compose.screens.dictrule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.DictRule
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DictRuleScreen(
    onBack: () -> Unit,
    onEditRule: (String?) -> Unit = {},
    onImportLocal: () -> Unit = {},
    onImportOnline: () -> Unit = {},
    onImportQr: () -> Unit = {},
    onImportDefault: () -> Unit = {},
    onExportSelection: (Set<DictRule>) -> Unit = {},
    viewModel: DictRuleViewModel = viewModel()
) {
    val context = LocalContext.current
    val rules by viewModel.dictRules.collectAsState(initial = emptyList())
    val selection by viewModel.selection.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<DictRule?>(null) }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (selection.isNotEmpty()) {
                            Text("已选择 ${selection.size} 项")
                        } else {
                            Text(context.getString(R.string.dict_rule))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("添加规则") },
                                onClick = {
                                    showMenu = false
                                    onEditRule(null)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("本地导入") },
                                onClick = {
                                    showMenu = false
                                    onImportLocal()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("在线导入") },
                                onClick = {
                                    showMenu = false
                                    onImportOnline()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("扫码导入") },
                                onClick = {
                                    showMenu = false
                                    onImportQr()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导入默认") },
                                onClick = {
                                    showMenu = false
                                    onImportDefault()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text("帮助") },
                                onClick = { showMenu = false },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (selection.isNotEmpty()) {
                    SelectionActionBar(
                        selectionCount = selection.size,
                        totalCount = rules.size,
                        onSelectAll = { viewModel.selectAll() },
                        onInvertSelection = { viewModel.invertSelection() },
                        onDelete = { showDeleteConfirm = true },
                        onEnable = { viewModel.enableSelection() },
                        onDisable = { viewModel.disableSelection() },
                        onExport = { onExportSelection(selection) },
                        onClearSelection = { viewModel.clearSelection() }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无字典规则")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        state = rememberLazyListState()
                    ) {
                        items(
                            items = rules,
                            key = { it.name }
                        ) { rule ->
                            DictRuleItem(
                                rule = rule,
                                isSelected = selection.any { it.name == rule.name },
                                onToggleSelection = { viewModel.toggleSelection(rule) },
                                onToggleEnabled = {
                                    rule.enabled = !rule.enabled
                                    viewModel.update(rule)
                                },
                                onEdit = {
                                    onEditRule(rule.name)
                                },
                                onDelete = {
                                    viewModel.delete(rule)
                                },
                                onShowMenu = {
                                    currentItem = rule
                                    showItemMenu = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除选中的 ${selection.size} 条规则吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelection()
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showItemMenu && currentItem != null) {
            AlertDialog(
                onDismissRequest = {
                    showItemMenu = false
                },
                title = { Text("操作") },
                text = {
                    Column {
                        TextButton(onClick = {
                            showItemMenu = false
                            onEditRule(currentItem?.name)
                        }) {
                            Text("编辑")
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { viewModel.delete(it) }
                        }) {
                            Text("删除")
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { viewModel.toTop(it) }
                        }) {
                            Text("置顶")
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { viewModel.toBottom(it) }
                        }) {
                            Text("置底")
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun DictRuleItem(
    rule: DictRule,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = rule.name,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = rule.enabled,
            onCheckedChange = { onToggleEnabled() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Info, contentDescription = "编辑")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除")
        }
        Box {
            IconButton(onClick = {
                onShowMenu()
            }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}

@Composable
fun SelectionActionBar(
    selectionCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDelete: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onExport: () -> Unit,
    onClearSelection: () -> Unit
) {
    if (selectionCount == 0) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSelectAll) {
            Text("全选")
        }
        TextButton(onClick = onInvertSelection) {
            Text("反选")
        }
        TextButton(onClick = onEnable) {
            Text("启用")
        }
        TextButton(onClick = onDisable) {
            Text("禁用")
        }
        TextButton(onClick = onExport) {
            Text("导出")
        }
        TextButton(onClick = onDelete) {
            Text("删除")
        }
        TextButton(onClick = onClearSelection) {
            Text("取消")
        }
    }
}
