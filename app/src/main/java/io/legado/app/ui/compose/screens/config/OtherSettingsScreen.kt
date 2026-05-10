package io.legado.app.ui.compose.screens.config

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppFreezeMonitor
import io.legado.app.help.DispatchersMonitor
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.model.CheckSource
import io.legado.app.model.ImageProvider
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.service.WebService
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isJsonObject
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import io.legado.app.utils.restart
import splitties.init.appCtx

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onShowCheckSourceConfig: (() -> Unit)? = null,
    onShowDirectLinkUploadConfig: (() -> Unit)? = null,
    onShowVideoSettings: (() -> Unit)? = null,
    onClearCache: (() -> Unit)? = null,
    onClearWebViewData: (() -> Unit)? = null,
    onShrinkDatabase: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val bookTreePicker = rememberLauncherForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        LanguageSetting()

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = context.getString(R.string.main_activity),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        AutoRefreshSetting()
        OnlyUpdateReadSetting(visible = AppConfig.autoRefreshBook)
        DefaultToReadSetting()
        ShowDiscoverySetting()
        ShowRssSetting()
        DefaultHomePageSetting()

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = context.getString(R.string.other_setting),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LocalPasswordSetting()
        UserAgentSetting()
        CustomHostsSetting()
        WebServiceWakeLockSetting()
        DefaultBookTreeUriSetting(bookTreePicker)
        SourceEditMaxLineSetting()
        CheckSourceSetting(onShowCheckSourceConfig)
        DirectLinkUploadSetting(onShowDirectLinkUploadConfig)
        CronetSetting()
        AntiAliasSetting()
        BitmapCacheSizeSetting()
        ImageRetainNumSetting()
        PreDownloadNumSetting()
        ReplaceEnableDefaultSetting()
        MediaButtonOnExitSetting()
        ReadAloudByMediaButtonSetting()
        IgnoreAudioFocusSetting()
        AutoClearExpiredSetting()
        ShowAddToShelfAlertSetting()
        UpdateToVariantSetting()
        AutoUpdateVariantSetting()
        ShowMangaUiSetting()
        VideoSetting(onShowVideoSettings)
        WebPortSetting()
        CleanCacheSetting(onClearCache)
        ClearWebViewDataSetting(onClearWebViewData)
        ShrinkDatabaseSetting(onShrinkDatabase)
        ThreadCountSetting()
        ProcessTextSetting()
        RecordLogSetting()
        RecordHeapDumpSetting()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) { { Text(summary) } } else null,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun ClickableSettingItem(
    title: String,
    summary: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) { { Text(summary) } } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSetting() {
    val context = LocalContext.current
    val languages = context.resources.getStringArray(R.array.language)
    val languageValues = context.resources.getStringArray(R.array.language_value)
    val currentLanguage = appCtx.getPrefString(PreferKey.language, "auto")
    val currentIndex = languageValues.indexOf(currentLanguage).coerceAtLeast(0)

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(languages[currentIndex]) }

    ListItem(
        headlineContent = { Text(context.getString(R.string.language)) },
        supportingContent = { Text(selectedText) },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { expanded = true }
                        .padding(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    languages.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                appCtx.putPrefString(PreferKey.language, languageValues[index])
                                selectedText = item
                                expanded = false
                                appCtx.restart()
                            },
                            trailingIcon = {
                                if (languageValues[index] == currentLanguage) {
                                    androidx.compose.material3.Icon(
                                        Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AutoRefreshSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.autoRefreshBook) }
    SwitchSettingItem(
        title = context.getString(R.string.pt_auto_refresh),
        summary = context.getString(R.string.ps_auto_refresh),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.autoRefresh, it)
    }
}

@Composable
private fun OnlyUpdateReadSetting(visible: Boolean) {
    val context = LocalContext.current
    if (!visible) return
    var checked by remember { mutableStateOf(AppConfig.onlyUpdateRead) }
    SwitchSettingItem(
        title = context.getString(R.string.only_update_read),
        summary = context.getString(R.string.ps_only_update_read),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.onlyUpdateRead, it)
    }
}

@Composable
private fun DefaultToReadSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(appCtx.getPrefBoolean(PreferKey.defaultToRead)) }
    SwitchSettingItem(
        title = context.getString(R.string.pt_default_read),
        summary = context.getString(R.string.ps_default_read),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.defaultToRead, it)
    }
}

