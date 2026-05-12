package io.legado.app.ui.compose.screens.explore

import androidx.compose.foundation.background as fondoBackground
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.book.isNotShelf
import io.legado.app.model.webBook.WebBook
import io.legado.app.ui.compose.theme.LegadoTheme
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreShowViewModel(application: android.app.Application) : AndroidViewModel(application) {

    var books by androidx.compose.runtime.mutableStateOf<List<SearchBook>>(emptyList())
        private set
    var isLoading by androidx.compose.runtime.mutableStateOf(false)
        private set
    var page by androidx.compose.runtime.mutableStateOf(1)
        private set
    var errorMessage by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    var exploreName by androidx.compose.runtime.mutableStateOf("")
        private set

    private val bookshelf: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private var sourceUrl: String? = null
    private var exploreUrl: String? = null
    private var bookSource: io.legado.app.data.entities.BookSource? = null

    init {
        viewModelScope.launch {
            appDb.bookDao.flowAll().mapLatest { books ->
                val keys = arrayListOf<String>()
                books.filterNot { it.isNotShelf }
                    .forEach {
                        keys.add("${it.name}-${it.author}")
                        keys.add(it.name)
                        keys.add(it.bookUrl)
                    }
                keys
            }.catch {
                AppLog.put("发现列表界面获取书籍数据失败\n${it.localizedMessage}", it)
            }.collect {
                bookshelf.clear()
                bookshelf.addAll(it)
            }
        }
    }

    fun initData(sourceUrl: String?, exploreUrl: String?, name: String?) {
        this.sourceUrl = sourceUrl
        this.exploreUrl = exploreUrl
        this.exploreName = name ?: ""
        viewModelScope.launch(IO) {
            if (bookSource == null && sourceUrl != null) {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            explore()
        }
    }

    fun explore() {
        val source = bookSource ?: return
        val url = exploreUrl ?: return
        isLoading = true
        errorMessage = null
        WebBook.exploreBook(viewModelScope, source, url, page)
            .onSuccess(IO) { searchBooks ->
                books = books + searchBooks
                page++
                isLoading = false
                appDb.searchBookDao.insert(*searchBooks.toTypedArray())
            }
            .onError {
                errorMessage = it.message
                isLoading = false
            }
    }

    fun isInBookShelf(book: SearchBook): Boolean {
        val key = if (book.author.isNotBlank()) "${book.name}-${book.author}" else book.name
        return bookshelf.contains(key) || bookshelf.contains(book.bookUrl)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreShowScreen(
    viewModel: ExploreShowViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    sourceUrl: String? = null,
    exploreUrl: String? = null,
    exploreName: String? = null,
    onBack: () -> Unit = {},
    onBookClick: (SearchBook) -> Unit = {}
) {
    val context = LocalContext.current
    val books = viewModel.books
    val isLoading = viewModel.isLoading
    val listState = rememberLazyListState()

    LaunchedEffect(sourceUrl, exploreUrl) {
        viewModel.initData(sourceUrl, exploreUrl, exploreName)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleIndex >= totalItems - 3
        }.collect { nearEnd: Boolean ->
            if (nearEnd && !isLoading && books.isNotEmpty()) {
                viewModel.explore()
            }
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = viewModel.exploreName.ifEmpty { context.getString(R.string.discovery) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (books.isEmpty() && !isLoading) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = viewModel.errorMessage ?: context.getString(R.string.empty),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = books,
                        key = { it.bookUrl }
                    ) { book ->
                        SearchBookItem(
                            book = book,
                            isInBookshelf = viewModel.isInBookShelf(book),
                            onClick = { onBookClick(book) }
                        )
                    }
                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("加载中...")
                            }
                        }
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
            contentDescription = "封面",
            modifier = Modifier
                .size(80.dp, 110.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
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
                            .clip(RoundedCornerShape(4.dp))
                            .fondoBackground(MaterialTheme.colorScheme.primary)
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
                                .fondoBackground(MaterialTheme.colorScheme.secondaryContainer)
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
