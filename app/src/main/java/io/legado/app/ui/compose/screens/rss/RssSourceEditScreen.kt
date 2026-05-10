package io.legado.app.ui.compose.screens.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.theme.LegadoTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssSourceEditScreen(
    viewModel: RssSourceEditViewModel = viewModel(),
    initialSourceUrl: String? = null,
    onNavigateBack: () -> Unit = {},
    onSave: (RssSource) -> Unit = {},
    onDebug: (RssSource) -> Unit = {},
    onLogin: (RssSource) -> Unit = {},
    onCopySource: (String) -> Unit = {},
    onShareSource: (String) -> Unit = {},
    onShareQr: (String, String) -> Unit = { _, _ -> },
    onPasteSource: (RssSource) -> Unit = {},
    onImportQr: () -> Unit = {},
    onImportFile: () -> Unit = {},
    onShowHelp: () -> Unit = {},
    onShowLog: () -> Unit = {},
    onSetVariable: (RssSource) -> Unit = {},
    onClearCookie: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val rssSource by viewModel.rssSource.collectAsState()
    
    var sourceName by remember { mutableStateOf("") }
    var sourceUrl by remember { mutableStateOf("") }
    var sourceIcon by remember { mutableStateOf("") }
    var sourceGroup by remember { mutableStateOf("") }
    var sourceComment by remember { mutableStateOf("") }
    var searchUrl by remember { mutableStateOf("") }
    var sortUrl by remember { mutableStateOf("") }
    var loginUrl by remember { mutableStateOf("") }
    var loginUi by remember { mutableStateOf("") }
    var loginCheckJs by remember { mutableStateOf("") }
    var coverDecodeJs by remember { mutableStateOf("") }
    var header by remember { mutableStateOf("") }
    var variableComment by remember { mutableStateOf("") }
    var concurrentRate by remember { mutableStateOf("") }
    var jsLib by remember { mutableStateOf("") }
    
    var startHtml by remember { mutableStateOf("") }
    var startStyle by remember { mutableStateOf("") }
    var startJs by remember { mutableStateOf("") }
    var preloadJs by remember { mutableStateOf("") }
    
    var ruleArticles by remember { mutableStateOf("") }
    var ruleNextPage by remember { mutableStateOf("") }
    var ruleTitle by remember { mutableStateOf("") }
    var rulePubDate by remember { mutableStateOf("") }
    var ruleDescription by remember { mutableStateOf("") }
    var ruleImage by remember { mutableStateOf("") }
    var ruleLink by remember { mutableStateOf("") }
    
    var ruleContent by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    var injectJs by remember { mutableStateOf("") }
    var contentWhitelist by remember { mutableStateOf("") }
    var contentBlacklist by remember { mutableStateOf("") }
    var shouldOverrideUrlLoading by remember { mutableStateOf("") }
    
    var enabled by remember { mutableStateOf(true) }
    var singleUrl by remember { mutableStateOf(false) }
    var enabledCookieJar by remember { mutableStateOf(false) }
    var preload by remember { mutableStateOf(false) }
    var enableJs by remember { mutableStateOf(true) }
    var loadWithBaseUrl by remember { mutableStateOf(true) }
    var showWebLog by remember { mutableStateOf(false) }
    var cacheFirst by remember { mutableStateOf(false) }
    
    var type by remember { mutableStateOf(0) }
    var articleStyle by remember { mutableStateOf(0) }
    
    var selectedTab by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var autoComplete by remember { mutableStateOf(false) }

    LaunchedEffect(initialSourceUrl) {
        viewModel.initData(initialSourceUrl) { source ->
            sourceName = source.sourceName
            sourceUrl = source.sourceUrl
            sourceIcon = source.sourceIcon
            sourceGroup = source.sourceGroup ?: ""
            sourceComment = source.sourceComment ?: ""
            searchUrl = source.searchUrl ?: ""
            sortUrl = source.sortUrl ?: ""
            loginUrl = source.loginUrl ?: ""
            loginUi = source.loginUi ?: ""
            loginCheckJs = source.loginCheckJs ?: ""
            coverDecodeJs = source.coverDecodeJs ?: ""
            header = source.header ?: ""
            variableComment = source.variableComment ?: ""
            concurrentRate = source.concurrentRate ?: ""
            jsLib = source.jsLib ?: ""
            
            startHtml = source.startHtml ?: ""
            startStyle = source.startStyle ?: ""
            startJs = source.startJs ?: ""
            preloadJs = source.preloadJs ?: ""
            
            ruleArticles = source.ruleArticles ?: ""
            ruleNextPage = source.ruleNextPage ?: ""
            ruleTitle = source.ruleTitle ?: ""
            rulePubDate = source.rulePubDate ?: ""
            ruleDescription = source.ruleDescription ?: ""
            ruleImage = source.ruleImage ?: ""
            ruleLink = source.ruleLink ?: ""
            
            ruleContent = source.ruleContent ?: ""
            style = source.style ?: ""
            injectJs = source.injectJs ?: ""
            contentWhitelist = source.contentWhitelist ?: ""
            contentBlacklist = source.contentBlacklist ?: ""
            shouldOverrideUrlLoading = source.shouldOverrideUrlLoading ?: ""
            
            enabled = source.enabled
            singleUrl = source.singleUrl
            enabledCookieJar = source.enabledCookieJar == true
            preload = source.preload
            enableJs = source.enableJs
            loadWithBaseUrl = source.loadWithBaseUrl
            showWebLog = source.showWebLog == true
            cacheFirst = source.cacheFirst == true
            
            type = source.type
            articleStyle = source.articleStyle
        }
    }

    fun buildRssSource(): RssSource {
        return RssSource(
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            sourceIcon = sourceIcon,
            sourceGroup = sourceGroup.takeIf { it.isNotEmpty() },
            sourceComment = sourceComment.takeIf { it.isNotEmpty() },
            searchUrl = searchUrl.takeIf { it.isNotEmpty() },
            sortUrl = sortUrl.takeIf { it.isNotEmpty() },
            loginUrl = loginUrl.takeIf { it.isNotEmpty() },
            loginUi = loginUi.takeIf { it.isNotEmpty() },
            loginCheckJs = loginCheckJs.takeIf { it.isNotEmpty() },
            coverDecodeJs = coverDecodeJs.takeIf { it.isNotEmpty() },
            header = header.takeIf { it.isNotEmpty() },
            variableComment = variableComment.takeIf { it.isNotEmpty() },
            concurrentRate = concurrentRate.takeIf { it.isNotEmpty() },
            jsLib = jsLib.takeIf { it.isNotEmpty() },
            startHtml = startHtml.takeIf { it.isNotEmpty() },
            startStyle = startStyle.takeIf { it.isNotEmpty() },
            startJs = startJs.takeIf { it.isNotEmpty() },
            preloadJs = preloadJs.takeIf { it.isNotEmpty() },
            ruleArticles = ruleArticles.takeIf { it.isNotEmpty() },
            ruleNextPage = ruleNextPage.takeIf { it.isNotEmpty() },
            ruleTitle = ruleTitle.takeIf { it.isNotEmpty() },
            rulePubDate = rulePubDate.takeIf { it.isNotEmpty() },
            ruleDescription = ruleDescription.takeIf { it.isNotEmpty() },
            ruleImage = ruleImage.takeIf { it.isNotEmpty() },
            ruleLink = ruleLink.takeIf { it.isNotEmpty() },
            ruleContent = ruleContent.takeIf { it.isNotEmpty() },
            style = style.takeIf { it.isNotEmpty() },
            injectJs = injectJs.takeIf { it.isNotEmpty() },
            contentWhitelist = contentWhitelist.takeIf { it.isNotEmpty() },
            contentBlacklist = contentBlacklist.takeIf { it.isNotEmpty() },
            shouldOverrideUrlLoading = shouldOverrideUrlLoading.takeIf { it.isNotEmpty() },
            enabled = enabled,
            singleUrl = singleUrl,
            enabledCookieJar = enabledCookieJar,
            preload = preload,
            enableJs = enableJs,
            loadWithBaseUrl = loadWithBaseUrl,
            showWebLog = showWebLog,
            cacheFirst = cacheFirst,
            type = type,
            articleStyle = articleStyle
        )
    }

    LegadoTheme {
        Scaffold(
            topBar = {
                LegadoTopAppBar(
                    title = context.getString(R.string.rss_source_edit),
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
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(context.getString(R.string.source_tab_base)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("开始") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("列表") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("WEB_VIEW") }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CheckboxWithLabel(
                                    checked = enabled,
                                    onCheckedChange = { enabled = it },
                                    label = context.getString(R.string.is_enable)
                                )
                                CheckboxWithLabel(
                                    checked = singleUrl,
                                    onCheckedChange = { singleUrl = it },
                                    label = context.getString(R.string.single_url)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CheckboxWithLabel(
                                    checked = enabledCookieJar,
                                    onCheckedChange = { enabledCookieJar = it },
                                    label = context.getString(R.string.auto_save_cookie)
                                )
                                CheckboxWithLabel(
                                    checked = preload,
                                    onCheckedChange = { preload = it },
                                    label = context.getString(R.string.enable_preload)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            EditField("源名称", sourceName) { sourceName = it }
                            EditField("源地址", sourceUrl) { sourceUrl = it }
                            EditField("源图标", sourceIcon) { sourceIcon = it }
                            EditField("源分组", sourceGroup) { sourceGroup = it }
                            EditField("源注释", sourceComment) { sourceComment = it }
                            EditField("搜索地址", searchUrl) { searchUrl = it }
                            EditField("排序地址", sortUrl) { sortUrl = it }
                            EditField("登录地址", loginUrl) { loginUrl = it }
                            EditField("登录UI", loginUi) { loginUi = it }
                            EditField("登录检查JS", loginCheckJs) { loginCheckJs = it }
                            EditField("封面解码JS", coverDecodeJs) { coverDecodeJs = it }
                            EditField("请求头", header) { header = it }
                            EditField("变量说明", variableComment) { variableComment = it }
                            EditField("并发率", concurrentRate) { concurrentRate = it }
                            EditField("jsLib", jsLib) { jsLib = it }
                        }
                        1 -> {
                            EditField("开始HTML", startHtml) { startHtml = it }
                            EditField("开始样式", startStyle) { startStyle = it }
                            EditField("开始JS", startJs) { startJs = it }
                            EditField("预加载JS", preloadJs) { preloadJs = it }
                        }
                        2 -> {
                            EditField("文章列表规则", ruleArticles) { ruleArticles = it }
                            EditField("下一页规则", ruleNextPage) { ruleNextPage = it }
                            EditField("标题规则", ruleTitle) { ruleTitle = it }
                            EditField("日期规则", rulePubDate) { rulePubDate = it }
                            EditField("描述规则", ruleDescription) { ruleDescription = it }
                            EditField("图片规则", ruleImage) { ruleImage = it }
                            EditField("链接规则", ruleLink) { ruleLink = it }
                        }
                        3 -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CheckboxWithLabel(
                                    checked = enableJs,
                                    onCheckedChange = { enableJs = it },
                                    label = context.getString(R.string.enable_js)
                                )
                                CheckboxWithLabel(
                                    checked = loadWithBaseUrl,
                                    onCheckedChange = { loadWithBaseUrl = it },
                                    label = context.getString(R.string.load_with_base_url)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CheckboxWithLabel(
                                    checked = showWebLog,
                                    onCheckedChange = { showWebLog = it },
                                    label = context.getString(R.string.load_with_web_log)
                                )
                                CheckboxWithLabel(
                                    checked = cacheFirst,
                                    onCheckedChange = { cacheFirst = it },
                                    label = context.getString(R.string.cache_first)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            EditField("内容规则", ruleContent) { ruleContent = it }
                            EditField("样式", style) { style = it }
                            EditField("注入JS", injectJs) { injectJs = it }
                            EditField("内容白名单", contentWhitelist) { contentWhitelist = it }
                            EditField("内容黑名单", contentBlacklist) { contentBlacklist = it }
                            EditField("URL拦截JS", shouldOverrideUrlLoading) { shouldOverrideUrlLoading = it }
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("保存") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onSave(source)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("调试源") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onDebug(source)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("登录") },
                onClick = {
                    showMenu = false
                    if (loginUrl.isNotEmpty()) {
                        val source = buildRssSource()
                        onLogin(source)
                    }
                },
                enabled = loginUrl.isNotEmpty(),
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("自动补全") },
                onClick = { autoComplete = !autoComplete },
                leadingIcon = { 
                    if (autoComplete) Icon(Icons.Default.Info, contentDescription = null)
                    else Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("复制源") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onCopySource(io.legado.app.utils.GSON.toJson(source))
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("粘贴源") },
                onClick = {
                    showMenu = false
                    onPasteSource(buildRssSource())
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("分享文本") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onShareSource(io.legado.app.utils.GSON.toJson(source))
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("分享二维码") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onShareQr(
                        io.legado.app.utils.GSON.toJson(source),
                        context.getString(R.string.share_rss_source)
                    )
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("扫码导入") },
                onClick = {
                    showMenu = false
                    onImportQr()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("文件导入") },
                onClick = {
                    showMenu = false
                    onImportFile()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("设置变量") },
                onClick = {
                    showMenu = false
                    val source = buildRssSource()
                    onSetVariable(source)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("清除Cookie") },
                onClick = {
                    showMenu = false
                    onClearCookie(sourceUrl)
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("日志") },
                onClick = {
                    showMenu = false
                    onShowLog()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text("帮助") },
                onClick = {
                    showMenu = false
                    onShowHelp()
                },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun CheckboxWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
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
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        "请输入",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RssSourceEditScreenPreview() {
    RssSourceEditScreen()
}
