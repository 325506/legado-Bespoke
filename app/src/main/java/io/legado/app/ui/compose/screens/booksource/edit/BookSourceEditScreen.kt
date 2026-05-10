package io.legado.app.ui.compose.screens.booksource.edit

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.legado.app.R
import io.legado.app.constant.BookSourceType
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.rule.BookInfoRule
import io.legado.app.data.entities.rule.ContentRule
import io.legado.app.data.entities.rule.ExploreRule
import io.legado.app.data.entities.rule.SearchRule
import io.legado.app.data.entities.rule.TocRule
import io.legado.app.ui.code.CodeEditActivity
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSourceEditScreen(
    bookSource: BookSource?,
    onSave: (BookSource) -> Unit,
    onDebug: (BookSource) -> Unit,
    onClearCookie: (String) -> Unit,
    onCopySource: (String) -> Unit,
    onPasteSource: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var source by remember { mutableStateOf(bookSource?.copy() ?: BookSource()) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showKeyboardAssist by remember { mutableStateOf(false) }
    var focusedFieldKey by remember { mutableStateOf<String?>(null) }
    var focusedFieldValue by remember { mutableStateOf("") }
    var focusedFieldHint by remember { mutableStateOf("") }

    val originalSource = bookSource ?: BookSource()

    val tabs = listOf(
        "基本信息",
        "搜索规则",
        "发现规则",
        "详情规则",
        "目录规则",
        "正文规则"
    )

    val typeNames = listOf("文本", "音频", "图片", "文件", "视频")
    val typeValues = listOf(
        BookSourceType.default,
        BookSourceType.audio,
        BookSourceType.image,
        BookSourceType.file,
        BookSourceType.video
    )

    BackHandler {
        if (!source.equal(originalSource)) {
            showExitConfirm = true
        } else {
            onNavigateBack()
        }
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(context.getString(R.string.edit_book_source)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!source.equal(originalSource)) {
                                showExitConfirm = true
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                    }
                )
            },
            bottomBar = {
                val view = LocalView.current
                var isKeyboardVisible by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                        val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
                        isKeyboardVisible = imeVisible
                        insets
                    }
                }

                if (showKeyboardAssist && isKeyboardVisible) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.ime),
                        tonalElevation = 4.dp
                    ) {
                        KeyboardAssistBar(
                            hint = focusedFieldHint,
                            value = focusedFieldValue,
                            onValueChange = { newValue ->
                                focusedFieldValue = newValue
                                when (focusedFieldKey) {
                                    "bookSourceUrl" -> source = source.copy(bookSourceUrl = newValue)
                                    "bookSourceName" -> source = source.copy(bookSourceName = newValue)
                                    "bookSourceGroup" -> source = source.copy(bookSourceGroup = newValue)
                                    "bookSourceComment" -> source = source.copy(bookSourceComment = newValue)
                                    "loginUrl" -> source = source.copy(loginUrl = newValue)
                                    "loginUi" -> source = source.copy(loginUi = newValue)
                                    "loginCheckJs" -> source = source.copy(loginCheckJs = newValue)
                                    "coverDecodeJs" -> source = source.copy(coverDecodeJs = newValue)
                                    "bookUrlPattern" -> source = source.copy(bookUrlPattern = newValue)
                                    "header" -> source = source.copy(header = newValue)
                                    "variableComment" -> source = source.copy(variableComment = newValue)
                                    "concurrentRate" -> source = source.copy(concurrentRate = newValue)
                                    "jsLib" -> source = source.copy(jsLib = newValue)
                                    "searchUrl" -> source = source.copy(searchUrl = newValue)
                                    "exploreUrl" -> source = source.copy(exploreUrl = newValue)
                                    else -> {
                                        val searchRule = source.getSearchRule()
                                        val exploreRule = source.getExploreRule()
                                        val bookInfoRule = source.getBookInfoRule()
                                        val tocRule = source.getTocRule()
                                        val contentRule = source.getContentRule()
                                        when (focusedFieldKey) {
                                            "checkKeyWord" -> source = source.copy(ruleSearch = searchRule.copy(checkKeyWord = newValue))
                                            "bookList" -> source = source.copy(ruleSearch = searchRule.copy(bookList = newValue))
                                            "name" -> source = source.copy(ruleSearch = searchRule.copy(name = newValue))
                                            "author" -> source = source.copy(ruleSearch = searchRule.copy(author = newValue))
                                            "kind" -> source = source.copy(ruleSearch = searchRule.copy(kind = newValue))
                                            "wordCount" -> source = source.copy(ruleSearch = searchRule.copy(wordCount = newValue))
                                            "lastChapter" -> source = source.copy(ruleSearch = searchRule.copy(lastChapter = newValue))
                                            "intro" -> source = source.copy(ruleSearch = searchRule.copy(intro = newValue))
                                            "coverUrl" -> source = source.copy(ruleSearch = searchRule.copy(coverUrl = newValue))
                                            "bookUrl" -> source = source.copy(ruleSearch = searchRule.copy(bookUrl = newValue))
                                            "init" -> source = source.copy(ruleBookInfo = bookInfoRule.copy(init = newValue))
                                            "tocUrl" -> source = source.copy(ruleBookInfo = bookInfoRule.copy(tocUrl = newValue))
                                            "canReName" -> source = source.copy(ruleBookInfo = bookInfoRule.copy(canReName = newValue))
                                            "downloadUrls" -> source = source.copy(ruleBookInfo = bookInfoRule.copy(downloadUrls = newValue))
                                            "preUpdateJs" -> source = source.copy(ruleToc = tocRule.copy(preUpdateJs = newValue))
                                            "chapterList" -> source = source.copy(ruleToc = tocRule.copy(chapterList = newValue))
                                            "chapterName" -> source = source.copy(ruleToc = tocRule.copy(chapterName = newValue))
                                            "chapterUrl" -> source = source.copy(ruleToc = tocRule.copy(chapterUrl = newValue))
                                            "formatJs" -> source = source.copy(ruleToc = tocRule.copy(formatJs = newValue))
                                            "isVolume" -> source = source.copy(ruleToc = tocRule.copy(isVolume = newValue))
                                            "updateTime" -> source = source.copy(ruleToc = tocRule.copy(updateTime = newValue))
                                            "isVip" -> source = source.copy(ruleToc = tocRule.copy(isVip = newValue))
                                            "isPay" -> source = source.copy(ruleToc = tocRule.copy(isPay = newValue))
                                            "nextTocUrl" -> source = source.copy(ruleToc = tocRule.copy(nextTocUrl = newValue))
                                            "content" -> source = source.copy(ruleContent = contentRule.copy(content = newValue))
                                            "nextContentUrl" -> source = source.copy(ruleContent = contentRule.copy(nextContentUrl = newValue))
                                            "subContent" -> source = source.copy(ruleContent = contentRule.copy(subContent = newValue))
                                            "replaceRegex" -> source = source.copy(ruleContent = contentRule.copy(replaceRegex = newValue))
                                            "title" -> source = source.copy(ruleContent = contentRule.copy(title = newValue))
                                            "sourceRegex" -> source = source.copy(ruleContent = contentRule.copy(sourceRegex = newValue))
                                            "imageStyle" -> source = source.copy(ruleContent = contentRule.copy(imageStyle = newValue))
                                            "imageDecode" -> source = source.copy(ruleContent = contentRule.copy(imageDecode = newValue))
                                            "webJs" -> source = source.copy(ruleContent = contentRule.copy(webJs = newValue))
                                            "payAction" -> source = source.copy(ruleContent = contentRule.copy(payAction = newValue))
                                            "callBackJs" -> source = source.copy(ruleContent = contentRule.copy(callBackJs = newValue))
                                        }
                                    }
                                }
                            },
                            onInsertText = { text ->
                                focusedFieldValue = focusedFieldValue + text
                            },
                            onOpenEditor = {
                                val intent = Intent(context, CodeEditActivity::class.java).apply {
                                    putExtra("text", focusedFieldValue)
                                    putExtra("title", focusedFieldHint)
                                }
                                context.startActivity(intent)
                            },
                            onDismiss = { showKeyboardAssist = false }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { typeExpanded = true }) {
                            Text(
                                when (source.bookSourceType) {
                                    BookSourceType.audio -> "音频"
                                    BookSourceType.image -> "图片"
                                    BookSourceType.file -> "文件"
                                    BookSourceType.video -> "视频"
                                    else -> "文本"
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            typeNames.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        typeExpanded = false
                                        source = source.copy(bookSourceType = typeValues[index])
                                    }
                                )
                            }
                        }
                    }

                    var localEnabled by remember(source.bookSourceUrl) { mutableStateOf(source.enabled) }
                    LaunchedEffect(source.enabled) { localEnabled = source.enabled }
                    Checkbox(
                        checked = localEnabled,
                        onCheckedChange = {
                            localEnabled = it
                            source = source.copy(enabled = it)
                        }
                    )
                    Text("启用")

                    var localEnabledExplore by remember(source.bookSourceUrl) { mutableStateOf(source.enabledExplore) }
                    LaunchedEffect(source.enabledExplore) { localEnabledExplore = source.enabledExplore }
                    Checkbox(
                        checked = localEnabledExplore,
                        onCheckedChange = {
                            localEnabledExplore = it
                            source = source.copy(enabledExplore = it)
                        }
                    )
                    Text("发现")

                    var localEnabledCookie by remember(source.bookSourceUrl) { mutableStateOf(source.enabledCookieJar ?: false) }
                    LaunchedEffect(source.enabledCookieJar) { localEnabledCookie = source.enabledCookieJar ?: false }
                    Checkbox(
                        checked = localEnabledCookie,
                        onCheckedChange = {
                            localEnabledCookie = it
                            source = source.copy(enabledCookieJar = it)
                        }
                    )
                    Text("Cookie")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var localEventListener by remember(source.bookSourceUrl) { mutableStateOf(source.eventListener ?: false) }
                    LaunchedEffect(source.eventListener) { localEventListener = source.eventListener ?: false }
                    Checkbox(
                        checked = localEventListener,
                        onCheckedChange = {
                            localEventListener = it
                            source = source.copy(eventListener = it)
                        }
                    )
                    Text("事件监听")

                    var localCustomButton by remember(source.bookSourceUrl) { mutableStateOf(source.customButton ?: false) }
                    LaunchedEffect(source.customButton) { localCustomButton = source.customButton ?: false }
                    Checkbox(
                        checked = localCustomButton,
                        onCheckedChange = {
                            localCustomButton = it
                            source = source.copy(customButton = it)
                        }
                    )
                    Text("定制按钮")
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> BasicInfoTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                        1 -> SearchRuleTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                        2 -> ExploreRuleTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                        3 -> BookInfoRuleTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                        4 -> TocRuleTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                        5 -> ContentRuleTab(
                            source,
                            focusedFieldKey,
                            { key, value, hint ->
                                focusedFieldKey = key
                                focusedFieldValue = value
                                focusedFieldHint = hint
                                showKeyboardAssist = true
                            }
                        ) { source = it }
                    }
                }

                Button(
                    onClick = { onSave(source) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("保存")
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("调试书源") },
                onClick = {
                    showMenu = false
                    onDebug(source)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("清除Cookie") },
                onClick = {
                    showMenu = false
                    onClearCookie(source.bookSourceUrl)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("复制书源") },
                onClick = {
                    showMenu = false
                    onCopySource(io.legado.app.utils.GSON.toJson(source))
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("粘贴书源") },
                onClick = {
                    showMenu = false
                    onPasteSource()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }

        if (showExitConfirm) {
            AlertDialog(
                onDismissRequest = { showExitConfirm = false },
                title = { Text("退出") },
                text = { Text("有未保存的修改，是否退出？") },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirm = false
                        onNavigateBack()
                    }) {
                        Text("退出")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun EditField(
    label: String,
    key: String,
    value: String?,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onValueChange: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        BasicTextField(
            value = value ?: "",
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (isFocused) {
                        onFocus(key, value ?: "", label)
                    }
                },
            textStyle = MaterialTheme.typography.bodyMedium,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (value.isNullOrEmpty()) {
                        Text(
                            text = "请输入",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        HorizontalDivider()
    }
}

@Composable
fun KeyboardAssistBar(
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    onInsertText: (String) -> Unit,
    onOpenEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    val commonSymbols = listOf("@", "##", "&&", "<js>", "{{", "}}", ":", ".", "$", "@css:", "@json:")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenEditor() }
            )
            TextButton(onClick = onDismiss) {
                Text("收起")
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            commonSymbols.forEach { symbol ->
                TextButton(
                    onClick = { onInsertText(symbol) },
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(symbol)
                }
            }
        }
    }
}

