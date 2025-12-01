package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.ui.components.*
import org.emvtools.util.HsmCommands

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HsmTesterScreen() {
    var selectedCommand by remember { mutableStateOf(HsmCommands.thalesCommands.first()) }
    var paramValues by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var messageHeader by remember { mutableStateOf("0000") }
    var builtCommand by remember { mutableStateOf("") }
    var commandHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    
    // Group commands by category
    val commandsByCategory = HsmCommands.thalesCommands.groupBy { it.category }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Command Selection & Parameters
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("HSM Command Tester (Thales payShield)")
            
            // Command selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Command", style = MaterialTheme.typography.titleSmall)
                    
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = "${selectedCommand.code} - ${selectedCommand.name}",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            commandsByCategory.forEach { (category, commands) ->
                                Text(
                                    category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                commands.forEach { cmd ->
                                    DropdownMenuItem(
                                        text = { Text("${cmd.code} - ${cmd.name}") },
                                        onClick = {
                                            selectedCommand = cmd
                                            paramValues = mutableMapOf()
                                            expanded = false
                                        }
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                    
                    Text(
                        selectedCommand.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Message header
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = messageHeader,
                        onValueChange = { messageHeader = it.take(8) },
                        label = { Text("Message Header") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Optional header (e.g., 0000)") }
                    )
                }
            }
            
            // Parameters
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Parameters", style = MaterialTheme.typography.titleSmall)
                        Divider()
                    }
                    
                    if (selectedCommand.parameters.isEmpty()) {
                        item {
                            Text(
                                "No parameters required",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(selectedCommand.parameters) { param ->
                            Column {
                                OutlinedTextField(
                                    value = paramValues[param.name] ?: param.defaultValue,
                                    onValueChange = {
                                        paramValues = paramValues.toMutableMap().apply { put(param.name, it) }
                                    },
                                    label = { 
                                        Text(
                                            "${param.name}${if (param.required) " *" else ""}",
                                            color = if (param.required) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    supportingText = { Text("${param.description} (${param.length})") }
                                )
                            }
                        }
                    }
                }
            }
            
            // Build button
            Button(
                onClick = {
                    builtCommand = HsmCommands.buildCommand(selectedCommand, paramValues, messageHeader)
                    commandHistory = commandHistory + (builtCommand to "")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Build, null)
                Spacer(Modifier.width(8.dp))
                Text("Build Command")
            }
        }
        
        VerticalDivider()
        
        // Right panel - Built Command & Reference
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Built Command")
            
            if (builtCommand.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Command String", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(
                                builtCommand,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Divider()
                        
                        Text("Hex View", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(
                                builtCommand.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Text("${builtCommand.length} characters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            
            // Expected Response Fields
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Expected Response Fields", style = MaterialTheme.typography.titleSmall)
                    Divider()
                    selectedCommand.responseFields.forEach { field ->
                        Text("• $field", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            // Response Code Reference
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(modifier = Modifier.padding(16.dp)) {
                    item {
                        Text("Response Codes Reference", style = MaterialTheme.typography.titleSmall)
                        Divider()
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    items(HsmCommands.responseCodeDescriptions.entries.toList()) { (code, desc) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                code,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(40.dp)
                            )
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (code == "00") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
            
            // Note about actual HSM connection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "This tool builds HSM commands for testing. Actual HSM communication requires network connectivity to the HSM.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

