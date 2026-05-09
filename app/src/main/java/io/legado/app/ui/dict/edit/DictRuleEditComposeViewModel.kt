package io.legado.app.ui.dict.edit

import android.app.Application
import io.legado.app.base.BaseViewModel
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import io.legado.app.utils.getClipText
import io.legado.app.utils.sendToClip
import io.legado.app.utils.toastOnUi

class DictRuleEditComposeViewModel(app: Application) : BaseViewModel(app) {

    var dictRule: DictRule? = null

    fun loadRule(name: String?, onSuccess: (DictRule) -> Unit) {
        execute {
            if (name != null) {
                appDb.dictRuleDao.getByName(name)
            } else {
                null
            }
        }.onSuccess { rule ->
            rule?.let { onSuccess(it) }
        }
    }

    fun save(dictRule: DictRule, onSuccess: () -> Unit) {
        execute {
            appDb.dictRuleDao.insert(dictRule)
        }.onSuccess {
            onSuccess()
        }.onError {
            val msg = "保存字典规则出错\n${it.localizedMessage}"
            AppLog.put(msg, it)
            context.toastOnUi(msg)
        }
    }

    fun copyRule(dictRule: DictRule) {
        context.sendToClip(GSON.toJson(dictRule))
    }

    fun pasteRule(success: (DictRule) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板没有内容")
            return
        }
        execute {
            GSON.fromJsonObject<DictRule>(text).getOrThrow()
        }.onSuccess {
            success.invoke(it)
        }.onError {
            context.toastOnUi("格式不对")
        }
    }
}
