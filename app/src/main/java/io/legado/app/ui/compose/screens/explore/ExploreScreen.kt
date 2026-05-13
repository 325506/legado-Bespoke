package io.legado.app.ui.compose.screens.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.data.entities.rule.FlexChildStyle
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.exploreKinds
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.SourceLoginJsExtensions
import io.legado.app.ui.main.explore.ExploreAdapter
import io.legado.app.utils.InfoMap
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: androidx.navigation.NavHostController? = null,
    viewModel: ExploreScreenViewModel = viewModel(),
    onOpenExplore: (sourceUrl: String, title: String, exploreUrl: String?) -> Unit = { _, _, _ -> },
    onEditSource: (sourceUrl: String) -> Unit = {},
    onToTop: (BookSourcePart) -> Unit = {},
    onDeleteSource: (BookSourcePart) -> Unit = {},
    onSearchBook: (BookSourcePart) -> Unit = {},
    onLogin: (BookSourcePart) -> Unit = {},
    onCompress: () -> Boolean = { false }
) {
    val context = LocalContext.current
    val exploreSources = viewModel.exploreSources
    val searchQuery = viewModel.searchQuery
    val groups = viewModel.groups
    val expandedIndex = viewModel.expandedIndex

    var showMenu by remember { mutableStateOf(false) }
    var showGroupFilterMenu by remember { mutableStateOf(false) }
    var sourceMenuSource by remember { mutableStateOf<BookSourcePart?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<BookSourcePart?>(null) }

    LegadoTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = context.getString(R.string.discovery),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        actions = {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .weight(1f),
                            placeholder = {
                                Text(context.getString(R.string.screen_find))
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {}),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            if (exploreSources.isEmpty() && searchQuery.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.explore_empty),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = exploreSources,
                        key = { _, item -> item.bookSourceUrl }
                    ) { index, source ->
                        ExploreSourceItem(
                            source = source,
                            isExpanded = expandedIndex == index,
                            onToggleExpand = { viewModel.toggleExpand(index) },
                            onOpenExplore = onOpenExplore,
                            onLongClick = { sourceMenuSource = source },
                            onSearchBook = onSearchBook,
                            onLogin = onLogin,
                            onRefresh = {
                                viewModel.refreshSource(source)
                            },
                            onEdit = onEditSource,
                            onToTop = { onToTop(source) },
                            onDelete = { onDeleteSource(source) }
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
                text = { Text(context.getString(R.string.group)) },
                onClick = {
                    showMenu = false
                    showGroupFilterMenu = true
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
                    viewModel.onGroupFilter("")
                }
            )
            if (groups.isNotEmpty()) {
                HorizontalDivider()
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group) },
                        onClick = {
                            showGroupFilterMenu = false
                            viewModel.onGroupFilter("group:$group")
                        }
                    )
                }
            }
        }

        sourceMenuSource?.let { source ->
            SourceDropdownMenu(
                source = source,
                onDismiss = { sourceMenuSource = null },
                onEdit = {
                    onEditSource(source.bookSourceUrl)
                },
                onToTop = {
                    onToTop(source)
                },
                onLogin = {
                    onLogin(source)
                },
                onSearch = {
                    onSearchBook(source)
                },
                onRefresh = {
                    viewModel.toggleExpand(-1)
                    viewModel.toggleExpand(exploreSources.indexOf(source))
                },
                onDelete = {
                    showDeleteConfirm = source
                }
            )
        }

        showDeleteConfirm?.let { source ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text(context.getString(R.string.draw)) },
                text = { Text("${context.getString(R.string.sure_del)}\n${source.bookSourceName}") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteSource(source)
                        showDeleteConfirm = null
                    }) {
                        Text(context.getString(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) {
                        Text(context.getString(R.string.no))
                    }
                }
            )
        }
    }
}