@Composable
private fun ShowDiscoverySetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.showDiscovery) }
    SwitchSettingItem(
        title = context.getString(R.string.show_discovery),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.showDiscovery, it)
        postEvent(EventBus.NOTIFY_MAIN, true)
    }
}

@Composable
private fun ShowRssSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.showRSS) }
    SwitchSettingItem(
        title = context.getString(R.string.show_rss),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.showRss, it)
        postEvent(EventBus.NOTIFY_MAIN, true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultHomePageSetting() {
    val context = LocalContext.current
    val pages = context.resources.getStringArray(R.array.default_home_page)
    val pageValues = context.resources.getStringArray(R.array.default_home_page_value)
    val currentPage = AppConfig.defaultHomePage
    val currentIndex = pageValues.indexOf(currentPage).coerceAtLeast(0)

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(pages[currentIndex]) }

    ListItem(
        headlineContent = { Text(context.getString(R.string.default_home_page)) },
        supportingContent = { Text(selectedText) },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { expanded = true }
                        .padding(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    pages.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                appCtx.putPrefString(PreferKey.defaultHomePage, pageValues[index])
                                selectedText = item
                                expanded = false
                            },
                            trailingIcon = {
                                if (pageValues[index] == currentPage) {
                                    androidx.compose.material3.Icon(
                                        Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun LocalPasswordSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.set_local_password),
        summary = context.getString(R.string.set_local_password_summary)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            title = { Text(context.getString(R.string.set_local_password)) },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("password") }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    LocalConfig.password = password
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserAgentSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.user_agent)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var userAgent by remember { mutableStateOf(AppConfig.userAgent ?: "") }
        AlertDialog(
            title = { Text(context.getString(R.string.user_agent)) },
            text = {
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    placeholder = { Text(context.getString(R.string.user_agent)) }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (userAgent.isBlank()) {
                        appCtx.removePref(PreferKey.userAgent)
                    } else {
                        appCtx.putPrefString(PreferKey.userAgent, userAgent)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomHostsSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.custom_hosts),
        summary = context.getString(R.string.custom_hosts_summary)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var customHosts by remember { mutableStateOf(AppConfig.customHosts ?: "") }
        AlertDialog(
            title = { Text(context.getString(R.string.custom_hosts)) },
            text = {
                OutlinedTextField(
                    value = customHosts,
                    onValueChange = { customHosts = it },
                    placeholder = { Text(context.getString(R.string.json_format)) }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    if (customHosts.isJsonObject()) {
                        appCtx.putPrefString(PreferKey.customHosts, customHosts)
                    } else {
                        appCtx.removePref(PreferKey.customHosts)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun WebServiceWakeLockSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(appCtx.getPrefBoolean(PreferKey.webServiceWakeLock)) }
    SwitchSettingItem(
        title = context.getString(R.string.web_service_wake_lock),
        summary = context.getString(R.string.web_service_wake_lock_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.webServiceWakeLock, it)
    }
}

@Composable
private fun DefaultBookTreeUriSetting(
    bookTreePicker: androidx.activity.result.ActivityResultLauncher<(HandleFileContract.HandleFileParam.() -> Unit)?>
) {
    val context = LocalContext.current
    val summary = AppConfig.defaultBookTreeUri ?: context.getString(R.string.book_tree_uri_s)
    ClickableSettingItem(
        title = context.getString(R.string.book_tree_uri_t),
        summary = summary
    ) {
        bookTreePicker.launch {
            title = context.getString(R.string.select_book_folder)
            mode = HandleFileContract.DIR_SYS
        }
    }
}

@Composable
private fun SourceEditMaxLineSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.source_edit_text_max_line),
        summary = context.getString(R.string.source_edit_max_line_summary, AppConfig.sourceEditMaxLine)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.sourceEditMaxLine.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.source_edit_text_max_line)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.sourceEditMaxLine = it
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CheckSourceSetting(onShowCheckSourceConfig: (() -> Unit)?) {
    val context = LocalContext.current
    ClickableSettingItem(
        title = context.getString(R.string.check_source_config),
        summary = CheckSource.summary
    ) {
        onShowCheckSourceConfig?.invoke()
    }
}

@Composable
private fun DirectLinkUploadSetting(onShowDirectLinkUploadConfig: (() -> Unit)?) {
    val context = LocalContext.current
    ClickableSettingItem(
        title = context.getString(R.string.direct_link_upload_rule),
        summary = context.getString(R.string.direct_link_upload_rule_summary)
    ) {
        onShowDirectLinkUploadConfig?.invoke()
    }
}

@Composable
private fun CronetSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.isCronet) }
    SwitchSettingItem(
        title = "Cronet",
        summary = context.getString(R.string.pref_cronet_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.cronet, it)
    }
}

@Composable
private fun AntiAliasSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.useAntiAlias) }
    SwitchSettingItem(
        title = context.getString(R.string.anti_alias),
        summary = context.getString(R.string.pref_anti_alias_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.antiAlias, it)
    }
}

