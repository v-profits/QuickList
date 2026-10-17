package com.quicklist.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focus.requestFocus() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.focusRequester(focus),
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
fun NewListDialog(
    muteBeep: () -> Boolean,
    onCreate: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var type by remember { mutableIntStateOf(TYPE_SHOPPING) }
    val shoppingName = stringResource(R.string.default_shopping)
    val todoName = stringResource(R.string.default_todo)
    val name = field.text
    val focus = remember { FocusRequester() }

    fun setName(s: String) {
        field = TextFieldValue(s, TextRange(s.length))
    }

    fun pickType(t: Int) {
        type = t
        // Suggest a name, but never overwrite something the user typed or dictated.
        if (name.isBlank() || name == shoppingName || name == todoName) {
            setName(if (t == TYPE_SHOPPING) shoppingName else todoName)
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    label = { Text(stringResource(R.string.list_name)) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focus),
                    trailingIcon = {
                        MicButton(muteBeep = muteBeep, onText = { spoken ->
                            setName(spoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                        })
                    },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onCreate(name, type) })
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
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onCreate(name, type) }) {
                Text(stringResource(R.string.create))
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
