package io.legado.app.ui.rss.read

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.ReadRssScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.article.ReadRecordDialog
import io.legado.app.ui.rss.source.edit.RssSourceEditComposeActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.showDialogFragment

class ReadRssComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<ReadRssViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.initData(intent)

        setContent {
            LegadoTheme(theme = themeMode) {
                ReadRssScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onEditSource = { sourceUrl ->
                        startActivity<RssSourceEditComposeActivity> {
                            putExtra("sourceUrl", sourceUrl)
                        }
                    },
                    onLogin = { sourceUrl ->
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", sourceUrl)
                        }
                    },
                    onReadRecord = { origin ->
                        showDialogFragment(ReadRecordDialog(origin))
                    },
                    onShowLog = {
                        showDialogFragment<AppLogDialog>()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.initData(intent)
    }

    companion object {
        fun start(context: android.content.Context, singleTop: Boolean, origin: String, title: String? = null, url: String? = null, startHtml: String? = null) {
            context.startActivity<ReadRssComposeActivity> {
                putExtra("origin", origin)
                putExtra("title", title)
                putExtra("openUrl", url)
                putExtra("startHtml", startHtml)
                if (singleTop) {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }
        }

        fun start(context: android.content.Context, origin: String, title: String?, link: String, sort: String) {
            context.startActivity<ReadRssComposeActivity> {
                putExtra("origin", origin)
                putExtra("title", title)
                putExtra("link", link)
                putExtra("sort", sort)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}
