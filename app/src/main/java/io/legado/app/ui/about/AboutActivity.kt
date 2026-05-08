package io.legado.app.ui.about

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.about.AboutScreen
import io.legado.app.ui.compose.theme.LegadoTheme

class AboutActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LegadoTheme(theme = themeMode) {
                val navController = rememberNavController()
                AboutScreen(navController = navController)
            }
        }
    }
}