@Composable
fun BasicInfoTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    EditField("书源URL", "bookSourceUrl", source.bookSourceUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(bookSourceUrl = it)) }
    EditField("书源名称", "bookSourceName", source.bookSourceName, focusedFieldKey, onFocus) { onUpdate(source.copy(bookSourceName = it)) }
    EditField("书源分组", "bookSourceGroup", source.bookSourceGroup, focusedFieldKey, onFocus) { onUpdate(source.copy(bookSourceGroup = it)) }
    EditField("注释", "bookSourceComment", source.bookSourceComment, focusedFieldKey, onFocus) { onUpdate(source.copy(bookSourceComment = it)) }
    EditField("登录URL", "loginUrl", source.loginUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(loginUrl = it)) }
    EditField("登录UI", "loginUi", source.loginUi, focusedFieldKey, onFocus) { onUpdate(source.copy(loginUi = it)) }
    EditField("登录检查JS", "loginCheckJs", source.loginCheckJs, focusedFieldKey, onFocus) { onUpdate(source.copy(loginCheckJs = it)) }
    EditField("封面解码JS", "coverDecodeJs", source.coverDecodeJs, focusedFieldKey, onFocus) { onUpdate(source.copy(coverDecodeJs = it)) }
    EditField("书籍URL模式", "bookUrlPattern", source.bookUrlPattern, focusedFieldKey, onFocus) { onUpdate(source.copy(bookUrlPattern = it)) }
    EditField("请求头", "header", source.header, focusedFieldKey, onFocus) { onUpdate(source.copy(header = it)) }
    EditField("变量注释", "variableComment", source.variableComment, focusedFieldKey, onFocus) { onUpdate(source.copy(variableComment = it)) }
    EditField("并发率", "concurrentRate", source.concurrentRate, focusedFieldKey, onFocus) { onUpdate(source.copy(concurrentRate = it)) }
    EditField("JS库", "jsLib", source.jsLib, focusedFieldKey, onFocus) { onUpdate(source.copy(jsLib = it)) }
}

