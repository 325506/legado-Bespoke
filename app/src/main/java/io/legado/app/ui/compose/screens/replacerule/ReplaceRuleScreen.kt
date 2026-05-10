package io.legado.app.ui.compose.screens.replacerule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReplaceRuleScreen(
    onBack: () -> Unit,
    onEditRule: (Long?) -> Unit = {},
    onImportLocal: () -> Unit = {},
    onImportOnline: () -> Unit = {},
    onImportQr: () -> Unit = {},
    onExportSelection: (Set<ReplaceRule>) -> Unit = {},
    viewModel: ReplaceRuleViewModel = viewModel()
) {
    val context = LocalContext.current
    val rules by viewModel.replaceRules.collectAsState(initial = emptyList())
    val selection by viewModel.selection.collectAsState()
    val groups by viewModel.groups.collectAsState(initial = emptyList())

    var showMenu by remember { mutableStateOf(false) }
    var showGroupMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<ReplaceRule?>(null) }
    var menuAnchorPosition by remember { mutableStateOf<IntOffset?>(null) }
    var searchKey by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = if (selection.isNotEmpty()) "已选择 ${selection.size} 项" else context.getString(R.string.replace_purify),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.back)
                            )
                        }
                    },
                    actions = {
                        Box {
                            TextButton(onClick = { showGroupMenu = true }) {
                                Text(selectedGroup ?: "分组")
                                Icon(Icons.Default.Info, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showGroupMenu,
                                onDismissRequest = { showGroupMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部分组") },
                                    onClick = {
                                        showGroupMenu = false
                                        selectedGroup = null
                                        viewModel.setSearchKey(null)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("启用") },
                                    onClick = {
                                        showGroupMenu = false
                                        selectedGroup = "启用"
                                        viewModel.setSearchKey("enabled")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("禁用") },
                                    onClick = {
                                        showGroupMenu = false
                                        selectedGroup = "禁用"
                                        viewModel.setSearchKey("disabled")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("未分组") },
                                    onClick = {
                                        showGroupMenu = false
                                        selectedGroup = "未分组"
                                        viewModel.setSearchKey("no_group")
                                    }
                                )
                                if (groups.isNotEmpty()) {
                                    Divider()
                                    groups.forEach { group ->
                                        DropdownMenuItem(
                                            text = { Text(group) },
                                            onClick = {
                                                showGroupMenu = false
                                                selectedGroup = group
                                                viewModel.setSearchKey("group:$group")
                                            }
                                        )
                                    }
                                }
                            }
                        }
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
                        Text("暂无替换规则")
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
                            key = { rule -> "${rule.id}_${rule.isEnabled}" }
                        ) { rule ->
                            ReplaceRuleItem(
                                rule = rule,
                                isSelected = selection.contains(rule),
                                enable = rule.isEnabled,
                                onCheckboxClick = {
                                    viewModel.toggleSelection(rule)
                                },
                                onToggleEnable = { enabled ->
                                    rule.isEnabled = enabled
                                    viewModel.update(rule)
                                },
                                onShowMenu = { anchorOffset ->
                                    currentItem = rule
                                    menuAnchorPosition = anchorOffset
                                    showItemMenu = true
                                },
                                onEdit = {
                                    onEditRule(rule.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的 ${selection.size} 条规则吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.deleteSelection()
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    menuAnchorPosition?.let { anchorOffset ->
        val density = LocalDensity.current
        DropdownMenu(
            expanded = showItemMenu,
            onDismissRequest = {
                showItemMenu = false
                menuAnchorPosition = null
            },
            offset = DpOffset(
                x = with(density) { (anchorOffset.x / density.density).toDp() },
                y = with(density) { (anchorOffset.y / density.density).toDp() }
            )
        ) {
            DropdownMenuItem(
                text = { Text("置顶") },
                onClick = {
                    showItemMenu = false
                    menuAnchorPosition = null
                    currentItem?.let { viewModel.toTop(it) }
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("置底") },
                onClick = {
                    showItemMenu = false
                    menuAnchorPosition = null
                    currentItem?.let { viewModel.toBottom(it) }
                },
                leadingIcon = {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = {
                    showItemMenu = false
                    menuAnchorPosition = null
                    onEditRule(currentItem?.id)
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showItemMenu = false
                    menuAnchorPosition = null
                    currentItem?.let { viewModel.delete(it) }
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReplaceRuleItem(
    rule: ReplaceRule,
    isSelected: Boolean,
    enable: Boolean,
    onCheckboxClick: () -> Unit,
    onToggleEnable: (Boolean) -> Unit,
    onShowMenu: (IntOffset) -> Unit,
    onEdit: () -> Unit
) {
    var menuAnchor by remember { mutableStateOf<IntOffset?>(null) }
    var localEnable by remember(rule.id) { mutableStateOf(enable) }

    LaunchedEffect(enable) {
        localEnable = enable
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onCheckboxClick() }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clickable { onEdit() }
        ) {
            Text(
                text = rule.getDisplayNameGroup(),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (localEnable) "已启用" else "已禁用",
                style = MaterialTheme.typography.bodySmall,
                color = if (localEnable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = localEnable,
            onCheckedChange = { enabled ->
                localEnable = enabled
                onToggleEnable(enabled)
            }
        )

        IconButton(
            onClick = {
                menuAnchor?.let { onShowMenu(it) }
            },
            modifier = Modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                menuAnchor = IntOffset(
                    bounds.left.toInt(),
                    bounds.bottom.toInt()
                )
            }
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多操作",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionActionBar(
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
    var showActionsMenu by remember { mutableStateOf(false) }
    val hasSelection = selectionCount > 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasSelection) {
                "$selectionCount / $totalCount"
            } else {
                "0 / $totalCount"
            },
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = { onSelectAll() },
                enabled = hasSelection
            ) {
                Text("全选")
            }

            TextButton(
                onClick = { onInvertSelection() },
                enabled = hasSelection
            ) {
                Text("反选")
            }

            TextButton(
                onClick = { onEnable() },
                enabled = hasSelection
            ) {
                Text("启用")
            }

            TextButton(
                onClick = { onDisable() },
                enabled = hasSelection
            ) {
                Text("禁用")
            }

            TextButton(
                onClick = { onExport() },
                enabled = hasSelection
            ) {
                Text("导出")
            }

            TextButton(
                onClick = { onDelete() },
                enabled = hasSelection,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("删除")
            }

            if (hasSelection) {
                TextButton(onClick = { onClearSelection() }) {
                    Text("取消选择")
                }
            }
        }
    }
}
