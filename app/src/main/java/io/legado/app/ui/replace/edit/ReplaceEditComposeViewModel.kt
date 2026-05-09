package io.legado.app.ui.replace.edit

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.exception.NoStackTraceException
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.GSON

class ReplaceEditComposeViewModel(app: Application) : BaseViewModel(app) {

    var replaceRule: ReplaceRule? = null

    fun loadRule(id: Long, onSuccess: (ReplaceRule) -> Unit) {
        execute {
            appDb.replaceRuleDao.findById(id)
        }.onSuccess { rule ->
            rule?.let { onSuccess(it) }
        }
    }

    fun createInitialRule(
        pattern: String?,
        isRegex: Boolean,
        scope: String?
    ): ReplaceRule {
        return ReplaceRule(
            name = pattern ?: "",
            pattern = pattern ?: "",
            isRegex = isRegex,
            scope = scope
        )
    }

    fun pasteRule(clipText: String, success: (ReplaceRule) -> Unit) {
        execute {
            if (clipText.isBlank()) {
                throw NoStackTraceException("剪贴板为空")
            }
            GSON.fromJsonObject<ReplaceRule>(clipText).getOrNull()
                ?: throw NoStackTraceException("格式不对")
        }.onSuccess { rule ->
            rule?.let { success(it) }
        }.onError {
            throw it
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
