package co.hermesdispatch.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
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

    LaunchedEffect(state.signedOut) { if (state.signedOut) onSignedOut() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LabeledValue("Bridge", state.bridgeUrl.ifBlank { "—" })
            LabeledValue("Background push", if (state.pushConfigured) "Connected" else "Not set up")

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

            OutlinedButton(onClick = viewModel::signOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
}
