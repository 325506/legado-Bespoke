package io.legado.app.ui.compose.screens.book.detail

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.livedata.observeAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.legado.app.R
import io.legado.app.constant.BookType
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.help.book.addType
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalTxt
import io.legado.app.help.book.isWebFile
import io.legado.app.help.book.removeType
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.ui.book.info.BookInfoViewModel
import io.legado.app.utils.ConvertUtils
import io.legado.app.utils.FileDoc
import io.legado.app.utils.sendToClip
import io.legado.app.utils.setHtml
import io.legado.app.utils.setMarkdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookInfoScreen(
    viewModel: BookInfoViewModel,
    onBack: () -> Unit,
    onReadBook: (Book, Boolean, Boolean) -> Unit,
    onShowChangeSource: (String, String) -> Unit,
    onShowChangeCover: (String, String) -> Unit,
    onShowGroupSelect: (Long) -> Unit,
    onShowPhoto: (String, Boolean) -> Unit,
    onShowVariableDialog: (String, String?, String?) -> Unit,
    onShowAppLog: () -> Unit,
    onShowWaitDialog: (Boolean) -> Unit,
    onBookDeleted: () -> Unit,
    onOpenToc: (String) -> Unit,
    onOpenInfoEdit: (String) -> Unit,
    onOpenSourceEdit: (String) -> Unit,
    onOpenSourceLogin: (String, String?) -> Unit,
    onShareBook: (Book) -> Unit,
    onSearchAuthor: (String) -> Unit,
    onSearchBookName: (String) -> Unit,
    onSearchKind: (String) -> Unit
) {
    val context = LocalContext.current
    val book by viewModel.bookData.observeAsState()
    val chapterList by viewModel.chapterListData.observeAsState()
    val waitDialogState by viewModel.waitDialogData.observeAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteOriginalFile by remember { mutableStateOf(LocalConfig.deleteBookOriginal) }

    LaunchedEffect(waitDialogState) {
        onShowWaitDialog(waitDialogState == true)
    }

    val coverUrl = book?.getDisplayCover()
    val isEInkMode = AppConfig.isEInkMode

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isEInkMode && coverUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.book_info),
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        if (viewModel.hasCustomBtn) {
                            IconButton(onClick = {
                                viewModel.bookSource?.customButton?.let {
                                    viewModel.getBook()?.let { b ->
                                        io.legado.app.model.SourceCallBack.callBackBtn(
                                            context as androidx.appcompat.app.AppCompatActivity,
                                            io.legado.app.model.SourceCallBack.CLICK_CUSTOM_BUTTON,
                                            viewModel.bookSource,
                                            b,
                                            null
                                        )
                                    }
                                }
                            }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_custom),
                                    contentDescription = stringResource(R.string.custom_button),
                                    tint = Color.White
                                )
                            }
                        }
                        if (viewModel.inBookshelf) {
                            IconButton(onClick = {
                                viewModel.getBook()?.let {
                                    onOpenInfoEdit(it.bookUrl)
                                }
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = Color.White
                                )
                            }
                        }
                        IconButton(onClick = {
                            viewModel.getBook()?.let { onShareBook(it) }
                        }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.share),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            viewModel.getBook()?.let { viewModel.refreshBook(it) }
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh),
                                tint = Color.White
                            )
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            BookInfoMoreMenu(
                                expanded = showMenu,
                                onDismiss = { showMenu = false },
                                viewModel = viewModel,
                                book = book,
                                onShowVariableDialog = onShowVariableDialog,
                                onShowAppLog = onShowAppLog,
                                onOpenSourceLogin = onOpenSourceLogin,
                                onOpenSourceEdit = onOpenSourceEdit
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    modifier = Modifier.statusBarsPadding()
                )
            },
            bottomBar = {
                book?.let { currentBook ->
                    BottomActionBar(
                        book = currentBook,
                        inBookshelf = viewModel.inBookshelf,
                        onShelfClick = {
                            if (viewModel.inBookshelf) {
                                showDeleteDialog = true
                            } else {
                                if (currentBook.isWebFile) {
                                    android.widget.Toast.makeText(
                                        context,
                                        R.string.download_and_import_file,
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    viewModel.addToBookshelf {}
                                }
                            }
                        },
                        onReadClick = {
                            if (currentBook.isWebFile) {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.download_and_import_file,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                if (!viewModel.inBookshelf) {
                                    currentBook.addType(BookType.notShelf)
                                    viewModel.saveBook(currentBook) {
                                        viewModel.saveChapterList {
                                            onReadBook(currentBook, false, false)
                                        }
                                    }
                                } else {
                                    viewModel.saveBook(currentBook) {
                                        onReadBook(currentBook, false, false)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                book?.let { currentBook ->
                    CoverSection(
                        book = currentBook,
                        onCoverClick = {
                            onShowChangeCover(currentBook.name, currentBook.author)
                        },
                        onCoverLongClick = {
                            currentBook.getDisplayCover()?.let { path ->
                                onShowPhoto(path, true)
                            }
                        }
                    )

                    InfoSection(
                        book = currentBook,
                        chapterList = chapterList,
                        viewModel = viewModel,
                        onAuthorClick = { onSearchAuthor(currentBook.getRealAuthor()) },
                        onOriginClick = {
                            if (!currentBook.isLocal && appDb.bookSourceDao.has(currentBook.origin)) {
                                onOpenSourceEdit(currentBook.origin)
                            } else if (!currentBook.isLocal) {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.error_no_source,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onChangeSourceClick = {
                            onShowChangeSource(currentBook.name, currentBook.author)
                        },
                        onChangeGroupClick = {
                            onShowGroupSelect(currentBook.group)
                        },
                        onViewTocClick = {
                            if (viewModel.chapterListData.value.isNullOrEmpty()) {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.chapter_list_empty,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@InfoSection
                            }
                            viewModel.getBook()?.let { b ->
                                if (!viewModel.inBookshelf) {
                                    viewModel.saveBook(b) {
                                        viewModel.saveChapterList {
                                            onOpenToc(b.bookUrl)
                                        }
                                    }
                                } else {
                                    onOpenToc(b.bookUrl)
                                }
                            }
                        },
                        onBookNameClick = { onSearchBookName(currentBook.name) },
                        onKindClick = { onSearchKind(it) }
                    )

                    IntroSection(book = currentBook)

                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }

    if (showDeleteDialog) {
        book?.let { currentBook ->
            DeleteBookDialog(
                book = currentBook,
                deleteOriginalFile = deleteOriginalFile,
                onDeleteOriginalFileChange = { deleteOriginalFile = it },
                onConfirm = {
                    LocalConfig.deleteBookOriginal = deleteOriginalFile
                    io.legado.app.model.SourceCallBack.callBackBook(
                        io.legado.app.model.SourceCallBack.DEL_BOOK_SHELF,
                        viewModel.bookSource,
                        currentBook
                    )
                    viewModel.delBook(deleteOriginalFile) {
                        onBookDeleted()
                    }
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

@Composable
private fun CoverSection(
    book: Book,
    onCoverClick: () -> Unit,
    onCoverLongClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .size(width = 110.dp, height = 160.dp)
                .clip(RoundedCornerShape(5.dp))
                .combinedClickable(
                    onClick = onCoverClick,
                    onLongClick = onCoverLongClick
                ),
            shape = RoundedCornerShape(5.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(book.getDisplayCover())
                    .placeholder(R.drawable.image_cover_default)
                    .error(R.drawable.image_cover_default)
                    .build(),
                contentDescription = stringResource(R.string.img_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun InfoSection(
    book: Book,
    chapterList: List<BookChapter>?,
    viewModel: BookInfoViewModel,
    onAuthorClick: () -> Unit,
    onOriginClick: () -> Unit,
    onChangeSourceClick: () -> Unit,
    onChangeGroupClick: () -> Unit,
    onViewTocClick: () -> Unit,
    onBookNameClick: () -> Unit,
    onKindClick: (String) -> Unit
) {
    val context = LocalContext.current
    var groupNames by remember { mutableStateOf("") }

    LaunchedEffect(book.group) {
        viewModel.loadGroup(book.group) {
            groupNames = it ?: context.getString(
                if (book.isLocal) R.string.local_no_group else R.string.no_group
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onBookNameClick)
            )
        }

        val kinds = remember(book) {
            val list = book.getKindList().toMutableList()
            if (book.isLocal) {
                runCatching {
                    val size = FileDoc.fromFile(book.bookUrl).size
                    if (size > 0) list.add(ConvertUtils.formatFileSize(size))
                }
            }
            list
        }
        if (kinds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                kinds.forEachIndexed { index, kind ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { onKindClick(kind) }
                    ) {
                        Text(
                            text = kind,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        InfoRow(
            iconRes = R.drawable.ic_author,
            text = stringResource(R.string.author_show, book.getRealAuthor()),
            onTextClick = onAuthorClick
        )

        InfoRowWithAction(
            iconRes = R.drawable.ic_web_outline,
            text = stringResource(R.string.origin_show, book.originName),
            onTextClick = onOriginClick,
            actionLabel = stringResource(R.string.change_origin),
            onActionClick = onChangeSourceClick
        )

        InfoRow(
            iconRes = R.drawable.ic_book_last,
            text = stringResource(R.string.lasted_show, book.latestChapterTitle ?: "")
        )

        InfoRowWithAction(
            iconRes = R.drawable.ic_groups,
            text = stringResource(R.string.group_s, groupNames),
            actionLabel = stringResource(R.string.change_group),
            onActionClick = onChangeGroupClick
        )

        if (!book.isWebFile) {
            val tocText = when {
                chapterList == null -> stringResource(R.string.toc_s, stringResource(R.string.loading))
                chapterList.isEmpty() -> stringResource(
                    R.string.toc_s,
                    stringResource(R.string.error_load_toc)
                )
                else -> stringResource(R.string.toc_s, book.durChapterTitle ?: "")
            }
            InfoRowWithAction(
                iconRes = R.drawable.ic_folder_open,
                text = tocText,
                actionLabel = stringResource(R.string.view_toc),
                onActionClick = onViewTocClick
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun InfoRow(
    iconRes: Int,
    text: String,
    onTextClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(if (onTextClick != null) Modifier.clickable(onClick = onTextClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InfoRowWithAction(
    iconRes: Int,
    text: String,
    onTextClick: (() -> Unit)? = null,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(if (onTextClick != null) Modifier.clickable(onClick = onTextClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onActionClick)
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun IntroSection(book: Book) {
    val intro = book.getDisplayIntro()
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp)
    ) {
        if (!intro.isNullOrBlank()) {
            if (intro.startsWith("<usehtml>") || intro.startsWith("<md>")) {
                AndroidView(
                    factory = { ctx ->
                        io.legado.app.ui.widget.text.ScrollTextView(ctx, null).apply {
                            textSize = 14f
                            setTextColor(
                                ctx.resources.getColor(R.color.secondaryText, null)
                            )
                            setPadding(
                                (8 * ctx.resources.displayMetrics.density).toInt(),
                                (8 * ctx.resources.displayMetrics.density).toInt(),
                                (8 * ctx.resources.displayMetrics.density).toInt(),
                                (8 * ctx.resources.displayMetrics.density).toInt()
                            )
                            minHeight = (48 * ctx.resources.displayMetrics.density).toInt()
                        }
                    },
                    update = { textView ->
                        if (intro.startsWith("<usehtml>")) {
                            val lastIndex = intro.lastIndexOf("<")
                            if (lastIndex < 9) {
                                textView.text = intro
                            } else {
                                val html = intro.substring(9, lastIndex)
                                textView.setHtml(html)
                            }
                        } else if (intro.startsWith("<md>")) {
                            val lastIndex = intro.lastIndexOf("<")
                            if (lastIndex < 4) {
                                textView.text = intro
                            } else {
                                textView.text = intro.substring(4, lastIndex)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = intro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { expanded = !expanded }
                )
                if (!expanded) {
                    Text(
                        text = "Show All",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomActionBar(
    book: Book,
    inBookshelf: Boolean,
    onShelfClick: () -> Unit,
    onReadClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (inBookshelf) stringResource(R.string.remove_from_bookshelf)
                else stringResource(R.string.add_to_bookshelf),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clickable(onClick = onShelfClick),
                textAlign = TextAlign.Center
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Read",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .clickable(onClick = onReadClick),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DeleteBookDialog(
    book: Book,
    deleteOriginalFile: Boolean,
    onDeleteOriginalFileChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.draw)) },
        text = {
            Column {
                Text(stringResource(R.string.sure_del))
                if (book.isLocal) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Checkbox(
                            checked = deleteOriginalFile,
                            onCheckedChange = onDeleteOriginalFileChange
                        )
                        Text(stringResource(R.string.delete_book_file))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun BookInfoMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    viewModel: BookInfoViewModel,
    book: Book?,
    onShowVariableDialog: (String, String?, String?) -> Unit,
    onShowAppLog: () -> Unit,
    onOpenSourceLogin: (String, String?) -> Unit,
    onOpenSourceEdit: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        book?.let { currentBook ->
            if (!viewModel.bookSource?.loginUrl.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.login)) },
                    onClick = {
                        onDismiss()
                        viewModel.bookSource?.let {
                            onOpenSourceLogin(it.bookSourceUrl, currentBook.bookUrl)
                        }
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.to_top)) },
                onClick = {
                    onDismiss()
                    viewModel.topBook()
                }
            )
            if (viewModel.bookSource != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.set_source_variable)) },
                    onClick = {
                        onDismiss()
                        scope.launch {
                            val source = viewModel.bookSource ?: return@launch
                            val variable = withContext(Dispatchers.IO) { source.getVariable() }
                            val comment = source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取")
                            onShowVariableDialog(source.getKey(), variable, comment)
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.set_book_variable)) },
                    onClick = {
                        onDismiss()
                        scope.launch {
                            val source = viewModel.bookSource ?: return@launch
                            val b = viewModel.getBook() ?: return@launch
                            val variable = withContext(Dispatchers.IO) { b.getCustomVariable() }
                            val comment = source.getDisplayVariableComment(
                                "书籍变量可在js中通过book.getVariable(\"custom\")获取"
                            )
                            onShowVariableDialog(b.bookUrl, variable, comment)
                        }
                    }
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_book_url)) },
                onClick = {
                    onDismiss()
                    context.sendToClip(currentBook.bookUrl ?: "")
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copy_toc_url)) },
                onClick = {
                    onDismiss()
                    context.sendToClip(currentBook.tocUrl ?: "")
                }
            )
            if (viewModel.bookSource != null) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.allow_update))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = currentBook.canUpdate,
                                onCheckedChange = null
                            )
                        }
                    },
                    onClick = {
                        currentBook.canUpdate = !currentBook.canUpdate
                        if (viewModel.inBookshelf) {
                            if (!currentBook.canUpdate) {
                                currentBook.removeType(BookType.updateError)
                            }
                            viewModel.saveBook(currentBook)
                        }
                    }
                )
            }
            if (currentBook.isLocalTxt) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.split_long_chapter))
                            Spacer(modifier = Modifier.weight(1f))
                            Checkbox(
                                checked = currentBook.getSplitLongChapter(),
                                onCheckedChange = null
                            )
                        }
                    },
                    onClick = {
                        currentBook.setSplitLongChapter(!currentBook.getSplitLongChapter())
                        viewModel.loadBookInfo(currentBook, false)
                    }
                )
            }
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.delete_alert))
                        Spacer(modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = LocalConfig.bookInfoDeleteAlert,
                            onCheckedChange = null
                        )
                    }
                },
                onClick = {
                    LocalConfig.bookInfoDeleteAlert = !LocalConfig.bookInfoDeleteAlert
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.clear_cache)) },
                onClick = {
                    onDismiss()
                    viewModel.clearCache(currentBook)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.log)) },
                onClick = {
                    onDismiss()
                    onShowAppLog()
                }
            )
        }
    }
}


