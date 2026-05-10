package io.legado.app.ui.rss.source.debug

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.R
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.RssSourceDebugScreen
import io.legado.app.ui.compose.screens.rss.RssSourceDebugViewModel
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.showDialogFragment

class RssSourceDebugComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<RssSourceDebugViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceKey = intent.getStringExtra("key")

        setContent {
            LegadoTheme(theme = themeMode) {
                RssSourceDebugScreen(
                    viewModel = viewModel,
                    sourceKey = sourceKey,
                    onNavigateBack = { finish() },
                    onShowListSrc = {
                        showDialogFragment(TextDialog("Html", viewModel.listSrc))
                    },
                    onShowContentSrc = {
                        showDialogFragment(TextDialog("Html", viewModel.contentSrc))
                    }
                )
            }
        }
    }
}
