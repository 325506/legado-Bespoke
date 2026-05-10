package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSourceDebugScreen(
    viewModel: RssSourceDebugViewModel = viewModel(),
    sourceKey: String? = null,
    onNavigateBack: () -> Unit = {},
    onShowListSrc: () -> Unit = {},
    onShowContentSrc: () -> Unit = {}
) {
    val context = LocalContext.current
    val debugLogs by viewModel.debugLogs.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val showHelp by viewModel.showHelp.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(sourceKey) {
        viewModel.initData(sourceKey)
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = context.getString(R.string.debug_source),
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
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "输入关键字搜索",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.startDebug(searchQuery)
                            }
                        }
                    ) {
                        Text("调试")
                    }
                }

                if (showHelp) {
                    HelpPanel(
                        onSearchClick = { keyword ->
                            searchQuery = keyword
                            viewModel.startDebug(keyword)
                        },
                        onCategoryClick = { category ->
                            searchQuery = category
                            viewModel.startDebug(category)
                        },
                        viewModel = viewModel
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(debugLogs) { log ->
                        DebugLogItem(log = log)
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("查看列表源码") },
                onClick = {
                    showMenu = false
                    onShowListSrc()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("查看内容源码") },
                onClick = {
                    showMenu = false
                    onShowContentSrc()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun HelpPanel(
    onSearchClick: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    viewModel: RssSourceDebugViewModel
) {
    val categoryText by viewModel.categoryText.collectAsState()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "调试搜索>>输入关键字，如：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugButton(text = "我的") { onSearchClick("我的") }
                DebugButton(text = "系统") { onSearchClick("系统") }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "调试分类>>输入分类URL，如：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            DebugButton(text = categoryText.ifEmpty { "系统::http://xxx" }) {
                if (categoryText.isNotEmpty() && !categoryText.startsWith("ERROR:")) {
                    onCategoryClick(categoryText)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "调试内容页>>输入内容页URL，如：",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            DebugButton(text = "https://m.qidian.com/book/1015609210") {
                onSearchClick("https://m.qidian.com/book/1015609210")
            }
        }
    }
}

@Composable
private fun DebugButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DebugLogItem(log: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = log,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RssSourceDebugScreenPreview() {
    RssSourceDebugScreen()
}
