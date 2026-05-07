package io.legado.app.ui.compose.base

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

abstract class ComposeViewModel : ViewModel() {
    abstract fun onCreate(savedInstanceState: Bundle?)
    abstract fun onDestroy()
}

@Composable
inline fun <reified VM : ViewModel> getComposeViewModel(): VM {
    return viewModel()
}
