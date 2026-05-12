package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.legado.app.R
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssScreen(
    navController: androidx.navigation.NavHostController? = null,
    viewModel: RssScreenViewModel = viewModel(),
    onOpenRss: (RssSource) -> Unit = {},
    onEdit: (RssSource) -> Unit = {},
    onToTop: (RssSource) -> Unit = {},
    onLogin: (RssSource) -> Unit = {},
    onDel: (RssSource) -> Unit = {},
    onDisable: (RssSource) -> Unit = {},
    onRuleSubscriptionClick: () -> Unit = {},
    onReadRecord: () -> Unit = {},
    onRssConfig: () -> Unit = {},
    onRssStar: () -> Unit = {}
) {
    val context = LocalContext.current
    val rssSources = viewModel.rssSources
    val searchQuery = viewModel.searchQuery
    val groups = viewModel.groups
    var showMenu by remember { mutableStateOf(false) }
    var showGroupFilterMenu by remember { mutableStateOf(false) }
    var sourceMenuSource by remember { mutableStateOf<RssSource?>(null) }

    LegadoTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = context.getString(R.string.rss),
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
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            modifier = Modifier
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        context.getString(R.string.rss),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    RssGridItem(
                        name = context.getString(R.string.rule_subscription),
                        iconPainter = painterResource(R.drawable.image_legado),
                        onClick = onRuleSubscriptionClick
                    )
                }
                items(
                    items = rssSources,
                    key = { it.sourceUrl }
                ) { source ->
                    RssGridItem(
                        name = source.sourceName,
                        iconUrl = source.sourceIcon,
                        onClick = { onOpenRss(source) },
                        onLongClick = { sourceMenuSource = source }
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("阅读记录") },
                onClick = {
                    showMenu = false
                    onReadRecord()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("订阅源管理") },
                onClick = {
                    showMenu = false
                    onRssConfig()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("收藏") },
                onClick = {
                    showMenu = false
                    onRssStar()
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
                Divider()
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
                onEdit = onEdit,
                onToTop = onToTop,
                onLogin = onLogin,
                onDel = onDel,
                onDisable = onDisable
            )
        }
    }
}

@Composable
private fun RssGridItem(
    name: String,
    iconPainter: Painter? = null,
    iconUrl: String? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(8.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.image_rss),
                error = painterResource(R.drawable.image_rss)
            )
        } else if (iconPainter != null) {
            Image(
                painter = iconPainter,
                contentDescription = name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SourceDropdownMenu(
    source: RssSource,
    onDismiss: () -> Unit,
    onEdit: (RssSource) -> Unit,
    onToTop: (RssSource) -> Unit,
    onLogin: (RssSource) -> Unit,
    onDel: (RssSource) -> Unit,
    onDisable: (RssSource) -> Unit
) {
    val hasLogin = !source.loginUrl.isNullOrBlank()

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text("编辑") },
            onClick = {
                onDismiss()
                onEdit(source)
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("置顶") },
            onClick = {
                onDismiss()
                onToTop(source)
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        if (hasLogin) {
            DropdownMenuItem(
                text = { Text("登录") },
                onClick = {
                    onDismiss()
                    onLogin(source)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
        DropdownMenuItem(
            text = { Text("删除") },
            onClick = {
                onDismiss()
                onDel(source)
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("禁用") },
            onClick = {
                onDismiss()
                onDisable(source)
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
    }
}