@Composable
private fun BitmapCacheSizeSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.bitmap_cache_size),
        summary = context.getString(R.string.bitmap_cache_size_summary, AppConfig.bitmapCacheSize)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.bitmapCacheSize.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.bitmap_cache_size)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.bitmapCacheSize = it
                        ImageProvider.bitmapLruCache.resize(ImageProvider.cacheSize)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImageRetainNumSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.image_retain_number),
        summary = context.getString(R.string.image_retain_number_summary, AppConfig.imageRetainNum)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.imageRetainNum.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.image_retain_number)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.imageRetainNum = it
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PreDownloadNumSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.pre_download),
        summary = context.getString(R.string.pre_download_s, AppConfig.preDownloadNum)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.preDownloadNum.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.pre_download)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.preDownloadNum = it
                        postEvent(PreferKey.preDownloadNum, "")
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReplaceEnableDefaultSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.replaceEnableDefault) }
    SwitchSettingItem(
        title = context.getString(R.string.replace_enable_default_t),
        summary = context.getString(R.string.replace_enable_default_s),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.replaceEnableDefault, it)
    }
}

@Composable
private fun MediaButtonOnExitSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.mediaButtonOnExit) }
    SwitchSettingItem(
        title = context.getString(R.string.media_button_on_exit_title),
        summary = context.getString(R.string.media_button_on_exit_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean("mediaButtonOnExit", it)
    }
}

@Composable
private fun ReadAloudByMediaButtonSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.readAloudByMediaButton) }
    SwitchSettingItem(
        title = context.getString(R.string.read_aloud_by_media_button_title),
        summary = context.getString(R.string.read_aloud_by_media_button_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.readAloudByMediaButton, it)
    }
}

@Composable
private fun IgnoreAudioFocusSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.ignoreAudioFocus) }
    SwitchSettingItem(
        title = context.getString(R.string.ignore_audio_focus_title),
        summary = context.getString(R.string.ignore_audio_focus_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.ignoreAudioFocus, it)
    }
}

@Composable
private fun AutoClearExpiredSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(appCtx.getPrefBoolean(PreferKey.autoClearExpired, true)) }
    SwitchSettingItem(
        title = context.getString(R.string.auto_clear_expired),
        summary = context.getString(R.string.auto_clear_expired_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.autoClearExpired, it)
    }
}

@Composable
private fun ShowAddToShelfAlertSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.showAddToShelfAlert) }
    SwitchSettingItem(
        title = context.getString(R.string.show_add_to_shelf_alert_title),
        summary = context.getString(R.string.show_add_to_shelf_alert_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.showAddToShelfAlert, it)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateToVariantSetting() {
    val context = LocalContext.current
    val variants = context.resources.getStringArray(R.array.default_app_variant)
    val variantValues = context.resources.getStringArray(R.array.default_app_variant_value)
    val currentVariant = AppConfig.updateToVariant
    val currentIndex = variantValues.indexOf(currentVariant).coerceAtLeast(0)

    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(variants[currentIndex]) }

    ListItem(
        headlineContent = { Text(context.getString(R.string.update_to_variant_title)) },
        supportingContent = {
            Text(context.getString(R.string.update_to_variant_summary))
            Text(selectedText)
        },
        trailingContent = {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                Text(
                    text = selectedText,
                    modifier = Modifier
                        .menuAnchor()
                        .clickable { expanded = true }
                        .padding(8.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    variants.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                appCtx.putPrefString(PreferKey.updateToVariant, variantValues[index])
                                selectedText = item
                                expanded = false
                            },
                            trailingIcon = {
                                if (variantValues[index] == currentVariant) {
                                    androidx.compose.material3.Icon(
                                        Icons.Default.Check,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AutoUpdateVariantSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.autoUpdateVariant) }
    SwitchSettingItem(
        title = context.getString(R.string.auto_update),
        summary = context.getString(R.string.auto_update_summary),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean("autoUpdateVariant", it)
    }
}

@Composable
private fun ShowMangaUiSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.showMangaUi) }
    SwitchSettingItem(
        title = context.getString(R.string.show_manga_ui),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.showMangaUi, it)
    }
}

