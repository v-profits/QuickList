package com.quicklist.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quicklist.app.R
import com.quicklist.app.data.TYPE_SHOPPING
import com.quicklist.app.data.TYPE_TODO
import kotlinx.coroutines.delay

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initial: String,
    confirmText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    muteBeep: (() -> Boolean)? = null
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
    val mic: (@Composable () -> Unit)? = if (muteBeep != null) {
        {
            MicButton(muteBeep = muteBeep, onText = { spoken ->
                text = spoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            })
        }
    } else null
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.replace("\n", " ") },
                label = { Text(label) },
                singleLine = false,
                maxLines = 2,
                modifier = Modifier.focusRequester(focus),
                trailingIcon = mic,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onConfirm(text) })
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text) }) { Text(confirmText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/**
 * Microphone button for dictating a short text into a field.
 * Handles the permission, listening state and cleanup itself.
 */
@Composable
fun MicButton(muteBeep: () -> Boolean, onText: (String) -> Unit) {
    val context = LocalContext.current
    val currentOnText by rememberUpdatedState(onText)
    var listening by remember { mutableStateOf(false) }
    val errorMsg = stringResource(R.string.voice_error)
    val unavailableMsg = stringResource(R.string.voice_unavailable)
    val permissionMsg = stringResource(R.string.voice_permission)

    val voice = remember {
        VoiceInput(
            context = context,
            shouldMute = muteBeep,
            onListening = { listening = it },
            onText = { text, _ -> currentOnText(text) },
            onError = { code ->
                if (code != SpeechRecognizer.ERROR_NO_MATCH && code != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    DisposableEffect(Unit) { onDispose { voice.destroy() } }

    fun begin() {
        if (!voice.isAvailable()) {
            Toast.makeText(context, unavailableMsg, Toast.LENGTH_SHORT).show()
            return
        }
        voice.start()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else Toast.makeText(context, permissionMsg, Toast.LENGTH_SHORT).show()
    }

    IconButton(onClick = {
        when {
            listening -> voice.stop()
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> begin()
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }) {
        Icon(
            if (listening) Icons.Default.Stop else Icons.Default.Mic,
            contentDescription = stringResource(R.string.voice),
            tint = if (listening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ListIconPicker(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ListIconSet.chunked(6).forEachIndexed { row, icons ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                icons.forEachIndexed { col, vector ->
                    val index = row * 6 + col
                    val isSelected = index == selected
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            vector,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NewListDialog(
    muteBeep: () -> Boolean,
    onCreate: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var type by remember { mutableIntStateOf(TYPE_SHOPPING) }
    var icon by remember { mutableIntStateOf(ICON_LIST) }
    var iconTouched by remember { mutableStateOf(false) }
    val shoppingName = stringResource(R.string.default_shopping)
    val todoName = stringResource(R.string.default_todo)
    val name = field.text
    val focus = remember { FocusRequester() }

    fun setName(s: String) {
        field = TextFieldValue(s, TextRange(s.length))
    }

    fun pickType(t: Int) {
        type = t
        // Suggest a name (and a matching icon) only while the user has not typed or picked their own.
        if (name.isBlank() || name == shoppingName || name == todoName) {
            setName(if (t == TYPE_SHOPPING) shoppingName else todoName)
            if (!iconTouched) icon = if (t == TYPE_SHOPPING) ICON_CART else ICON_CHECK
        }
    }

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_list)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it.copy(text = it.text.replace("\n", " ")) },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.focusRequester(focus),
                    trailingIcon = {
                        MicButton(muteBeep = muteBeep, onText = { spoken ->
                            setName(spoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        })
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate(name, type, icon) })
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TYPE_SHOPPING,
                        onClick = { pickType(TYPE_SHOPPING) },
                        label = { Text(stringResource(R.string.type_shopping)) },
                        leadingIcon = {
                            Icon(
                                if (type == TYPE_SHOPPING) Icons.Default.Check else Icons.Default.ShoppingCart,
                                contentDescription = null
                            )
                        }
                    )
                    FilterChip(
                        selected = type == TYPE_TODO,
                        onClick = { pickType(TYPE_TODO) },
                        label = { Text(stringResource(R.string.type_todo)) },
                        leadingIcon = {
                            Icon(
                                if (type == TYPE_TODO) Icons.Default.Check else Icons.Default.CheckCircle,
                                contentDescription = null
                            )
                        }
                    )
                }
                Text(stringResource(R.string.list_icon), style = MaterialTheme.typography.labelLarge)
                ListIconPicker(selected = icon, onSelect = { icon = it; iconTouched = true })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, type, icon) }) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Rename a list and change its icon. */
@Composable
fun EditListDialog(
    initialName: String,
    initialIcon: Int,
    type: Int,
    muteBeep: () -> Boolean,
    onConfirm: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length))) }
    var icon by remember { mutableIntStateOf(resolveListIconIndex(initialIcon, type)) }
    val focus = remember { FocusRequester() }
    val name = field.text

    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_list)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it.copy(text = it.text.replace("\n", " ")) },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.focusRequester(focus),
                    trailingIcon = {
                        MicButton(muteBeep = muteBeep, onText = { spoken ->
                            val t = spoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            field = TextFieldValue(t, TextRange(t.length))
                        })
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name, icon) })
                )
                Text(stringResource(R.string.list_icon), style = MaterialTheme.typography.labelLarge)
                ListIconPicker(selected = icon, onSelect = { icon = it })
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name, icon) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
