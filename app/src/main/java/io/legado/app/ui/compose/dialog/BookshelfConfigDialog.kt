package io.legado.app.ui.compose.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.compose.screens.bookshelf.BookshelfViewModel
import io.legado.app.utils.postEvent
import io.legado.app.constant.EventBus

@Composable
fun BookshelfConfigDialog(
    viewModel: BookshelfViewModel,
    onDismiss: () -> Unit
) {
    var bookGroupStyle by remember { mutableIntStateOf(AppConfig.bookGroupStyle.coerceIn(0, 1)) }
    var bookshelfLayout by remember { mutableIntStateOf(AppConfig.bookshelfLayout.coerceIn(0, 7)) }
    var bookshelfSort by remember { mutableIntStateOf(AppConfig.bookshelfSort.coerceIn(0, 5)) }
    var showBookname by remember { mutableIntStateOf(AppConfig.showBookname.coerceIn(0, 2)) }
    var showUnread by remember { mutableStateOf(AppConfig.showUnread) }
    var showLastUpdateTime by remember { mutableStateOf(AppConfig.showLastUpdateTime) }
    var showWaitUpCount by remember { mutableStateOf(AppConfig.showWaitUpCount) }
    var showBookshelfFastScroller by remember { mutableStateOf(AppConfig.showBookshelfFastScroller) }
    var bookshelfMargin by remember { mutableIntStateOf(AppConfig.bookshelfMargin.coerceIn(0, 60)) }

    val layoutOptions = listOf(
        R.string.layout_list,
        R.string.layout_list_compact,
        R.string.layout_grid2,
        R.string.layout_grid3,
        R.string.layout_grid4,
        R.string.layout_grid5,
        R.string.layout_grid6
    )

    val sortOptions = listOf(
        R.string.bookshelf_px_0,
        R.string.bookshelf_px_1,
        R.string.bookshelf_px_2,
        R.string.bookshelf_px_3,
        R.string.bookshelf_px_4,
        R.string.bookshelf_px_5
    )

    val booknameOptions = listOf(
        R.string.show,
        R.string.hide,
        R.string.overlay
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.bookshelf_layout),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.group_style),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (bookGroupStyle == 0),
                                onClick = { bookGroupStyle = 0 },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (bookGroupStyle == 0),
                            onClick = null
                        )
                        Text(
                            text = "Tab",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .selectable(
                                selected = (bookGroupStyle == 1),
                                onClick = { bookGroupStyle = 1 },
                                role = Role.RadioButton
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (bookGroupStyle == 1),
                            onClick = null
                        )
                        Text(
                            text = "Folder",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showUnread,
                            onClick = { showUnread = !showUnread },
                            role = Role.Switch
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_unread),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showUnread,
                        onCheckedChange = null
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showLastUpdateTime,
                            onClick = { showLastUpdateTime = !showLastUpdateTime },
                            role = Role.Switch
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_last_update_time),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showLastUpdateTime,
                        onCheckedChange = null
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showWaitUpCount,
                            onClick = { showWaitUpCount = !showWaitUpCount },
                            role = Role.Switch
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_wait_up_count),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showWaitUpCount,
                        onCheckedChange = null
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = showBookshelfFastScroller,
                            onClick = { showBookshelfFastScroller = !showBookshelfFastScroller },
                            role = Role.Switch
                        )
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.show_bookshelf_fast_scroller),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showBookshelfFastScroller,
                        onCheckedChange = null
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.view),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    layoutOptions.forEachIndexed { index, stringRes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (index == bookshelfLayout),
                                    onClick = { bookshelfLayout = index },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (index == bookshelfLayout),
                                onClick = null
                            )
                            Text(
                                text = stringResource(stringRes),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.sort),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    sortOptions.forEachIndexed { index, stringRes ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = (index == bookshelfSort),
                                    onClick = { bookshelfSort = index },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (index == bookshelfSort),
                                onClick = null
                            )
                            Text(
                                text = stringResource(stringRes),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                if (bookshelfLayout >= 2) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.book_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        booknameOptions.forEachIndexed { index, stringRes ->
                            Row(
                                modifier = Modifier
                                    .selectable(
                                        selected = (index == showBookname),
                                        onClick = { showBookname = index },
                                        role = Role.RadioButton
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (index == showBookname),
                                    onClick = null
                                )
                                Text(
                                    text = stringResource(stringRes),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.margin),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { if (bookshelfMargin > 0) bookshelfMargin-- }
                        ) {
                            Text("-")
                        }
                        Text(
                            text = "$bookshelfMargin",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        TextButton(
                            onClick = { if (bookshelfMargin < 60) bookshelfMargin++ }
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val notifyMain = AppConfig.bookGroupStyle != bookGroupStyle
                    val layoutChanged = AppConfig.showBookname != showBookname ||
                            AppConfig.bookshelfMargin != bookshelfMargin ||
                            AppConfig.bookshelfLayout != bookshelfLayout

                    if (AppConfig.bookGroupStyle != bookGroupStyle) {
                        AppConfig.bookGroupStyle = bookGroupStyle
                    }
                    if (AppConfig.showBookname != showBookname) {
                        AppConfig.showBookname = showBookname
                    }
                    if (AppConfig.bookshelfMargin != bookshelfMargin) {
                        AppConfig.bookshelfMargin = bookshelfMargin
                    }
                    if (AppConfig.showUnread != showUnread) {
                        AppConfig.showUnread = showUnread
                    }
                    if (AppConfig.showLastUpdateTime != showLastUpdateTime) {
                        AppConfig.showLastUpdateTime = showLastUpdateTime
                    }
                    if (AppConfig.showWaitUpCount != showWaitUpCount) {
                        AppConfig.showWaitUpCount = showWaitUpCount
                        postEvent(EventBus.UP_BOOKSHELF, true)
                    }
                    if (AppConfig.showBookshelfFastScroller != showBookshelfFastScroller) {
                        AppConfig.showBookshelfFastScroller = showBookshelfFastScroller
                    }
                    if (AppConfig.bookshelfSort != bookshelfSort) {
                        AppConfig.bookshelfSort = bookshelfSort
                    }
                    if (AppConfig.bookshelfLayout != bookshelfLayout) {
                        AppConfig.bookshelfLayout = bookshelfLayout
                    }

                    viewModel.updateLayoutConfig()

                    if (layoutChanged) {
                        postEvent(EventBus.RECREATE, "")
                    }
                    if (notifyMain) {
                        postEvent(EventBus.NOTIFY_MAIN, false)
                    }

                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
