package io.legado.app.ui.config

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.EventBus
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.config.ConfigScreen
import io.legado.app.ui.video.config.SettingsDialog
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment

class ConfigComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val configViewModel by viewModels<ConfigViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val configTag = intent.getStringExtra("configTag") ?: run {
            finish()
            return
        }

        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }

        launchCompose {
            ConfigScreen(
                configTag = configTag,
                onBack = { finish() },
                onShowCheckSourceConfig = {
                    showDialogFragment<CheckSourceConfig>()
                },
                onShowDirectLinkUploadConfig = {
                    showDialogFragment<DirectLinkUploadConfig>()
                },
                onShowVideoSettings = {
                    SettingsDialog(this).show(supportFragmentManager, "VideoSettings")
                },
                onClearCache = {
                    configViewModel.clearCache()
                },
                onClearWebViewData = {
                    configViewModel.clearWebViewData()
                },
                onShrinkDatabase = {
                    configViewModel.shrinkDatabase()
                }
            )
        }
    }
}
