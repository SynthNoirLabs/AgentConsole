package com.example.agentconsole

import android.os.Bundle
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

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
    val uiState by ExecutionStore.state.collectAsState()
    var workingDir by rememberSaveable { mutableStateOf("~/projects/your-repo") }
    var prompt by rememberSaveable { mutableStateOf("Summarize this codebase and suggest the next three refactors.") }
    var selectedAgent by rememberSaveable { mutableStateOf(Agent.CLAUDE) }
    val termuxInstalled = remember { TermuxRunner.isTermuxInstalled(context) }

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
            StatusCard(termuxInstalled = termuxInstalled, uiState = uiState)

            AgentDropdown(
                selectedAgent = selectedAgent,
                onSelected = { selectedAgent = it }
            )

            OutlinedTextField(
                value = workingDir,
                onValueChange = { workingDir = it },
                label = { Text("Repo / working directory") },
                supportingText = { Text("Examples: ~/projects/myrepo or /sdcard/Download/myrepo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = { Text("Prompt") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        TermuxRunner.run(
                            context = context,
                            agent = selectedAgent,
                            prompt = prompt,
                            workingDir = workingDir
                        )
                    },
                    enabled = !uiState.isRunning
                ) {
                    Text(if (uiState.isRunning) "Running\u2026" else "Run")
                }

                Button(onClick = { TermuxRunner.openTermux(context) }) {
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
fun StatusCard(termuxInstalled: Boolean, uiState: ExecutionUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
            Text("\u2022 Termux installed: ${if (termuxInstalled) "yes" else "no"}")
            Text("\u2022 Grant this app: Run commands in Termux environment")
            Text("\u2022 In Termux set: allow-external-apps=true")
            Text("\u2022 Put your repo somewhere Termux can reach")
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
