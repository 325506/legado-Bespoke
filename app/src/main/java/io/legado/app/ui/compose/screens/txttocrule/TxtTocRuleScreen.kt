package io.legado.app.ui.compose.screens.txttocrule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TxtTocRuleScreen(
    onBack: () -> Unit,
    onImportLocal: () -> Unit = {},
    onImportOnline: () -> Unit = {},
    onImportQr: () -> Unit = {},
    onImportDefault: () -> Unit = {},
    onHelp: () -> Unit = {},
    onExportSelection: (Set<TxtTocRule>) -> Unit = {},
    viewModel: TxtTocRuleViewModel = viewModel()
) {
    val context = LocalContext.current
    val rules by viewModel.txtTocRules.collectAsState(initial = emptyList())
    val selection by viewModel.selection.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<TxtTocRule?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRuleId by remember { mutableStateOf<Long?>(null) }
    var menuAnchorPosition by remember { mutableStateOf<IntOffset?>(null) }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (selection.isNotEmpty()) {
                            Text("已选择 ${selection.size} 项")
                        } else {
                            Text(context.getString(R.string.txt_toc_rule))
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
                                    editingRuleId = null
                                    showEditDialog = true
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
                                text = { Text("导入默认规则") },
                                onClick = {
                                    showMenu = false
                                    onImportDefault()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("帮助") },
                                onClick = {
                                    showMenu = false
                                    onHelp()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (rules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
                            key = { rule -> "${rule.id}_${rule.enable}" }
                        ) { rule ->
                            TxtTocRuleItem(
                                rule = rule,
                                isSelected = selection.contains(rule),
                                enable = rule.enable,
                                onCheckboxClick = {
                                    viewModel.toggleSelection(rule)
                                },
                                onToggleEnable = { enabled ->
                                    rule.enable = enabled
                                    viewModel.update(rule)
                                },
                                onShowMenu = { anchorOffset ->
                                    currentItem = rule
                                    menuAnchorPosition = anchorOffset
                                    showItemMenu = true
                                },
                                onEdit = {
                                    editingRuleId = rule.id
                                    showEditDialog = true
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
                    currentItem?.let {
                        editingRuleId = it.id
                        showEditDialog = true
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("删除") },
                onClick = {
                    showItemMenu = false
                    menuAnchorPosition = null
                    currentItem?.let { viewModel.del(it) }
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )
        }
    }

    if (showEditDialog) {
        TxtTocRuleEditDialog(
            ruleId = editingRuleId,
            onDismiss = { showEditDialog = false },
            onSave = { rule ->
                viewModel.save(rule)
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxtTocRuleItem(
    rule: TxtTocRule,
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
                text = rule.name,
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row {
            TextButton(
                onClick = onSelectAll,
                enabled = hasSelection
            ) {
                Text(if (hasSelection) "取消全选" else "全选")
            }
            TextButton(
                onClick = onInvertSelection,
                enabled = hasSelection
            ) {
                Text("反选")
            }
            TextButton(
                onClick = onClearSelection,
                enabled = hasSelection
            ) {
                Text("取消")
            }
            Box {
                TextButton(
                    onClick = { showActionsMenu = true },
                    enabled = hasSelection
                ) {
                    Text("操作")
                }
                DropdownMenu(
                    expanded = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("启用") },
                        onClick = {
                            showActionsMenu = false
                            onEnable()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("禁用") },
                        onClick = {
                            showActionsMenu = false
                            onDisable()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            showActionsMenu = false
                            onDelete()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("导出") },
                        onClick = {
                            showActionsMenu = false
                            onExport()
                        }
                    )
                }
            }
        }
    }
}
