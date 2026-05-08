package io.legado.app.ui.compose.screens.importbook

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.ui.book.import.local.ImportBook
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.utils.ArchiveUtils
import io.legado.app.utils.FileDoc

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBookScreen(
    onBack: () -> Unit,
    onReadBook: (io.legado.app.data.entities.Book) -> Unit,
    viewModel: ImportBookViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.dataFlow.collectAsState(initial = emptyList())
    val listState = rememberLazyListState()

    var showMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var archiveFiles by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentArchiveDoc by remember { mutableStateOf<FileDoc?>(null) }

    val selectFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.selectFolder(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.initRootDoc()
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(R.string.import_local)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!viewModel.goBackDir()) {
                                onBack()
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = context.getString(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { selectFolderLauncher.launch(null) }) {
                            Icon(Icons.Default.Create, contentDescription = "选择文件夹")
                        }
                        IconButton(onClick = { viewModel.scanFolder() }) {
                            Icon(Icons.Default.Search, contentDescription = "扫描文件夹")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("排序") },
                                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showSortMenu = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("导入文件名") },
                                    onClick = {
                                        showMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除选中") },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (uiState.isEmpty() && viewModel.rootDoc == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.empty_msg_import_book),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState, key = { item -> item.file.toString() }) { item ->
                        ImportBookItem(
                            item = item,
                            onClick = {
                                if (item.isDir) {
                                    viewModel.nextDoc(item.file)
                                } else if (item.isOnBookShelf) {
                                    viewModel.startRead(item.file, onReadBook)
                                } else if (ArchiveUtils.isArchive(item.file.name)) {
                                    currentArchiveDoc = item.file
                                    viewModel.getArchiveFiles(item.file) { files ->
                                        archiveFiles = files
                                        showArchiveDialog = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSortMenu) {
        AlertDialog(
            onDismissRequest = { showSortMenu = false },
            title = { Text("排序方式") },
            text = {
                Column {
                    Text(
                        text = "按名称",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.upSort(0)
                                showSortMenu = false
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "按大小",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.upSort(1)
                                showSortMenu = false
                            }
                            .padding(vertical = 12.dp)
                    )
                    Text(
                        text = "按时间",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.upSort(2)
                                showSortMenu = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortMenu = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除选中的文件吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                }) {
                    Text("删除")
                }
            }
        )
    }

    if (showArchiveDialog && currentArchiveDoc != null) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("选择要导入的文件") },
            text = {
                LazyColumn {
                    items(archiveFiles) { fileName ->
                        Text(
                            text = fileName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentArchiveDoc?.let { doc ->
                                        viewModel.importArchiveFile(doc, fileName) { book ->
                                            onReadBook(book)
                                        }
                                    }
                                    showArchiveDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun ImportBookItem(
    item: ImportBook,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.isDir) {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else if (item.isOnBookShelf) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.isDir) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.file.name.substringAfterLast(".", ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
