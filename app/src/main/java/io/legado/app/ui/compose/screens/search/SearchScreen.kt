package io.legado.app.ui.compose.screens.search

import android.app.Application
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.SearchBook
import io.legado.app.data.entities.SearchKeyword
import io.legado.app.help.book.isNotShelf
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.ui.book.search.SearchViewModel
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.putPrefBoolean
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialKey: String? = null,
    initialSearchScope: String? = null,
    onBack: () -> Unit = {},
    onBookClick: (name: String, author: String, bookUrl: String) -> Unit = { _, _, _ -> },
    onSourceManage: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var searchQuery by remember { mutableStateOf(initialKey ?: "") }
    var isSearchFieldFocused by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSearchScopeDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showEmptyResultDialog by remember { mutableStateOf(false) }
    var emptyResultMessage by remember { mutableStateOf("") }

    val isSearching by viewModel.isSearchLiveData.observeAsState()
    val searchBooks by viewModel.searchBookLiveData.observeAsState()
    val searchFinishEmpty by viewModel.searchFinishLiveData.observeAsState()

    val bookshelfKeys = remember { mutableStateListOf<String>() }
    val historyKeys = remember { mutableStateListOf<SearchKeyword>() }
    val matchedBooks = remember { mutableStateListOf<Book>() }
    val groups = remember { mutableStateListOf<String>() }

    var isManualStopSearch by remember { mutableStateOf(false) }
    var precisionSearch by remember { mutableStateOf(context.getPrefBoolean(PreferKey.precisionSearch)) }

    val listState = rememberLazyListState()

    LaunchedEffect(initialSearchScope) {
        initialSearchScope?.let {
            viewModel.searchScope.update(it, postValue = false, save = false)
        }
    }

    LaunchedEffect(Unit) {
        appDb.bookSourceDao.flowEnabledGroups().collect { groupList ->
            groups.clear()
            groups.addAll(groupList)
        }
    }

    LaunchedEffect(Unit) {
        appDb.bookDao.flowAll().mapLatest { books ->
            val keys = arrayListOf<String>()
            books.filterNot { it.isNotShelf }.forEach {
                keys.add("${it.name}-${it.author}")
                keys.add(it.name)
                keys.add(it.bookUrl)
            }
            keys
        }.catch {
            AppLog.put("搜索界面获取书籍列表失败\n${it.localizedMessage}", it)
        }.collect {
            bookshelfKeys.clear()
            bookshelfKeys.addAll(it)
        }
    }

    LaunchedEffect(searchQuery) {
        delay(300)
        if (searchQuery.isBlank()) {
            appDb.searchKeywordDao.flowByTime()
        } else {
            appDb.searchKeywordDao.flowSearch(searchQuery)
        }.catch {
            AppLog.put("搜索界面获取搜索历史数据失败\n${it.localizedMessage}", it)
        }.flowOn(IO).conflate().collect {
            historyKeys.clear()
            historyKeys.addAll(it)
        }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(searchQuery) {
        delay(300)
        if (searchQuery.isBlank()) {
            matchedBooks.clear()
        } else {
            appDb.bookDao.flowSearch(searchQuery).conflate().collect {
                matchedBooks.clear()
                matchedBooks.addAll(it)
            }
        }
    }

    LaunchedEffect(searchFinishEmpty) {
        if (searchFinishEmpty == true && !viewModel.searchScope.isAll()) {
            val displayScope = viewModel.searchScope.display
            if (precisionSearch) {
                emptyResultMessage = "${displayScope}分组搜索结果为空，是否关闭精准搜索？"
            } else {
                emptyResultMessage = "${displayScope}分组搜索结果为空，是否切换到全部分组？"
            }
            showEmptyResultDialog = true
        }
    }

    LaunchedEffect(viewModel.searchScope.stateLiveData) {
        viewModel.searchScope.stateLiveData.observeForever {
            if (searchBooks?.isNotEmpty() == true && searchQuery.isNotBlank()) {
                viewModel.searchKey = ""
                viewModel.search(searchQuery)
            }
        }
    }

    val showInputHelp = isSearchFieldFocused && isSearching != true && searchBooks?.isEmpty() != false

    fun isInBookshelf(book: SearchBook): Boolean {
        val name = book.name
        val author = book.author
        val bookUrl = book.bookUrl
        val key = if (author.isNotBlank()) "$name-$author" else name
        return bookshelfKeys.contains(key) || bookshelfKeys.contains(bookUrl)
    }

    fun doSearch(key: String) {
        keyboardController?.hide()
        isManualStopSearch = false
        viewModel.saveSearchKey(key.trim())
        viewModel.searchKey = ""
        viewModel.search(key.trim())
    }

    LaunchedEffect(initialKey) {
        if (!initialKey.isNullOrBlank()) {
            searchQuery = initialKey
            doSearch(initialKey)
        } else {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(isSearching) {
        if (isSearching == true) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(listState, isSearching) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = layoutInfo.totalItemsCount
            Triple(isSearching, lastVisibleIndex, totalItems)
        }.collect { result ->
            val (searching, lastVisible, total) = result
            if (searching == false && viewModel.searchKey.isNotEmpty() && viewModel.hasMore
                && !isManualStopSearch && total > 0 && lastVisible >= total - 2
            ) {
                viewModel.search("")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { newText ->
                                searchQuery = newText
                                if (newText.isNotBlank()) {
                                    viewModel.stop()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        doSearch(searchQuery)
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = context.getString(R.string.search_book_key),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    viewModel.stop()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchQuery.isNotBlank()) {
                            doSearch(searchQuery)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = context.getString(R.string.search)
                        )
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = precisionSearch,
                                            onCheckedChange = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(context.getString(R.string.precision_search))
                                    }
                                },
                                onClick = {
                                    precisionSearch = !precisionSearch
                                    context.putPrefBoolean(PreferKey.precisionSearch, precisionSearch)
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.searchKey = ""
                                        viewModel.search(searchQuery)
                                    }
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(context.getString(R.string.book_source_manage)) },
                                onClick = {
                                    onSourceManage()
                                    showMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(context.getString(R.string.groups_or_source)) },
                                onClick = {
                                    showSearchScopeDialog = true
                                    showMenu = false
                                }
                            )
                            if (groups.isNotEmpty()) {
                                HorizontalDivider()
                                Text(
                                    text = context.getString(R.string.all_source),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                val scopeDisplayNames = viewModel.searchScope.displayNames
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = scopeDisplayNames.isEmpty(),
                                                onClick = null
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(context.getString(R.string.all_source))
                                        }
                                    },
                                    onClick = {
                                        viewModel.searchScope.update("")
                                        showMenu = false
                                    }
                                )
                                groups.forEach { group ->
                                    val isSelected = scopeDisplayNames.contains(group)
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = isSelected, onCheckedChange = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(group)
                                            }
                                        },
                                        onClick = {
                                            if (isSelected) {
                                                viewModel.searchScope.remove(group)
                                            } else {
                                                viewModel.searchScope.update(group)
                                            }
                                            showMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isSearching == true) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                )
            }

            if (showInputHelp) {
                InputHelpView(
                    matchedBooks = matchedBooks,
                    historyKeys = historyKeys,
                    onBookClick = { book ->
                        onBookClick(book.name, book.author, book.bookUrl)
                    },
                    onHistoryClick = { key ->
                        scope.launch {
                            if (searchQuery == key) {
                                doSearch(key)
                            } else if (withContext(IO) { appDb.bookDao.findByName(key).isEmpty() }) {
                                doSearch(key)
                                searchQuery = key
                            } else {
                                searchQuery = key
                            }
                        }
                    },
                    onHistoryLongClick = { keyword ->
                        viewModel.deleteHistory(keyword)
                    },
                    onClearHistory = {
                        showClearHistoryDialog = true
                    }
                )
            } else {
                val books: List<SearchBook> = searchBooks ?: emptyList()
                if (books.isEmpty() && isSearching != true && searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = context.getString(R.string.search_content_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(state = listState) {
                        items(
                            items = books,
                            key = { book: SearchBook -> book.bookUrl }
                        ) { book ->
                            SearchBookItem(
                                book = book,
                                isInBookshelf = isInBookshelf(book),
                                onClick = {
                                    onBookClick(book.name, book.author, book.bookUrl)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 96.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        if (isSearching == true) {
                            item {
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

            if (isSearching == true || (!isManualStopSearch && viewModel.hasMore && isSearching == false && viewModel.searchKey.isNotEmpty())) {
                FloatingActionButton(
                    onClick = {
                        if (isSearching == true) {
                            isManualStopSearch = true
                            viewModel.stop()
                        } else {
                            viewModel.search("")
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (isSearching == true) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isSearching == true) context.getString(R.string.stop) else context.getString(R.string.search)
                    )
                }
            }
        }
    }

    if (showSearchScopeDialog) {
        SearchScopeDialog(
            groups = groups.toList(),
            currentScope = viewModel.searchScope,
            onConfirm = { newScope ->
                viewModel.searchScope.update(newScope.toString())
                showSearchScopeDialog = false
            },
            onDismiss = { showSearchScopeDialog = false }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text(context.getString(R.string.draw)) },
            text = { Text(context.getString(R.string.sure_clear_search_history)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryDialog = false
                }) {
                    Text(context.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }

    if (showEmptyResultDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyResultDialog = false },
            title = { Text(context.getString(R.string.draw)) },
            text = { Text(emptyResultMessage) },
            confirmButton = {
                TextButton(onClick = {
                    if (precisionSearch) {
                        precisionSearch = false
                        context.putPrefBoolean(PreferKey.precisionSearch, false)
                        viewModel.searchKey = ""
                        viewModel.search(searchQuery)
                    } else {
                        viewModel.searchScope.update("")
                    }
                    showEmptyResultDialog = false
                }) {
                    Text(context.getString(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyResultDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InputHelpView(
    matchedBooks: List<Book>,
    historyKeys: List<SearchKeyword>,
    onBookClick: (Book) -> Unit,
    onHistoryClick: (String) -> Unit,
    onHistoryLongClick: (SearchKeyword) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (matchedBooks.isNotEmpty()) {
            item {
                Text(
                    text = context.getString(R.string.bookshelf),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            item {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    matchedBooks.forEach { book ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { onBookClick(book) }
                        ) {
                            Text(
                                text = book.name,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.searchHistory),
                    style = MaterialTheme.typography.titleSmall
                )
                if (historyKeys.isNotEmpty()) {
                    Text(
                        text = context.getString(R.string.clear),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClearHistory() }
                    )
                }
            }
        }
        item {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                historyKeys.forEach { keyword ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clickable { onHistoryClick(keyword.word) }
                    ) {
                        Text(
                            text = keyword.word,
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBookItem(
    book: SearchBook,
    isInBookshelf: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        AsyncImage(
            model = book.coverUrl,
            contentDescription = context.getString(R.string.img_cover),
            modifier = Modifier
                .size(80.dp, 110.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.image_cover_default),
            error = painterResource(R.drawable.image_cover_default)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .height(110.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isInBookshelf) {
                    Spacer(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (book.origins.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${book.origins.size}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Text(
                text = context.getString(R.string.author_show, book.author),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val kinds = book.getKindList()
            if (kinds.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    kinds.take(3).forEach { kind ->
                        Text(
                            text = kind,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (!book.latestChapterTitle.isNullOrEmpty()) {
                Text(
                    text = context.getString(R.string.lasted_show, book.latestChapterTitle),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = book.trimIntro(context),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScopeDialog(
    groups: List<String>,
    currentScope: SearchScope,
    onConfirm: (SearchScope) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSourceMode by remember { mutableStateOf(currentScope.isSource()) }
    val selectedGroups = remember { mutableStateListOf<String>() }
    var selectedSource by remember { mutableStateOf<BookSourcePart?>(null) }
    val sources = remember { mutableStateListOf<BookSourcePart>() }
    var sourceSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        selectedGroups.addAll(currentScope.displayNames.filter { !it.contains("::") })
    }

    LaunchedEffect(isSourceMode, sourceSearchQuery) {
        if (isSourceMode) {
            delay(300)
            val flow = if (sourceSearchQuery.isBlank()) {
                appDb.bookSourceDao.flowAll()
            } else {
                appDb.bookSourceDao.flowSearch(sourceSearchQuery)
            }
            flow.catch {
                AppLog.put("搜索范围对话框获取书源出错", it)
            }.flowOn(IO).conflate().collect {
                sources.clear()
                sources.addAll(it)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.search_scope)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (!isSourceMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSourceMode = false }
                    ) {
                        Text(
                            text = context.getString(R.string.group),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (!isSourceMode) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSourceMode) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isSourceMode = true }
                    ) {
                        Text(
                            text = context.getString(R.string.book_source),
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isSourceMode) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSourceMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = sourceSearchQuery,
                            onValueChange = { sourceSearchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            decorationBox = { innerTextField ->
                                if (sourceSearchQuery.isEmpty()) {
                                    Text(
                                        text = context.getString(R.string.search),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    if (!isSourceMode) {
                        items(groups) { group ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (selectedGroups.contains(group)) {
                                            selectedGroups.remove(group)
                                        } else {
                                            selectedGroups.add(group)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = selectedGroups.contains(group),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedGroups.add(group)
                                        else selectedGroups.remove(group)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(group, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        items(sources) { source ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSource = source }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedSource == source,
                                    onClick = { selectedSource = source }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(source.bookSourceName, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = {
                    onConfirm(SearchScope(""))
                }) {
                    Text(context.getString(R.string.all_source))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }
                TextButton(onClick = {
                    if (isSourceMode) {
                        selectedSource?.let { onConfirm(SearchScope(it)) } ?: onConfirm(SearchScope(""))
                    } else {
                        onConfirm(SearchScope(selectedGroups))
                    }
                }) {
                    Text(context.getString(android.R.string.ok))
                }
            }
        }
    )
}
