package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.FragmentRssComposeBinding
import io.legado.app.ui.compose.screens.rss.RssScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.rss.article.ReadRecordDialog
import io.legado.app.ui.rss.article.RssSortComposeActivity
import io.legado.app.ui.rss.favorites.RssFavoritesComposeActivity
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.rss.source.manage.RssSourceComposeActivity
import io.legado.app.ui.rss.subscription.RuleSubActivity
import io.legado.app.utils.openUrl
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class RssComposeFragment() : VMBaseFragment<RssViewModel>(R.layout.fragment_rss_compose), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<RssViewModel>()
    private val binding by viewBinding(FragmentRssComposeBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.composeView.setContent {
            LegadoTheme {
                RssScreen(
                    onOpenRss = { rssSource ->
                        openRss(rssSource)
                    },
                    onEdit = { rssSource ->
                        startActivity<RssSourceEditActivity> {
                            putExtra("sourceUrl", rssSource.sourceUrl)
                        }
                    },
                    onToTop = { rssSource ->
                        viewModel.topSource(rssSource)
                    },
                    onLogin = { rssSource ->
                        startActivity<io.legado.app.ui.login.SourceLoginActivity> {
                            putExtra("type", "rssSource")
                            putExtra("key", rssSource.sourceUrl)
                        }
                    },
                    onDel = { rssSource ->
                        viewModel.del(rssSource)
                    },
                    onDisable = { rssSource ->
                        viewModel.disable(rssSource)
                    },
                    onRuleSubscriptionClick = {
                        startActivity<RuleSubActivity>()
                    },
                    onReadRecord = {
                        showDialogFragment(ReadRecordDialog())
                    },
                    onRssConfig = {
                        startActivity<RssSourceComposeActivity>()
                    },
                    onRssStar = {
                        startActivity<RssFavoritesComposeActivity>()
                    }
                )
            }
        }
    }

    private fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            viewModel.getSingleUrl(rssSource) { url ->
                if (url.startsWith("http", true)) {
                    ReadRssActivity.start(
                        requireContext(),
                        true,
                        rssSource.sourceUrl,
                        rssSource.sourceName,
                        url
                    )
                } else {
                    context?.openUrl(url)
                }
            }
        } else {
            viewModel.launchRssWithHtml(rssSource, noStartHtml = {
                startActivity<RssSortComposeActivity> {
                    putExtra("sourceUrl", rssSource.sourceUrl)
                }
            }) { html ->
                ReadRssActivity.start(
                    requireContext(),
                    true,
                    rssSource.sourceUrl,
                    rssSource.sourceName,
                    startHtml = html
                )
            }
        }
    }
}
