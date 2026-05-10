package io.legado.app.ui.rss.favorites

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.RssFavoritesComposeViewModel
import io.legado.app.ui.compose.screens.rss.RssFavoritesScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.rss.read.ReadRssComposeActivity

class RssFavoritesComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<RssFavoritesComposeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LegadoTheme(theme = themeMode) {
                RssFavoritesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onReadRss = { rssStar ->
                        ReadRssComposeActivity.start(
                            this,
                            rssStar.origin,
                            rssStar.title,
                            rssStar.link,
                            rssStar.group ?: ""
                        )
                    }
                )
            }
        }
    }
}
