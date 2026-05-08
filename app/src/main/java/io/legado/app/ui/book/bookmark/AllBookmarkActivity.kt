package io.legado.app.ui.book.bookmark

import android.os.Bundle
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.bookmark.BookmarkScreen

/**
 * 所有书签 - Compose 版本
 */
class AllBookmarkActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            BookmarkScreen(
                onBack = { finish() }
            )
        }
    }
}