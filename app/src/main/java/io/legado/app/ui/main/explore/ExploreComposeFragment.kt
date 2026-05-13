package io.legado.app.ui.main.explore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.databinding.FragmentExploreComposeBinding
import io.legado.app.ui.book.explore.ExploreShowComposeActivity
import io.legado.app.ui.book.search.SearchComposeActivity
import io.legado.app.ui.compose.booksource.edit.BookSourceEditComposeActivity
import io.legado.app.ui.compose.booksource.login.SourceLoginComposeActivity
import io.legado.app.ui.compose.screens.explore.ExploreScreen
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class ExploreComposeFragment() : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore_compose),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreComposeBinding::bind)

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.composeView.setContent {
            LegadoTheme {
                ExploreScreen(
                    onOpenExplore = { sourceUrl, title, exploreUrl ->
                        startActivity<ExploreShowComposeActivity> {
                            putExtra("exploreName", title)
                            putExtra("sourceUrl", sourceUrl)
                            putExtra("exploreUrl", exploreUrl)
                        }
                    },
                    onEditSource = { sourceUrl ->
                        startActivity<BookSourceEditComposeActivity> {
                            putExtra("sourceUrl", sourceUrl)
                        }
                    },
                    onToTop = { source ->
                        viewModel.topSource(source)
                    },
                    onDeleteSource = { source ->
                        viewModel.deleteSource(source)
                    },
                    onSearchBook = { source ->
                        SearchComposeActivity.start(requireContext(), source)
                    },
                    onLogin = { source ->
                        startActivity<SourceLoginComposeActivity> {
                            putExtra("type", "bookSource")
                            putExtra("key", source.bookSourceUrl)
                        }
                    }
                )
            }
        }
    }

    fun compressExplore(): Boolean {
        return false
    }
}
