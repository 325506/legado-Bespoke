package io.legado.app.ui.compose.screens.bookshelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.cache.CacheActivity
import io.legado.app.ui.book.import.local.ImportBookComposeActivity
import io.legado.app.ui.book.import.remote.RemoteBookActivity
import io.legado.app.ui.book.manage.BookshelfManageActivity
import io.legado.app.ui.book.search.SearchComposeActivity
import io.legado.app.utils.cnCompare
import io.legado.app.utils.startActivity
import io.legado.app.utils.toTimeAgo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: BookshelfViewModel,
    onBookClick: (Book) -> Unit = {},
    onBookLongClick: (Book) -> Unit = {},
    onGroupClick: (BookGroup) -> Unit = {},
    onGroupLongClick: (BookGroup) -> Unit = {},
    onUpdateToc: (List<Book>, Boolean) -> Unit = { _, _ -> },
    onAddUrl: () -> Unit = {},
    onExportBookshelf: (List<Book>) -> Unit = {},
    onImportBookshelf: () -> Unit = {},
    onShowLog: () -> Unit = {},
    onShowGroupManage: () -> Unit = {},
    onShowBookshelfConfig: () -> Unit = {},
    position: Int = 0
) {
    val context = LocalContext.current
    val bookGroups by viewModel.bookGroups.collectAsState()
    val books by viewModel.books.collectAsState()
    val groupId by viewModel.groupId.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val layoutConfig by viewModel.layoutConfig.collectAsState()
    val bookshelfLayout = layoutConfig.bookshelfLayout
    val bookGroupStyle = layoutConfig.bookGroupStyle
    val showUnread = layoutConfig.showUnread
    val showLastUpdateTime = layoutConfig.showLastUpdateTime
    val showBookname = layoutConfig.showBookname

    var showMenu by remember { mutableStateOf(false) }

    val title = if (groupId == BookGroup.IdRoot) {
        context.getString(R.string.bookshelf)
    } else {
        val group = bookGroups.firstOrNull { it.groupId == groupId }
        if (group != null) {
            "${context.getString(R.string.bookshelf)}(${group.groupName})"
        } else {
            context.getString(R.string.bookshelf)
        }
    }

    if (bookGroupStyle == 1) {
        BookshelfStyle2(
            viewModel = viewModel,
            title = title,
            books = books,
            bookGroups = bookGroups,
            groupId = groupId,
            isRefreshing = isRefreshing,
            bookshelfLayout = bookshelfLayout,
            showUnread = showUnread,
            showLastUpdateTime = showLastUpdateTime,
            showBookname = showBookname,
            showMenu = showMenu,
            onShowMenuChange = { showMenu = it },
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            onGroupClick = onGroupClick,
            onGroupLongClick = onGroupLongClick,
            onUpdateToc = onUpdateToc,
            onAddUrl = onAddUrl,
            onExportBookshelf = onExportBookshelf,
            onImportBookshelf = onImportBookshelf,
            onShowLog = onShowLog,
            onShowGroupManage = onShowGroupManage,
            onShowBookshelfConfig = onShowBookshelfConfig
        )
    } else {
        BookshelfStyle1(
            viewModel = viewModel,
            bookGroups = bookGroups,
            isRefreshing = isRefreshing,
            bookshelfLayout = bookshelfLayout,
            showUnread = showUnread,
            showLastUpdateTime = showLastUpdateTime,
            showBookname = showBookname,
            showMenu = showMenu,
            onShowMenuChange = { showMenu = it },
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            onUpdateToc = onUpdateToc,
            onAddUrl = onAddUrl,
            onExportBookshelf = onExportBookshelf,
            onImportBookshelf = onImportBookshelf,
            onShowLog = onShowLog,
            onShowGroupManage = onShowGroupManage,
            onShowBookshelfConfig = onShowBookshelfConfig
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookshelfStyle1(
    viewModel: BookshelfViewModel,
    bookGroups: List<BookGroup>,
    isRefreshing: Boolean,
    bookshelfLayout: Int,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    showBookname: Int,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    onUpdateToc: (List<Book>, Boolean) -> Unit,
    onAddUrl: () -> Unit = {},
    onExportBookshelf: (List<Book>) -> Unit = {},
    onImportBookshelf: () -> Unit = {},
    onShowLog: () -> Unit = {},
    onShowGroupManage: () -> Unit = {},
    onShowBookshelfConfig: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (bookGroups.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = minOf(AppConfig.saveTabPosition, bookGroups.size - 1).coerceAtLeast(0),
        pageCount = { bookGroups.size }
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(context.getString(R.string.bookshelf)) },
                    actions = {
                        IconButton(onClick = {
                            context.startActivity<SearchComposeActivity>()
                        }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                        IconButton(onClick = {
                            onShowBookshelfConfig()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_view_quilt),
                                contentDescription = null
                            )
                        }
                        Box {
                            IconButton(onClick = { onShowMenuChange(true) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = null)
                            }
                            BookshelfMenu(
                                expanded = showMenu,
                                onDismiss = { onShowMenuChange(false) },
                                groupId = bookGroups.getOrNull(pagerState.currentPage)?.groupId
                                    ?: BookGroup.IdRoot,
                                books = emptyList(),
                                onlyUpdateRead = bookGroups.getOrNull(pagerState.currentPage)?.onlyUpdateRead
                                    ?: false,
                                onUpdateToc = onUpdateToc,
                                onShowBookshelfConfig = onShowBookshelfConfig,
                                onShowAddUrlDialog = onAddUrl,
                                onShowImportBookshelfDialog = onImportBookshelf,
                                onExportBookshelf = onExportBookshelf,
                                onShowLog = onShowLog,
                                onShowGroupManage = onShowGroupManage
                            )
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    bookGroups.forEachIndexed { index, group ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                                AppConfig.saveTabPosition = index
                            },
                            text = { Text(group.groupName) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            val group = bookGroups.getOrNull(page)
            if (group != null) {
                BooksPage(
                    groupId = group.groupId,
                    bookSort = group.getRealBookSort(),
                    enableRefresh = group.enableRefresh,
                    onlyUpdateRead = group.onlyUpdateRead,
                    bookshelfLayout = bookshelfLayout,
                    showUnread = showUnread,
                    showLastUpdateTime = showLastUpdateTime,
                    showBookname = showBookname,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick,
                    onUpdateToc = onUpdateToc
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BooksPage(
    groupId: Long,
    bookSort: Int,
    enableRefresh: Boolean,
    onlyUpdateRead: Boolean,
    bookshelfLayout: Int,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    showBookname: Int,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    onUpdateToc: (List<Book>, Boolean) -> Unit
) {
    val context = LocalContext.current
    val books = remember(groupId) {
        io.legado.app.data.appDb.bookDao.flowByGroup(groupId)
    }.collectAsState(initial = emptyList())

    val sortedBooks = remember(books.value, bookSort) {
        val list = books.value
        when (bookSort) {
            1 -> list.sortedByDescending { it.latestChapterTime }
            2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
            3 -> list.sortedBy { it.order }
            4 -> list.sortedByDescending { kotlin.math.max(it.latestChapterTime, it.durChapterTime) }
            5 -> list.sortedWith { o1, o2 -> o1.author.cnCompare(o2.author) }
            else -> list.sortedByDescending { it.durChapterTime }
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (enableRefresh && sortedBooks.isNotEmpty()) {
                isRefreshing = true
                onUpdateToc(sortedBooks, onlyUpdateRead)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (sortedBooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = context.getString(R.string.bookshelf_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (bookshelfLayout >= 2) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(bookshelfLayout),
                    contentPadding = PaddingValues(
                        start = 8.dp, end = 8.dp,
                        top = 8.dp, bottom = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(sortedBooks, key = { it.bookUrl }) { book ->
                        BookGridItem(
                            book = book,
                            showBookname = showBookname,
                            showUnread = showUnread,
                            onClick = { onBookClick(book) },
                            onLongClick = { onBookLongClick(book) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(sortedBooks, key = { it.bookUrl }) { book ->
                        BookListItem(
                            book = book,
                            compact = bookshelfLayout == 1,
                            showUnread = showUnread,
                            showLastUpdateTime = showLastUpdateTime,
                            onClick = { onBookClick(book) },
                            onLongClick = { onBookLongClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookshelfStyle2(
    viewModel: BookshelfViewModel,
    title: String,
    books: List<Book>,
    bookGroups: List<BookGroup>,
    groupId: Long,
    isRefreshing: Boolean,
    bookshelfLayout: Int,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    showBookname: Int,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBookClick: (Book) -> Unit,
    onBookLongClick: (Book) -> Unit,
    onGroupClick: (BookGroup) -> Unit,
    onGroupLongClick: (BookGroup) -> Unit,
    onUpdateToc: (List<Book>, Boolean) -> Unit,
    onAddUrl: () -> Unit = {},
    onExportBookshelf: (List<Book>) -> Unit = {},
    onImportBookshelf: () -> Unit = {},
    onShowLog: () -> Unit = {},
    onShowGroupManage: () -> Unit = {},
    onShowBookshelfConfig: () -> Unit = {}
) {
    val context = LocalContext.current

    val items: List<Any> = if (groupId == BookGroup.IdRoot) {
        bookGroups + books
    } else {
        books
    }

    val enableRefresh = if (groupId == BookGroup.IdRoot) {
        true
    } else {
        bookGroups.firstOrNull { it.groupId == groupId }?.enableRefresh ?: true
    }

    val onlyUpdateRead = if (groupId == BookGroup.IdRoot) {
        false
    } else {
        bookGroups.firstOrNull { it.groupId == groupId }?.onlyUpdateRead ?: false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                actions = {
                    IconButton(onClick = {
                        context.startActivity<SearchComposeActivity>()
                    }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    IconButton(onClick = {
                        onShowBookshelfConfig()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_view_quilt),
                            contentDescription = null
                        )
                    }
                    Box {
                        IconButton(onClick = { onShowMenuChange(true) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                        BookshelfMenu(
                            expanded = showMenu,
                            onDismiss = { onShowMenuChange(false) },
                            groupId = groupId,
                            books = books,
                            onlyUpdateRead = onlyUpdateRead,
                            onUpdateToc = onUpdateToc,
                            onShowBookshelfConfig = onShowBookshelfConfig,
                            onShowAddUrlDialog = onAddUrl,
                            onShowImportBookshelfDialog = onImportBookshelf,
                            onExportBookshelf = onExportBookshelf,
                            onShowLog = onShowLog,
                            onShowGroupManage = onShowGroupManage
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (enableRefresh && items.isNotEmpty()) {
                    viewModel.setRefreshing(true)
                    onUpdateToc(books, onlyUpdateRead)
                    viewModel.setRefreshing(false)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.bookshelf_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (bookshelfLayout >= 2) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(bookshelfLayout),
                        contentPadding = PaddingValues(
                            start = 8.dp, end = 8.dp,
                            top = 8.dp, bottom = 8.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items, key = { item ->
                            when (item) {
                                is Book -> item.bookUrl
                                is BookGroup -> "group_${item.groupId}"
                                else -> item.hashCode()
                            }
                        }) { item ->
                            when (item) {
                                is BookGroup -> GroupGridItem(
                                    group = item,
                                    showBookname = showBookname,
                                    onClick = { onGroupClick(item) },
                                    onLongClick = { onGroupLongClick(item) }
                                )
                                is Book -> BookGridItem(
                                    book = item,
                                    showBookname = showBookname,
                                    showUnread = showUnread,
                                    onClick = { onBookClick(item) },
                                    onLongClick = { onBookLongClick(item) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(items, key = { item ->
                            when (item) {
                                is Book -> item.bookUrl
                                is BookGroup -> "group_${item.groupId}"
                                else -> item.hashCode()
                            }
                        }) { item ->
                            when (item) {
                                is BookGroup -> GroupListItem(
                                    group = item,
                                    onClick = { onGroupClick(item) },
                                    onLongClick = { onGroupLongClick(item) }
                                )
                                is Book -> BookListItem(
                                    book = item,
                                    compact = bookshelfLayout == 1,
                                    showUnread = viewModel.showUnread,
                                    showLastUpdateTime = viewModel.showLastUpdateTime,
                                    onClick = { onBookClick(item) },
                                    onLongClick = { onBookLongClick(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookListItem(
    book: Book,
    compact: Boolean = false,
    showUnread: Boolean = true,
    showLastUpdateTime: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AsyncImage(
            model = book.getDisplayCover(),
            contentDescription = book.name,
            modifier = Modifier
                .size(width = 66.dp, height = 90.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.image_cover_default),
            error = painterResource(R.drawable.image_cover_default)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .align(Alignment.CenterVertically)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showUnread && !book.isLocal) {
                    val unread = book.getUnreadChapterNum()
                    if (unread > 0) {
                        Badge {
                            Text(
                                text = if (unread > 99) "99+" else unread.toString(),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_author),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = book.author ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showLastUpdateTime) {
                    if (book.durChapterTime > 0) {
                        Text(
                            text = book.durChapterTime.toTimeAgo(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_history),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = book.durChapterTitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_last),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = book.latestChapterTitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BookGridItem(
    book: Book,
    showBookname: Int = 0,
    showUnread: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box {
            AsyncImage(
                model = book.getDisplayCover(),
                contentDescription = book.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.image_cover_default),
                error = painterResource(R.drawable.image_cover_default)
            )
            if (showUnread && !book.isLocal) {
                val unread = book.getUnreadChapterNum()
                if (unread > 0) {
                    Badge(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = if (unread > 99) "99+" else unread.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        if (showBookname != 1) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun GroupListItem(
    group: BookGroup,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AsyncImage(
            model = group.cover,
            contentDescription = group.groupName,
            modifier = Modifier
                .size(width = 66.dp, height = 90.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.image_cover_default),
            error = painterResource(R.drawable.image_cover_default)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GroupGridItem(
    group: BookGroup,
    showBookname: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AsyncImage(
            model = group.cover,
            contentDescription = group.groupName,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.image_cover_default),
            error = painterResource(R.drawable.image_cover_default)
        )
        if (showBookname != 1) {
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun BookshelfMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    groupId: Long,
    books: List<Book>,
    onlyUpdateRead: Boolean,
    onUpdateToc: (List<Book>, Boolean) -> Unit,
    onShowBookshelfConfig: () -> Unit = {},
    onShowAddUrlDialog: () -> Unit = {},
    onShowImportBookshelfDialog: () -> Unit = {},
    onExportBookshelf: (List<Book>) -> Unit = {},
    onShowLog: () -> Unit = {},
    onShowGroupManage: () -> Unit = {}
) {
    val context = LocalContext.current

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(context.getString(R.string.update_toc)) },
            onClick = {
                onUpdateToc(books, onlyUpdateRead)
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.book_local)) },
            onClick = {
                context.startActivity<ImportBookComposeActivity>()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.add_remote_book)) },
            onClick = {
                context.startActivity<RemoteBookActivity>()
                onDismiss()
            },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.add_url)) },
            onClick = {
                onShowAddUrlDialog()
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_add_online), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.bookshelf_management)) },
            onClick = {
                context.startActivity<BookshelfManageActivity> {
                    putExtra("groupId", groupId)
                }
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_arrange), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.cache_export)) },
            onClick = {
                context.startActivity<CacheActivity> {
                    putExtra("groupId", groupId)
                }
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_download_line), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.group_manage)) },
            onClick = {
                onShowGroupManage()
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_groups), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.bookshelf_layout)) },
            onClick = {
                onShowBookshelfConfig()
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_view_quilt), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.export_bookshelf)) },
            onClick = {
                onExportBookshelf(books)
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_export), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.import_bookshelf)) },
            onClick = {
                onShowImportBookshelfDialog()
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_import), contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.log)) },
            onClick = {
                onShowLog()
                onDismiss()
            },
            leadingIcon = { Icon(painterResource(R.drawable.ic_cfg_about), contentDescription = null) }
        )
    }
}
