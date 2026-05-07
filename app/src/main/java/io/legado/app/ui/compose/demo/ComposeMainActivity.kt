package io.legado.app.ui.compose.demo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.navigation.LegadoNavHost
import io.legado.app.ui.compose.theme.LegadoTheme

class ComposeMainActivity : ComposeActivity(
    fullScreen = true,
    theme = Theme.Auto,
    toolBarTheme = Theme.Auto,
    transparent = false
) {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegadoTheme(theme = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    LegadoNavHost(navController = navController)
                }
            }
        }
    }
}