@Composable
private fun SourceDropdownMenu(
    source: BookSourcePart,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToTop: () -> Unit,
    onLogin: () -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(context.getString(R.string.edit)) },
            onClick = { onDismiss(); onEdit() },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.to_top)) },
            onClick = { onDismiss(); onToTop() },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        if (source.hasLoginUrl) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.login)) },
                onClick = { onDismiss(); onLogin() },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text(context.getString(R.string.search)) },
            onClick = { onDismiss(); onSearch() },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.refresh)) },
            onClick = { onDismiss(); onRefresh() },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.delete)) },
            onClick = { onDismiss(); onDelete() },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreSourceItem(
    source: BookSourcePart,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpenExplore: (sourceUrl: String, title: String, exploreUrl: String?) -> Unit,
    onLongClick: () -> Unit,
    onSearchBook: (BookSourcePart) -> Unit,
    onLogin: (BookSourcePart) -> Unit = {},
    onRefresh: () -> Unit = {},
    onEdit: (sourceUrl: String) -> Unit = {},
    onToTop: (BookSourcePart) -> Unit = {},
    onDelete: (BookSourcePart) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var kinds by remember { mutableStateOf<List<ExploreKind>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSourceMenu by remember { mutableStateOf(false) }

    val infoMap = remember(source.bookSourceUrl) {
        ExploreAdapter.exploreInfoMapList[source.bookSourceUrl]
            ?: InfoMap(source.bookSourceUrl).also {
                ExploreAdapter.exploreInfoMapList.put(source.bookSourceUrl, it)
            }
    }

    val bookSource = remember(source.bookSourceUrl) {
        mutableStateOf<io.legado.app.data.entities.BookSource?>(null)
    }

    suspend fun refreshKinds() {
        isLoading = true
        try {
            withContext(IO) { source.clearExploreKindsCache() }
            kinds = withContext(IO) { source.exploreKinds() }
        } catch (_: Exception) {
            kinds = emptyList()
        }
        isLoading = false
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            isLoading = true
            try {
                kinds = withContext(IO) { source.exploreKinds() }
            } catch (_: Exception) {
                kinds = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(source.bookSourceUrl) {
        withContext(IO) {
            bookSource.value = appDb.bookSourceDao.getBookSource(source.bookSourceUrl)
        }
    }

    val sourceJsExtensions = remember(context, bookSource.value) {
        bookSource.value?.let { bs ->
            SourceLoginJsExtensions(
                context as? androidx.appcompat.app.AppCompatActivity,
                bs,
                callback = object : SourceLoginJsExtensions.Callback {
                    override fun upUiData(data: Map<String, Any?>?) {}
                    override fun reUiView(deltaUp: Boolean) {
                        scope.launch { refreshKinds() }
                    }
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onToggleExpand)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = source.bookSourceName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowForward,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { onSearchBook(source) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = context.getString(R.string.search),
                    modifier = Modifier.size(18.dp)
                )
            }
            if (source.hasLoginUrl) {
                IconButton(
                    onClick = { onLogin(source) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = context.getString(R.string.login),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = {
                    scope.launch { refreshKinds() }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = context.getString(R.string.refresh),
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = { showSourceMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showSourceMenu,
            onDismissRequest = { showSourceMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(context.getString(R.string.edit)) },
                onClick = { showSourceMenu = false; onEdit(source.bookSourceUrl) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.to_top)) },
                onClick = { showSourceMenu = false; onToTop(source) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.delete)) },
                onClick = { showSourceMenu = false; onDelete(source) },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }

        AnimatedVisibility(visible = isExpanded && kinds.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                kinds.forEach { kind ->
                    val styleModifier = kind.style().toModifier()
                    when (kind.type) {
                        ExploreKind.Type.url -> {
                            Box(modifier = styleModifier) {
                                ExploreKindUrlChip(
                                    kind = kind,
                                    infoMap = infoMap,
                                    source = bookSource.value,
                                    onOpenExplore = { url ->
                                        if (kind.title.startsWith("ERROR:")) {
                                            return@ExploreKindUrlChip
                                        }
                                        onOpenExplore(source.bookSourceUrl, kind.title, url)
                                    }
                                )
                            }
                        }
                        ExploreKind.Type.button -> {
                            Box(modifier = styleModifier) {
                                ExploreKindButtonChip(
                                    kind = kind,
                                    infoMap = infoMap,
                                    source = bookSource.value,
                                    sourceJsExtensions = sourceJsExtensions,
                                    scope = scope,
                                    onRefreshKinds = { scope.launch { refreshKinds() } }
                                )
                            }
                        }
                        ExploreKind.Type.toggle -> {
                            Box(modifier = styleModifier) {
                                ExploreKindToggleChip(
                                    kind = kind,
                                    infoMap = infoMap,
                                    source = bookSource.value,
                                    sourceJsExtensions = sourceJsExtensions,
                                    scope = scope,
                                    onRefreshKinds = { scope.launch { refreshKinds() } }
                                )
                            }
                        }
                        ExploreKind.Type.text -> {
                            Box(modifier = styleModifier) {
                                ExploreKindTextField(
                                    kind = kind,
                                    infoMap = infoMap,
                                    source = bookSource.value,
                                    sourceJsExtensions = sourceJsExtensions,
                                    scope = scope,
                                    onRefreshKinds = { scope.launch { refreshKinds() } }
                                )
                            }
                        }
                        ExploreKind.Type.select -> {
                            Box(modifier = styleModifier) {
                                ExploreKindSelectField(
                                    kind = kind,
                                    infoMap = infoMap,
                                    source = bookSource.value,
                                    sourceJsExtensions = sourceJsExtensions,
                                    scope = scope,
                                    onRefreshKinds = { scope.launch { refreshKinds() } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isQuotedLiteral(viewName: String?): Boolean {
    return viewName != null && viewName.length in 3..19 && viewName.first() == '\'' && viewName.last() == '\''
}

private fun resolveViewName(viewName: String?, title: String): String {
    if (viewName == null) return title
    if (isQuotedLiteral(viewName)) {
        return viewName.substring(1, viewName.length - 1)
    }
    return title
}

private suspend fun evalUiJs(jsStr: String, source: BaseSource?, infoMap: InfoMap): String? {
    val src = source ?: return null
    return try {
        com.script.rhino.runScriptWithContext {
            src.evalJS(jsStr) {
                put("infoMap", infoMap)
            }.toString()
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun evalButtonClick(
    jsStr: String,
    source: BaseSource?,
    infoMap: InfoMap,
    name: String,
    java: SourceLoginJsExtensions?
) {
    val src = source ?: return
    try {
        com.script.rhino.runScriptWithContext {
            src.evalJS(jsStr) {
                put("java", java)
                put("infoMap", infoMap)
            }
        }
    } catch (_: Exception) {
    }
}

private suspend fun tryEvalViewName(
    viewName: String?,
    source: io.legado.app.data.entities.BookSource?,
    infoMap: InfoMap,
    fallback: String
): String {
    if (viewName == null) return fallback
    if (isQuotedLiteral(viewName)) {
        return viewName.substring(1, viewName.length - 1)
    }
    val result = withContext(IO) { evalUiJs(viewName, source, infoMap) }
    return if (result.isNullOrEmpty()) "null" else result
}

@Composable
private fun ExploreKindUrlChip(
    kind: ExploreKind,
    infoMap: InfoMap,
    source: io.legado.app.data.entities.BookSource?,
    onOpenExplore: (url: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayText by remember { mutableStateOf(resolveViewName(kind.viewName, kind.title)) }

    LaunchedEffect(kind.viewName) {
        displayText = tryEvalViewName(kind.viewName, source, infoMap, kind.title)
    }

    Text(
        text = displayText,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable {
                val url = kind.url ?: return@clickable
                onOpenExplore(url)
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = kind.style().layout_justifySelf.toTextAlign()
    )
}

@Composable
private fun ExploreKindButtonChip(
    kind: ExploreKind,
    infoMap: InfoMap,
    source: io.legado.app.data.entities.BookSource?,
    sourceJsExtensions: SourceLoginJsExtensions?,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefreshKinds: () -> Unit = {}
) {
    var displayText by remember { mutableStateOf(resolveViewName(kind.viewName, kind.title)) }

    LaunchedEffect(kind.viewName) {
        displayText = tryEvalViewName(kind.viewName, source, infoMap, kind.title)
    }

    Text(
        text = displayText,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable {
                val action = kind.action?.takeIf { it.isNotBlank() } ?: return@clickable
                scope.launch(IO) {
                    evalButtonClick(action, source, infoMap, kind.title, sourceJsExtensions)
                    onRefreshKinds()
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = kind.style().layout_justifySelf.toTextAlign()
    )
}

@Composable
private fun ExploreKindToggleChip(
    kind: ExploreKind,
    infoMap: InfoMap,
    source: io.legado.app.data.entities.BookSource?,
    sourceJsExtensions: SourceLoginJsExtensions?,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefreshKinds: () -> Unit = {}
) {
    val chars = kind.chars?.filterNotNull() ?: listOf("chars", "is null")
    val title = kind.title
    val left = kind.style().layout_justifySelf != "right"

    var currentChar by remember(kind.title) {
        val infoV = infoMap[title]
        mutableStateOf(
            if (infoV.isNullOrEmpty()) {
                (kind.default ?: chars.firstOrNull() ?: "").also { infoMap[title] = it }
            } else {
                infoV
            }
        )
    }

    var displayName by remember { mutableStateOf(resolveViewName(kind.viewName, title)) }

    LaunchedEffect(kind.viewName) {
        displayName = tryEvalViewName(kind.viewName, source, infoMap, title)
    }

    val displayText = if (left) "$currentChar$displayName" else "$displayName$currentChar"

    Text(
        text = displayText,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable {
                val currentIndex = chars.indexOf(currentChar)
                val nextIndex = (currentIndex + 1) % chars.size
                currentChar = chars.getOrNull(nextIndex) ?: ""
                infoMap[title] = currentChar
                scope.launch(IO) {
                    kind.action?.takeIf { it.isNotBlank() }?.let { action ->
                        evalButtonClick(action, source, infoMap, title, sourceJsExtensions)
                    }
                    onRefreshKinds()
                }
            }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = kind.style().layout_justifySelf.toTextAlign()
    )
}

@Composable
private fun ExploreKindTextField(
    kind: ExploreKind,
    infoMap: InfoMap,
    source: io.legado.app.data.entities.BookSource?,
    sourceJsExtensions: SourceLoginJsExtensions?,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefreshKinds: () -> Unit = {}
) {
    var hintText by remember { mutableStateOf(resolveViewName(kind.viewName, kind.title)) }

    LaunchedEffect(kind.viewName) {
        hintText = tryEvalViewName(kind.viewName, source, infoMap, kind.title)
    }

    var textValue by remember { mutableStateOf(infoMap[kind.title] ?: "") }
    var actionJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = textValue,
            onValueChange = { newValue ->
                val old = textValue
                textValue = newValue
                infoMap[kind.title] = newValue
                if (kind.action != null && newValue != old) {
                    actionJob?.cancel()
                    actionJob = scope.launch(IO) {
                        delay(600)
                        evalButtonClick(kind.action!!, source, infoMap, kind.title, sourceJsExtensions)
                        onRefreshKinds()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 0.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { innerTextField ->
                if (textValue.isEmpty()) {
                    Text(
                        text = hintText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                innerTextField()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExploreKindSelectField(
    kind: ExploreKind,
    infoMap: InfoMap,
    source: io.legado.app.data.entities.BookSource?,
    sourceJsExtensions: SourceLoginJsExtensions?,
    scope: kotlinx.coroutines.CoroutineScope,
    onRefreshKinds: () -> Unit = {}
) {
    val chars = kind.chars?.filterNotNull() ?: listOf("chars", "is null")
    var isInitialized by remember { mutableStateOf(false) }

    var selectedValue by remember(kind.title) {
        val infoV = infoMap[kind.title]
        mutableStateOf(
            if (infoV.isNullOrEmpty()) {
                (kind.default ?: chars.firstOrNull() ?: "").also { infoMap[kind.title] = it }
            } else {
                infoV
            }
        )
    }

    var showDropdown by remember { mutableStateOf(false) }

    var label by remember { mutableStateOf(resolveViewName(kind.viewName, kind.title)) }

    LaunchedEffect(kind.viewName) {
        label = tryEvalViewName(kind.viewName, source, infoMap, kind.title)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable { showDropdown = true }
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = selectedValue,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }

    DropdownMenu(
        expanded = showDropdown,
        onDismissRequest = {
            showDropdown = false
            isInitialized = true
        }
    ) {
        chars.forEach { char ->
            DropdownMenuItem(
                text = { Text(char) },
                onClick = {
                    selectedValue = char
                    infoMap[kind.title] = char
                    showDropdown = false
                    isInitialized = true
                    scope.launch(IO) {
                        kind.action?.takeIf { it.isNotBlank() }?.let { action ->
                            evalButtonClick(action, source, infoMap, kind.title, sourceJsExtensions)
                        }
                        onRefreshKinds()
                    }
                }
            )
        }
    }
}

private fun String?.toTextAlign(): TextAlign {
    return when (this) {
        "flex_start" -> TextAlign.Start
        "flex_end" -> TextAlign.End
        "center" -> TextAlign.Center
        else -> TextAlign.Center
    }
}

private fun String?.toArrangement(): Arrangement.Horizontal {
    return when (this) {
        "flex_start" -> Arrangement.Start
        "flex_end" -> Arrangement.End
        "center" -> Arrangement.Center
        else -> Arrangement.Start
    }
}

private fun FlexChildStyle.toModifier(): Modifier {
    var modifier: Modifier = Modifier
    if (layout_flexBasisPercent > 0f) {
        modifier = modifier.fillMaxWidth(layout_flexBasisPercent)
    }
    if (layout_wrapBefore) {
        modifier = modifier.fillMaxWidth()
    }
    return modifier
}
