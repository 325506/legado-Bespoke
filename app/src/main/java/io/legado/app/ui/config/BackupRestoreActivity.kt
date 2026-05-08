package io.legado.app.ui.config

import android.os.Bundle
import io.legado.app.constant.Theme
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.backup.BackupRestoreScreen
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp

class BackupRestoreActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this
        launchCompose {
            BackupRestoreScreen(
                onBack = { finish() },
                onHelp = {
                    activity.showHelp("webDavHelp")
                },
                onLog = {
                    activity.showDialogFragment<AppLogDialog>()
                }
            )
        }
    }
}
