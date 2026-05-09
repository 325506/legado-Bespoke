package io.legado.app.ui.compose.screens.txttocrule

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.legado.app.base.BaseViewModel
import io.legado.app.data.appDb
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.help.DefaultData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers.IO

class TxtTocRuleViewModel(app: Application) : BaseViewModel(app) {

    private val _txtTocRules = MutableStateFlow<List<TxtTocRule>>(emptyList())
    val txtTocRules: StateFlow<List<TxtTocRule>> = _txtTocRules.asStateFlow()

    private val _selection = MutableStateFlow<Set<TxtTocRule>>(emptySet())
    val selection: StateFlow<Set<TxtTocRule>> = _selection.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(true)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    init {
        observeTxtTocRules()
    }

    private fun observeTxtTocRules() {
        viewModelScope.launch {
            appDb.txtTocRuleDao.observeAll()
                .catch { }
                .flowOn(IO)
                .conflate()
                .collect { rules ->
                    _txtTocRules.value = rules
                }
        }
    }

    fun toggleSelection(rule: TxtTocRule) {
        val current = _selection.value.toMutableSet()
        if (current.contains(rule)) {
            current.remove(rule)
        } else {
            current.add(rule)
        }
        _selection.value = current
    }

    fun selectAll() {
        _selection.value = _txtTocRules.value.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    fun invertSelection() {
        val current = _selection.value.toMutableSet()
        val all = _txtTocRules.value.toSet()
        _selection.value = all.filter { !current.contains(it) }.toSet()
    }

    fun save(txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.insert(txtTocRule)
        }
    }

    fun del(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.delete(*txtTocRule)
            clearSelection()
        }
    }

    fun update(vararg txtTocRule: TxtTocRule) {
        execute {
            appDb.txtTocRuleDao.update(*txtTocRule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

    fun toTop(vararg rules: TxtTocRule) {
        execute {
            val minOrder = appDb.txtTocRuleDao.minOrder - 1
            rules.forEachIndexed { index, source ->
                source.serialNumber = minOrder - index
            }
            appDb.txtTocRuleDao.update(*rules)
        }
    }

    fun toBottom(vararg sources: TxtTocRule) {
        execute {
            val maxOrder = appDb.txtTocRuleDao.maxOrder + 1
            sources.forEachIndexed { index, source ->
                source.serialNumber = maxOrder + index
            }
            appDb.txtTocRuleDao.update(*sources)
        }
    }

    fun upOrder() {
        execute {
            val sources = appDb.txtTocRuleDao.all
            for ((index: Int, source: TxtTocRule) in sources.withIndex()) {
                source.serialNumber = index + 1
            }
            appDb.txtTocRuleDao.update(*sources.toTypedArray())
        }
    }

    fun enableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map { it.copy(enable = true) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

    fun disableSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            val array = selected.map { it.copy(enable = false) }.toTypedArray()
            appDb.txtTocRuleDao.insert(*array)
        }
    }

    fun deleteSelection() {
        val selected = _selection.value
        if (selected.isEmpty()) return
        execute {
            appDb.txtTocRuleDao.delete(*selected.toTypedArray())
            clearSelection()
        }
    }
}
