package io.legado.app.ui.compose.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.RssSource
import io.legado.app.help.config.AppConfig
import io.legado.app.service.WebService
import io.legado.app.ui.book.explore.ExploreShowComposeActivity
import io.legado.app.ui.book.search.SearchComposeActivity
import io.legado.app.ui.compose.booksource.edit.BookSourceEditComposeActivity
import io.legado.app.ui.compose.booksource.login.SourceLoginComposeActivity
import io.legado.app.ui.compose.dialog.BookshelfConfigDialog
import io.legado.app.ui.compose.screens.bookshelf.BookshelfScreen
import io.legado.app.ui.compose.screens.bookshelf.BookshelfViewModel
import io.legado.app.ui.compose.screens.explore.ExploreScreen
import io.legado.app.ui.compose.screens.explore.ExploreScreenViewModel
import io.legado.app.ui.compose.screens.my.MyScreen
import io.legado.app.ui.compose.screens.rss.RssScreen
import io.legado.app.ui.main.rss.RssViewModel
import io.legado.app.ui.rss.article.RssSortComposeActivity
import io.legado.app.ui.rss.favorites.RssFavoritesComposeActivity
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.rss.source.manage.RssSourceComposeActivity
import io.legado.app.ui.rss.subscription.RuleSubActivity
import io.legado.app.utils.openUrl
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.startActivity

data class MainTab(
    val id: Int,
    val labelResId: Int,
    val iconResId: Int,
    val selectedIconResId: Int
)

