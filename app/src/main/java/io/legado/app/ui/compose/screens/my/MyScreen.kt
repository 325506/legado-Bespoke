package io.legado.app.ui.compose.screens.my

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.ThemeConfig
import io.legado.app.service.WebService
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.compose.booksource.BookSourceComposeActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleComposeActivity
import io.legado.app.ui.config.BackupRestoreActivity
import io.legado.app.ui.config.ConfigComposeActivity
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.dict.rule.DictRuleComposeActivity
import io.legado.app.ui.file.FileManageComposeActivity
import io.legado.app.ui.replace.ReplaceRuleComposeActivity
import io.legado.app.ui.compose.components.LegadoTopAppBar
import io.legado.app.utils.getPrefString
import io.legado.app.utils.putPrefString
import splitties.init.appCtx

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyScreen(
    onShowHelp: () -> Unit = {},
    webServiceState: Pair<Boolean, String> = Pair(WebService.isRun, if (WebService.isRun) WebService.hostAddress else ""),
    onWebServiceToggle: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            LegadoTopAppBar(
                title = context.getString(R.string.my),
                actions = {
                    IconButton(onClick = onShowHelp) {
                        Icon(
                            painter = painterResource(R.drawable.ic_help),
                            contentDescription = context.getString(R.string.help)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
        ) {
            SourceManageItem()
            TxtTocRuleItem()
            ReplaceManageItem()
            DictRuleItem()
            ThemeModeItem()
            WebServiceItem(webServiceState, onWebServiceToggle)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = context.getString(R.string.setting),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            BackupRestoreItem()
            ThemeSettingItem()
            OtherSettingItem()

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = context.getString(R.string.other),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            BookmarkItem()
            ReadRecordItem()
            FileManageItem()
            AboutItem()
            ExitItem()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SourceManageItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.book_source_manage)) },
        supportingContent = { Text(context.getString(R.string.book_source_manage_desc)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_source),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, BookSourceComposeActivity::class.java))
        }
    )
}

@Composable
private fun TxtTocRuleItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.txt_toc_rule)) },
        supportingContent = { Text(context.getString(R.string.config_txt_toc_rule)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_source),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, TxtTocRuleComposeActivity::class.java))
        }
    )
}

@Composable
private fun ReplaceManageItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.replace_purify)) },
        supportingContent = { Text(context.getString(R.string.replace_purify_desc)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_replace),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, ReplaceRuleComposeActivity::class.java))
        }
    )
}

@Composable
private fun DictRuleItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.dict_rule)) },
        supportingContent = { Text(context.getString(R.string.config_dict_rule)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_translate),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, DictRuleComposeActivity::class.java))
        }
    )
}

@Composable
private fun ThemeModeItem() {
    val context = LocalContext.current
    val themeModes = context.resources.getStringArray(R.array.theme_mode)
    val themeModeValues = context.resources.getStringArray(R.array.theme_mode_v)
    val currentThemeMode = appCtx.getPrefString(PreferKey.themeMode, "0")
    val currentIndex = themeModeValues.indexOf(currentThemeMode).coerceAtLeast(0)

    var selectedText by mutableStateOf(themeModes[currentIndex])

    LaunchedEffect(currentThemeMode) {
        val newIndex = themeModeValues.indexOf(currentThemeMode).coerceAtLeast(0)
        selectedText = themeModes[newIndex]
    }

    ListItem(
        headlineContent = { Text(context.getString(R.string.theme_mode)) },
        supportingContent = { Text(selectedText) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_theme),
                contentDescription = null
            )
        },
        trailingContent = {
            Text(
                text = "切换",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val nextIndex = (themeModeValues.indexOf(currentThemeMode) + 1) % themeModeValues.size
                    appCtx.putPrefString(PreferKey.themeMode, themeModeValues[nextIndex])
                    ThemeConfig.applyDayNight(context)
                }
            )
        }
    )
}

@Composable
private fun WebServiceItem(
    webServiceState: Pair<Boolean, String>,
    onWebServiceToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val (isChecked, summary) = webServiceState

    ListItem(
        headlineContent = { Text(context.getString(R.string.web_service)) },
        supportingContent = { Text(summary) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_web),
                contentDescription = null
            )
        },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = onWebServiceToggle
            )
        },
        modifier = Modifier.clickable {
            onWebServiceToggle(!isChecked)
        }
    )
}

@Composable
private fun BackupRestoreItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.backup_restore)) },
        supportingContent = { Text(context.getString(R.string.web_dav_set_import_old)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_backup),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, BackupRestoreActivity::class.java))
        }
    )
}

@Composable
private fun ThemeSettingItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.theme_setting)) },
        supportingContent = { Text(context.getString(R.string.theme_setting_s)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_theme),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            val intent = Intent(context, ConfigComposeActivity::class.java).apply {
                putExtra("configTag", ConfigTag.THEME_CONFIG)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
private fun OtherSettingItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.other_setting)) },
        supportingContent = { Text(context.getString(R.string.other_setting_s)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_other),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            val intent = Intent(context, ConfigComposeActivity::class.java).apply {
                putExtra("configTag", ConfigTag.OTHER_CONFIG)
            }
            context.startActivity(intent)
        }
    )
}

@Composable
private fun BookmarkItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.bookmark)) },
        supportingContent = { Text(context.getString(R.string.all_bookmark)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, AllBookmarkActivity::class.java))
        }
    )
}

@Composable
private fun ReadRecordItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.read_record)) },
        supportingContent = { Text(context.getString(R.string.read_record_summary)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, ReadRecordActivity::class.java))
        }
    )
}

@Composable
private fun FileManageItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.file_manage)) },
        supportingContent = { Text(context.getString(R.string.file_manage_summary)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_folder_outline),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, FileManageComposeActivity::class.java))
        }
    )
}

@Composable
private fun AboutItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.about)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_cfg_about),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    )
}

@Composable
private fun ExitItem() {
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(context.getString(R.string.exit)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_exit),
                contentDescription = null
            )
        },
        modifier = Modifier.clickable {
            (context as? android.app.Activity)?.finish()
        }
    )
}
