package co.hermesdispatch.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onSignedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var profile by remember(state.profile) { mutableStateOf(state.profile) }
    var bridgeUrlField by remember(state.bridgeUrl) { mutableStateOf(state.bridgeUrl) }
    var tokenField by remember { mutableStateOf("") }

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }
    LaunchedEffect(state.connectionSaved) {
        if (state.connectionSaved) { tokenField = ""; viewModel.ackConnectionSaved() }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Connection", style = MaterialTheme.typography.titleMedium)
            Text(
                "The Hermes Dispatch bridge this app talks to.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(
                value = bridgeUrlField,
                onValueChange = { bridgeUrlField = it },
                label = { Text("Bridge URL") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tokenField,
                onValueChange = { tokenField = it },
                label = { Text("Bridge token") },
                placeholder = { Text("Leave blank to keep current") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            state.connectionError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { viewModel.saveConnection(bridgeUrlField, tokenField) },
                enabled = !state.savingConnection &&
                    bridgeUrlField.isNotBlank() &&
                    (bridgeUrlField != state.bridgeUrl || tokenField.isNotBlank()),
            ) { Text(if (state.savingConnection) "Connecting…" else "Save connection") }

            HorizontalDivider()

            Text("Lock-screen alerts", style = MaterialTheme.typography.titleMedium)
            if (state.pushConfigured && state.pushTopic.isNotBlank()) {
                Text(
                    "Install the ntfy app, then subscribe to this topic to get task " +
                        "progress on your lock screen (server ${state.pushBaseUrl}):",
                    style = MaterialTheme.typography.bodySmall,
                )
                LabeledValue("ntfy topic", state.pushTopic)
            } else {
                Text(
                    "Not configured on the bridge (set NTFY_TOPIC).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider()

            Text("Profile", style = MaterialTheme.typography.titleMedium)
            Text(
                "Switch which Hermes profile this app talks to.",
                style = MaterialTheme.typography.bodySmall,
            )
            if (state.availableProfiles.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.availableProfiles.forEach { name ->
                        FilterChip(
                            selected = name == profile,
                            onClick = { profile = name; viewModel.saveProfile(name) },
                            label = { Text(name) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = profile,
                onValueChange = { profile = it },
                label = { Text("Active profile") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.saveProfile(profile) },
                enabled = profile != state.profile,
            ) { Text("Save profile") }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            Text("Model", style = MaterialTheme.typography.titleMedium)
            Text(
                "The model this profile's agent uses. Pick a working one if replies fail.",
                style = MaterialTheme.typography.bodySmall,
            )
            var modelMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { modelMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.currentModel.ifBlank { "Select model" })
                }
                DropdownMenu(expanded = modelMenu, onDismissRequest = { modelMenu = false }) {
                    state.models.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.model) },
                            onClick = { viewModel.setModel(opt); modelMenu = false },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            Text("Preferences", style = MaterialTheme.typography.titleMedium)
            SettingSwitchRow(
                title = "Alert on failed runs",
                subtitle = "Get a push when any scheduled job fails (once per failing streak), " +
                    "even without a per-job bell.",
                checked = state.alertOnFailures,
                onCheckedChange = viewModel::setAlertOnFailures,
            )
            SettingSwitchRow(
                title = "Server transcription",
                subtitle = "Send voice to the bridge for speech-to-text instead of using on-device.",
                checked = state.serverStt,
                onCheckedChange = viewModel::setServerStt,
            )
            SettingSwitchRow(
                title = "Encrypt notifications (E2EE)",
                subtitle = "AES-encrypt push payloads so the relay only sees ciphertext; " +
                    "this app decrypts and shows them.",
                checked = state.encryptedPush,
                onCheckedChange = viewModel::setEncryptedPush,
            )
            SettingSwitchRow(
                title = "Bug reporting",
                subtitle = "Capture this app's own logs so you can share a diagnostic report. " +
                    "Secrets/keys are redacted, and you review it before sharing.",
                checked = state.bugReporting,
                onCheckedChange = viewModel::setBugReporting,
            )
            if (state.bugReporting) {
                OutlinedButton(
                    onClick = viewModel::prepareReport,
                    enabled = !state.preparingReport,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (state.preparingReport) "Collecting…" else "Share diagnostic report") }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }

    val context = LocalContext.current
    state.reportPreview?.let { report ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReport,
            title = { Text("Diagnostic report") },
            text = {
                Column {
                    Text(
                        "Review the redacted report below, then share it. Nothing is sent automatically.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        report,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = co.hermesdispatch.app.diag.DiagnosticReporter.writeShareFile(context, report)
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Hermes Dispatch diagnostic report")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, "Share report"))
                    viewModel.dismissReport()
                }) { Text("Share") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissReport) { Text("Close") } },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}
