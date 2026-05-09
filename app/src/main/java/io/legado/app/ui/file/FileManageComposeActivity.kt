package io.legado.app.ui.file

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.filemanage.FileManageScreen
import io.legado.app.ui.compose.screens.filemanage.FileManageViewModel
import androidx.activity.viewModels

class FileManageComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto
    
    private val viewModel: FileManageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!viewModel.goBackDir()) {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
        
        launchCompose {
            FileManageScreen(
                onBack = { finish() },
                viewModel = viewModel
            )
        }
    }
}
