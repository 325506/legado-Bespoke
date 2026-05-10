package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
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
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSourceScreen(
    viewModel: RssSourceViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEditSource: (String) -> Unit = {},
    onDelete: (RssSource) -> Unit = {},
    onUpdate: (Array<out RssSource>) -> Unit = {},
    onImportLocal: () -> Unit = {},
    onImportOnline: () -> Unit = {},
    onQrCodeImport: () -> Unit = {},
    onGroupManage: () -> Unit = {},
    onImportDefault: () -> Unit = {},
    onShowHelp: () -> Unit = {}
) {
    val context = LocalContext.current
    val sources by viewModel.rssSources.collectAsState(initial = emptyList())
    val selection by viewModel.selection.collectAsState()
    val groups by viewModel.groups.collectAsState(initial = emptyList())

    var showSearch by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showGroupFilterMenu by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showRemoveGroupDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = if (selection.isNotEmpty()) "已选择 ${selection.size} 项" else context.getString(R.string.rss_source),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                    }
                )
            },
            bottomBar = {
                if (selection.isNotEmpty()) {
                    SelectionActionBar(
                        selectionCount = selection.size,
                        totalCount = sources.size,
                        onSelectAll = { viewModel.selectAll() },
                        onInvertSelection = { viewModel.invertSelection() },
                        onDelete = { showDeleteConfirm = true },
                        onEnable = { viewModel.enableSelection() },
                        onDisable = { viewModel.disableSelection() },
                        onTop = { viewModel.topSelection() },
                        onBottom = { viewModel.bottomSelection() },
                        onAddGroup = { showAddGroupDialog = true },
                        onRemoveGroup = { showRemoveGroupDialog = true },
                        onExport = { viewModel.exportSelection(selection.toList()) },
                        onShare = { viewModel.shareSelection(selection.toList()) },
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
                if (showSearch) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.search(it)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        context.getString(R.string.search_rss_source),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = rememberLazyListState()
                ) {
                    items(
                        items = sources,
                        key = { it.sourceUrl }
                    ) { source ->
                        RssSourceItem(
                            source = source,
                            isSelected = selection.any { it.sourceUrl == source.sourceUrl },
                            onToggleSelection = { viewModel.toggleSelection(source) },
                            onToggleEnabled = {
                                viewModel.update(source.copy(enabled = !source.enabled))
                            },
                            onEdit = { onEditSource(source.sourceUrl) },
                            onDelete = { onDelete(source) },
                            onTop = { viewModel.topSource(source) },
                            onBottom = { viewModel.bottomSource(source) }
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("添加订阅源") },
                onClick = {
                    showMenu = false
                    onEditSource("")
                },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("导入本地") },
                onClick = {
                    showMenu = false
                    onImportLocal()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("导入在线") },
                onClick = {
                    showMenu = false
                    onImportOnline()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("扫码导入") },
                onClick = {
                    showMenu = false
                    onQrCodeImport()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("导入默认源") },
                onClick = {
                    showMenu = false
                    onImportDefault()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("分组管理") },
                onClick = {
                    showMenu = false
                    onGroupManage()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("排序") },
                onClick = {
                    showMenu = false
                    showSortMenu = true
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("分组筛选") },
                onClick = {
                    showMenu = false
                    showGroupFilterMenu = true
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("帮助") },
                onClick = {
                    showMenu = false
                    onShowHelp()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }

        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("手动排序") },
                onClick = {
                    showSortMenu = false
                    viewModel.sort(RssSourceSort.Default)
                }
            )
            DropdownMenuItem(
                text = { Text("按名称") },
                onClick = {
                    showSortMenu = false
                    viewModel.sort(RssSourceSort.Name)
                }
            )
            DropdownMenuItem(
                text = { Text("按地址") },
                onClick = {
                    showSortMenu = false
                    viewModel.sort(RssSourceSort.Url)
                }
            )
        }

        DropdownMenu(
            expanded = showGroupFilterMenu,
            onDismissRequest = { showGroupFilterMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("全部") },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup("")
                }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.enabled)) },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup(context.getString(R.string.enabled))
                }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.disabled)) },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup(context.getString(R.string.disabled))
                }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.need_login)) },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup(context.getString(R.string.need_login))
                }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.no_group)) },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup(context.getString(R.string.no_group))
                }
            )
            if (groups.isNotEmpty()) {
                Divider()
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            showGroupFilterMenu = false
                            viewModel.filterByGroup("group:$group")
                        }
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(context.getString(R.string.draw)) },
                text = { Text(context.getString(R.string.sure_del)) },
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

        if (showAddGroupDialog) {
            var groupName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddGroupDialog = false },
                title = { Text(context.getString(R.string.add_group)) },
                text = {
                    BasicTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (groupName.isEmpty()) {
                                Text(
                                    context.getString(R.string.group_name),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (groupName.isNotEmpty()) {
                            viewModel.selectionAddToGroups(selection.toList(), groupName)
                        }
                        showAddGroupDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddGroupDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        if (showRemoveGroupDialog) {
            var groupName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showRemoveGroupDialog = false },
                title = { Text(context.getString(R.string.remove_group)) },
                text = {
                    BasicTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (groupName.isEmpty()) {
                                Text(
                                    context.getString(R.string.group_name),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (groupName.isNotEmpty()) {
                            viewModel.selectionRemoveFromGroups(selection.toList(), groupName)
                        }
                        showRemoveGroupDialog = false
                    }) {
                        Text("确定")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveGroupDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun RssSourceItem(
    source: RssSource,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTop: () -> Unit,
    onBottom: () -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = {
            Text(
                text = source.sourceName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = source.sourceUrl,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() }
            )
        },
        trailingContent = {
            Row {
                Switch(
                    checked = source.enabled,
                    onCheckedChange = { onToggleEnabled() }
                )
                IconButton(onClick = { showItemMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }
        },
        modifier = Modifier.clickable { onEdit() }
    )

    DropdownMenu(
        expanded = showItemMenu,
        onDismissRequest = { showItemMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("编辑") },
            onClick = {
                showItemMenu = false
                onEdit()
            }
        )
        DropdownMenuItem(
            text = { Text("置顶") },
            onClick = {
                showItemMenu = false
                onTop()
            }
        )
        DropdownMenuItem(
            text = { Text("置底") },
            onClick = {
                showItemMenu = false
                onBottom()
            }
        )
        DropdownMenuItem(
            text = { Text("删除") },
            onClick = {
                showItemMenu = false
                onDelete()
            }
        )
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
    onTop: () -> Unit,
    onBottom: () -> Unit,
    onAddGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onClearSelection: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "已选 $selectionCount/$totalCount",
                style = MaterialTheme.typography.bodyMedium
            )
            Row {
                TextButton(onClick = onSelectAll) {
                    Text("全选")
                }
                TextButton(onClick = onInvertSelection) {
                    Text("反选")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("启用") },
            onClick = {
                showMenu = false
                onEnable()
            }
        )
        DropdownMenuItem(
            text = { Text("禁用") },
            onClick = {
                showMenu = false
                onDisable()
            }
        )
        DropdownMenuItem(
            text = { Text("置顶") },
            onClick = {
                showMenu = false
                onTop()
            }
        )
        DropdownMenuItem(
            text = { Text("置底") },
            onClick = {
                showMenu = false
                onBottom()
            }
        )
        DropdownMenuItem(
            text = { Text("添加分组") },
            onClick = {
                showMenu = false
                onAddGroup()
            }
        )
        DropdownMenuItem(
            text = { Text("移除分组") },
            onClick = {
                showMenu = false
                onRemoveGroup()
            }
        )
        DropdownMenuItem(
            text = { Text("导出") },
            onClick = {
                showMenu = false
                onExport()
            }
        )
        DropdownMenuItem(
            text = { Text("分享") },
            onClick = {
                showMenu = false
                onShare()
            }
        )
        DropdownMenuItem(
            text = { Text("取消选择") },
            onClick = {
                showMenu = false
                onClearSelection()
            }
        )
    }
}

enum class RssSourceSort {
    Default, Name, Url
}
