package com.quicklist.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.core.content.ContextCompat
import com.quicklist.app.util.QuickParser
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quicklist.app.R
import com.quicklist.app.data.Category
import com.quicklist.app.data.ItemEntity
import com.quicklist.app.data.REPEAT_NONE
import com.quicklist.app.data.TYPE_SHOPPING
import com.quicklist.app.util.SortMode
import com.quicklist.app.util.buildShareText
import com.quicklist.app.util.formatDue
import com.quicklist.app.util.sortItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat

private enum class ListDialog { RENAME, DELETE, SORT, CLEAR, PASTE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(vm: AppViewModel, listId: Long, focusInput: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(listId) { vm.select(listId) }

    val listState by vm.currentList.collectAsState()
    val allItems by vm.currentItems.collectAsState()
    val titles by vm.knownTitles.collectAsState()

    // Guard against stale data while switching between lists.
    val list = listState?.takeIf { it.id == listId }
    val items = remember(allItems, listId) { allItems.filter { it.listId == listId } }
    val isShopping = list?.type == TYPE_SHOPPING

    var input by rememberSaveable { mutableStateOf("") }
    var editing by remember { mutableStateOf<ItemEntity?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<ListDialog?>(null) }
    var showDone by rememberSaveable { mutableStateOf(true) }
    var pasteInitial by remember { mutableStateOf("") }
    val clipboard = LocalClipboardManager.current

    val sortMode = SortMode.fromOrdinal(list?.sortMode ?: 2)
    val grouped = list != null && list.type == TYPE_SHOPPING && list.groupByCategory
    val sorted = remember(items, sortMode) { sortItems(items, sortMode) }
    val active = remember(sorted) { sorted.filter { !it.done } }
    val completed = remember(items) { items.filter { it.done }.sortedByDescending { it.doneAt ?: 0L } }
    val groups = remember(active, grouped) {
        if (grouped) {
            active.groupBy { Category.fromOrdinal(it.category) }
                .toSortedMap(compareBy<Category> { if (it == Category.OTHER) 99 else it.ordinal })
        } else emptyMap()
    }

    val activeTitles = remember(items) { items.filter { !it.done }.map { it.title.lowercase() }.toSet() }
    val suggestions = remember(input, titles, activeTitles) {
        val q = input.substringAfterLast(',').substringAfterLast(';').trim().lowercase()
        titles.asSequence()
            .filter { t ->
                val lower = t.lowercase()
                lower !in activeTitles && lower != q &&
                        (q.isEmpty() || lower.startsWith(q) || lower.contains(" $q"))
            }
            .take(10).toList()
    }

    val currency = remember { NumberFormat.getCurrencyInstance() }
    val totalPrice = items.sumOf { it.price ?: 0.0 }
    val hasPrices = items.any { it.price != null }

    val deletedText = stringResource(R.string.item_deleted)
    val undoText = stringResource(R.string.undo)
    val voiceUnavailable = stringResource(R.string.voice_unavailable)
    val voicePrompt = stringResource(R.string.voice_prompt)

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(listId) {
        if (focusInput || vm.autoKeyboard) {
            delay(350)
            runCatching { focusRequester.requestFocus(); keyboard?.show() }
        }
    }

    val voiceError = stringResource(R.string.voice_error)
    val voicePermission = stringResource(R.string.voice_permission)
    var listening by remember { mutableStateOf(false) }

    val voice = remember {
        VoiceInput(
            context = context,
            shouldMute = { vm.muteVoiceBeep },
            onListening = { listening = it },
            onText = { text, _ -> input = text },
            onError = { code ->
                if (code != SpeechRecognizer.ERROR_NO_MATCH && code != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    scope.launch { snackbar.showSnackbar(voiceError) }
                }
            }
        )
    }
    DisposableEffect(Unit) { onDispose { voice.destroy() } }

    fun beginVoice() {
        if (!voice.isAvailable()) {
            scope.launch { snackbar.showSnackbar(voiceUnavailable) }
            return
        }
        voice.start()
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginVoice() else scope.launch { snackbar.showSnackbar(voicePermission) }
    }

    fun toggleVoice() {
        if (listening) {
            voice.stop()
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoice()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun submit() {
        val t = input.trim()
        if (t.isEmpty()) return
        vm.addFromInput(listId, t)
        input = ""
        runCatching { focusRequester.requestFocus() }
    }

    fun deleteItem(item: ItemEntity) {
        vm.deleteItem(item)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val r = snackbar.showSnackbar(deletedText, actionLabel = undoText)
            if (r == SnackbarResult.ActionPerformed) vm.restoreItem(item)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name.orEmpty(), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (list != null) {
                        IconButton(onClick = { dialog = ListDialog.SORT }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.menu_sort))
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (isShopping) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_group)) },
                                        trailingIcon = { if (list.groupByCategory) Icon(Icons.Default.Check, null) },
                                        onClick = {
                                            menuOpen = false
                                            vm.updateList(list.copy(groupByCategory = !list.groupByCategory))
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.rename)) },
                                    onClick = { menuOpen = false; dialog = ListDialog.RENAME }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_share)) },
                                    onClick = {
                                        menuOpen = false
                                        val send = Intent(Intent.ACTION_SEND)
                                            .setType("text/plain")
                                            .putExtra(Intent.EXTRA_TEXT, buildShareText(list, items))
                                        context.startActivity(Intent.createChooser(send, null))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_paste)) },
                                    leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        pasteInitial = clipboard.getText()?.text.orEmpty()
                                        dialog = ListDialog.PASTE
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_uncheck_all)) },
                                    onClick = { menuOpen = false; vm.uncheckAll(listId) }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_clear_done)) },
                                    onClick = { menuOpen = false; dialog = ListDialog.CLEAR }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.menu_delete_list), color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = { menuOpen = false; dialog = ListDialog.DELETE }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            InputBar(
                value = input,
                onValueChange = { input = it },
                hint = stringResource(if (isShopping) R.string.input_hint_shopping else R.string.input_hint_todo),
                suggestions = suggestions,
                focusRequester = focusRequester,
                onSubmit = { submit() },
                onSuggestion = { vm.addTitle(listId, it); input = "" },
                listening = listening,
                onVoice = { toggleVoice() }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (items.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.list_progress, completed.size, items.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (isShopping && hasPrices) {
                        Text(
                            stringResource(R.string.total_label, currency.format(totalPrice)),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { completed.size.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 8.dp)) {
                if (list != null && items.isEmpty()) {
                    item(key = "empty") {
                        Column(
                            Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Checklist, contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(Modifier.size(12.dp))
                            Text(stringResource(R.string.empty_list_title), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(R.string.empty_list_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                if (grouped) {
                    for ((cat, catItems) in groups) {
                        if (groups.size > 1 || cat != Category.OTHER) item(key = "h_${cat.name}") {
                            Text(
                                "${cat.emoji}  ${stringResource(cat.label)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(catItems, key = { it.id }) { item ->
                            SwipeRow(Modifier.animateItem(), onDelete = { deleteItem(item) }) {
                                ItemRow(item, currency, onToggle = { vm.toggle(item) }, onClick = { editing = item })
                            }
                        }
                    }
                } else {
                    items(active, key = { it.id }) { item ->
                        SwipeRow(Modifier.animateItem(), onDelete = { deleteItem(item) }) {
                            ItemRow(item, currency, onToggle = { vm.toggle(item) }, onClick = { editing = item })
                        }
                    }
                }

                if (completed.isNotEmpty()) {
                    item(key = "completed_header") {
                        Row(
                            Modifier.fillMaxWidth().clickable { showDone = !showDone }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.completed_count, completed.size),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (showDone) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (showDone) {
                        items(completed, key = { it.id }) { item ->
                            SwipeRow(Modifier.animateItem(), onDelete = { deleteItem(item) }) {
                                ItemRow(item, currency, onToggle = { vm.toggle(item) }, onClick = { editing = item })
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- Dialogs ----
    editing?.let { item ->
        ItemEditDialog(
            item = item,
            isShopping = isShopping,
            muteBeep = { vm.muteVoiceBeep },
            onDismiss = { editing = null },
            onSave = { vm.updateItem(it); editing = null },
            onDelete = { editing = null; deleteItem(item) }
        )
    }

    if (list != null) {
        when (dialog) {
            ListDialog.RENAME -> EditListDialog(
                initialName = list.name,
                initialIcon = list.icon,
                type = list.type,
                muteBeep = { vm.muteVoiceBeep },
                onConfirm = { name, icon ->
                    vm.updateList(list.copy(name = name.trim(), icon = icon))
                    dialog = null
                },
                onDismiss = { dialog = null }
            )
            ListDialog.DELETE -> ConfirmDialog(
                title = stringResource(R.string.delete_list_title),
                message = stringResource(R.string.delete_list_msg, list.name),
                confirmText = stringResource(R.string.delete),
                onConfirm = { dialog = null; vm.deleteList(listId); onBack() },
                onDismiss = { dialog = null }
            )
            ListDialog.CLEAR -> ConfirmDialog(
                title = stringResource(R.string.clear_done_title),
                message = stringResource(R.string.clear_done_msg),
                confirmText = stringResource(R.string.clear),
                onConfirm = { vm.clearDone(listId); dialog = null },
                onDismiss = { dialog = null }
            )
            ListDialog.SORT -> AlertDialog(
                onDismissRequest = { dialog = null },
                title = { Text(stringResource(R.string.sort_title)) },
                text = {
                    Column {
                        SortMode.forType(list.type).forEach { mode ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .selectable(
                                        selected = mode == sortMode,
                                        role = Role.RadioButton,
                                        onClick = {
                                            vm.updateList(list.copy(sortMode = mode.ordinal))
                                            dialog = null
                                        }
                                    )
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = mode == sortMode, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(stringResource(mode.label))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
            ListDialog.PASTE -> PasteDialog(
                initial = pasteInitial,
                onAdd = { vm.addPasted(listId, it); dialog = null },
                onDismiss = { dialog = null }
            )
            null -> Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(modifier: Modifier, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
        content = { content() }
    )
}

@Composable
private fun ItemRow(item: ItemEntity, currency: NumberFormat, onToggle: () -> Unit, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.done) subtle else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.quantity.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.quantity, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (item.note.isNotBlank()) {
                Text(
                    item.note, style = MaterialTheme.typography.bodySmall, color = subtle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            val due = item.dueAt
            if (due != null) {
                val overdue = !item.done && due < System.currentTimeMillis()
                val c = if (overdue) MaterialTheme.colorScheme.error else subtle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = c, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatDue(ctx, due), style = MaterialTheme.typography.bodySmall, color = c)
                    if (item.repeat != REPEAT_NONE) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = c, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
        if (item.priority > 0 && !item.done) {
            Icon(
                Icons.Default.Flag, contentDescription = null,
                tint = if (item.priority >= 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
        }
        item.price?.let {
            Text(
                currency.format(it), style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    suggestions: List<String>,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onSuggestion: (String) -> Unit,
    listening: Boolean,
    onVoice: () -> Unit
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))) {
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions, key = { it }) { s ->
                        SuggestionChip(onClick = { onSuggestion(s) }, label = { Text(s) })
                    }
                }
            }
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    placeholder = { Text(hint, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    trailingIcon = {
                        IconButton(onClick = onVoice) {
                            Icon(
                                if (listening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = stringResource(R.string.voice),
                                tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = onSubmit, enabled = value.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                }
            }
        }
    }
}

@Composable
private fun PasteDialog(initial: String, onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val count = remember(text) { QuickParser.parseShared(text).size }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.paste_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 10,
                    placeholder = { Text(stringResource(R.string.paste_hint)) }
                )
                Text(
                    stringResource(R.string.paste_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(enabled = count > 0, onClick = { onAdd(text) }) { Text(stringResource(R.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