@Composable
fun SearchRuleTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    val rule = source.getSearchRule()
    EditField("搜索URL", "searchUrl", source.searchUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(searchUrl = it)) }
    EditField("检查关键字", "checkKeyWord", rule.checkKeyWord, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(checkKeyWord = it))) }
    EditField("书籍列表", "bookList", rule.bookList, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(bookList = it))) }
    EditField("书名", "name", rule.name, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(name = it))) }
    EditField("作者", "author", rule.author, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(author = it))) }
    EditField("分类", "kind", rule.kind, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(kind = it))) }
    EditField("字数", "wordCount", rule.wordCount, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(wordCount = it))) }
    EditField("最新章节", "lastChapter", rule.lastChapter, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(lastChapter = it))) }
    EditField("简介", "intro", rule.intro, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(intro = it))) }
    EditField("封面URL", "coverUrl", rule.coverUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(coverUrl = it))) }
    EditField("书籍URL", "bookUrl", rule.bookUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleSearch = rule.copy(bookUrl = it))) }
}

@Composable
fun ExploreRuleTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    val rule = source.getExploreRule()
    EditField("发现URL", "exploreUrl", source.exploreUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(exploreUrl = it)) }
    EditField("书籍列表", "bookList", rule.bookList, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(bookList = it))) }
    EditField("书名", "name", rule.name, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(name = it))) }
    EditField("作者", "author", rule.author, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(author = it))) }
    EditField("分类", "kind", rule.kind, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(kind = it))) }
    EditField("字数", "wordCount", rule.wordCount, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(wordCount = it))) }
    EditField("最新章节", "lastChapter", rule.lastChapter, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(lastChapter = it))) }
    EditField("简介", "intro", rule.intro, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(intro = it))) }
    EditField("封面URL", "coverUrl", rule.coverUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(coverUrl = it))) }
    EditField("书籍URL", "bookUrl", rule.bookUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleExplore = rule.copy(bookUrl = it))) }
}

