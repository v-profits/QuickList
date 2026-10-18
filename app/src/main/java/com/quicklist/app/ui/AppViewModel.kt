package com.quicklist.app.ui

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quicklist.app.R
import com.quicklist.app.data.ItemEntity
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.Prefs
import com.quicklist.app.data.Repository
import com.quicklist.app.data.TYPE_SHOPPING
import com.quicklist.app.util.ParsedItem
import com.quicklist.app.util.QuickParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListSummary(val list: ListEntity, val total: Int, val done: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)
    private val prefs = Prefs(app)

    // ---- Settings state ----
    var themeMode by mutableIntStateOf(prefs.theme); private set
    var dynamicColor by mutableStateOf(prefs.dynamicColor); private set
    var autoKeyboard by mutableStateOf(prefs.autoKeyboard); private set
    var muteVoiceBeep by mutableStateOf(prefs.muteVoiceBeep); private set
    var lastListId by mutableLongStateOf(prefs.lastListId); private set
    var language by mutableStateOf(
        AppCompatDelegate.getApplicationLocales().toLanguageTags().take(2)
    ); private set

    val messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    fun updateTheme(mode: Int) {
        themeMode = mode
        prefs.theme = mode
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(mode))
    }

    fun updateLanguage(tag: String) {
        language = tag
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun updateDynamicColor(v: Boolean) { dynamicColor = v; prefs.dynamicColor = v }
    fun updateAutoKeyboard(v: Boolean) { autoKeyboard = v; prefs.autoKeyboard = v }
    fun updateMuteVoiceBeep(v: Boolean) { muteVoiceBeep = v; prefs.muteVoiceBeep = v }
    fun setLast(id: Long) { lastListId = id; prefs.lastListId = id }

    // ---- Lists ----
    val lists: StateFlow<List<ListSummary>?> = combine(repo.observeLists(), repo.observeCounts()) { ls, counts ->
        val byId = counts.associateBy { it.listId }
        ls.map { ListSummary(it, byId[it.id]?.total ?: 0, byId[it.id]?.done ?: 0) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { repo.ensureDefaults() }
    }

    fun createList(name: String, type: Int, icon: Int, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.createList(name, type, icon)) }
    }

    fun renameList(id: Long, name: String) { viewModelScope.launch { repo.renameList(id, name) } }
    fun deleteList(id: Long) { viewModelScope.launch { repo.deleteList(id) } }
    fun updateList(list: ListEntity) { viewModelScope.launch { repo.updateList(list) } }

    suspend fun firstListOfType(type: Int): Long? {
        repo.ensureDefaults()
        return repo.allLists().firstOrNull { it.type == type }?.id
    }

    // ---- Current list ----
    private val selectedId = MutableStateFlow<Long?>(null)

    val currentList: StateFlow<ListEntity?> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf<ListEntity?>(null) else repo.observeList(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentItems: StateFlow<List<ItemEntity>> = selectedId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repo.observeItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knownTitles = MutableStateFlow<List<String>>(emptyList())

    fun select(id: Long) {
        selectedId.value = id
        setLast(id)
        refreshTitles()
    }

    private fun refreshTitles() {
        viewModelScope.launch { knownTitles.value = repo.suggestionTitles() }
    }

    // ---- Items ----
    fun addFromInput(listId: Long, text: String) {
        viewModelScope.launch {
            repo.addParsed(listId, text)
            refreshTitles()
        }
    }

    fun addTitle(listId: Long, title: String) {
        viewModelScope.launch {
            repo.addItems(listId, listOf(ParsedItem(title)))
            refreshTitles()
        }
    }

    /** Text pasted from the clipboard / received from a messenger. */
    fun addPasted(listId: Long, text: String) {
        viewModelScope.launch {
            repo.addItems(listId, QuickParser.parseShared(text))
            refreshTitles()
        }
    }

    fun quickAdd(listId: Long, text: String) {
        setLast(listId)
        addFromInput(listId, text)
    }

    suspend fun addShared(text: String): Long? {
        repo.ensureDefaults()
        val all = repo.allLists()
        val target = all.firstOrNull { it.id == lastListId }
            ?: all.firstOrNull { it.type == TYPE_SHOPPING }
            ?: all.firstOrNull()
            ?: return null
        repo.addItems(target.id, QuickParser.parseShared(text))
        setLast(target.id)
        return target.id
    }

    fun toggle(item: ItemEntity) { viewModelScope.launch { repo.setDone(item.id, !item.done) } }
    fun updateItem(item: ItemEntity) { viewModelScope.launch { repo.update(item) } }
    fun deleteItem(item: ItemEntity) { viewModelScope.launch { repo.delete(item.id) } }
    fun restoreItem(item: ItemEntity) { viewModelScope.launch { repo.restore(item) } }
    fun clearDone(listId: Long) { viewModelScope.launch { repo.clearDone(listId) } }
    fun uncheckAll(listId: Long) { viewModelScope.launch { repo.uncheckAll(listId) } }

    // ---- Backup ----
    fun exportTo(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = repo.exportJson()
                getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(json.toByteArray())
                } ?: error("no stream")
                messages.emit(R.string.export_done)
            } catch (e: Exception) {
                messages.emit(R.string.export_failed)
            }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val text = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("no stream")
                repo.importJson(text)
                messages.emit(R.string.import_done)
            } catch (e: Exception) {
                messages.emit(R.string.import_failed)
            }
        }
    }
}
