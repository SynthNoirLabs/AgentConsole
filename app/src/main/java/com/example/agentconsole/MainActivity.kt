package com.example.agentconsole

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint

private const val PREFS_NAME = "agent_console_prefs"
private const val PREF_AGENT = "last_agent"
private const val PREF_WORKDIR = "last_workdir"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AgentConsoleApp()
            }
        }
    }
}

@Composable
fun AgentConsoleApp() {
    val context = LocalContext.current
    val viewModel: MainViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val termuxRepository = remember { TermuxRepository() }
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
    val termuxInstalled = remember { viewModel.checkTermuxInstalled(context) }
    val batteryOptimized = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            !pm.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }

    val workdirError = remember(workingDir) { termuxRepository.validateWorkingDir(workingDir) }
    val promptError = remember(prompt) {
        when {
            prompt.isBlank() -> "Prompt must not be empty."
            prompt.contains('\u0000') -> "Prompt must not contain null bytes."
            prompt.toByteArray(Charsets.UTF_8).size > TermuxRepository.MAX_PROMPT_SIZE ->
                "Prompt exceeds maximum allowed size (${TermuxRepository.MAX_PROMPT_SIZE / 1024}KB)."
            else -> null
        }
    }
    val canRun = !uiState.isRunning && workdirError == null && promptError == null

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Agent Console") })
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

            AgentDropdown(
                selectedAgent = selectedAgent,
                onSelected = {
                    selectedAgent = it
                    prefs.edit().putString(PREF_AGENT, it.name).apply()
                }
            )

            OutlinedTextField(
                value = workingDir,
                onValueChange = {
                    workingDir = it
                    prefs.edit().putString(PREF_WORKDIR, it).apply()
                },
                label = { Text("Repo / working directory") },
                supportingText = {
                    Text(workdirError ?: "Examples: ~/projects/myrepo or /sdcard/Download/myrepo")
                },
                isError = workdirError != null,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
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
                    Text(if (uiState.isRunning) "Running\u2026" else "Run")
                }

                Button(onClick = { termuxRepository.openTermux(context) }) {
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
            Text("\u2022 Termux installed: ${if (termuxInstalled) "yes" else "no"}")
            Text("\u2022 Grant this app: Run commands in Termux environment")
            Text("\u2022 In Termux set: allow-external-apps=true")
            Text("\u2022 Put your repo somewhere Termux can reach")
            if (batteryOptimized) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "\u26a0\ufe0f Battery optimization is enabled for this app. " +
                        "Background commands may be killed. " +
                        "Disable in Settings \u2192 Apps \u2192 Agent Console \u2192 Battery.",
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