@Composable
fun BookInfoRuleTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    val rule = source.getBookInfoRule()
    EditField("初始化", "init", rule.init, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(init = it))) }
    EditField("书名", "name", rule.name, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(name = it))) }
    EditField("作者", "author", rule.author, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(author = it))) }
    EditField("分类", "kind", rule.kind, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(kind = it))) }
    EditField("字数", "wordCount", rule.wordCount, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(wordCount = it))) }
    EditField("最新章节", "lastChapter", rule.lastChapter, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(lastChapter = it))) }
    EditField("简介", "intro", rule.intro, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(intro = it))) }
    EditField("封面URL", "coverUrl", rule.coverUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(coverUrl = it))) }
    EditField("目录URL", "tocUrl", rule.tocUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(tocUrl = it))) }
    EditField("可重命名", "canReName", rule.canReName, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(canReName = it))) }
    EditField("下载URL", "downloadUrls", rule.downloadUrls, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleBookInfo = rule.copy(downloadUrls = it))) }
}

@Composable
fun TocRuleTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    val rule = source.getTocRule()
    EditField("更新前JS", "preUpdateJs", rule.preUpdateJs, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(preUpdateJs = it))) }
    EditField("章节列表", "chapterList", rule.chapterList, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(chapterList = it))) }
    EditField("章节名称", "chapterName", rule.chapterName, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(chapterName = it))) }
    EditField("章节URL", "chapterUrl", rule.chapterUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(chapterUrl = it))) }
    EditField("格式化JS", "formatJs", rule.formatJs, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(formatJs = it))) }
    EditField("是否卷", "isVolume", rule.isVolume, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(isVolume = it))) }
    EditField("更新时间", "updateTime", rule.updateTime, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(updateTime = it))) }
    EditField("是否VIP", "isVip", rule.isVip, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(isVip = it))) }
    EditField("是否付费", "isPay", rule.isPay, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(isPay = it))) }
    EditField("下一目录URL", "nextTocUrl", rule.nextTocUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleToc = rule.copy(nextTocUrl = it))) }
}

