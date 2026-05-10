package io.legado.app.ui.compose.screens.config

import android.content.Intent
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.config.ConfigViewModel
import io.legado.app.ui.config.CoverConfigFragment
import io.legado.app.ui.config.ThemeConfigFragment
import io.legado.app.ui.config.WelcomeConfigFragment
import io.legado.app.ui.config.BackupRestoreActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    configTag: String,
    onBack: () -> Unit,
    viewModel: ConfigViewModel = viewModel(),
    onShowCheckSourceConfig: (() -> Unit)? = null,
    onShowDirectLinkUploadConfig: (() -> Unit)? = null,
    onShowVideoSettings: (() -> Unit)? = null,
    onClearCache: (() -> Unit)? = null,
    onClearWebViewData: (() -> Unit)? = null,
    onShrinkDatabase: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var titleRes by remember { mutableStateOf(R.string.other_setting) }

    LaunchedEffect(configTag) {
        titleRes = when (configTag) {
            ConfigTag.OTHER_CONFIG -> R.string.other_setting
            ConfigTag.THEME_CONFIG -> R.string.theme_setting
            ConfigTag.COVER_CONFIG -> R.string.cover_config
            ConfigTag.WELCOME_CONFIG -> R.string.welcome_style
            else -> R.string.other_setting
        }
    }

    Scaffold(
        topBar = {
            LegadoTopAppBar(
                title = context.getString(titleRes),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = context.getString(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* 更多信息 */ }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "info"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (configTag) {
                ConfigTag.BACKUP_CONFIG -> {
                    LaunchedEffect(Unit) {
                        context.startActivity(Intent(context, BackupRestoreActivity::class.java))
                        onBack()
                    }
                }
                ConfigTag.OTHER_CONFIG -> {
                    OtherSettingsScreen(
                        onNavigateBack = onBack,
                        onShowCheckSourceConfig = onShowCheckSourceConfig,
                        onShowDirectLinkUploadConfig = onShowDirectLinkUploadConfig,
                        onShowVideoSettings = onShowVideoSettings,
                        onClearCache = onClearCache,
                        onClearWebViewData = onClearWebViewData,
                        onShrinkDatabase = onShrinkDatabase
                    )
                }
                else -> {
                    FragmentContainer(
                        configTag = configTag
                    )
                }
            }
        }
    }
}

@Composable
fun FragmentContainer(
    configTag: String
) {
    val context = LocalContext.current
    val fragmentContainerId = remember { View.generateViewId() }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = fragmentContainerId
            }
        },
        update = { view ->
            val fragmentManager = (context as? androidx.fragment.app.FragmentActivity)?.supportFragmentManager
            val fragment = when (configTag) {
                ConfigTag.THEME_CONFIG -> ThemeConfigFragment()
                ConfigTag.COVER_CONFIG -> CoverConfigFragment()
                ConfigTag.WELCOME_CONFIG -> WelcomeConfigFragment()
                else -> null
            }

            fragment?.let {
                fragmentManager?.beginTransaction()
                    ?.replace(view.id, it, configTag)
                    ?.commit()
            }
        }
    )
}
