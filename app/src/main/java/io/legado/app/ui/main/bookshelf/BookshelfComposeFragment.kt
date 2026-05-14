package io.legado.app.ui.main.bookshelf

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.DialogBookshelfConfigBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.compose.screens.bookshelf.BookshelfScreen
import io.legado.app.ui.compose.screens.bookshelf.BookshelfViewModel
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.main.MainViewModel
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.checkByIndex
import io.legado.app.utils.getCheckedIndex
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.postEvent
import io.legado.app.utils.readText
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi
import io.legado.app.constant.EventBus

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
                        requireContext().startActivityForBook(book)
                    },
                    onBookLongClick = { book ->
                        startActivity<BookInfoActivity> {
                            putExtra("name", book.name)
                            putExtra("author", book.author)
                        }
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
                    onShowBookshelfConfig = { configBookshelf() }
                )
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

    private fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            var bookshelfLayout = AppConfig.bookshelfLayout
            var bookshelfSort = AppConfig.bookshelfSort
            var showBookname = AppConfig.showBookname
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        if (AppConfig.bookGroupStyle !in 0 until spGroupStyle.count) {
                            AppConfig.bookGroupStyle = 0
                        }
                        if (bookshelfLayout !in 0 until rgLayout.childCount) {
                            bookshelfLayout = 0
                            AppConfig.bookshelfLayout = 0
                        }
                        if (bookshelfSort !in 0 until rgSort.childCount) {
                            bookshelfSort = 0
                            AppConfig.bookshelfSort = 0
                        }
                        if (showBookname !in 0 until rgbLayout.childCount) {
                            showBookname = 0
                            AppConfig.showBookname = 0
                        }
                        spGroupStyle.setSelection(AppConfig.bookGroupStyle)
                        swShowUnread.isChecked = AppConfig.showUnread
                        swShowLastUpdateTime.isChecked = AppConfig.showLastUpdateTime
                        swShowWaitUpBooks.isChecked = AppConfig.showWaitUpCount
                        swShowBookshelfFastScroller.isChecked = AppConfig.showBookshelfFastScroller
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgbLayout.checkByIndex(showBookname)
                        if (bookshelfLayout < 2) {
                            bookNameChoice.visibility = View.GONE
                        }
                        rgLayout.setOnCheckedChangeListener { group, checkedId ->
                            val index = group.getCheckedIndex()
                            bookNameChoice.visibility = if (index > 1) View.VISIBLE else View.GONE
                        }
                        rgSort.checkByIndex(bookshelfSort)
                        margin.progress = AppConfig.bookshelfMargin
                    }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    var notifyMain = false
                    var recreate = false
                    if (AppConfig.bookGroupStyle != spGroupStyle.selectedItemPosition) {
                        AppConfig.bookGroupStyle = spGroupStyle.selectedItemPosition
                        notifyMain = true
                    }
                    if (showBookname != rgbLayout.getCheckedIndex()) {
                        AppConfig.showBookname = rgbLayout.getCheckedIndex()
                        recreate = true
                    }
                    if (AppConfig.bookshelfMargin != margin.progress) {
                        AppConfig.bookshelfMargin = margin.progress
                        recreate = true
                    }
                    if (AppConfig.showUnread != swShowUnread.isChecked) {
                        AppConfig.showUnread = swShowUnread.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showLastUpdateTime != swShowLastUpdateTime.isChecked) {
                        AppConfig.showLastUpdateTime = swShowLastUpdateTime.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (AppConfig.showWaitUpCount != swShowWaitUpBooks.isChecked) {
                        AppConfig.showWaitUpCount = swShowWaitUpBooks.isChecked
                        activityViewModel.postUpBooksLiveData(true)
                    }
                    if (AppConfig.showBookshelfFastScroller != swShowBookshelfFastScroller.isChecked) {
                        AppConfig.showBookshelfFastScroller = swShowBookshelfFastScroller.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        AppConfig.bookshelfSort = rgSort.getCheckedIndex()
                    }
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        AppConfig.bookshelfLayout = rgLayout.getCheckedIndex()
                        recreate = true
                    }
                    if (recreate) {
                        postEvent(EventBus.RECREATE, "")
                    } else if (notifyMain) {
                        postEvent(EventBus.NOTIFY_MAIN, false)
                    }
                }
            }
            cancelButton()
        }
    }
}
