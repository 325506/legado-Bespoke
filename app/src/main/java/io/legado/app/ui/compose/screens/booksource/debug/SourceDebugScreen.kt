package io.legado.app.ui.compose.screens.booksource.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceDebugScreen(
    viewModel: SourceDebugComposeViewModel,
    onNavigateBack: () -> Unit,
    onScanQrCode: () -> Unit,
    onShowSource: (String, String?) -> Unit,
    onRefreshExplore: () -> Unit
) {
    val context = LocalContext.current
    var searchKey by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(true) }
    var debugMessages by remember { mutableStateOf<List<String>>(emptyList()) }

    val bookSource = viewModel.bookSource

    LaunchedEffect(Unit) {
        viewModel.observe { state, msg ->
            debugMessages = debugMessages + msg
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(R.string.debug_source)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                        value = searchKey,
                        onValueChange = { searchKey = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        decorationBox = { innerTextField ->
                            if (searchKey.isEmpty()) {
                                Text("输入搜索关键字", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }
                            innerTextField()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (searchKey.isNotEmpty()) {
                            debugMessages = emptyList()
                            debugMessages = debugMessages + "开始调试搜索: $searchKey"
                            viewModel.startDebug(searchKey,
                                start = { debugMessages = debugMessages + "调试开始" },
                                error = { debugMessages = debugMessages + "未获取到书源" }
                            )
                        }
                    }) {
                        Text("搜索")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { showHelp = !showHelp }) {
                        Icon(Icons.Default.Info, contentDescription = "帮助")
                    }
                }

                if (showHelp && bookSource != null) {
                    HelpSection(
                        viewModel = viewModel,
                        bookSource = bookSource,
                        onSearchClick = { key ->
                            searchKey = key
                        },
                        onExploreClick = { title, url ->
                            searchKey = "$title::$url"
                        }
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    debugMessages.forEach { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("扫码调试") },
                onClick = {
                    showMenu = false
                    onScanQrCode()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("搜索源码") },
                onClick = {
                    showMenu = false
                    onShowSource("搜索源码", viewModel.searchSrc)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("详情页源码") },
                onClick = {
                    showMenu = false
                    onShowSource("详情页源码", viewModel.bookSrc)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("目录页源码") },
                onClick = {
                    showMenu = false
                    onShowSource("目录页源码", viewModel.tocSrc)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("正文页源码") },
                onClick = {
                    showMenu = false
                    onShowSource("正文页源码", viewModel.contentSrc)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("刷新发现") },
                onClick = {
                    showMenu = false
                    onRefreshExplore()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
fun HelpSection(
    viewModel: SourceDebugComposeViewModel,
    bookSource: io.legado.app.data.entities.BookSource,
    onSearchClick: (String) -> Unit,
    onExploreClick: (String, String) -> Unit
) {
    var showExploreSelector by remember { mutableStateOf(false) }
    val exploreKinds = viewModel.exploreKindsList
    val selectedExplore = viewModel.getSelectedExploreKind()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text("调试搜索>>输入关键字，如：", style = MaterialTheme.typography.bodySmall)
        Row {
            val checkKeyWord = bookSource.ruleSearch?.checkKeyWord ?: "我的"
            TextButton(onClick = { onSearchClick(checkKeyWord) }) {
                Text(checkKeyWord)
            }
            TextButton(onClick = { onSearchClick("系统") }) {
                Text("系统")
            }
        }

        Text("调试发现>>输入发现URL，如：", style = MaterialTheme.typography.bodySmall)
        TextButton(
            onClick = {
                selectedExplore?.let { (title, url) ->
                    if (!title.isNullOrBlank() && !url.isNullOrBlank()) {
                        onExploreClick(title, url)
                    }
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = {
                    selectedExplore?.let { (title, url) ->
                        if (!title.isNullOrBlank() && !url.isNullOrBlank()) {
                            onExploreClick(title, url)
                        }
                    }
                },
                onLongClick = {
                    if (exploreKinds.size > 1) {
                        showExploreSelector = true
                    }
                }
            )
        ) {
            Text(
                text = selectedExplore?.let { (title, url) ->
                    "${title ?: ""}::${url ?: ""}"
                } ?: "无发现URL",
                color = if (selectedExplore?.second?.isNullOrBlank() == true) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Text("调试详情页>>输入详情页URL，如：", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            bookSource.bookUrlPattern?.let { onSearchClick("https://example.com/book/123") }
        }) {
            Text("https://example.com/book/123")
        }

        Text("调试目录页>>输入目录页URL，如：", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            onSearchClick("++https://example.com/toc/123")
        }) {
            Text("++https://example.com/toc/123")
        }

        Text("调试正文页>>输入正文页URL，如：", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            onSearchClick("--https://example.com/content/123")
        }) {
            Text("--https://example.com/content/123")
        }
    }

    if (showExploreSelector && exploreKinds.isNotEmpty()) {
        val titles = exploreKinds.map { it.first ?: "" }
        AlertDialog(
            onDismissRequest = { showExploreSelector = false },
            title = { Text("选择发现") },
            text = {
                Column {
                    exploreKinds.forEachIndexed { index, (title, url) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectExploreKind(index)
                                    showExploreSelector = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(title ?: "")
                            Text(url ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExploreSelector = false }) {
                    Text("取消")
                }
            }
        )
    }
}
