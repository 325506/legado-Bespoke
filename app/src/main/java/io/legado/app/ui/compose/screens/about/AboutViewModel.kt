package io.legado.app.ui.compose.screens.about

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.legado.app.R
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.constant.AppLog
import io.legado.app.help.CrashHandler
import io.legado.app.help.config.AppConfig
import io.legado.app.help.update.AppUpdate
import io.legado.app.ui.about.CrashLogsDialog
import io.legado.app.ui.about.UpdateDialog
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.*
import io.legado.app.utils.compress.ZipUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File

data class AboutMenuItem(
    val key: String,
    val titleRes: Int,
    val summaryRes: Int? = null
)

class AboutViewModel : ViewModel() {

    val appVersion = appInfo.versionName

    val menuItems = listOf(
        AboutMenuItem("contributors", R.string.contributors, R.string.contributors_summary_sigma),
        AboutMenuItem("update_log", R.string.update_log, null),
        AboutMenuItem("check_update", R.string.check_update, null)
    )

    val otherItems = listOf(
        AboutMenuItem("crashLog", R.string.crash_log, null),
        AboutMenuItem("saveLog", R.string.save_log, null),
        AboutMenuItem("createHeapDump", R.string.create_heap_dump, null),
        AboutMenuItem("privacyPolicy", R.string.privacy_policy, null),
        AboutMenuItem("license", R.string.license, null),
        AboutMenuItem("disclaimer", R.string.disclaimer, null)
    )

    fun onMenuItemClick(context: Context, item: AboutMenuItem) {
        when (item.key) {
            "contributors" -> context.openUrl(context.getString(R.string.contributors_url))
            "update_log" -> showMdFile(context, context.getString(R.string.update_log), "updateLog.md")
            "check_update" -> checkUpdate(context)
            "crashLog" -> showCrashLogsDialog(context)
            "saveLog" -> saveLog()
            "createHeapDump" -> createHeapDump()
            "privacyPolicy" -> showMdFile(context, context.getString(R.string.privacy_policy), "privacyPolicy.md")
            "license" -> showMdFile(context, context.getString(R.string.license), "LICENSE.md")
            "disclaimer" -> showMdFile(context, context.getString(R.string.disclaimer), "disclaimer.md")
        }
    }

    fun onShare(context: Context) {
        context.share(
            context.getString(R.string.app_share_description_sigma),
            context.getString(R.string.app_name)
        )
    }

    fun onScoring(context: Context) {
        context.openUrl("market://details?id=${context.packageName}")
    }

    fun onGzhClick(context: Context) {
        context.sendToClip(context.getString(R.string.legado_gzh))
    }

    fun onMailClick(context: Context) {
        context.sendMail(context.getString(R.string.email))
    }

    private fun showMdFile(context: Context, title: String, fileName: String) {
        viewModelScope.launch {
            kotlin.runCatching {
                val mdText = context.assets.open(fileName).readBytes().decodeToString()
                val activity = context as? AppCompatActivity
                if (activity != null) {
                    activity.showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
                }
            }
        }
    }

    private fun showCrashLogsDialog(context: Context) {
        val activity = context as? AppCompatActivity
        if (activity != null) {
            activity.showDialogFragment(CrashLogsDialog())
        }
    }

    private fun checkUpdate(context: Context) {
        viewModelScope.launch {
            kotlin.runCatching {
                val activity = context as? AppCompatActivity ?: return@runCatching
                AppUpdate.giteeUpdate.run {
                    check(viewModelScope)
                        .onSuccess {
                            activity.showDialogFragment(UpdateDialog(it))
                        }.onError {
                            appCtx.toastOnUi("${context.getString(R.string.check_update)}\n${it.localizedMessage}")
                        }
                }
            }
        }
    }

    private fun saveLog() {
        viewModelScope.launch {
            kotlin.runCatching {
                val backupPath = AppConfig.backupPath ?: let {
                    appCtx.toastOnUi("未设置备份目录")
                    return@runCatching
                }
                if (!AppConfig.recordLog) {
                    appCtx.toastOnUi("未开启日志记录，请去其他设置里打开记录日志")
                    delay(3000)
                }
                val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
                copyLogs(doc)
                copyHeapDump(doc)
                appCtx.toastOnUi("已保存至备份目录")
            }.onFailure {
                AppLog.put("保存日志出错\n${it.localizedMessage}", it, true)
            }
        }
    }

    private fun createHeapDump() {
        viewModelScope.launch {
            kotlin.runCatching {
                val backupPath = AppConfig.backupPath ?: let {
                    appCtx.toastOnUi("未设置备份目录")
                    return@runCatching
                }
                if (!AppConfig.recordHeapDump) {
                    appCtx.toastOnUi("未开启堆转储记录，请去其他设置里打开记录堆转储")
                    delay(3000)
                }
                appCtx.toastOnUi("开始创建堆转储")
                System.gc()
                CrashHandler.doHeapDump(true)
                val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
                if (!copyHeapDump(doc)) {
                    appCtx.toastOnUi("未找到堆转储文件")
                } else {
                    appCtx.toastOnUi("已保存至备份目录")
                }
            }.onFailure {
                AppLog.put("保存堆转储失败\n${it.localizedMessage}", it)
            }
        }
    }

    private fun copyLogs(doc: FileDoc) {
        val cacheDir = appCtx.externalCache
        val logFiles = File(cacheDir, "logs")
        val crashFiles = File(cacheDir, "crash")
        val logcatFile = File(cacheDir, "logcat.txt")

        dumpLogcat(logcatFile)

        val zipFile = File(cacheDir, "logs.zip")
        ZipUtils.zipFiles(arrayListOf(logFiles, crashFiles, logcatFile), zipFile)

        doc.find("logs.zip")?.delete()

        zipFile.inputStream().use { input ->
            doc.createFileIfNotExist("logs.zip").openOutputStream().getOrNull()
                ?.use {
                    input.copyTo(it)
                }
        }
        zipFile.delete()
    }

    private fun copyHeapDump(doc: FileDoc): Boolean {
        val heapFile = FileDoc.fromFile(File(appCtx.externalCache, "heapDump")).list()
            ?.firstOrNull() ?: return false
        doc.find("heapDump")?.delete()
        val heapDumpDoc = doc.createFolderIfNotExist("heapDump")
        heapFile.openInputStream().getOrNull()?.use { input ->
            heapDumpDoc.createFileIfNotExist(heapFile.name).openOutputStream().getOrNull()
                ?.use {
                    input.copyTo(it)
                }
        }
        return true
    }

    private fun dumpLogcat(file: File) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            file.outputStream().use {
                process.inputStream.copyTo(it)
            }
        } catch (e: Exception) {
            AppLog.put("保存Logcat失败\n$e", e)
        }
    }
}
