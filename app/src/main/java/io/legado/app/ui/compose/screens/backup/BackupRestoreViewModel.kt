package io.legado.app.ui.compose.screens.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.Restore
import io.legado.app.utils.FileDoc
import io.legado.app.utils.checkWrite
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

data class BackupUiState(
    val webDavUrl: String = "",
    val webDavAccount: String = "",
    val webDavPassword: String = "",
    val webDavDir: String = "legado",
    val webDavDeviceName: String = "",
    val syncBookProgress: Boolean = true,
    val syncBookProgressPlus: Boolean = false,
    val backupPath: String = "",
    val onlyLatestBackup: Boolean = true,
    val autoCheckNewBackup: Boolean = true,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupProgress: String = "",
    val restoreProgress: String = "",
    val webDavBackupNames: List<String> = emptyList(),
    val showRestoreDialog: Boolean = false,
    val showRestoreError: Boolean = false,
    val restoreErrorMessage: String = ""
)

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private var backupJob: Job? = null
    private var restoreJob: Job? = null

    init {
        loadConfig()
    }

    private fun loadConfig() {
        _uiState.value = _uiState.value.copy(
            webDavUrl = appCtx.getPrefString(PreferKey.webDavUrl) ?: "",
            webDavAccount = appCtx.getPrefString(PreferKey.webDavAccount) ?: "",
            webDavPassword = appCtx.getPrefString(PreferKey.webDavPassword) ?: "",
            webDavDir = AppConfig.webDavDir ?: "legado",
            webDavDeviceName = AppConfig.webDavDeviceName ?: "",
            syncBookProgress = AppConfig.syncBookProgress,
            syncBookProgressPlus = AppConfig.syncBookProgressPlus,
            backupPath = AppConfig.backupPath ?: "",
            onlyLatestBackup = AppConfig.onlyLatestBackup,
            autoCheckNewBackup = AppConfig.autoCheckNewBackup
        )
    }

    fun updateWebDavUrl(value: String) {
        appCtx.putPrefString(PreferKey.webDavUrl, value)
        _uiState.value = _uiState.value.copy(webDavUrl = value)
    }

    fun updateWebDavAccount(value: String) {
        appCtx.putPrefString(PreferKey.webDavAccount, value)
        _uiState.value = _uiState.value.copy(webDavAccount = value)
    }

    fun updateWebDavPassword(value: String) {
        appCtx.putPrefString(PreferKey.webDavPassword, value)
        _uiState.value = _uiState.value.copy(webDavPassword = value)
    }

    fun updateWebDavDir(value: String) {
        appCtx.putPrefString(PreferKey.webDavDir, value)
        _uiState.value = _uiState.value.copy(webDavDir = value)
    }

    fun updateWebDavDeviceName(value: String) {
        appCtx.putPrefString(PreferKey.webDavDeviceName, value)
        _uiState.value = _uiState.value.copy(webDavDeviceName = value)
    }

    fun updateSyncBookProgress(value: Boolean) {
        appCtx.putPrefBoolean(PreferKey.syncBookProgress, value)
        _uiState.value = _uiState.value.copy(syncBookProgress = value)
    }

    fun updateSyncBookProgressPlus(value: Boolean) {
        appCtx.putPrefBoolean(PreferKey.syncBookProgressPlus, value)
        _uiState.value = _uiState.value.copy(syncBookProgressPlus = value)
    }

    fun updateBackupPath(value: String) {
        if (value.isEmpty()) {
            appCtx.removePref(PreferKey.backupPath)
        } else {
            appCtx.putPrefString(PreferKey.backupPath, value)
        }
        _uiState.value = _uiState.value.copy(backupPath = value)
    }

    fun updateOnlyLatestBackup(value: Boolean) {
        appCtx.putPrefBoolean(PreferKey.onlyLatestBackup, value)
        _uiState.value = _uiState.value.copy(onlyLatestBackup = value)
    }

    fun updateAutoCheckNewBackup(value: Boolean) {
        appCtx.putPrefBoolean(PreferKey.autoCheckNewBackup, value)
        _uiState.value = _uiState.value.copy(autoCheckNewBackup = value)
    }

    fun backup(backupPath: String, onNeedSelectDir: () -> Unit) {
        if (backupPath.isNullOrEmpty()) {
            onNeedSelectDir()
            return
        }
        if (backupPath.isContentScheme()) {
            viewModelScope.launch {
                val canWrite = withContext(IO) {
                    FileDoc.fromDir(backupPath).checkWrite()
                }
                if (canWrite) {
                    doBackup(backupPath)
                } else {
                    onNeedSelectDir()
                }
            }
        } else {
            doBackup(backupPath)
        }
    }

    private fun doBackup(backupPath: String) {
        _uiState.value = _uiState.value.copy(isBackingUp = true, backupProgress = "备份中…")
        backupJob?.cancel()
        backupJob = viewModelScope.launch {
            try {
                Backup.backupLocked(getApplication(), backupPath)
                appCtx.toastOnUi(R.string.backup_success)
            } catch (e: Throwable) {
                ensureActive()
                AppLog.put("备份出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi(appCtx.getString(R.string.backup_fail, e.localizedMessage))
            } finally {
                ensureActive()
                _uiState.value = _uiState.value.copy(isBackingUp = false, backupProgress = "")
            }
        }
    }

    fun restore(onSelectLocal: () -> Unit) {
        _uiState.value = _uiState.value.copy(isRestoring = true, restoreProgress = "加载中…")
        restoreJob?.cancel()
        restoreJob = viewModelScope.launch {
            try {
                val names = withContext(IO) { AppWebDav.getBackupNames() }
                if (AppWebDav.isJianGuoYun && names.size > 700) {
                    appCtx.toastOnUi("由于坚果云限制列出文件数量，部分备份可能未显示，请及时清理旧备份")
                }
                if (names.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    _uiState.value = _uiState.value.copy(
                        isRestoring = false,
                        restoreProgress = "",
                        webDavBackupNames = names,
                        showRestoreDialog = true
                    )
                } else {
                    throw NoStackTraceException("Web dav no back up file")
                }
            } catch (e: Throwable) {
                ensureActive()
                AppLog.put("恢复备份出错WebDavError\n${e.localizedMessage}", e)
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    restoreProgress = "",
                    showRestoreError = true,
                    restoreErrorMessage = e.localizedMessage ?: "未知错误"
                )
            }
        }
    }

    fun restoreWebDav(name: String) {
        _uiState.value = _uiState.value.copy(isRestoring = true, restoreProgress = "恢复中…")
        viewModelScope.launch {
            try {
                AppWebDav.restoreWebDav(name)
            } catch (e: Throwable) {
                AppLog.put("WebDav恢复出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi("WebDav恢复出错\n${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isRestoring = false, restoreProgress = "")
            }
        }
    }

    fun restoreFromLocal(uri: Uri) {
        _uiState.value = _uiState.value.copy(isRestoring = true, restoreProgress = "恢复中…")
        viewModelScope.launch {
            try {
                Restore.restore(appCtx, uri)
            } catch (e: Throwable) {
                AppLog.put("本地恢复出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi("本地恢复出错\n${e.localizedMessage}")
            } finally {
                _uiState.value = _uiState.value.copy(isRestoring = false, restoreProgress = "")
            }
        }
    }

    fun dismissRestoreError() {
        _uiState.value = _uiState.value.copy(showRestoreError = false)
    }

    fun dismissRestoreDialog() {
        _uiState.value = _uiState.value.copy(
            showRestoreDialog = false,
            webDavBackupNames = emptyList()
        )
    }

    fun showRestoreDialog() {
        _uiState.value = _uiState.value.copy(showRestoreDialog = true)
    }

    fun cancelBackup() {
        backupJob?.cancel()
        _uiState.value = _uiState.value.copy(isBackingUp = false, backupProgress = "")
    }

    fun cancelRestore() {
        restoreJob?.cancel()
        _uiState.value = _uiState.value.copy(isRestoring = false, restoreProgress = "")
    }
}
