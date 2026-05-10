package io.legado.app.ui.compose.booksource.login

import android.os.Bundle
import androidx.activity.viewModels
import io.legado.app.constant.Theme
import io.legado.app.data.entities.BaseSource
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.booksource.login.SourceLoginScreen
import io.legado.app.ui.login.SourceLoginViewModel
import io.legado.app.utils.showDialogFragment

class SourceLoginComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<SourceLoginViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(intent, success = { source ->
            launchCompose {
                SourceLoginScreen(
                    source = source,
                    onNavigateBack = { finish() },
                    onShowDialog = {
                        showDialogFragment<io.legado.app.ui.login.SourceLoginDialog>()
                    }
                )
            }
        }, error = {
            finish()
        })
    }
}
