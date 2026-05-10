package io.legado.app.ui.compose.screens.booksource

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.source.debug.BookSourceDebugActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookSourceScreen(
    viewModel: BookSourceViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEditSource: (String) -> Unit = {},
    onDebugSource: (String) -> Unit = {},
    onLoginSource: (String) -> Unit = {},
    onSearchBook: (BookSourcePart) -> Unit = {},
    onExportSelection: (List<BookSourcePart>) -> Unit = {},
    onShareSelection: (List<BookSourcePart>) -> Unit = {},
    onImportLocal: () -> Unit = {},
    onImportOnline: () -> Unit = {},
    onQrCodeImport: () -> Unit = {},
    onGroupManage: () -> Unit = {},
    onCheckSource: (List<BookSourcePart>) -> Unit = {}
) {
    val context = LocalContext.current
    val sources by viewModel.bookSources.collectAsState(initial = emptyList())
    val selection by viewModel.selection.collectAsState()
    val groups by viewModel.groups.collectAsState(initial = emptyList())

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showGroupFilterMenu by remember { mutableStateOf(false) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showRemoveGroupDialog by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf(false) }
    var currentItem by remember { mutableStateOf<BookSourcePart?>(null) }

    var sortType by remember { mutableStateOf(BookSourceSort.Default) }
    var sortAscending by remember { mutableStateOf(true) }
    var groupSourcesByDomain by remember { mutableStateOf(false) }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        if (selection.isNotEmpty()) {
                            Text("已选择 ${selection.size} 项")
                        } else {
                            Text(context.getString(R.string.book_source))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
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
                        onEnableExplore = { viewModel.enableSelectExplore() },
                        onDisableExplore = { viewModel.disableSelectExplore() },
                        onTop = { viewModel.topSelection() },
                        onBottom = { viewModel.bottomSelection() },
                        onAddGroup = { showAddGroupDialog = true },
                        onRemoveGroup = { showRemoveGroupDialog = true },
                        onExport = { onExportSelection(selection.toList()) },
                        onShare = { onShareSelection(selection.toList()) },
                        onCheckSource = { onCheckSource(selection.toList()) },
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
                                    Text("搜索书源", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                            viewModel.search("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = rememberLazyListState()
                ) {
                    items(
                        items = sources,
                        key = { it.bookSourceUrl }
                    ) { source ->
                        BookSourceItem(
                            source = source,
                            isSelected = selection.any { it.bookSourceUrl == source.bookSourceUrl },
                            onToggleSelection = { viewModel.toggleSelection(source) },
                            onToggleEnabled = {
                                source.enabled = !source.enabled
                                viewModel.update(source)
                            },
                            onToggleEnabledExplore = {
                                source.enabledExplore = !source.enabledExplore
                                viewModel.updateExplore(source)
                            },
                            onEdit = { onEditSource(source.bookSourceUrl) },
                            onDelete = { viewModel.delete(source) },
                            onDebug = { onDebugSource(source.bookSourceUrl) },
                            onSearchBook = { onSearchBook(source) },
                            onTop = { viewModel.topSource(source) },
                            onBottom = { viewModel.bottomSource(source) },
                            onShowMenu = {
                                currentItem = source
                                showItemMenu = true
                            }
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
                text = { Text("添加书源") },
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
                text = { Text("按域名分组") },
                onClick = {
                    showMenu = false
                    groupSourcesByDomain = !groupSourcesByDomain
                    viewModel.groupByDomain(groupSourcesByDomain)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("帮助") },
                onClick = {
                    showMenu = false
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
                    sortType = BookSourceSort.Default
                    viewModel.sort(sortType, sortAscending)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("按权重") },
                onClick = {
                    showSortMenu = false
                    sortType = BookSourceSort.Weight
                    viewModel.sort(sortType, sortAscending)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("按名称") },
                onClick = {
                    showSortMenu = false
                    sortType = BookSourceSort.Name
                    viewModel.sort(sortType, sortAscending)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("按地址") },
                onClick = {
                    showSortMenu = false
                    sortType = BookSourceSort.Url
                    viewModel.sort(sortType, sortAscending)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("升序/降序") },
                onClick = {
                    showSortMenu = false
                    sortAscending = !sortAscending
                    viewModel.sort(sortType, sortAscending)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
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
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("已启用") },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup("enabled")
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("未启用") },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup("disabled")
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("需登录") },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup("need_login")
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("无分组") },
                onClick = {
                    showGroupFilterMenu = false
                    viewModel.filterByGroup("no_group")
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            Divider()
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group) },
                    onClick = {
                        showGroupFilterMenu = false
                        viewModel.filterByGroup("group:$group")
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("删除") },
                text = { Text("确定要删除选中的 ${selection.size} 条书源吗？") },
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
                title = { Text("添加分组") },
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
                                Text("分组名称", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                title = { Text("移除分组") },
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
                                Text("分组名称", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
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
                            onEditSource(currentItem?.bookSourceUrl ?: "")
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
                            onDebugSource(currentItem?.bookSourceUrl ?: "")
                        }) {
                            Text("调试")
                        }
                        if (currentItem?.hasLoginUrl == true) {
                            TextButton(onClick = {
                                showItemMenu = false
                                onLoginSource(currentItem?.bookSourceUrl ?: "")
                            }) {
                                Text("登录")
                            }
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { onSearchBook(it) }
                        }) {
                            Text("搜索")
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { viewModel.topSource(it) }
                        }) {
                            Text("置顶")
                        }
                        TextButton(onClick = {
                            showItemMenu = false
                            currentItem?.let { viewModel.bottomSource(it) }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookSourceItem(
    source: BookSourcePart,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleEnabledExplore: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDebug: () -> Unit,
    onSearchBook: () -> Unit,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    onShowMenu: () -> Unit
) {
    var localEnabled by remember(source.bookSourceUrl) { mutableStateOf(source.enabled) }
    var localEnabledExplore by remember(source.bookSourceUrl) { mutableStateOf(source.enabledExplore) }

    LaunchedEffect(source.enabled) {
        localEnabled = source.enabled
    }
    LaunchedEffect(source.enabledExplore) {
        localEnabledExplore = source.enabledExplore
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onToggleSelection() },
                onLongClick = { onShowMenu() }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggleSelection() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = source.bookSourceName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            source.bookSourceGroup?.takeIf { it.isNotBlank() }?.let { group ->
                Text(
                    text = group,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = localEnabled,
            onCheckedChange = {
                localEnabled = it
                onToggleEnabled()
            },
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = {
            localEnabledExplore = !localEnabledExplore
            onToggleEnabledExplore()
        }) {
            Icon(
                imageVector = if (localEnabledExplore) Icons.Default.Info else Icons.Default.Info,
                contentDescription = "发现",
                tint = if (localEnabledExplore) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onShowMenu() }) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多")
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
    onEnableExplore: () -> Unit,
    onDisableExplore: () -> Unit,
    onTop: () -> Unit,
    onBottom: () -> Unit,
    onAddGroup: () -> Unit,
    onRemoveGroup: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onCheckSource: () -> Unit,
    onClearSelection: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("已选择 $selectionCount/$totalCount 项")
                TextButton(onClick = onClearSelection) {
                    Text("取消选择")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onSelectAll) {
                    Text("全选")
                }
                TextButton(onClick = onInvertSelection) {
                    Text("反选")
                }
                TextButton(onClick = onDelete) {
                    Text("删除")
                }
                TextButton(onClick = onEnable) {
                    Text("启用")
                }
                TextButton(onClick = onDisable) {
                    Text("禁用")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEnableExplore) {
                    Text("启用发现")
                }
                TextButton(onClick = onDisableExplore) {
                    Text("禁用发现")
                }
                TextButton(onClick = onTop) {
                    Text("置顶")
                }
                TextButton(onClick = onBottom) {
                    Text("置底")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAddGroup) {
                    Text("添加分组")
                }
                TextButton(onClick = onRemoveGroup) {
                    Text("移除分组")
                }
                TextButton(onClick = onExport) {
                    Text("导出")
                }
                TextButton(onClick = onShare) {
                    Text("分享")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onCheckSource) {
                    Text("校验书源")
                }
            }
        }
    }
}
