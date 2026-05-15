package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.info.BookInfoComposeActivity
import io.legado.app.ui.compose.dialog.BookshelfConfigDialog
import io.legado.app.ui.compose.screens.bookshelf.BookshelfScreen
import io.legado.app.ui.compose.screens.bookshelf.BookshelfViewModel
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.readText
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi

class BookshelfComposeFragment() :
    VMBaseFragment<BookshelfViewModel>(R.layout.fragment_bookshelf_compose),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    val activityViewModel by activityViewModels<MainViewModel>()
    override val viewModel by viewModels<BookshelfViewModel>()
    private lateinit var composeView: ComposeView
    private var showConfigDialog by mutableStateOf(false)

    private val exportResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage(DirectLinkUpload.getSummary())
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    requireContext().sendToClip(uri.toString())
                }
            }
        }
    }

    private val importBookshelf = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(requireContext())?.let { text ->
                viewModel.importBookshelf(text, viewModel.groupId.value)
            }
        }.onFailure {
            toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    private val waitDialog by lazy {
        WaitDialog(requireContext()).apply {
            setOnCancelListener {
                viewModel.addBookJob?.cancel()
            }
        }
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        composeView = view.findViewById(R.id.compose_view)
        composeView.setContent {
            LegadoTheme {
                BookshelfScreen(
                    viewModel = viewModel,
                    onBookClick = { book ->
                        startActivity<BookInfoComposeActivity> {
                            putExtra("name", book.name)
                            putExtra("author", book.author)
                        }
                    },
                    onBookLongClick = { book ->
                        requireContext().startActivityForBook(book)
                    },
                    onGroupClick = { group ->
                        viewModel.setGroupId(group.groupId)
                    },
                    onGroupLongClick = { group ->
                        showDialogFragment<GroupManageDialog>()
                    },
                    onUpdateToc = { books, onlyUpdateRead ->
                        activityViewModel.upToc(books, onlyUpdateRead)
                    },
                    onAddUrl = { showAddBookByUrlAlert() },
                    onExportBookshelf = { exportBookshelf(it) },
                    onImportBookshelf = { importBookshelfAlert() },
                    onShowLog = { showDialogFragment<AppLogDialog>() },
                    onShowGroupManage = { showDialogFragment<GroupManageDialog>() },
                    onShowBookshelfConfig = { showConfigDialog = true }
                )

                if (showConfigDialog) {
                    BookshelfConfigDialog(
                        viewModel = viewModel,
                        onDismiss = { showConfigDialog = false }
                    )
                }
            }
        }

        viewModel.addBookProgressLiveData.observe(viewLifecycleOwner) { count ->
            if (count < 0) {
                waitDialog.dismiss()
            } else {
                waitDialog.setText("添加中... ($count)")
            }
        }
    }

    fun gotoTop() {
    }

    fun backToRoot(): Boolean {
        return viewModel.backToRoot()
    }

    private fun showAddBookByUrlAlert() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    waitDialog.setText("添加中...")
                    waitDialog.show()
                    viewModel.addBookByUrl(it)
                }
            }
            cancelButton()
        }
    }

    private fun exportBookshelf(books: List<Book>) {
        viewModel.exportBookshelf(books) { file ->
            exportResult.launch {
                mode = HandleFileContract.EXPORT
                fileData =
                    HandleFileContract.FileData("bookshelf.json", file, "application/json")
            }
        }
    }

    private fun importBookshelfAlert() {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, viewModel.groupId.value)
                }
            }
            cancelButton()
            neutralButton(R.string.select_file) {
                importBookshelf.launch {
                    mode = HandleFileContract.FILE
                    allowExtensions = arrayOf("txt", "json")
                }
            }
        }
    }

}
