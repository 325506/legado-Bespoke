package io.legado.app.ui.compose.screens.dictrule

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers.IO

class DictRuleViewModel(app: Application) : BaseViewModel(app) {

    private val _dictRules = MutableStateFlow<List<DictRule>>(emptyList())
    val dictRules: StateFlow<List<DictRule>> = _dictRules.asStateFlow()

    private val _selection = MutableStateFlow<Set<DictRule>>(emptySet())
    val selection: StateFlow<Set<DictRule>> = _selection.asStateFlow()

    init {
        observeDictRules()
    }

    private fun observeDictRules() {
        viewModelScope.launch {
            appDb.dictRuleDao.flowAll()
                .catch { }
                .flowOn(IO)
                .conflate()
                .collect { rules ->
                    _dictRules.value = rules
                }
        }
    }

    fun toggleSelection(rule: DictRule) {
        val current = _selection.value.toMutableSet()
        if (current.any { it.name == rule.name }) {
            current.removeIf { it.name == rule.name }
        } else {
            current.add(rule)
        }
        _selection.value = current
    }

    fun selectAll() {
        _selection.value = _dictRules.value.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun invertSelection() {
        val current = _selection.value.toMutableSet()
        val all = _dictRules.value.toSet()
        _selection.value = all.filter { !current.any { r -> r.name == it.name } }.toSet()
    }

    fun update(vararg rule: DictRule) {
        execute {
            appDb.dictRuleDao.update(*rule)
        }
    }

    fun delete(rule: DictRule) {
        execute {
            appDb.dictRuleDao.delete(rule)
        }
    }

    fun deleteSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.dictRuleDao.delete(*selected.toTypedArray())
            clearSelection()
        }
    }

    fun enableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map {
                it.enabled = true
                it
            }.toTypedArray()
            appDb.dictRuleDao.update(*array)
        }
    }

    fun disableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map {
                it.enabled = false
                it
            }.toTypedArray()
            appDb.dictRuleDao.update(*array)
        }
    }

    fun toTop(rule: DictRule) {
        execute {
            val minSort = appDb.dictRuleDao.all.minOfOrNull { it.sortNumber } ?: 0
            rule.sortNumber = minSort - 1
            appDb.dictRuleDao.update(rule)
        }
    }

    fun toBottom(rule: DictRule) {
        execute {
            val maxSort = appDb.dictRuleDao.all.maxOfOrNull { it.sortNumber } ?: 0
            rule.sortNumber = maxSort + 1
            appDb.dictRuleDao.update(rule)
        }
    }

    fun topSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val minSort = appDb.dictRuleDao.all.minOfOrNull { it.sortNumber } ?: 0
            var sortNumber = minSort - selected.size
            selected.forEach {
                it.sortNumber = ++sortNumber
            }
            appDb.dictRuleDao.update(*selected.toTypedArray())
        }
    }

    fun bottomSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val maxSort = appDb.dictRuleDao.all.maxOfOrNull { it.sortNumber } ?: 0
            var sortNumber = maxSort
            selected.forEach {
                it.sortNumber = sortNumber++
            }
            appDb.dictRuleDao.update(*selected.toTypedArray())
        }
    }

    fun upOrder() {
        execute {
            val rules = appDb.dictRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.sortNumber = index + 1
            }
            appDb.dictRuleDao.update(*rules.toTypedArray())
        }
    }

    fun importDefault() {
        execute {
            io.legado.app.help.DefaultData.importDefaultDictRules()
        }
    }
}