@Composable
fun ContentRuleTab(
    source: BookSource,
    focusedFieldKey: String?,
    onFocus: (String, String, String) -> Unit,
    onUpdate: (BookSource) -> Unit
) {
    val rule = source.getContentRule()
    EditField("正文内容", "content", rule.content, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(content = it))) }
    EditField("下一正文URL", "nextContentUrl", rule.nextContentUrl, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(nextContentUrl = it))) }
    EditField("子内容", "subContent", rule.subContent, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(subContent = it))) }
    EditField("替换正则", "replaceRegex", rule.replaceRegex, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(replaceRegex = it))) }
    EditField("章节名称", "title", rule.title, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(title = it))) }
    EditField("来源正则", "sourceRegex", rule.sourceRegex, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(sourceRegex = it))) }
    EditField("图片样式", "imageStyle", rule.imageStyle, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(imageStyle = it))) }
    EditField("图片解码", "imageDecode", rule.imageDecode, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(imageDecode = it))) }
    EditField("WebJS", "webJs", rule.webJs, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(webJs = it))) }
    EditField("付费操作", "payAction", rule.payAction, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(payAction = it))) }
    EditField("回调JS", "callBackJs", rule.callBackJs, focusedFieldKey, onFocus) { onUpdate(source.copy(ruleContent = rule.copy(callBackJs = it))) }
}
