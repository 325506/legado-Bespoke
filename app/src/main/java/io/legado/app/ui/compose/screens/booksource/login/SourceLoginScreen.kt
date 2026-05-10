package io.legado.app.ui.compose.screens.booksource.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.legado.app.data.entities.BaseSource
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.WebViewLoginFragment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceLoginScreen(
    source: BaseSource,
    onNavigateBack: () -> Unit,
    onShowDialog: () -> Unit
) {
    val context = LocalContext.current

    LegadoTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("登录: ${source.getTag()}") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (source.loginUi.isNullOrEmpty()) {
                AndroidView(
                    factory = { ctx ->
                        val fragment = WebViewLoginFragment()
                        fragment.arguments = android.os.Bundle().apply {
                            putString("sourceUrl", source.getKey())
                        }
                        android.widget.FrameLayout(ctx).apply {
                            id = android.view.View.generateViewId()
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            } else {
                LaunchedEffect(Unit) {
                    onShowDialog()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}
