package io.legado.app.ui.compose.screens.replacerule

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers.IO

class ReplaceRuleViewModel(app: Application) : BaseViewModel(app) {

    private val _replaceRules = MutableStateFlow<List<ReplaceRule>>(emptyList())
    val replaceRules: StateFlow<List<ReplaceRule>> = _replaceRules.asStateFlow()

    private val _selection = MutableStateFlow<Set<ReplaceRule>>(emptySet())
    val selection: StateFlow<Set<ReplaceRule>> = _selection.asStateFlow()

    private val _groups = MutableStateFlow<List<String>>(emptyList())
    val groups: StateFlow<List<String>> = _groups.asStateFlow()

    private val _searchKey = MutableStateFlow<String?>(null)

    init {
        observeReplaceRules()
        observeGroups()
    }

    private fun observeGroups() {
        viewModelScope.launch {
            appDb.replaceRuleDao.flowGroups()
                .catch { }
                .flowOn(IO)
                .conflate()
                .collect { groupsList ->
                    _groups.value = groupsList
                }
        }
    }

    private fun observeReplaceRules() {
        viewModelScope.launch {
            _searchKey
                .catch { }
                .flowOn(IO)
                .collect { key ->
                    when {
                        key.isNullOrEmpty() -> {
                            appDb.replaceRuleDao.flowAll()
                        }
                        key == "enabled" -> {
                            appDb.replaceRuleDao.flowEnabled()
                        }
                        key == "disabled" -> {
                            appDb.replaceRuleDao.flowDisabled()
                        }
                        key == "no_group" -> {
                            appDb.replaceRuleDao.flowNoGroup()
                        }
                        key.startsWith("group:") -> {
                            val group = key.substringAfter("group:")
                            appDb.replaceRuleDao.flowGroupSearch("%$group%")
                        }
                        else -> {
                            appDb.replaceRuleDao.flowSearch("%$key%")
                        }
                    }.catch {
                    }.flowOn(IO).conflate().collect { rules ->
                        _replaceRules.value = rules
                    }
                }
        }
    }

    fun setSearchKey(key: String?) {
        _searchKey.value = key
    }

    fun toggleSelection(rule: ReplaceRule) {
        val current = _selection.value.toMutableSet()
        if (current.any { it.id == rule.id }) {
            current.removeIf { it.id == rule.id }
        } else {
            current.add(rule)
        }
        _selection.value = current
    }

    fun selectAll() {
        _selection.value = _replaceRules.value.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun invertSelection() {
        val current = _selection.value.toMutableSet()
        val all = _replaceRules.value.toSet()
        _selection.value = all.filter { !current.any { r -> r.id == it.id } }.toSet()
    }

    fun update(vararg rule: ReplaceRule) {
        execute {
            appDb.replaceRuleDao.update(*rule)
        }
    }

    fun delete(rule: ReplaceRule) {
        execute {
            appDb.replaceRuleDao.delete(rule)
        }
    }

    fun toTop(rule: ReplaceRule) {
        execute {
            rule.order = appDb.replaceRuleDao.minOrder - 1
            appDb.replaceRuleDao.update(rule)
        }
    }

    fun toBottom(rule: ReplaceRule) {
        execute {
            rule.order = appDb.replaceRuleDao.maxOrder + 1
            appDb.replaceRuleDao.update(rule)
        }
    }

    fun topSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            var minOrder = appDb.replaceRuleDao.minOrder - selected.size
            selected.forEach {
                it.order = ++minOrder
            }
            appDb.replaceRuleDao.update(*selected.toTypedArray())
        }
    }

    fun bottomSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            var maxOrder = appDb.replaceRuleDao.maxOrder
            selected.forEach {
                it.order = maxOrder++
            }
            appDb.replaceRuleDao.update(*selected.toTypedArray())
        }
    }

    fun upOrder() {
        execute {
            val rules = appDb.replaceRuleDao.all
            for ((index, rule) in rules.withIndex()) {
                rule.order = index + 1
            }
            appDb.replaceRuleDao.update(*rules.toTypedArray())
        }
    }

    fun enableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map {
                it.isEnabled = true
                it
            }.toTypedArray()
            appDb.replaceRuleDao.update(*array)
        }
    }

    fun disableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map {
                it.isEnabled = false
                it
            }.toTypedArray()
            appDb.replaceRuleDao.update(*array)
        }
    }

    fun deleteSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.replaceRuleDao.delete(*selected.toTypedArray())
            clearSelection()
        }
    }
}
