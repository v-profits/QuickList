package com.quicklist.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.quicklist.app.R
import com.quicklist.app.data.ListEntity
import com.quicklist.app.data.TYPE_SHOPPING
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: AppViewModel,
    onOpenList: (Long, Boolean) -> Unit,
    onSettings: () -> Unit
) {
    val lists by vm.lists.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var showNew by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<ListEntity?>(null) }
    var deleting by remember { mutableStateOf<ListEntity?>(null) }
    val openLabel = stringResource(R.string.open)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showNew = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_list))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        bottomBar = {
            val ls = lists
            if (!ls.isNullOrEmpty()) {
                QuickAddBar(
                    muteBeep = { vm.muteVoiceBeep },
                    lists = ls,
                    selectedId = vm.lastListId,
                    onSelect = { vm.setLast(it) },
                    onAdd = { id, text ->
                        vm.quickAdd(id, text)
                        val name = ls.firstOrNull { it.list.id == id }?.list?.name.orEmpty()
                        scope.launch {
                            snackbar.currentSnackbarData?.dismiss()
                            val r = snackbar.showSnackbar(ctx.getString(R.string.added_to, name), actionLabel = openLabel)
                            if (r == SnackbarResult.ActionPerformed) onOpenList(id, false)
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            val ls = lists
            when {
                ls == null -> Unit
                ls.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.home_empty), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.size(16.dp))
                    Button(onClick = { showNew = true }) { Text(stringResource(R.string.new_list)) }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ls, key = { it.list.id }) { s ->
                        ListCard(
                            summary = s,
                            onClick = { onOpenList(s.list.id, false) },
                            onRename = { renaming = s.list },
                            onDelete = { deleting = s.list }
                        )
                    }
                }
            }
        }
    }

    if (showNew) {
        NewListDialog(
            muteBeep = { vm.muteVoiceBeep },
            onCreate = { name, type ->
                showNew = false
                vm.createList(name, type) { id -> onOpenList(id, true) }
            },
            onDismiss = { showNew = false }
        )
    }
    renaming?.let { l ->
        TextInputDialog(
            title = stringResource(R.string.rename),
            label = stringResource(R.string.list_name),
            initial = l.name,
            confirmText = stringResource(R.string.save),
            muteBeep = { vm.muteVoiceBeep },
            onConfirm = { vm.renameList(l.id, it); renaming = null },
            onDismiss = { renaming = null }
        )
    }
    deleting?.let { l ->
        ConfirmDialog(
            title = stringResource(R.string.delete_list_title),
            message = stringResource(R.string.delete_list_msg, l.name),
            confirmText = stringResource(R.string.delete),
            onConfirm = { vm.deleteList(l.id); deleting = null },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun ListCard(
    summary: ListSummary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val list = summary.list
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (list.type == TYPE_SHOPPING) Icons.Default.ShoppingCart else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    list.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                val status = when {
                    summary.total == 0 -> stringResource(R.string.list_empty)
                    summary.done == summary.total -> stringResource(R.string.list_all_done)
                    else -> stringResource(R.string.list_progress, summary.done, summary.total)
                }
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (summary.total > 0) {
                    LinearProgressIndicator(
                        progress = { summary.done.toFloat() / summary.total },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp)
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename)) },
                        onClick = { menu = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = { menu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddBar(
    lists: List<ListSummary>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onAdd: (Long, String) -> Unit,
    muteBeep: () -> Boolean
) {
    var text by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val selected = lists.firstOrNull { it.list.id == selectedId } ?: lists.first()

    fun submit() {
        val t = text.trim()
        if (t.isEmpty()) return
        onAdd(selected.list.id, t)
        text = ""
    }

    Surface(tonalElevation = 3.dp, shadowElevation = 4.dp) {
        Column(
            Modifier
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box {
                AssistChip(
                    onClick = { menu = true },
                    label = { Text(stringResource(R.string.add_to, selected.list.name), maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            if (selected.list.type == TYPE_SHOPPING) Icons.Default.ShoppingCart else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) }
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    lists.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.list.name) },
                            onClick = { onSelect(s.list.id); menu = false }
                        )
                    }
                }
            }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.quick_add_hint)) },
                    trailingIcon = { MicButton(muteBeep = muteBeep, onText = { spoken -> text = spoken }) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() })
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = { submit() }, enabled = text.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.add))
                }
            }
        }
    }
}
