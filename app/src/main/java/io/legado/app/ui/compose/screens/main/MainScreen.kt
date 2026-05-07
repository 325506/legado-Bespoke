package io.legado.app.ui.compose.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import io.legado.app.ui.compose.components.LegadoBottomNavigation
import io.legado.app.ui.compose.components.LegadoBottomNavItem
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.ui.compose.screens.bookshelf.BookshelfScreen
import io.legado.app.ui.compose.screens.explore.ExploreScreen
import io.legado.app.ui.compose.screens.my.MyConfigScreen
import io.legado.app.ui.compose.screens.rss.RssScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavHostController) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val bottomNavItems = listOf(
        LegadoBottomNavItem(
            id = 0,
            label = "书架",
            icon = Icons.Default.Home
        ),
        LegadoBottomNavItem(
            id = 1,
            label = "发现",
            icon = Icons.Default.Star
        ),
        LegadoBottomNavItem(
            id = 2,
            label = "RSS",
            icon = Icons.Default.Info
        ),
        LegadoBottomNavItem(
            id = 3,
            label = "我的",
            icon = Icons.Default.Person
        )
    )

    val titles = listOf(
        "书架",
        "发现",
        "RSS",
        "我的"
    )

    Scaffold(
        topBar = {
            LegadoTopAppBar(
                title = titles[selectedTab]
            )
        },
        bottomBar = {
            LegadoBottomNavigation(
                items = bottomNavItems,
                selectedItem = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            when (selectedTab) {
                0 -> BookshelfScreen(navController)
                1 -> ExploreScreen(navController)
                2 -> RssScreen(navController)
                3 -> MyConfigScreen(navController)
            }
        }
    }
}
