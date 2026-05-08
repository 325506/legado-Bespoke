package io.legado.app.ui.compose.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.theme.LegadoTheme

abstract class ComposeActivity(
    fullScreen: Boolean = true,
    theme: Theme = Theme.Auto,
    toolBarTheme: Theme = Theme.Auto,
    transparent: Boolean = false
) : AppCompatActivity() {

    protected abstract val themeMode: Theme

    protected fun launchCompose(content: @Composable () -> Unit) {
        setContent {
            LegadoTheme(theme = themeMode) {
                content()
            }
        }
    }
}
