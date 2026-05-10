package io.legado.app.ui.compose.booksource.login

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.setPadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.flexbox.FlexboxLayout
import com.script.rhino.runScriptWithContext
import io.legado.app.R
import io.legado.app.constant.AppConst
import io.legado.app.constant.AppLog
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BaseSource
import io.legado.app.data.entities.rule.RowUi
import io.legado.app.databinding.ItemFilletTextBinding
import io.legado.app.databinding.ItemSelectorSingleBinding
import io.legado.app.databinding.ItemSourceEditBinding
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.http.CookieStore
import io.legado.app.help.webView.PooledWebView
import io.legado.app.help.webView.WebViewPool
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.SourceLoginJsExtensions
import io.legado.app.ui.login.SourceLoginViewModel
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

class SourceLoginComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<SourceLoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(intent, success = { source ->
            launchCompose {
                SourceLoginScreen(
                    source = source,
                    viewModel = viewModel,
                    activity = this@SourceLoginComposeActivity,
                    onNavigateBack = { finish() },
                    onShowLog = { showLogDialog() }
                )
            }
        }, error = {
            finish()
        })
    }

    private fun showLogDialog() {
        showDialogFragment<AppLogDialog>()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceLoginScreen(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    activity: AppCompatActivity,
    onNavigateBack: () -> Unit,
    onShowLog: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("登录: ${source.getTag()}") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            loginSubmit(source, viewModel, activity, onNavigateBack)
                        }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "确定"
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多"
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (source.loginUi.isNullOrEmpty()) {
                WebViewLoginContent(
                    source = source,
                    viewModel = viewModel,
                    modifier = Modifier.padding(paddingValues),
                    onFinish = onNavigateBack
                )
            } else {
                CustomUiLoginContent(
                    source = source,
                    viewModel = viewModel,
                    activity = activity,
                    modifier = Modifier.padding(paddingValues),
                    onFinish = onNavigateBack
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("查看请求头") },
                onClick = {
                    showMenu = false
                    source.getLoginHeader()?.let { header ->
                        activity.alert(
                            title = activity.getString(R.string.login_header),
                            message = header
                        ) {
                            positiveButton(R.string.copy_text) {
                                appCtx.sendToClip(header)
                            }
                        }
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("复制请求头") },
                onClick = {
                    showMenu = false
                    source.getLoginHeader()?.let {
                        appCtx.sendToClip(it)
                        context.toastOnUi("已复制")
                    }
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("删除请求头") },
                onClick = {
                    showMenu = false
                    source.removeLoginHeader()
                    context.toastOnUi("已删除")
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )
            DropdownMenuItem(
                text = { Text("日志") },
                onClick = {
                    showMenu = false
                    onShowLog()
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )
        }
    }
}

private fun loginSubmit(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    activity: AppCompatActivity,
    onFinish: () -> Unit
) {
    val loginUrl = source.getLoginJs()
    val loginInfo = viewModel.loginInfo
    val sourceLoginJsExtensions = SourceLoginJsExtensions(
        activity,
        source,
        viewModel.bookType,
        null
    )

    viewModel.execute {
        val loginData = loginInfo.toMutableMap()
        if (loginData.isEmpty()) {
            source.removeLoginInfo()
        } else if (source.putLoginInfo(GSON.toJson(loginData))) {
            try {
                val buttonFunctionJS =
                    "if (typeof login=='function'){ login.apply(this); } else { throw('Function login not implements!!!') }"
                val loginJS = loginUrl ?: return@execute
                runScriptWithContext {
                    source.evalJS("$loginJS\n$buttonFunctionJS") {
                        put("java", sourceLoginJsExtensions)
                        put("result", loginData)
                        put("book", viewModel.book)
                        put("chapter", viewModel.chapter)
                        put("isLongClick", false)
                    }
                }
            } catch (e: Exception) {
                AppLog.put("登录出错\n${e.localizedMessage}", e)
                withContext(Dispatchers.Main) {
                    activity.toastOnUi("登录出错\n${e.localizedMessage}")
                }
            }
        }
    }.onSuccess {
        withContext(Dispatchers.Main) {
            activity.toastOnUi(R.string.success)
            onFinish()
        }
    }
}

@Composable
fun WebViewLoginContent(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var checking by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var pooledWebView by remember { mutableStateOf<PooledWebView?>(null) }

    LaunchedEffect(Unit) {
        val pv = WebViewPool.acquire(context)
        pooledWebView = pv
        webViewRef = pv.realWebView
        pv.realWebView.onResume()
        pv.realWebView.settings.apply {
            useWideViewPort = true
            loadWithOverviewMode = true
            viewModel.headerMap[AppConst.UA_NAME]?.let {
                userAgentString = it
            }
        }
        val loginUrl = source.loginUrl
        if (!loginUrl.isNullOrBlank()) {
            val absoluteUrl = NetworkUtils.getAbsoluteURL(source.getKey(), loginUrl)
            pv.realWebView.loadUrl(absoluteUrl, viewModel.headerMap)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pooledWebView?.let { WebViewPool.release(it) }
            webViewRef?.onPause()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                webViewRef?.let {
                    (it.parent as? ViewGroup)?.removeView(it)
                    it
                } ?: WebView(ctx).also { webViewRef = it }
            },
            update = { webView ->
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        val cookie = CookieManager.getInstance().getCookie(url)
                        CookieStore.setCookie(source.getKey(), cookie)
                        super.onPageStarted(view, url, favicon)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        val cookie = CookieManager.getInstance().getCookie(url)
                        CookieStore.setCookie(source.getKey(), cookie)
                        if (checking) {
                            checking = false
                            onFinish()
                        }
                        super.onPageFinished(view, url)
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        return shouldOverrideUrlLoading(request.url)
                    }

                    private fun shouldOverrideUrlLoading(url: Uri): Boolean {
                        return when (url.scheme) {
                            "http", "https" -> false
                            else -> {
                                context.toastOnUi("将跳转到其他应用")
                                true
                            }
                        }
                    }

                    @SuppressLint("WebViewClientOnReceivedSslError")
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        handler?.proceed()
                    }
                }
                webView.webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        progress = newProgress
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (progress < 100) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Button(
                onClick = {
                    if (!checking) {
                        checking = true
                        context.toastOnUi("正在检查登录状态")
                        val loginUrl = source.loginUrl
                        if (!loginUrl.isNullOrBlank()) {
                            val absoluteUrl =
                                NetworkUtils.getAbsoluteURL(source.getKey(), loginUrl)
                            webViewRef?.loadUrl(absoluteUrl, viewModel.headerMap)
                        }
                    }
                },
                modifier = Modifier.padding(4.dp)
            ) {
                Text("检查登录")
            }
        }
    }
}

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
@Composable
fun CustomUiLoginContent(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    activity: AppCompatActivity,
    modifier: Modifier = Modifier,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var rowUis by remember { mutableStateOf<List<RowUi>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshKey by remember { mutableStateOf(0) }

    val sourceLoginJsExtensions = remember(source) {
        SourceLoginJsExtensions(
            activity,
            source,
            viewModel.bookType,
            object : SourceLoginJsExtensions.Callback {
                override fun upUiData(data: Map<String, Any?>?) {
                    activity.runOnUiThread {
                        handleUpUiData(data, rowUis, viewModel, activity)
                        refreshKey++
                    }
                }

                override fun reUiView(deltaUp: Boolean) {
                    activity.runOnUiThread {
                        handleReUiView(source, viewModel, activity) { newRowUis ->
                            rowUis = newRowUis
                            refreshKey++
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        val loginUiStr = source.loginUi
        if (loginUiStr.isNullOrBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        val codeStr = when {
            loginUiStr.startsWith("@js:") -> loginUiStr.substring(4)
            loginUiStr.startsWith("<js>") -> loginUiStr.substring(
                4,
                loginUiStr.lastIndexOf("<")
            )
            else -> null
        }
        if (codeStr != null) {
            withContext(Dispatchers.IO) {
                val loginUiJson = evalUiJs(source, viewModel, sourceLoginJsExtensions)
                rowUis = loginUi(loginUiJson)
            }
        } else {
            rowUis = loginUi(loginUiStr)
        }
        isLoading = false
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    NestedScrollView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS

                        val flexbox = FlexboxLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setPadding(
                                3.dpToPx(),
                                3.dpToPx(),
                                3.dpToPx(),
                                3.dpToPx()
                            )
                            setFlexWrap(com.google.android.flexbox.FlexWrap.WRAP)
                            setFlexDirection(com.google.android.flexbox.FlexDirection.ROW)
                            clipToPadding = false
                        }
                        addView(flexbox)
                    }
                },
                update = { scrollView ->
                    val flexbox = scrollView.getChildAt(0) as FlexboxLayout
                    flexbox.removeAllViews()
                    val loginInfo = viewModel.loginInfo
                    val loginUrl = source.getLoginJs()

                    rowUis?.forEachIndexed { index, rowUi ->
                        val view = when (rowUi.type) {
                            "text" -> createTextEditView(
                                activity,
                                flexbox,
                                rowUi,
                                index,
                                loginInfo,
                                source,
                                viewModel,
                                sourceLoginJsExtensions,
                                loginUrl,
                                isPassword = false
                            )

                            "password" -> createTextEditView(
                                activity,
                                flexbox,
                                rowUi,
                                index,
                                loginInfo,
                                source,
                                viewModel,
                                sourceLoginJsExtensions,
                                loginUrl,
                                isPassword = true
                            )

                            "button" -> createButtonView(
                                activity,
                                flexbox,
                                rowUi,
                                index,
                                loginInfo,
                                source,
                                viewModel,
                                sourceLoginJsExtensions,
                                loginUrl
                            )

                            "toggle" -> createToggleView(
                                activity,
                                flexbox,
                                rowUi,
                                index,
                                loginInfo,
                                source,
                                viewModel,
                                sourceLoginJsExtensions,
                                loginUrl
                            )

                            "select" -> createSelectView(
                                activity,
                                flexbox,
                                rowUi,
                                index,
                                loginInfo,
                                source,
                                viewModel,
                                sourceLoginJsExtensions,
                                loginUrl
                            )

                            else -> null
                        }
                        view?.let {
                            it.id = index + 1000
                            rowUi.style().apply(it)
                            flexbox.addView(it)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Button(
                onClick = {
                    loginSubmit(source, viewModel, activity, onFinish)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("登录")
            }
        }
    }
}

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
private fun createTextEditView(
    activity: AppCompatActivity,
    flexbox: FlexboxLayout,
    rowUi: RowUi,
    index: Int,
    loginInfo: MutableMap<String, String>,
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    loginUrl: String?,
    isPassword: Boolean
): View {
    val binding = ItemSourceEditBinding.inflate(activity.layoutInflater, flexbox, false)
    val editText = binding.editText
    val textInputLayout = binding.textInputLayout

    val viewName = rowUi.viewName
    if (viewName == null) {
        textInputLayout.hint = rowUi.name
    } else if (viewName.length in 3..19 && viewName.first() == '\'' && viewName.last() == '\'') {
        textInputLayout.hint = viewName.substring(1, viewName.length - 1)
    } else {
        textInputLayout.hint = rowUi.name
        Coroutine.async(viewModel.viewModelScope, Dispatchers.IO) {
            evalUiJs(source, viewModel, sourceLoginJsExtensions, viewName)
        }.onSuccess { n ->
            textInputLayout.hint = if (n.isNullOrEmpty()) "null" else n
        }.onError {
            textInputLayout.hint = "err"
        }
    }

    if (isPassword) {
        editText.inputType =
            InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
    }

    editText.setText(loginInfo[rowUi.name] ?: rowUi.default ?: "")

    val style = rowUi.style()
    when (style.layout_justifySelf) {
        "center" -> editText.gravity = Gravity.CENTER
        "flex_end" -> editText.gravity = Gravity.END
    }

    rowUi.action?.let { action ->
        val handler = buildMainHandler()
        val watcher = object : TextWatcher {
            private var content: String? = null
            private val runnable = Runnable {
                handleButtonClick(
                    source, action, rowUi.name, false,
                    viewModel, sourceLoginJsExtensions, loginUrl
                )
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                content = s.toString()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val reContent = s.toString()
                if (reContent != content) {
                    handler.removeCallbacks(runnable)
                    handler.postDelayed(runnable, 600)
                }
            }
        }
        editText.addTextChangedListener(watcher)
    }

    return binding.root
}

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
private fun createButtonView(
    activity: AppCompatActivity,
    flexbox: FlexboxLayout,
    rowUi: RowUi,
    index: Int,
    loginInfo: MutableMap<String, String>,
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    loginUrl: String?
): View {
    val binding = ItemFilletTextBinding.inflate(activity.layoutInflater, flexbox, false)
    val viewName = rowUi.viewName
    if (viewName == null) {
        binding.textView.text = rowUi.name
    } else if (viewName.length in 3..19 && viewName.first() == '\'' && viewName.last() == '\'') {
        val n = viewName.substring(1, viewName.length - 1)
        rowUi.viewName = n
        binding.textView.text = n
    } else {
        binding.textView.text = rowUi.name
        Coroutine.async(viewModel.viewModelScope, Dispatchers.IO) {
            evalUiJs(source, viewModel, sourceLoginJsExtensions, viewName)
        }.onSuccess { n ->
            if (n.isNullOrEmpty()) {
                binding.textView.text = "null"
            } else {
                rowUi.viewName = n
                binding.textView.text = n
            }
        }.onError {
            binding.textView.text = "err"
        }
    }

    binding.textView.setPadding(16.dpToPx())
    var lastClickTime = 0L
    var downTime = 0L

    binding.root.setOnClickListener {
        handleButtonClick(
            source, rowUi.action, rowUi.name, false,
            viewModel, sourceLoginJsExtensions, loginUrl
        )
    }

    binding.root.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.isSelected = true
                downTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                view.isSelected = false
                val upTime = System.currentTimeMillis()
                if (upTime - lastClickTime < 200) {
                    return@setOnTouchListener true
                }
                lastClickTime = upTime
                handleButtonClick(
                    source, rowUi.action, rowUi.name, upTime > downTime + 666,
                    viewModel, sourceLoginJsExtensions, loginUrl
                )
            }
            MotionEvent.ACTION_CANCEL -> {
                view.isSelected = false
            }
        }
        true
    }

    return binding.root
}

@SuppressLint("SetTextI18n", "ClickableViewAccessibility")
private fun createToggleView(
    activity: AppCompatActivity,
    flexbox: FlexboxLayout,
    rowUi: RowUi,
    index: Int,
    loginInfo: MutableMap<String, String>,
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    loginUrl: String?
): View {
    val binding = ItemFilletTextBinding.inflate(activity.layoutInflater, flexbox, false)
    var newName = rowUi.name
    var left = true

    val style = rowUi.style()
    when (style.layout_justifySelf) {
        "flex_start" -> binding.textView.gravity = Gravity.START
        "flex_end" -> binding.textView.gravity = Gravity.END
        "right" -> left = false
    }

    val chars = rowUi.chars?.filterNotNull() ?: listOf("chars is null")
    val infoV = loginInfo[rowUi.name]
    var char = if (infoV.isNullOrEmpty()) {
        rowUi.default ?: chars[0]
    } else {
        infoV
    }
    loginInfo[rowUi.name] = char

    val viewName = rowUi.viewName
    if (viewName == null) {
        binding.textView.text = if (left) char + rowUi.name else rowUi.name + char
    } else if (viewName.length in 3..19 && viewName.first() == '\'' && viewName.last() == '\'') {
        val n = viewName.substring(1, viewName.length - 1)
        rowUi.viewName = n
        newName = n
        binding.textView.text = if (left) char + n else n + char
    } else {
        binding.textView.text = if (left) char + rowUi.name else rowUi.name + char
        Coroutine.async(viewModel.viewModelScope, Dispatchers.IO) {
            evalUiJs(source, viewModel, sourceLoginJsExtensions, viewName)
        }.onSuccess { n ->
            if (n.isNullOrEmpty()) {
                binding.textView.text = char + "null"
            } else {
                rowUi.viewName = n
                newName = n
                binding.textView.text = if (left) char + n else n + char
            }
        }.onError {
            binding.textView.text = char + "err"
        }
    }

    binding.textView.setPadding(16.dpToPx())
    var lastClickTime = 0L
    var downTime = 0L

    binding.root.setOnClickListener {
        val currentIndex = chars.indexOf(char)
        val nextIndex = (currentIndex + 1) % chars.size
        char = chars.getOrNull(nextIndex) ?: ""
        loginInfo[rowUi.name] = char
        binding.textView.text = if (left) char + newName else newName + char
        handleButtonClick(
            source, rowUi.action, rowUi.name, false,
            viewModel, sourceLoginJsExtensions, loginUrl
        )
    }

    binding.root.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                view.isSelected = true
                downTime = System.currentTimeMillis()
            }
            MotionEvent.ACTION_UP -> {
                view.isSelected = false
                val upTime = System.currentTimeMillis()
                if (upTime - lastClickTime < 200) {
                    return@setOnTouchListener true
                }
                lastClickTime = upTime
                val currentIndex = chars.indexOf(char)
                val nextIndex = (currentIndex + 1) % chars.size
                char = chars.getOrNull(nextIndex) ?: ""
                loginInfo[rowUi.name] = char
                binding.textView.text = if (left) char + newName else newName + char
                handleButtonClick(
                    source, rowUi.action, rowUi.name, upTime > downTime + 666,
                    viewModel, sourceLoginJsExtensions, loginUrl
                )
            }
            MotionEvent.ACTION_CANCEL -> {
                view.isSelected = false
            }
        }
        true
    }

    return binding.root
}

@SuppressLint("SetTextI18n")
private fun createSelectView(
    activity: AppCompatActivity,
    flexbox: FlexboxLayout,
    rowUi: RowUi,
    index: Int,
    loginInfo: MutableMap<String, String>,
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    loginUrl: String?
): View {
    val binding = ItemSelectorSingleBinding.inflate(activity.layoutInflater, flexbox, false)
    val viewName = rowUi.viewName

    if (viewName == null) {
        binding.spName.text = rowUi.name
    } else if (viewName.length in 3..19 && viewName.first() == '\'' && viewName.last() == '\'') {
        binding.spName.text = viewName.substring(1, viewName.length - 1)
    } else {
        binding.spName.text = rowUi.name
        Coroutine.async(viewModel.viewModelScope, Dispatchers.IO) {
            evalUiJs(source, viewModel, sourceLoginJsExtensions, viewName)
        }.onSuccess { n ->
            binding.spName.text = if (n.isNullOrEmpty()) "null" else n
        }.onError {
            binding.spName.text = "err"
        }
    }

    val chars = rowUi.chars?.filterNotNull() ?: listOf("chars", "is null")
    val adapter = ArrayAdapter(
        activity,
        R.layout.item_text_common,
        chars
    )
    adapter.setDropDownViewResource(R.layout.item_spinner_dropdown)
    val selector = binding.spType
    selector.adapter = adapter
    val infoV = loginInfo[rowUi.name]
    val char = if (infoV.isNullOrEmpty()) {
        rowUi.default ?: chars[0]
    } else {
        infoV
    }
    loginInfo[rowUi.name] = char
    val i = chars.indexOf(char)
    selector.setSelectionSafely(i)

    selector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        var isInitializing = true
        override fun onItemSelected(
            parent: AdapterView<*>?,
            view: View?,
            position: Int,
            id: Long
        ) {
            if (isInitializing) {
                isInitializing = false
                return
            }
            loginInfo[rowUi.name] = chars[position]
            if (rowUi.action != null) {
                handleButtonClick(
                    source, rowUi.action, rowUi.name, false,
                    viewModel, sourceLoginJsExtensions, loginUrl
                )
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    val style = rowUi.style()
    when (style.layout_justifySelf) {
        "flex_start" -> selector.gravity = Gravity.START
        "flex_end" -> selector.gravity = Gravity.END
    }

    return binding.root
}

private fun handleButtonClick(
    source: BaseSource,
    action: String?,
    name: String,
    isLongClick: Boolean,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    loginUrl: String?
) {
    if (action.isAbsUrl()) {
        viewModel.context.openUrl(action!!)
    } else if (action != null) {
        viewModel.execute(context = Dispatchers.IO) {
            val loginJS = loginUrl ?: return@execute
            kotlin.runCatching {
                runScriptWithContext {
                    source.evalJS("$loginJS\n$action") {
                        put("java", sourceLoginJsExtensions)
                        put("result", viewModel.loginInfo.toMutableMap())
                        put("book", viewModel.book)
                        put("chapter", viewModel.chapter)
                        put("isLongClick", isLongClick)
                    }
                }
            }.onFailure { e ->
                ensureActive()
                AppLog.put("LoginUI Button $name JavaScript error", e)
            }
        }
    }
}

private suspend fun evalUiJs(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    sourceLoginJsExtensions: SourceLoginJsExtensions,
    jsStr: String? = null
): String? {
    val loginUiStr = jsStr ?: source.loginUi ?: return null
    val codeStr = when {
        loginUiStr.startsWith("@js:") -> loginUiStr.substring(4)
        loginUiStr.startsWith("<js>") -> loginUiStr.substring(
            4,
            loginUiStr.lastIndexOf("<")
        )
        else -> return loginUiStr
    }
    val loginJS = source.getLoginJs() ?: ""
    return try {
        withContext(Dispatchers.IO) {
            runScriptWithContext {
                source.evalJS("$loginJS\n$codeStr") {
                    put("java", sourceLoginJsExtensions)
                    put("result", viewModel.loginInfo.toMutableMap())
                    put("book", viewModel.book)
                    put("chapter", viewModel.chapter)
                }.toString()
            }
        }
    } catch (e: Exception) {
        AppLog.put(source.getTag() + " loginUi err:" + (e.localizedMessage ?: e.toString()), e)
        null
    }
}

private fun loginUi(json: String?): List<RowUi>? {
    return GSON.fromJsonArray<RowUi>(json).onFailure {
        AppLog.put("loginUi json parse err:" + it.localizedMessage, it)
    }.getOrNull()
}

@SuppressLint("SetTextI18n")
private fun handleUpUiData(
    data: Map<String, Any?>?,
    rowUis: List<RowUi>?,
    viewModel: SourceLoginViewModel,
    activity: AppCompatActivity
) {
    val loginInfo = viewModel.loginInfo
    if (data == null) {
        val newLoginInfo: MutableMap<String, String> = mutableMapOf()
        rowUis?.forEachIndexed { index, rowUi ->
            val default = rowUi.default
            val rowView = activity.findViewById<View>(index + 1000) ?: return@forEachIndexed
            when (rowView) {
                is io.legado.app.ui.widget.text.TextInputLayout -> {
                    val value = default ?: ""
                    newLoginInfo[rowUi.name] = value
                    rowView.editText?.setText(value)
                }
                is TextView -> {
                    when (rowUi.type) {
                        "button" -> {
                            rowView.text = rowUi.viewName ?: rowUi.name
                        }
                        "toggle" -> {
                            val char = default ?: run {
                                val chars =
                                    rowUi.chars?.filterNotNull() ?: listOf("chars is null")
                                chars.getOrNull(0) ?: ""
                            }
                            newLoginInfo[rowUi.name] = char
                            val name = rowUi.viewName ?: rowUi.name
                            val left = rowUi.style?.layout_justifySelf != "right"
                            rowView.text = if (left) char + name else name + char
                        }
                    }
                }
                is LinearLayout -> {
                    val chars = rowUi.chars?.filterNotNull() ?: listOf("chars", "is null")
                    val idx = chars.indexOf(default)
                    newLoginInfo[rowUi.name] = default ?: run {
                        chars.getOrNull(0) ?: ""
                    }
                    rowView.findViewById<AppCompatSpinner>(R.id.sp_type)?.setSelectionSafely(idx)
                }
            }
        }
        viewModel.loginInfo = newLoginInfo
        return
    }
    data.forEach { (key, value) ->
        val valueStr = value?.toString()
        val idx = rowUis?.indexOfFirst { it.name == key } ?: -1
        if (idx != -1) {
            val rowUi = rowUis?.getOrNull(idx) ?: return@forEach
            val v = valueStr ?: rowUi.default
            val rowView = activity.findViewById<View>(idx + 1000) ?: return@forEach
            when (rowView) {
                is io.legado.app.ui.widget.text.TextInputLayout -> {
                    val finalValue = v ?: ""
                    loginInfo[rowUi.name] = finalValue
                    rowView.editText?.setText(finalValue)
                }
                is TextView -> {
                    when (rowUi.type) {
                        "button" -> {
                            rowView.text = v ?: rowUi.viewName ?: key
                        }
                        "toggle" -> {
                            val char = v ?: run {
                                val chars =
                                    rowUi.chars?.filterNotNull() ?: listOf("chars is null")
                                chars.getOrNull(0) ?: ""
                            }
                            loginInfo[rowUi.name] = char
                            val name = rowUi.viewName ?: rowUi.name
                            val left = rowUi.style?.layout_justifySelf != "right"
                            rowView.text = if (left) char + name else name + char
                        }
                    }
                }
                is LinearLayout -> {
                    val items = rowUi.chars?.filterNotNull() ?: listOf("chars", "is null")
                    val i = items.indexOf(v)
                    rowView.findViewById<AppCompatSpinner>(R.id.sp_type)?.setSelectionSafely(i)
                }
            }
        } else {
            loginInfo[key] = valueStr ?: ""
        }
    }
}

private fun handleReUiView(
    source: BaseSource,
    viewModel: SourceLoginViewModel,
    activity: AppCompatActivity,
    onUpdate: (List<RowUi>?) -> Unit
) {
    val loginUiStr = source.loginUi ?: return
    val codeStr = loginUiStr.let {
        when {
            it.startsWith("@js:") -> it.substring(4)
            it.startsWith("<js>") -> it.substring(4, it.lastIndexOf("<"))
            else -> null
        }
    }
    val sourceLoginJsExtensions = SourceLoginJsExtensions(
        activity,
        source,
        viewModel.bookType,
        null
    )

    if (codeStr != null) {
        activity.lifecycleScope.launch(Dispatchers.Main) {
            val loginUiJson = withContext(Dispatchers.IO) {
                evalUiJs(source, viewModel, sourceLoginJsExtensions, codeStr)
            }
            val newRowUis = loginUi(loginUiJson)
            onUpdate(newRowUis)
        }
    } else {
        val newRowUis = loginUi(loginUiStr)
        onUpdate(newRowUis)
    }
}
