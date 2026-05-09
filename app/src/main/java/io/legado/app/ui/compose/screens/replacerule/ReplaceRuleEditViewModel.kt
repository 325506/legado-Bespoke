package io.legado.app.ui.compose.screens.replacerule

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule

class ReplaceRuleEditViewModel(app: Application) : BaseViewModel(app) {

    fun loadRule(id: Long, onSuccess: (ReplaceRule) -> Unit) {
        execute {
            appDb.replaceRuleDao.findById(id)
        }.onSuccess { rule ->
            rule?.let { onSuccess(it) }
        }
    }

    fun save(replaceRule: ReplaceRule, onSuccess: () -> Unit) {
        execute {
            replaceRule.checkValid()
            if (replaceRule.order == Int.MIN_VALUE) {
                replaceRule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(replaceRule)
        }.onSuccess {
            onSuccess()
        }.onError {
            throw it
        }
    }
}
