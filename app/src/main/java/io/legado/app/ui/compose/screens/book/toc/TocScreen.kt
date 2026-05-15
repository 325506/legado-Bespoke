package io.legado.app.ui.compose.screens.book.toc

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.R
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.Bookmark
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalTxt
import io.legado.app.help.book.isVideo
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.toc.TocViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocScreen(
    viewModel: TocViewModel,
    onBack: () -> Unit,
    onChapterSelected: (index: Int, chapterPos: Int, chapterChanged: Boolean, durVolumeIndex: Int, chapterInVolumeIndex: Int) -> Unit,
    onShowTocRegexDialog: (String?) -> Unit,
    onShowAppLog: () -> Unit,
    onShowBookmarkDialog: (Bookmark, Int) -> Unit,
    onExportBookmark: () -> Unit = {},
    onExportBookmarkMd: () -> Unit = {},
    onReverseToc: () -> Unit = {},
    onSplitLongChapterChanged: (Boolean) -> Unit = {},
    onUseReplaceChanged: (Boolean) -> Unit = {},
    onLoadWordCountChanged: (Boolean) -> Unit = {},
    isSplitLongChapter: Boolean = false,
    isUseReplace: Boolean = false,
    isLoadWordCount: Boolean = false,
    isLocalTxt: Boolean = false
) {
    val context = LocalContext.current
    val book by viewModel.bookData.observeAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchKey by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        stringResource(R.string.chapter_list),
        stringResource(R.string.bookmark)
    )

    Scaffold(
        topBar = {
            if (isSearching) {
                SearchTopBar(
                    searchKey = searchKey,
                    onSearchKeyChange = { newKey ->
                        searchKey = newKey
                        viewModel.searchKey = newKey
                        if (selectedTab == 1) {
                            viewModel.startBookmarkSearch(newKey)
                        } else {
                            viewModel.startChapterListSearch(newKey)
                        }
                    },
                    onClose = {
                        isSearching = false
                        searchKey = ""
                        viewModel.searchKey = ""
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            edgePadding = 0.dp,
                            indicator = { tabPositions ->
                                SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            divider = {},
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            text = title,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChapterListTab(
                    viewModel = viewModel,
                    book = book,
                    searchKey = searchKey,
                    onChapterClick = { chapter ->
                        val durChapterIndex = book?.durChapterIndex ?: 0
                        val chapterChanged = chapter.index != durChapterIndex
                        if (book?.isVideo == true) {
                            val volumes = arrayListOf<BookChapter>()
                            viewModel.chapterListCallBack?.let { cb ->
                                val chapterList = appDb.bookChapterDao.getChapterList(viewModel.bookUrl)
                                chapterList.forEach { ch ->
                                    if (ch.isVolume) volumes.add(ch)
                                }
                            }
                            var chapterInVolumeIndex = 0
                            var durVolumeIndex = 0
                            if (volumes.isNotEmpty()) {
                                for ((index, volume) in volumes.reversed().withIndex()) {
                                    val first = chapter.index
                                    if (volume.index < first) {
                                        chapterInVolumeIndex = first - volume.index - 1
                                        durVolumeIndex = volumes.size - index - 1
                                        break
                                    } else if (volume.index == first) {
                                        chapterInVolumeIndex = 0
                                        durVolumeIndex = volumes.size - index - 1
                                        break
                                    }
                                }
                            } else {
                                chapterInVolumeIndex = chapter.index
                            }
                            onChapterSelected(
                                chapter.index, 0, chapterChanged,
                                durVolumeIndex, chapterInVolumeIndex
                            )
                        } else {
                            onChapterSelected(chapter.index, 0, chapterChanged, 0, 0)
                        }
                    }
                )
                1 -> BookmarkTab(
                    viewModel = viewModel,
                    book = book,
                    searchKey = searchKey,
                    onBookmarkClick = { bookmark ->
                        onChapterSelected(
                            bookmark.chapterIndex,
                            bookmark.chapterPos,
                            true,
                            0,
                            0
                        )
                    },
                    onBookmarkLongClick = { bookmark, pos ->
                        onShowBookmarkDialog(bookmark, pos)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchKey: String,
    onSearchKeyChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null
                )
            }
            TextField(
                value = searchKey,
                onValueChange = onSearchKeyChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.search)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
private fun ChapterListTab(
    viewModel: TocViewModel,
    book: Book?,
    searchKey: String?,
    onChapterClick: (BookChapter) -> Unit
) {
    val context = LocalContext.current
    var chapters by remember { mutableStateOf<List<BookChapter>>(emptyList()) }
    var cacheFileNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val durChapterIndex = book?.durChapterIndex ?: 0

    LaunchedEffect(book, searchKey) {
        book?.let { b ->
            withContext(Dispatchers.IO) {
                val end = b.simulatedTotalChapterNum() - 1
                val list = if (searchKey.isNullOrBlank()) {
                    appDb.bookChapterDao.getChapterList(viewModel.bookUrl, 0, end)
                } else {
                    appDb.bookChapterDao.search(viewModel.bookUrl, searchKey, 0, end)
                }
                val cacheFiles = BookHelp.getChapterFiles(b)
                withContext(Dispatchers.Main) {
                    chapters = list
                    cacheFileNames = cacheFiles
                }
            }
        }
    }

    LaunchedEffect(chapters) {
        if (chapters.isNotEmpty() && searchKey.isNullOrBlank()) {
            var scrollPos = 0
            chapters.forEachIndexed { index, chapter ->
                if (chapter.index >= durChapterIndex) return@forEachIndexed
                scrollPos = index
            }
            if (scrollPos > 0) {
                listState.scrollToItem(scrollPos)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        book?.let { b ->
            ChapterInfoBar(
                book = b,
                onScrollToTop = {
                    scope.launch { listState.scrollToItem(0) }
                },
                onScrollToBottom = {
                    scope.launch {
                        if (chapters.isNotEmpty()) {
                            listState.scrollToItem(chapters.size - 1)
                        }
                    }
                },
                onScrollToCurrent = {
                    scope.launch {
                        var pos = 0
                        chapters.forEachIndexed { index, chapter ->
                            if (chapter.index >= durChapterIndex) return@forEachIndexed
                            pos = index
                        }
                        listState.scrollToItem(pos)
                    }
                }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(chapters, key = { it.index }) { chapter ->
                ChapterItem(
                    chapter = chapter,
                    isDur = chapter.index == durChapterIndex,
                    isLocal = book?.isLocal == true,
                    isCached = cacheFileNames.contains(chapter.getFileName()),
                    useReplace = AppConfig.tocUiUseReplace && (book?.getUseReplaceRule() == true),
                    showWordCount = AppConfig.tocCountWords,
                    book = book,
                    onClick = { onChapterClick(chapter) }
                )
            }
        }
    }
}

@Composable
private fun ChapterInfoBar(
    book: Book,
    onScrollToTop: () -> Unit,
    onScrollToBottom: () -> Unit,
    onScrollToCurrent: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onScrollToTop, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "${book.durChapterTitle}(${book.durChapterIndex + 1}/${book.simulatedTotalChapterNum()})",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onScrollToCurrent)
                    .padding(horizontal = 8.dp)
            )
            IconButton(onClick = onScrollToBottom, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ChapterItem(
    chapter: BookChapter,
    isDur: Boolean,
    isLocal: Boolean,
    isCached: Boolean,
    useReplace: Boolean,
    showWordCount: Boolean,
    book: Book?,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var displayTitle by remember { mutableStateOf(chapter.title) }

    LaunchedEffect(chapter.title, useReplace) {
        withContext(Dispatchers.IO) {
            val replaceRules = book?.let {
                ContentProcessor.get(it.name, it.origin).getTitleReplaceRules()
            }
            val title = chapter.getDisplayTitle(
                replaceRules = replaceRules,
                useReplace = useReplace,
                replaceBook = book?.toReplaceBook()
            )
            withContext(Dispatchers.Main) {
                displayTitle = title
            }
        }
    }

    val bgColor = if (chapter.isVolume) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        Color.Transparent
    }

    val textColor = if (isDur) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!chapter.tag.isNullOrEmpty() || (showWordCount && !chapter.wordCount.isNullOrEmpty() && !chapter.isVolume)) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tagText = chapter.tag
                        if (!tagText.isNullOrEmpty()) {
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val wordCountText = chapter.wordCount
                        if (showWordCount && !wordCountText.isNullOrEmpty() && !chapter.isVolume) {
                            Text(
                                text = wordCountText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (chapter.isVip && !chapter.isPay) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.size(4.dp))
            }
            if (isDur) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else if (!isLocal && !chapter.isVolume && !isCached) {
                Icon(
                    painter = painterResource(R.drawable.ic_outline_cloud_24),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BookmarkTab(
    viewModel: TocViewModel,
    book: Book?,
    searchKey: String?,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkLongClick: (Bookmark, Int) -> Unit
) {
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    val durChapterIndex = book?.durChapterIndex ?: 0
    val listState = rememberLazyListState()

    LaunchedEffect(book, searchKey) {
        book?.let { b ->
            try {
                val flow = if (searchKey.isNullOrBlank()) {
                    appDb.bookmarkDao.flowByBook(b.name, b.author)
                } else {
                    appDb.bookmarkDao.flowSearch(b.name, b.author, searchKey)
                }
                flow.collect { list ->
                    bookmarks = list
                }
            } catch (e: Exception) {
                io.legado.app.constant.AppLog.put("目录界面获取书签数据失败\n${e.localizedMessage}", e)
            }
        }
    }

    LaunchedEffect(bookmarks) {
        if (bookmarks.isNotEmpty()) {
            var scrollPos = 0
            bookmarks.forEachIndexed { index, bookmark ->
                if (bookmark.chapterIndex >= durChapterIndex) return@forEachIndexed
                scrollPos = index
            }
            if (scrollPos > 0) {
                listState.scrollToItem(scrollPos)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(bookmarks, key = { it.time }) { bookmark ->
            BookmarkItem(
                bookmark = bookmark,
                onClick = { onBookmarkClick(bookmark) },
                onLongClick = { onBookmarkLongClick(bookmark, bookmarks.indexOf(bookmark)) }
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = bookmark.chapterName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (bookmark.bookText.isNotEmpty()) {
                Text(
                    text = bookmark.bookText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (bookmark.content.isNotEmpty()) {
                Text(
                    text = bookmark.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
