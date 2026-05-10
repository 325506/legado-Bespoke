package io.legado.app.ui.compose.booksource.debug

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.booksource.debug.SourceDebugComposeViewModel
import io.legado.app.ui.compose.screens.booksource.debug.SourceDebugScreen
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.showDialogFragment
import kotlinx.coroutines.launch

class SourceDebugComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<SourceDebugComposeViewModel>()
    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            startSearch(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(intent.getStringExtra("key")) {
            launchCompose {
                SourceDebugScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() },
                    onScanQrCode = {
                        qrCodeResult.launch(null)
                    },
                    onShowSource = { title, source ->
                        source?.let {
                            showDialogFragment(TextDialog(title, it))
                        }
                    },
                    onRefreshExplore = {
                        lifecycleScope.launch {
                            viewModel.clearExploreCache()
                        }
                    }
                )
            }
        }
    }

    private fun startSearch(key: String) {
        viewModel.startDebug(key,
            start = { },
            error = { }
        )
    }
}
