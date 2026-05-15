package io.legado.app.ui.book.explore

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.explore.ExploreShowScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.utils.startActivity

class ExploreShowComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<io.legado.app.ui.compose.screens.explore.ExploreShowViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceUrl = intent.getStringExtra("sourceUrl")
        val exploreUrl = intent.getStringExtra("exploreUrl")
        val exploreName = intent.getStringExtra("exploreName")

        setContent {
            LegadoTheme(theme = themeMode) {
                ExploreShowScreen(
                    viewModel = viewModel,
                    sourceUrl = sourceUrl,
                    exploreUrl = exploreUrl,
                    exploreName = exploreName,
                    onBack = { finish() },
                    onBookClick = { book ->
                        startActivity<io.legado.app.ui.book.info.BookInfoComposeActivity> {
                            putExtra("name", book.name)
                            putExtra("author", book.author)
                            putExtra("bookUrl", book.bookUrl)
                        }
                    }
                )
            }
        }
    }
}
