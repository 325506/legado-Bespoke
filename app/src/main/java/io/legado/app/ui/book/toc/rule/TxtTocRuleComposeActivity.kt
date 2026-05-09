package io.legado.app.ui.book.toc.rule

import android.os.Bundle
import io.legado.app.constant.Theme
import io.legado.app.ui.compose.base.ComposeActivity
import io.legado.app.ui.compose.screens.txttocrule.TxtTocRuleScreen
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp

class TxtTocRuleComposeActivity : ComposeActivity() {

    override val themeMode: Theme = Theme.Auto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this
        launchCompose {
            TxtTocRuleScreen(
                onBack = { finish() },
                onAddRule = {
                    activity.showDialogFragment(TxtTocRuleEditDialog())
                },
                onEditRule = { ruleId ->
                    activity.showDialogFragment(TxtTocRuleEditDialog(ruleId))
                },
                onHelp = {
                    activity.showHelp("txtTocRuleHelp")
                }
            )
        }
    }
}
