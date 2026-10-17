package com.quicklist.app.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quicklist.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(ctx.getString(it)) }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importFrom(uri)
    }
    val versionName = remember {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(Modifier.padding(pad).verticalScroll(rememberScrollState())) {
            SectionTitle(R.string.settings_appearance)
            Text(stringResource(R.string.theme), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
            OptionRow(vm.themeMode == 0, stringResource(R.string.theme_system)) { vm.updateTheme(0) }
            OptionRow(vm.themeMode == 1, stringResource(R.string.theme_light)) { vm.updateTheme(1) }
            OptionRow(vm.themeMode == 2, stringResource(R.string.theme_dark)) { vm.updateTheme(2) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    stringResource(R.string.dynamic_colors),
                    stringResource(R.string.dynamic_colors_desc),
                    vm.dynamicColor
                ) { vm.updateDynamicColor(it) }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text(stringResource(R.string.language), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
            OptionRow(vm.language.isEmpty(), stringResource(R.string.lang_system)) { vm.updateLanguage("") }
            OptionRow(vm.language == "en", stringResource(R.string.lang_en)) { vm.updateLanguage("en") }
            OptionRow(vm.language == "ru", stringResource(R.string.lang_ru)) { vm.updateLanguage("ru") }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle(R.string.settings_behavior)
            SwitchRow(
                stringResource(R.string.auto_keyboard),
                stringResource(R.string.auto_keyboard_desc),
                vm.autoKeyboard
            ) { vm.updateAutoKeyboard(it) }
            SwitchRow(
                stringResource(R.string.mute_beep),
                stringResource(R.string.mute_beep_desc),
                vm.muteVoiceBeep
            ) { vm.updateMuteVoiceBeep(it) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle(R.string.settings_data)
            Text(
                stringResource(R.string.data_note),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_backup)) },
                modifier = Modifier.clickable { exportLauncher.launch("quicklist-backup.json") }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_backup)) },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            SectionTitle(R.string.settings_about)
            Text(
                stringResource(R.string.version, versionName),
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                stringResource(R.string.widget_hint),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(
        stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun OptionRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.clickable { onChange(!checked) }
    )
}
