package io.legado.app.ui.rss.article

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.rss.RssSortComposeViewModel
import io.legado.app.ui.compose.screens.rss.RssSortScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.read.ReadRssComposeActivity
import io.legado.app.utils.startActivity
import io.legado.app.utils.showDialogFragment

class RssSortComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<RssSortComposeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(intent)

        setContent {
            LegadoTheme(theme = themeMode) {
                RssSortScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onReadRss = { rssArticle ->
                        ReadRssComposeActivity.start(
                            this,
                            rssArticle.origin,
                            rssArticle.title,
                            rssArticle.link,
                            rssArticle.sort ?: ""
                        )
                    },
                    onEditSource = { sourceUrl ->
                        startActivity<io.legado.app.ui.rss.source.edit.RssSourceEditComposeActivity> {
                            putExtra("sourceUrl", sourceUrl)
                        }
                    },
                    onLogin = { sourceUrl ->
                        startActivity<SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", sourceUrl)
                        }
                    },
                    onSetVariable = {
                        viewModel.rssSource?.let { source ->
                            io.legado.app.ui.widget.dialog.VariableDialog(
                                getString(io.legado.app.R.string.set_source_variable),
                                source.getKey(),
                                source.getVariable(),
                                source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取")
                            ).let { dialog ->
                                showDialogFragment(dialog)
                            }
                        }
                    },
                    onReadRecord = {
                        viewModel.rssSource?.sourceUrl?.let {
                            showDialogFragment(ReadRecordDialog(it))
                        }
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
        fun start(context: android.content.Context, sortUrl: String?, sourceUrl: String, key: String? = null) {
            context.startActivity<RssSortComposeActivity> {
                putExtra("sortUrl", sortUrl)
                putExtra("sourceUrl", sourceUrl)
                putExtra("key", key)
            }
        }
    }
}
