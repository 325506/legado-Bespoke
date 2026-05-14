package io.legado.app.ui.book.search

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.search.SearchScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.utils.startActivity

class SearchComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<SearchViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.getStringExtra("key")
        val searchScope = intent.getStringExtra("searchScope")

        setContent {
            LegadoTheme(theme = themeMode) {
                SearchScreen(
                    viewModel = viewModel,
                    initialKey = key,
                    initialSearchScope = searchScope,
                    onBack = { finish() },
                    onBookClick = { name, author, bookUrl ->
                        startActivity<io.legado.app.ui.book.info.BookInfoActivity> {
                            putExtra("name", name)
                            putExtra("author", author)
                            putExtra("bookUrl", bookUrl)
                        }
                    },
                    onSourceManage = {
                        startActivity<io.legado.app.ui.compose.booksource.BookSourceComposeActivity>()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.resume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.pause()
    }

    companion object {
        fun start(context: Context, key: String?, searchScope: String? = null) {
            context.startActivity<SearchComposeActivity> {
                putExtra("key", key)
                putExtra("searchScope", searchScope)
            }
        }

        fun start(context: Context, source: BookSource, key: String? = null) {
            context.startActivity<SearchComposeActivity> {
                putExtra("key", key)
                putExtra("searchScope", SearchScope(source).toString())
            }
        }

        fun start(context: Context, source: BookSourcePart, key: String? = null) {
            context.startActivity<SearchComposeActivity> {
                putExtra("key", key)
                putExtra("searchScope", SearchScope(source).toString())
            }
        }
    }
}
