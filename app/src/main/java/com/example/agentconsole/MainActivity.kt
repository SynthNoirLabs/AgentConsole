package com.example.agentconsole

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.agentconsole.ui.navigation.AgentConsoleNavGraph
import com.example.agentconsole.ui.theme.AgentConsoleTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

private const val PREFS_NAME = "agent_console_prefs"
private const val PREF_AGENT = "last_agent"
private const val PREF_WORKDIR = "last_workdir"
private const val PREFS_DEBOUNCE_MS = 500L

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentConsoleTheme {
                AgentConsoleNavGraph()
            }
        }
    }
}

@Composable
fun AgentConsoleApp(onNavigateToHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: MainViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var workingDir by rememberSaveable {
        mutableStateOf(prefs.getString(PREF_WORKDIR, "~/projects/your-repo") ?: "~/projects/your-repo")
    }
    var prompt by rememberSaveable { mutableStateOf("Summarize this codebase and suggest the next three refactors.") }
    var selectedAgent by rememberSaveable {
        val savedName = prefs.getString(PREF_AGENT, null)
        val agent = Agent.entries.find { it.name == savedName } ?: Agent.CLAUDE
        mutableStateOf(agent)
    }
    var termuxInstalled by remember { mutableStateOf(viewModel.isTermuxInstalled()) }
    var batteryOptimized by remember { mutableStateOf(isBatteryOptimized(context)) }

    LifecycleResumeEffect(Unit) {
        termuxInstalled = viewModel.isTermuxInstalled()
        batteryOptimized = isBatteryOptimized(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(workingDir) {
        delay(PREFS_DEBOUNCE_MS)
        prefs.edit().putString(PREF_WORKDIR, workingDir).apply()
    }

    val workdirError = remember(workingDir) { viewModel.validateWorkingDir(workingDir) }
    val promptError = remember(prompt) { viewModel.validatePrompt(prompt) }
    val canRun = !uiState.isRunning && workdirError == null && promptError == null

    val dirPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            // Convert URI to a path string. This is a simplified approach.
            // In a real app, you might need to resolve the actual path or use the URI directly.
            val path = it.path?.replace("/tree/primary:", "/sdcard/") ?: it.toString()
            workingDir = path
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agent Console") },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(termuxInstalled = termuxInstalled, uiState = uiState, batteryOptimized = batteryOptimized)

            uiState.historyError?.let { error ->
                HistoryErrorBanner(error = error, onDismiss = { viewModel.dismissHistoryError() })
            }

            AgentDropdown(
                selectedAgent = selectedAgent,
                onSelected = {
                    selectedAgent = it
                    prefs.edit().putString(PREF_AGENT, it.name).apply()
                }
            )

            OutlinedTextField(
                value = workingDir,
                onValueChange = { workingDir = it },
                label = { Text("Repo / working directory") },
                supportingText = {
                    Text(workdirError ?: "Examples: ~/projects/myrepo or /sdcard/Download/myrepo")
                },
                isError = workdirError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { dirPickerLauncher.launch(null) }) {
                        Icon(Icons.Default.Folder, contentDescription = "Pick Directory")
                    }
                }
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                supportingText = if (promptError != null) {{ Text(promptError) }} else null,
                isError = promptError != null,
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.run(
                            agent = selectedAgent,
                            prompt = prompt,
                            workingDir = workingDir
                        )
                    },
                    enabled = canRun
                ) {
                    Text(if (uiState.isRunning) "Running…" else "Run")
                }

                Button(onClick = { viewModel.openTermux(context) }) {
                    Text("Open Termux")
                }
            }

            OutputCard(
                title = "stdout",
                value = uiState.stdout.ifBlank { "No stdout yet." }
            )

            OutputCard(
                title = "stderr",
                value = buildString {
                    append(uiState.stderr)
                    if (uiState.internalErrorMessage.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("Termux internal error: ")
                        append(uiState.internalErrorMessage)
                    }
                }.ifBlank { "No stderr yet." }
            )
        }
    }
}

@Composable
fun StatusCard(termuxInstalled: Boolean, uiState: ExecutionUiState, batteryOptimized: Boolean = false) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            Text("• Termux installed: ${if (termuxInstalled) "yes" else "no"}")
            Text("• Grant this app: Run commands in Termux environment")
            Text("• In Termux set: allow-external-apps=true")
            Text("• Put your repo somewhere Termux can reach")
            if (batteryOptimized) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "⚠️ Battery optimization is enabled for this app. " +
                        "Background commands may be killed. " +
                        "Disable in Settings → Apps → Agent Console → Battery.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Status: ${uiState.status}")
            if (uiState.activeAgent.isNotBlank()) Text("Agent: ${uiState.activeAgent}")
            if (uiState.workingDir.isNotBlank()) Text("Working dir: ${uiState.workingDir}")
            uiState.exitCode?.let { Text("Exit code: $it") }
            uiState.internalErrorCode?.let { Text("Termux internal err: $it") }
        }
    }
}

@Composable
fun HistoryErrorBanner(error: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentDropdown(selectedAgent: Agent, onSelected: (Agent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedAgent.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Agent") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Agent.entries.forEach { agent ->
                DropdownMenuItem(
                    text = { Text(agent.displayName) },
                    onClick = {
                        onSelected(agent)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun OutputCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun isBatteryOptimized(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName).not()
    } else {
        false
    }
}