@Composable
fun MainScreen(
    onUpBooksCount: Int = 0,
    onBookLongClick: (Book) -> Unit = {},
    onUpdateToc: (List<Book>, Boolean) -> Unit = { _, _ -> },
    onShowLog: () -> Unit = {},
    onShowGroupManage: () -> Unit = {},
    onShowHelp: () -> Unit = {}
) {
    val context = LocalContext.current
    val showDiscovery = AppConfig.showDiscovery
    val showRss = AppConfig.showRSS

    val tabs = remember(showDiscovery, showRss) {
        val list = mutableListOf<MainTab>()
        list.add(MainTab(0, R.string.bookshelf, R.drawable.ic_bottom_books_e, R.drawable.ic_bottom_books_s))
        if (showDiscovery) {
            list.add(MainTab(1, R.string.discovery, R.drawable.ic_bottom_explore_e, R.drawable.ic_bottom_explore_s))
        }
        if (showRss) {
            list.add(MainTab(2, R.string.rss, R.drawable.ic_bottom_rss_feed_e, R.drawable.ic_bottom_rss_feed_s))
        }
        list.add(MainTab(3, R.string.my, R.drawable.ic_bottom_person_e, R.drawable.ic_bottom_person_s))
        list
    }

    val defaultHomePage = AppConfig.defaultHomePage
    var selectedTab by rememberSaveable {
        val initialTab = when {
            defaultHomePage == "explore" && showDiscovery -> 1
            defaultHomePage == "rss" && showRss -> {
                if (showDiscovery) 2 else 1
            }
            defaultHomePage == "my" -> tabs.lastIndex
            else -> 0
        }
        mutableIntStateOf(initialTab)
    }

    var showBookshelfConfig by remember { mutableStateOf(false) }
    var webServiceRunning by remember { mutableStateOf(WebService.isRun) }
    var webServiceAddress by remember { mutableStateOf(if (WebService.isRun) WebService.hostAddress else "") }

    val bookshelfViewModel: BookshelfViewModel = viewModel()
    val rssViewModel: RssViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            if (tab.id == 0 && onUpBooksCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text("$onUpBooksCount")
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (selectedTab == index) tab.selectedIconResId else tab.iconResId
                                        ),
                                        contentDescription = stringResource(tab.labelResId)
                                    )
                                }
                            } else {
                                Icon(
                                    painter = painterResource(
                                        if (selectedTab == index) tab.selectedIconResId else tab.iconResId
                                    ),
                                    contentDescription = stringResource(tab.labelResId)
                                )
                            }
                        },
                        label = { Text(text = stringResource(tab.labelResId)) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val currentTab = tabs.getOrNull(selectedTab)?.id ?: 0
            when (currentTab) {
                0 -> {
                    BookshelfScreen(
                        viewModel = bookshelfViewModel,
                        onBookClick = { book ->
                            context.startActivity<io.legado.app.ui.book.info.BookInfoComposeActivity> {
                                putExtra("name", book.name)
                                putExtra("author", book.author)
                            }
                        },
                        onBookLongClick = onBookLongClick,
                        onUpdateToc = onUpdateToc,
                        onShowLog = onShowLog,
                        onShowGroupManage = onShowGroupManage,
                        onShowBookshelfConfig = { showBookshelfConfig = true }
                    )
                }

                1 -> {
                    val exploreViewModel: ExploreScreenViewModel = viewModel()
                    ExploreScreen(
                        viewModel = exploreViewModel,
                        onOpenExplore = { sourceUrl, title, exploreUrl ->
                            context.startActivity<ExploreShowComposeActivity> {
                                putExtra("exploreName", title)
                                putExtra("sourceUrl", sourceUrl)
                                putExtra("exploreUrl", exploreUrl)
                            }
                        },
                        onEditSource = { sourceUrl ->
                            context.startActivity<BookSourceEditComposeActivity> {
                                putExtra("sourceUrl", sourceUrl)
                            }
                        },
                        onToTop = { source ->
                            exploreViewModel.topSource(source)
                        },
                        onDeleteSource = { source ->
                            exploreViewModel.deleteSource(source)
                        },
                        onSearchBook = { source ->
                            SearchComposeActivity.start(context, source)
                        },
                        onLogin = { source ->
                            context.startActivity<SourceLoginComposeActivity> {
                                putExtra("type", "bookSource")
                                putExtra("key", source.bookSourceUrl)
                            }
                        }
                    )
                }

                2 -> {
                    RssScreen(
                        onOpenRss = { rssSource ->
                            openRssSource(context, rssSource, rssViewModel)
                        },
                        onEdit = { rssSource ->
                            context.startActivity<RssSourceEditActivity> {
                                putExtra("sourceUrl", rssSource.sourceUrl)
                            }
                        },
                        onToTop = { rssSource ->
                            rssViewModel.topSource(rssSource)
                        },
                        onDel = { rssSource ->
                            rssViewModel.del(rssSource)
                        },
                        onDisable = { rssSource ->
                            rssViewModel.disable(rssSource)
                        },
                        onLogin = { rssSource ->
                            context.startActivity<io.legado.app.ui.login.SourceLoginActivity> {
                                putExtra("type", "rssSource")
                                putExtra("key", rssSource.sourceUrl)
                            }
                        },
                        onRuleSubscriptionClick = {
                            context.startActivity<RuleSubActivity>()
                        },
                        onRssConfig = {
                            context.startActivity<RssSourceComposeActivity>()
                        },
                        onRssStar = {
                            context.startActivity<RssFavoritesComposeActivity>()
                        }
                    )
                }

                3 -> {
                    MyScreen(
                        onShowHelp = onShowHelp,
                        webServiceState = Pair(webServiceRunning, webServiceAddress.ifEmpty { context.getString(R.string.web_service_desc) }),
                        onWebServiceToggle = { checked ->
                            webServiceRunning = checked
                            context.putPrefBoolean(io.legado.app.constant.PreferKey.webService, checked)
                            if (checked) {
                                WebService.start(context)
                            } else {
                                WebService.stop(context)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showBookshelfConfig) {
        BookshelfConfigDialog(
            viewModel = bookshelfViewModel,
            onDismiss = { showBookshelfConfig = false }
        )
    }
}

private fun openRssSource(context: android.content.Context, rssSource: RssSource, rssViewModel: RssViewModel) {
    if (rssSource.singleUrl) {
        rssViewModel.getSingleUrl(rssSource) { url ->
            if (url.startsWith("http", true)) {
                ReadRssActivity.start(
                    context,
                    true,
                    rssSource.sourceUrl,
                    rssSource.sourceName,
                    url
                )
            } else {
                context.openUrl(url)
            }
        }
    } else {
        rssViewModel.launchRssWithHtml(rssSource, noStartHtml = {
            context.startActivity<RssSortComposeActivity> {
                putExtra("sourceUrl", rssSource.sourceUrl)
            }
        }) { html ->
            ReadRssActivity.start(
                context,
                true,
                rssSource.sourceUrl,
                rssSource.sourceName,
                startHtml = html
            )
        }
    }
}
