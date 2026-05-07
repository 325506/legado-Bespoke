package io.legado.app.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import io.legado.app.ui.compose.screens.main.MainScreen
import io.legado.app.ui.compose.screens.bookshelf.BookshelfScreen
import io.legado.app.ui.compose.screens.explore.ExploreScreen
import io.legado.app.ui.compose.screens.rss.RssScreen
import io.legado.app.ui.compose.screens.my.MyConfigScreen
import io.legado.app.ui.compose.screens.book.detail.BookDetailScreen
import io.legado.app.ui.compose.screens.book.search.BookSearchScreen
import io.legado.app.ui.compose.screens.config.ConfigScreen
import io.legado.app.ui.compose.screens.about.AboutScreen

@Composable
fun LegadoNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Main.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController)
        }

        composable(Screen.Bookshelf.route) {
            BookshelfScreen(navController)
        }

        composable(Screen.Explore.route) {
            ExploreScreen(navController)
        }

        composable(Screen.Rss.route) {
            RssScreen(navController)
        }

        composable(Screen.MyConfig.route) {
            MyConfigScreen(navController)
        }

        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("bookUrl") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookUrl = backStackEntry.arguments?.getString("bookUrl") ?: return@composable
            BookDetailScreen(navController, bookUrl)
        }

        composable(Screen.BookSearch.route) {
            BookSearchScreen(navController)
        }

        composable(Screen.Config.route) {
            ConfigScreen(navController)
        }

        composable(Screen.About.route) {
            AboutScreen(navController)
        }
    }
}
