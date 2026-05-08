package io.legado.app.ui.book.import.local

import android.os.Bundle
import io.legado.app.constant.Theme
import io.legado.app.data.entities.Book
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.importbook.ImportBookScreen
import io.legado.app.utils.startActivityForBook

class ImportBookComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchCompose {
            ImportBookScreen(
                onBack = { finish() },
                onReadBook = { book ->
                    startReadBook(book)
                }
            )
        }
    }

    private fun startReadBook(book: Book) {
        startActivityForBook(book)
        finish()
    }
}
