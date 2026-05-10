package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.RssArticle
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSortScreen(
    viewModel: RssSortComposeViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onReadRss: (RssArticle) -> Unit = {},
    onEditSource: (String) -> Unit = {},
    onLogin: (String) -> Unit = {},
    onSetVariable: () -> Unit = {},
    onReadRecord: () -> Unit = {}
) {
    val context = LocalContext.current
    val sortList by viewModel.sortList.collectAsState()
    val sourceName = viewModel.sourceName ?: ""
    var showMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { sortList.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(sortList) {
        if (pagerState.pageCount > 0 && pagerState.currentPage >= pagerState.pageCount) {
            pagerState.scrollToPage(0)
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = if (sortList.size == 1 && sortList.first().first.isNotEmpty()) {
                        viewModel.searchKey ?: sortList.first().first
                    } else {
                        sourceName
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                        }
                    },
                    actions = {
                        if (!viewModel.rssSource?.searchUrl.isNullOrBlank()) {
                            IconButton(onClick = { showSearch = !showSearch }) {
                                Icon(Icons.Default.Search, contentDescription = context.getString(R.string.search))
                            }
                        }
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
                if (showSearch && !viewModel.rssSource?.searchUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(context.getString(R.string.search)) },
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (searchQuery.isNotBlank()) {
                                viewModel.searchKey = searchQuery
                                viewModel.updateSortList()
                                showSearch = false
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    }
                }

                if (sortList.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 16.dp
                    ) {
                        sortList.forEachIndexed { index, pair ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                },
                                text = { Text(pair.first, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val sort = sortList.getOrNull(page) ?: return@HorizontalPager
                    RssArticlesPage(
                        sortName = sort.first,
                        sortUrl = sort.second,
                        searchKey = viewModel.searchKey,
                        rssSource = viewModel.rssSource,
                        articleStyle = viewModel.articleStyle,
                        onReadRss = onReadRss
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (!viewModel.rssSource?.loginUrl.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text(context.getString(R.string.login)) },
                    onClick = {
                        showMenu = false
                        viewModel.rssSource?.sourceUrl?.let(onLogin)
                    },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
            DropdownMenuItem(
                text = { Text(context.getString(R.string.refresh_sort)) },
                onClick = {
                    showMenu = false
                    viewModel.clearSortCache { viewModel.updateSortList() }
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.set_source_variable)) },
                onClick = {
                    showMenu = false
                    onSetVariable()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.edit_source)) },
                onClick = {
                    showMenu = false
                    viewModel.rssSource?.sourceUrl?.let(onEditSource)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.switchLayout)) },
                onClick = {
                    showMenu = false
                    viewModel.switchLayout()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.read_record)) },
                onClick = {
                    showMenu = false
                    onReadRecord()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(context.getString(R.string.clear)) },
                onClick = {
                    showMenu = false
                    viewModel.clearArticles()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
fun RssArticlesPage(
    sortName: String,
    sortUrl: String,
    searchKey: String?,
    rssSource: io.legado.app.data.entities.RssSource?,
    articleStyle: Int,
    onReadRss: (RssArticle) -> Unit
) {
    val viewModel: RssArticlesComposeViewModel = viewModel()
    val articles by viewModel.articles.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sortName, sortUrl) {
        viewModel.init(sortName, sortUrl, searchKey)
        rssSource?.let { viewModel.loadArticles(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (articleStyle) {
            2 -> {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(articles.size) { index ->
                        RssArticleItemGrid(articles[index], onReadRss)
                    }
                    if (hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                rssSource?.let { viewModel.loadMore(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            4 -> {
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(articles.size) { index ->
                        RssArticleItemGrid(articles[index], onReadRss)
                    }
                    if (hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                rssSource?.let { viewModel.loadMore(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(articles, key = { it.link + it.sort }) { article ->
                        RssArticleItemList(article, onReadRss)
                    }
                    if (hasMore) {
                        item {
                            LaunchedEffect(Unit) {
                                rssSource?.let { viewModel.loadMore(it) }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }

        if (loading && articles.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun RssArticleItemList(
    article: RssArticle,
    onClick: (RssArticle) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(article) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = article.title ?: "",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    color = if (article.read) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.pubDate ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (!article.image.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(16.dp))
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 68.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
fun RssArticleItemGrid(
    article: RssArticle,
    onClick: (RssArticle) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(article) },
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            if (!article.image.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Text(
                text = article.title ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
                color = if (article.read) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = article.pubDate ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
