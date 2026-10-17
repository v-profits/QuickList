package com.quicklist.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.quicklist.app.R
import com.quicklist.app.data.Category
import com.quicklist.app.data.ItemEntity
import com.quicklist.app.util.formatDue
import com.quicklist.app.util.formatPlain
import java.util.Calendar

@Composable
fun ItemEditDialog(
    item: ItemEntity,
    isShopping: Boolean,
    muteBeep: () -> Boolean,
    onDismiss: () -> Unit,
    onSave: (ItemEntity) -> Unit,
    onDelete: () -> Unit
) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf(item.title) }
    var note by remember { mutableStateOf(item.note) }
    var quantity by remember { mutableStateOf(item.quantity) }
    var price by remember { mutableStateOf(item.price?.let { formatPlain(it) } ?: "") }
    var category by remember { mutableIntStateOf(item.category) }
    var priority by remember { mutableIntStateOf(item.priority) }
    var dueAt by remember { mutableStateOf(item.dueAt) }
    var repeat by remember { mutableIntStateOf(item.repeat) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun pickDateTime() {
        val base = Calendar.getInstance().apply {
            timeInMillis = dueAt ?: System.currentTimeMillis()
            if (dueAt == null) {
                set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
            }
        }
        DatePickerDialog(
            ctx,
            { _, y, m, d ->
                val c = Calendar.getInstance().apply {
                    timeInMillis = base.timeInMillis
                    set(y, m, d)
                }
                TimePickerDialog(
                    ctx,
                    { _, h, min ->
                        c.set(Calendar.HOUR_OF_DAY, h)
                        c.set(Calendar.MINUTE, min)
                        c.set(Calendar.SECOND, 0)
                        c.set(Calendar.MILLISECOND, 0)
                        dueAt = c.timeInMillis
                        if (Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), DateFormat.is24HourFormat(ctx)
                ).show()
            },
            base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val categories = Category.entries
    val categoryLabels = categories.map { "${it.emoji} ${stringResource(it.label)}" }
    val repeatLabels = listOf(
        stringResource(R.string.repeat_none), stringResource(R.string.repeat_daily),
        stringResource(R.string.repeat_weekly), stringResource(R.string.repeat_monthly)
    )
    val priorityLabels = listOf(
        stringResource(R.string.priority_none), stringResource(R.string.priority_medium), stringResource(R.string.priority_high)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_item)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    trailingIcon = {
                        MicButton(muteBeep = muteBeep, onText = { spoken ->
                            title = spoken.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        })
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                if (isShopping) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantity, onValueChange = { quantity = it },
                            label = { Text(stringResource(R.string.field_quantity)) },
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = price, onValueChange = { price = it },
                            label = { Text(stringResource(R.string.field_price)) },
                            singleLine = true, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    DropdownField(
                        label = stringResource(R.string.field_category),
                        valueText = categoryLabels[category.coerceIn(0, categories.size - 1)],
                        options = categoryLabels,
                        onSelect = { category = it }
                    )
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_note)) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                if (!isShopping) {
                    Text(stringResource(R.string.field_priority), style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        priorityLabels.forEachIndexed { i, label ->
                            FilterChip(selected = priority == i, onClick = { priority = i }, label = { Text(label) })
                        }
                    }
                    Text(stringResource(R.string.field_due), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { pickDateTime() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                            Text(
                                dueAt?.let { formatDue(ctx, it) } ?: stringResource(R.string.set_due),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        if (dueAt != null) {
                            IconButton(onClick = { dueAt = null; repeat = 0 }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    }
                    if (dueAt != null) {
                        DropdownField(
                            label = stringResource(R.string.field_repeat),
                            valueText = repeatLabels[repeat.coerceIn(0, 3)],
                            options = repeatLabels,
                            onSelect = { repeat = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim(),
                            note = note.trim(),
                            quantity = quantity.trim(),
                            price = price.replace(',', '.').toDoubleOrNull(),
                            category = category,
                            priority = priority,
                            dueAt = dueAt,
                            repeat = if (dueAt == null) 0 else repeat
                        )
                    )
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun DropdownField(label: String, valueText: String, options: List<String>, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $valueText", modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}
