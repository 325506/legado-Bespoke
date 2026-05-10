package io.legado.app.ui.compose.screens.rss

import android.annotation.SuppressLint
import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.SystemClock
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.help.WebCacheManager
import io.legado.app.help.config.AppConfig
import io.legado.app.help.http.CookieManager
import io.legado.app.help.http.newCallResponse
import io.legado.app.help.http.text
import io.legado.app.help.webView.PooledWebView
import io.legado.app.help.webView.WebJsExtensions
import io.legado.app.help.webView.WebJsExtensions.Companion.JS_INJECTION
import io.legado.app.help.webView.WebJsExtensions.Companion.JS_URL
import io.legado.app.help.webView.WebJsExtensions.Companion.basicJs
import io.legado.app.help.webView.WebJsExtensions.Companion.nameBasic
import io.legado.app.help.webView.WebJsExtensions.Companion.nameCache
import io.legado.app.help.webView.WebJsExtensions.Companion.nameJava
import io.legado.app.help.webView.WebJsExtensions.Companion.nameSource
import io.legado.app.help.webView.WebJsExtensions.Companion.nameUrl
import io.legado.app.help.webView.WebViewPool
import io.legado.app.help.webView.WebViewPool.BLANK_HTML
import io.legado.app.help.webView.WebViewPool.DATA_HTML
import io.legado.app.ui.rss.read.ReadRssViewModel
import io.legado.app.ui.rss.read.RssJsExtensions
import io.legado.app.ui.rss.read.VisibleWebView
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import java.io.ByteArrayInputStream
import java.lang.ref.WeakReference
import java.net.URLDecoder
import java.util.regex.PatternSyntaxException

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReadRssScreen(
    viewModel: ReadRssViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onEditSource: (String) -> Unit = {},
    onLogin: (String) -> Unit = {},
    onReadRecord: (String?) -> Unit = {},
    onShowLog: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? AppCompatActivity

    var title by remember { mutableStateOf("") }
    var isStarred by remember { mutableStateOf(false) }
    var isTtsPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf<String?>(null) }

    var pooledWebView by remember { mutableStateOf<PooledWebView?>(null) }
    var customViewContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var interfaceInjected by remember { mutableStateOf<String?>(null) }
    var needClearHistory by remember { mutableStateOf(true) }
    val refreshNameList = remember { mutableListOf<String>() }

    val rssJsExtensions = remember(activity) {
        activity?.let { RssJsExtensions(it, viewModel.rssSource) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val titleObserver = androidx.lifecycle.Observer<String> { title = it ?: "" }
        viewModel.upTitleData.observe(lifecycleOwner, titleObserver)

        val starObserver = androidx.lifecycle.Observer<Boolean> { isStarred = viewModel.rssStar != null }
        viewModel.upStarMenuData.observe(lifecycleOwner, starObserver)

        val ttsObserver = androidx.lifecycle.Observer<Boolean> { isTtsPlaying = it }
        viewModel.upTtsMenuData.observe(lifecycleOwner, ttsObserver)

        onDispose {
            viewModel.upTitleData.removeObserver(titleObserver)
            viewModel.upStarMenuData.removeObserver(starObserver)
            viewModel.upTtsMenuData.removeObserver(ttsObserver)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pooledWebView?.let { WebViewPool.release(it) }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            viewModel.rssArticle?.let {
                                viewModel.addFavorite()
                                io.legado.app.ui.rss.favorites.RssFavoritesDialog(it).let { dialog ->
                                    (activity as? androidx.fragment.app.FragmentActivity)?.let { fa ->
                                        dialog.show(fa.supportFragmentManager, "favorite")
                                    }
                                }
                            }
                        }) {
                            Icon(
                                if (isStarred) Icons.Default.Star else Icons.Default.Info,
                                contentDescription = context.getString(R.string.favorite)
                            )
                        }
                        IconButton(onClick = {
                            val webView = pooledWebView?.realWebView ?: return@IconButton
                            if (viewModel.tts?.isSpeaking == true) {
                                viewModel.tts?.stop()
                            } else {
                                webView.settings.javaScriptEnabled = true
                                webView.evaluateJavascript("document.documentElement.outerHTML") {
                                    val html = StringEscapeUtils.unescapeJson(it).replace("^\"|\"$".toRegex(), "")
                                    viewModel.readAloud(
                                        Jsoup.parse(html).textArray().joinToString("\n")
                                    )
                                }
                            }
                        }) {
                            Icon(
                                if (isTtsPlaying) Icons.Default.Info else Icons.Default.Info,
                                contentDescription = context.getString(R.string.read_aloud)
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (progress < 100) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.TopCenter)
                )
            }

            AndroidView(
                factory = { ctx ->
                    val pw = WebViewPool.acquire(ctx)
                    pooledWebView = pw
                    val webView = pw.realWebView

                    webView.webChromeClient = object : WebChromeClient() {
                        override fun getDefaultVideoPoster(): Bitmap {
                            return super.getDefaultVideoPoster() ?: createBitmap(100, 100)
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }

                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                            isFullscreen = true
                            customViewCallback = callback
                            (webView.parent as? ViewGroup)?.let { parent ->
                                parent.removeView(webView)
                            }
                            view?.let {
                                (webView.parent as? FrameLayout)?.addView(it)
                                    ?: run {
                                        val container = (ctx as? Activity)?.findViewById<FrameLayout>(android.R.id.content)
                                        container?.addView(it)
                                    }
                            }
                        }

                        override fun onHideCustomView() {
                            isFullscreen = false
                            customViewCallback?.onCustomViewHidden()
                        }

                        override fun onCloseWindow(window: WebView?) {
                            onNavigateBack()
                        }

                        override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                            viewModel.rssSource?.let { source ->
                                if (source.showWebLog) {
                                    val messageLevel = consoleMessage.messageLevel().name
                                    val message = consoleMessage.message()
                                    AppLog.put("${source.getTag()}${messageLevel}: $message",
                                        io.legado.app.exception.NoStackTraceException("\n${message}\n- Line ${consoleMessage.lineNumber()} of ${consoleMessage.sourceId()}"))
                                    return true
                                }
                            }
                            return false
                        }
                    }

                    webView.addJavascriptInterface(object {
                        @JavascriptInterface
                        fun lockOrientation(orientation: String) {
                            activity?.runOnUiThread {
                                activity.requestedOrientation = when (orientation) {
                                    "portrait", "portrait-primary" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    "portrait-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                                    "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    "landscape-primary" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    "landscape-secondary" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                }
                            }
                        }

                        @JavascriptInterface
                        fun unlockOrientation() {
                            activity?.runOnUiThread {
                                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }

                        @JavascriptInterface
                        fun fullScreen(flag: Boolean) {
                            activity?.runOnUiThread {
                                if (flag) {
                                    isFullscreen = true
                                } else {
                                    isFullscreen = false
                                }
                            }
                        }

                        @JavascriptInterface
                        fun refreshPage() {
                            activity?.runOnUiThread {
                                webView.reload()
                            }
                        }
                    }, nameBasic)

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return shouldOverrideUrlLoading(request.url)
                        }

                        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "KotlinRedundantDiagnosticSuppress")
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            return shouldOverrideUrlLoading(url.toUri())
                        }

                        private fun shouldOverrideUrlLoading(url: Uri): Boolean {
                            viewModel.rssSource?.let { source ->
                                source.shouldOverrideUrlLoading?.takeUnless(String::isNullOrBlank)?.let { js ->
                                    val result = runCatching {
                                        source.evalJS(js) {
                                            put("java", rssJsExtensions)
                                            put("url", url.toString())
                                        }.toString()
                                    }.getOrNull()
                                    if (result.isTrue()) return true
                                }
                            }
                            return when (url.scheme) {
                                "http", "https" -> false
                                else -> {
                                    true
                                }
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            if (needClearHistory) {
                                needClearHistory = false
                                webView.clearHistory()
                            }
                            super.onPageStarted(view, url, favicon)
                            webView.evaluateJavascript(basicJs, null)
                        }

                        private var jsInjected = false

                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val url = request.url.toString()
                            val source = viewModel.rssSource ?: return super.shouldInterceptRequest(view, request)
                            if (request.isForMainFrame) {
                                if (viewModel.hasPreloadJs) {
                                    jsInjected = false
                                    if (url.startsWith("data:text/html;") || request.method == "POST") {
                                        return super.shouldInterceptRequest(view, request)
                                    }
                                    return runBlocking(Dispatchers.IO) {
                                        getModifiedContentWithJs(url, request) ?: super.shouldInterceptRequest(view, request)
                                    }
                                }
                            } else if (!jsInjected && url == nameUrl) {
                                jsInjected = true
                                val preloadJs = source.preloadJs ?: ""
                                return WebResourceResponse(
                                    "text/javascript",
                                    "utf-8",
                                    ByteArrayInputStream("(() => {$JS_INJECTION\n$preloadJs\n})();".toByteArray())
                                )
                            }
                            val blacklist = source.contentBlacklist?.splitNotBlank(",")
                            if (!blacklist.isNullOrEmpty()) {
                                blacklist.forEach {
                                    try {
                                        if (url.startsWith(it) || url.matches(it.toRegex())) {
                                            return createEmptyResource()
                                        }
                                    } catch (e: PatternSyntaxException) {
                                        AppLog.put("黑名单规则正则语法错误", e)
                                    }
                                }
                            } else {
                                val whitelist = source.contentWhitelist?.splitNotBlank(",")
                                if (!whitelist.isNullOrEmpty()) {
                                    whitelist.forEach {
                                        try {
                                            if (url.startsWith(it) || url.matches(it.toRegex())) {
                                                return super.shouldInterceptRequest(view, request)
                                            }
                                        } catch (e: PatternSyntaxException) {
                                            AppLog.put("白名单规则正则语法错误", e)
                                        }
                                    }
                                    return createEmptyResource()
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        private suspend fun getModifiedContentWithJs(url: String, request: WebResourceRequest): WebResourceResponse? {
                            try {
                                val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                                val res = io.legado.app.help.http.okHttpClient.newCallResponse {
                                    url(url)
                                    method(request.method, null)
                                    if (!cookie.isNullOrEmpty()) {
                                        addHeader("Cookie", cookie)
                                    }
                                    request.requestHeaders?.forEach { (key, value) ->
                                        addHeader(key, value)
                                    }
                                }
                                res.headers("Set-Cookie").forEach { setCookie ->
                                    android.webkit.CookieManager.getInstance().setCookie(url, setCookie)
                                }
                                val body = res.body
                                val contentType = body.contentType()
                                val mimeType = contentType?.toString()?.substringBefore(";") ?: "text/html"
                                val charset = contentType?.charset() ?: Charsets.UTF_8
                                val bodyText = body.text().let { originalText ->
                                    val headIndex = originalText.indexOf("<head", ignoreCase = true)
                                    if (headIndex >= 0) {
                                        val closingHeadIndex = originalText.indexOf('>', startIndex = headIndex)
                                        if (closingHeadIndex >= 0) {
                                            StringBuilder(originalText).insert(closingHeadIndex + 1, JS_URL).toString()
                                        } else originalText
                                    } else originalText
                                }
                                return WebResourceResponse(mimeType, charset.name(), ByteArrayInputStream(bodyText.toByteArray(charset)))
                            } catch (_: Exception) {
                                return null
                            }
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            view.title?.let { t ->
                                if (t != url && t != view.url && t.isNotBlank() && url != BLANK_HTML && !url.contains(t)) {
                                    title = t
                                }
                            }
                            viewModel.rssSource?.injectJs?.let {
                                if (it.isNotBlank()) {
                                    view.evaluateJavascript(it, null)
                                }
                            }
                        }

                        @SuppressLint("WebViewClientOnReceivedSslError")
                        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                            handler?.proceed()
                        }

                        private fun createEmptyResource(): WebResourceResponse {
                            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
                        }
                    }

                    webView.setOnLongClickListener {
                        val hitTestResult = webView.hitTestResult
                        if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE ||
                            hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                            hitTestResult.extra?.let { webPic ->
                                showImageDialog = webPic
                                return@setOnLongClickListener true
                            }
                        }
                        false
                    }

                    webView.setDownloadListener { url, _, contentDisposition, _, _ ->
                        var fileName = URLUtil.guessFileName(url, contentDisposition, null)
                        fileName = URLDecoder.decode(fileName, "UTF-8")
                    }

                    webView
                },
                modifier = Modifier.fillMaxSize()
            )

            viewModel.contentLiveData.observe(lifecycleOwner) { content ->
                viewModel.rssArticle?.let {
                    val webView = pooledWebView?.realWebView ?: return@observe
                    upWebviewSettings(webView)
                    initJavascriptInterface(webView)
                    val rssSource = viewModel.rssSource
                    val html = viewModel.clHtml(content, rssSource?.style)
                    val url = NetworkUtils.getAbsoluteURL(it.origin, it.link).substringBefore("@js")
                    val baseUrl = if (rssSource?.loadWithBaseUrl == false) null else url
                    webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", url)
                }
            }

            viewModel.urlLiveData.observe(lifecycleOwner) { urlState ->
                val webView = pooledWebView?.realWebView ?: return@observe
                upWebviewSettings(webView, urlState.getUserAgent())
                initJavascriptInterface(webView)
                CookieManager.applyToWebView(urlState.url)
                webView.loadUrl(urlState.url, urlState.headerMap)
            }

            viewModel.htmlLiveData.observe(lifecycleOwner) { html ->
                val webView = pooledWebView?.realWebView ?: return@observe
                viewModel.rssSource?.let {
                    upWebviewSettings(webView)
                    initJavascriptInterface(webView)
                    val baseUrl = if (it.loadWithBaseUrl) it.sourceUrl else null
                    webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", it.sourceUrl)
                }
            }
        }
    }

    BackHandler {
        val webView = pooledWebView?.realWebView
        if (isFullscreen) {
            customViewCallback?.onCustomViewHidden()
            return@BackHandler
        }
        if (webView?.canGoBack() == true) {
            val list = webView.copyBackForwardList()
            val size = list.size
            if (size == 1) {
                onNavigateBack()
                return@BackHandler
            }
            val currentIndex = list.currentIndex
            val currentItem = list.currentItem
            val currentUrl = currentItem?.originalUrl ?: BLANK_HTML
            val currentTitle = currentItem?.title
            var steps = 1
            for (i in currentIndex - 1 downTo 0) {
                val item = list.getItemAtIndex(i)
                val itemTitle = item.title
                val index = refreshNameList.indexOf(itemTitle)
                if (index != -1) {
                    refreshNameList.removeAt(index)
                    steps++
                    continue
                }
                val itemUrl = item.originalUrl
                if (itemUrl == BLANK_HTML) {
                    onNavigateBack()
                    return@BackHandler
                }
                if (itemUrl != currentUrl || itemTitle != currentTitle) break
                if (currentUrl == DATA_HTML) break
                steps++
            }
            if (steps == size) {
                onNavigateBack()
                return@BackHandler
            }
            webView.goBackOrForward(-steps)
        } else {
            onNavigateBack()
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text(context.getString(R.string.refresh)) },
            onClick = {
                showMenu = false
                pooledWebView?.realWebView?.reload()
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.share)) },
            onClick = {
                showMenu = false
                val url = pooledWebView?.realWebView?.url ?: viewModel.rssArticle?.link
                url?.let { (context as? Activity)?.share(it) }
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
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
            text = { Text(context.getString(R.string.open_in_browser)) },
            onClick = {
                showMenu = false
                pooledWebView?.realWebView?.url?.let {
                    (context as? Activity)?.openUrl(it)
                }
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(context.getString(R.string.read_record)) },
            onClick = {
                showMenu = false
                onReadRecord(viewModel.rssSource?.sourceUrl)
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
            text = { Text(context.getString(R.string.log)) },
            onClick = {
                showMenu = false
                onShowLog()
            },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
        )
    }

    if (showImageDialog != null) {
        AlertDialog(
            onDismissRequest = { showImageDialog = null },
            title = { Text(context.getString(R.string.action_save)) },
            text = { Text(context.getString(R.string.action_save) + "?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageDialog?.let { webPic ->
                        val path = io.legado.app.utils.ACache.get().getAsString(AppConst.imagePathKey)
                        if (path.isNullOrEmpty()) {
                            viewModel.saveImage(webPic, Uri.EMPTY)
                        } else {
                            viewModel.saveImage(webPic, path.toUri())
                        }
                    }
                    showImageDialog = null
                }) {
                    Text(context.getString(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImageDialog = null }) {
                    Text(context.getString(android.R.string.cancel))
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun upWebviewSettings(webView: WebView, userAgent: String? = null) {
    val activity = webView.context as? AppCompatActivity ?: return
    val vm = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
        .create(ReadRssViewModel::class.java)
    vm.rssSource?.let { s ->
        webView.settings.run {
            this.userAgentString = userAgent ?: vm.headerMap[AppConst.UA_NAME] ?: AppConfig.userAgent
            javaScriptEnabled = s.enableJs
            cacheMode = if (s.cacheFirst) WebSettings.LOAD_CACHE_ELSE_NETWORK else WebSettings.LOAD_DEFAULT
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun initJavascriptInterface(webView: WebView) {
    val activity = webView.context as? AppCompatActivity ?: return
    val vm = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
        .create(ReadRssViewModel::class.java)
    vm.rssSource?.let { source ->
        val interfaceInjected = (webView.tag as? String?)
        if (interfaceInjected != source.sourceUrl) {
            webView.tag = source.sourceUrl
            if (!vm.hasPreloadJs) return
            val webJsExtensions = WebJsExtensions(source, activity, webView)
            webView.addJavascriptInterface(webJsExtensions, nameJava)
            webView.addJavascriptInterface(source, nameSource)
            webView.addJavascriptInterface(WebCacheManager, nameCache)
        }
    }
}