@Composable
private fun VideoSetting(onShowVideoSettings: (() -> Unit)?) {
    val context = LocalContext.current
    ClickableSettingItem(
        title = context.getString(R.string.video_setting),
        summary = context.getString(R.string.video_setting_summary)
    ) {
        onShowVideoSettings?.invoke()
    }
}

@Composable
private fun WebPortSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.web_port_title),
        summary = context.getString(R.string.web_port_summary, AppConfig.webPort)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.webPort.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.web_port_title)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.webPort = it
                        if (WebService.isRun) {
                            WebService.stop(context)
                            WebService.start(context)
                        }
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CleanCacheSetting(onClearCache: (() -> Unit)?) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.clear_cache),
        summary = context.getString(R.string.clear_cache_summary)
    ) {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            title = { Text(context.getString(R.string.clear_cache)) },
            text = { Text(context.getString(R.string.sure_del)) },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onClearCache?.invoke()
                }) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ClearWebViewDataSetting(onClearWebViewData: (() -> Unit)?) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.clear_webview_data),
        summary = context.getString(R.string.clear_webview_data_summary)
    ) {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            title = { Text(context.getString(R.string.clear_webview_data)) },
            text = { Text(context.getString(R.string.sure_del)) },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onClearWebViewData?.invoke()
                }) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ShrinkDatabaseSetting(onShrinkDatabase: (() -> Unit)?) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.shrink_database),
        summary = context.getString(R.string.shrink_database_summary)
    ) {
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            title = { Text(context.getString(R.string.shrink_database)) },
            text = { Text(context.getString(R.string.sure)) },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onShrinkDatabase?.invoke()
                }) {
                    Text(context.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(context.getString(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ThreadCountSetting() {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    ClickableSettingItem(
        title = context.getString(R.string.threads_num_title),
        summary = context.getString(R.string.threads_num, AppConfig.threadCount)
    ) {
        showDialog = true
    }

    if (showDialog) {
        var value by remember { mutableStateOf(AppConfig.threadCount.toString()) }
        AlertDialog(
            title = { Text(context.getString(R.string.threads_num_title)) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it }
                )
            },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    value.toIntOrNull()?.let {
                        AppConfig.threadCount = it
                        postEvent(PreferKey.threadCount, "")
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProcessTextSetting() {
    val context = LocalContext.current
    val packageManager = appCtx.packageManager
    val componentName = ComponentName(appCtx, SharedReceiverActivity::class.java.name)

    fun isProcessTextEnabled(): Boolean {
        return packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun setProcessTextEnable(enable: Boolean) {
        if (enable) {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP
            )
        } else {
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP
            )
        }
    }

    var checked by remember { mutableStateOf(isProcessTextEnabled()) }
    SwitchSettingItem(
        title = context.getString(R.string.add_to_text_context_menu_t),
        summary = context.getString(R.string.add_to_text_context_menu_s),
        checked = checked
    ) {
        checked = it
        setProcessTextEnable(it)
        appCtx.putPrefBoolean(PreferKey.processText, it)
    }
}

@Composable
private fun RecordLogSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(appCtx.getPrefBoolean(PreferKey.recordLog)) }
    SwitchSettingItem(
        title = context.getString(R.string.record_log),
        summary = context.getString(R.string.record_debug_log),
        checked = checked
    ) {
        checked = it
        AppConfig.recordLog = it
        appCtx.putPrefBoolean(PreferKey.recordLog, it)
        LogUtils.upLevel()
        LogUtils.logDeviceInfo()
        com.jeremyliao.liveeventbus.LiveEventBus.config().enableLogger(AppConfig.recordLog)
        AppFreezeMonitor.init(appCtx)
        DispatchersMonitor.init()
    }
}

@Composable
private fun RecordHeapDumpSetting() {
    val context = LocalContext.current
    var checked by remember { mutableStateOf(AppConfig.recordHeapDump) }
    SwitchSettingItem(
        title = context.getString(R.string.record_heap_dump_t),
        summary = context.getString(R.string.record_heap_dump_s),
        checked = checked
    ) {
        checked = it
        appCtx.putPrefBoolean(PreferKey.recordHeapDump, it)
    }
}
