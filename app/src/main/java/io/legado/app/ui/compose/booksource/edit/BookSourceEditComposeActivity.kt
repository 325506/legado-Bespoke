package io.legado.app.ui.compose.booksource.edit

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import io.legado.app.constant.Theme
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSource
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.booksource.debug.SourceDebugComposeActivity
import io.legado.app.ui.compose.screens.booksource.edit.BookSourceEditComposeViewModel
import io.legado.app.ui.compose.screens.booksource.edit.BookSourceEditScreen
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

class BookSourceEditComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    private val viewModel by viewModels<BookSourceEditComposeViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.initData(intent) {
            launchCompose {
                BookSourceEditScreen(
                    bookSource = viewModel.bookSource,
                    onSave = { source ->
                        viewModel.save(source) {
                            setResult(RESULT_OK, Intent().putExtra("origin", it.bookSourceUrl))
                            finish()
                        }
                    },
                    onDebug = { source ->
                        viewModel.save(source) {
                            val intent = android.content.Intent(this@BookSourceEditComposeActivity, SourceDebugComposeActivity::class.java).apply {
                                putExtra("key", it.bookSourceUrl)
                            }
                            startActivity(intent)
                        }
                    },
                    onClearCookie = { url ->
                        viewModel.clearCookie(url)
                        toastOnUi("Cookie已清除")
                    },
                    onCopySource = { json ->
                        sendToClip(json)
                        toastOnUi("已复制到剪贴板")
                    },
                    onPasteSource = {
                        viewModel.pasteSource { source ->
                            toastOnUi("粘贴成功")
                        }
                    },
                    onNavigateBack = {
                        val source = viewModel.bookSource ?: BookSource()
                        val originalSource = intent.getStringExtra("sourceUrl")?.let { url ->
                            appDb.bookSourceDao.getBookSource(url)
                        } ?: BookSource()
                        if (!source.equal(originalSource)) {
                            AlertDialog.Builder(this@BookSourceEditComposeActivity)
                                .setTitle("退出")
                                .setMessage("有未保存的修改，是否退出？")
                                .setPositiveButton("确定") { _, _ -> finish() }
                                .setNegativeButton("取消", null)
                                .show()
                        } else {
                            finish()
                        }
                    }
                )
            }
        }
    }
}
