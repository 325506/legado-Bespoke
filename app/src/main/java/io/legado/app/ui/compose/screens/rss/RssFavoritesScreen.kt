package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.RssStar
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme
import kotlinx.coroutines.launch

sealed class DeleteConfirmDialog {
    data class Star(val star: RssStar) : DeleteConfirmDialog()
    data class Group(val group: String) : DeleteConfirmDialog()
    object All : DeleteConfirmDialog()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssFavoritesScreen(
    viewModel: RssFavoritesComposeViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onReadRss: (RssStar) -> Unit = {},
    onDeleteStar: (RssStar) -> Unit = {}
) {
    val context = LocalContext.current
    val groupList by viewModel.groupList.collectAsState()
    val favoritesMap by viewModel.favoritesMap.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf<DeleteConfirmDialog?>(null) }

    val pagerState = rememberPagerState(pageCount = { groupList.size })
    val coroutineScope = rememberCoroutineScope()

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = context.getString(R.string.favorites),
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
                if (groupList.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        groupList.forEachIndexed { index, group ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(group, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val group = groupList.getOrNull(page) ?: return@HorizontalPager
                    val favorites = favoritesMap[group] ?: emptyList()
                    RssFavoritesList(
                        favorites = favorites,
                        onReadRss = onReadRss,
                        onDeleteStar = { star ->
                            deleteDialog = DeleteConfirmDialog.Star(star)
                        }
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            groupList.forEachIndexed { index, group ->
                DropdownMenuItem(
                    text = { Text(group) },
                    onClick = {
                        showMenu = false
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(context.getString(R.string.delete_select_group)) },
                onClick = {
                    showMenu = false
                    val group = groupList.getOrNull(pagerState.currentPage) ?: return@DropdownMenuItem
                    deleteDialog = DeleteConfirmDialog.Group(group)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.delete_all)) },
                onClick = {
                    showMenu = false
                    deleteDialog = DeleteConfirmDialog.All
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }

        when (val dialog = deleteDialog) {
            is DeleteConfirmDialog.Star -> {
                AlertDialog(
                    onDismissRequest = { deleteDialog = null },
                    title = { Text(context.getString(R.string.draw)) },
                    text = { Text(context.getString(R.string.sure_del) + "\n<" + dialog.star.title + ">") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteStar(dialog.star)
                            deleteDialog = null
                        }) {
                            Text(context.getString(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteDialog = null }) {
                            Text(context.getString(android.R.string.cancel))
                        }
                    }
                )
            }
            is DeleteConfirmDialog.Group -> {
                AlertDialog(
                    onDismissRequest = { deleteDialog = null },
                    title = { Text(context.getString(R.string.draw)) },
                    text = { Text(context.getString(R.string.sure_del) + "\n<" + dialog.group + ">" + context.getString(R.string.group)) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteGroup(dialog.group)
                            deleteDialog = null
                        }) {
                            Text(context.getString(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteDialog = null }) {
                            Text(context.getString(android.R.string.cancel))
                        }
                    }
                )
            }
            DeleteConfirmDialog.All -> {
                AlertDialog(
                    onDismissRequest = { deleteDialog = null },
                    title = { Text(context.getString(R.string.draw)) },
                    text = { Text(context.getString(R.string.sure_del) + "\n<" + context.getString(R.string.all) + ">" + context.getString(R.string.favorite)) },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteAll()
                            deleteDialog = null
                        }) {
                            Text(context.getString(android.R.string.ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deleteDialog = null }) {
                            Text(context.getString(android.R.string.cancel))
                        }
                    }
                )
            }
            null -> {}
        }
    }
}

@Composable
fun RssFavoritesList(
    favorites: List<RssStar>,
    onReadRss: (RssStar) -> Unit,
    onDeleteStar: (RssStar) -> Unit
) {
    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("暂无收藏", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(favorites, key = { it.origin + it.link }) { rssStar ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onReadRss(rssStar) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = rssStar.title ?: "",
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = rssStar.origin ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { onDeleteStar(rssStar) }) {
                        Icon(Icons.Default.Info, contentDescription = "删除")
                    }
                }
            }
        }
    }
}
