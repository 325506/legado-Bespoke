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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.help.source.exploreKinds
import io.legado.app.ui.compose.theme.LegadoTheme
import kotlinx.coroutines.Dispatchers.IO
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
                            onSearchBook = onSearchBook
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
    onSearchBook: (BookSourcePart) -> Unit
) {
    var kinds by remember { mutableStateOf<List<ExploreKind>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

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

        AnimatedVisibility(visible = isExpanded && kinds.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                kinds.forEach { kind ->
                    when (kind.type) {
                        ExploreKind.Type.url, ExploreKind.Type.button -> {
                            ExploreKindChip(
                                text = kind.title,
                                onClick = {
                                    if (kind.type == ExploreKind.Type.url) {
                                        val url = kind.url ?: return@ExploreKindChip
                                        if (kind.title.startsWith("ERROR:")) {
                                            return@ExploreKindChip
                                        }
                                        onOpenExplore(source.bookSourceUrl, kind.title, url)
                                    }
                                }
                            )
                        }
                        ExploreKind.Type.toggle -> {
                            val chars = kind.chars?.filterNotNull() ?: listOf("chars", "is null")
                            var currentChar by remember {
                                mutableStateOf(
                                    kind.default ?: chars.firstOrNull() ?: ""
                                )
                            }
                            ExploreKindChip(
                                text = "$currentChar${kind.title}",
                                onClick = {
                                    val currentIndex = chars.indexOf(currentChar)
                                    val nextIndex = (currentIndex + 1) % chars.size
                                    currentChar = chars.getOrNull(nextIndex) ?: ""
                                }
                            )
                        }
                        ExploreKind.Type.text -> {
                            var textValue by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
                                modifier = Modifier
                                    .width(160.dp)
                                    .padding(2.dp),
                                placeholder = { Text(kind.title, fontSize = 12.sp) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall
                            )
                        }
                        ExploreKind.Type.select -> {
                            ExploreKindChip(
                                text = kind.title,
                                onClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreKindChip(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
