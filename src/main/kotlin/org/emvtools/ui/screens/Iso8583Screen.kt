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
import org.emvtools.util.Iso8583Parser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Iso8583Screen() {
    var mode by remember { mutableStateOf("Build") }
    var mti by remember { mutableStateOf("0200") }
    var rawMessage by remember { mutableStateOf("") }
    var parsedMessage by remember { mutableStateOf<Iso8583Parser.Iso8583Message?>(null) }
    var error by remember { mutableStateOf("") }
    
    // Field values for building
    var fieldValues by remember { mutableStateOf(mutableMapOf<Int, String>()) }
    var showFieldDialog by remember { mutableStateOf(false) }
    var selectedFieldNum by remember { mutableStateOf(2) }
    var selectedFieldValue by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Input
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("ISO8583 Message Builder")
            
            // Mode selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "Build",
                    onClick = { mode = "Build" },
                    label = { Text("Build") },
                    leadingIcon = { Icon(Icons.Default.Build, null) }
                )
                FilterChip(
                    selected = mode == "Parse",
                    onClick = { mode = "Parse" },
                    label = { Text("Parse") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
            }
            
            if (mode == "Build") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // MTI selection
                        Text("Message Type Indicator (MTI)", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = mti,
                                onValueChange = { mti = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("MTI") },
                                singleLine = true,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                Iso8583Parser.mtiDescriptions[mti] ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                        
                        Divider()
                        
                        // Add field button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fields", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { showFieldDialog = true }) {
                                Icon(Icons.Default.Add, "Add Field")
                            }
                        }
                        
                        // Field list
                        fieldValues.toSortedMap().forEach { (fieldNum, value) ->
                            val fieldDef = Iso8583Parser.fieldDefinitions[fieldNum]
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "F$fieldNum: ${fieldDef?.name ?: "Unknown"}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Text(
                                            value.take(40) + if (value.length > 40) "..." else "",
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(onClick = {
                                        fieldValues = fieldValues.toMutableMap().apply { remove(fieldNum) }
                                    }) {
                                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                error = ""
                                try {
                                    parsedMessage = Iso8583Parser.build(mti, fieldValues)
                                } catch (e: Exception) {
                                    error = e.message ?: "Error building message"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Build Message")
                        }
                    }
                }
            } else {
                // Parse mode
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rawMessage,
                            onValueChange = { rawMessage = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                            label = { Text("ISO8583 Message (hex)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            maxLines = 10
                        )
                        
                        Button(
                            onClick = {
                                error = ""
                                try {
                                    parsedMessage = Iso8583Parser.parse(rawMessage)
                                } catch (e: Exception) {
                                    error = e.message ?: "Error parsing message"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = rawMessage.isNotEmpty()
                        ) {
                            Text("Parse Message")
                        }
                    }
                }
            }
            
            if (error.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        
        VerticalDivider()
        
        // Right panel - Result
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (parsedMessage != null) {
                var viewMode by remember { mutableStateOf("Fields") }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = viewMode == "Fields", onClick = { viewMode = "Fields" }, label = { Text("Fields") })
                    FilterChip(selected = viewMode == "Hex", onClick = { viewMode = "Hex" }, label = { Text("Hex") })
                    FilterChip(selected = viewMode == "JSON", onClick = { viewMode = "JSON" }, label = { Text("JSON") })
                }
                
                when (viewMode) {
                    "Fields" -> {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                KeyValueRow("MTI", "${parsedMessage!!.mti} (${Iso8583Parser.mtiDescriptions[parsedMessage!!.mti] ?: "Unknown"})")
                                KeyValueRow("Bitmap", parsedMessage!!.bitmap)
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parsedMessage!!.fields.toSortedMap().entries.toList()) { (fieldNum, value) ->
                                val fieldDef = Iso8583Parser.fieldDefinitions[fieldNum]
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            "Field $fieldNum: ${fieldDef?.name ?: "Unknown"}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (fieldDef != null) {
                                            Text(
                                                "${fieldDef.lengthType} | ${fieldDef.dataType} | Max: ${fieldDef.maxLength}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                                        SelectionContainer {
                                            Text(
                                                value.chunked(32).joinToString("\n"),
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Hex" -> {
                        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            SelectionContainer {
                                Text(
                                    parsedMessage!!.toHex().chunked(32).joinToString("\n"),
                                    modifier = Modifier.padding(16.dp),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    "JSON" -> {
                        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            SelectionContainer {
                                Text(
                                    parsedMessage!!.toJson(),
                                    modifier = Modifier.padding(16.dp),
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Build or parse a message to see results", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    
    // Add Field Dialog
    if (showFieldDialog) {
        AlertDialog(
            onDismissRequest = { showFieldDialog = false },
            title = { Text("Add Field") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Field number dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = "F$selectedFieldNum: ${Iso8583Parser.fieldDefinitions[selectedFieldNum]?.name ?: "Unknown"}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Field") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            Iso8583Parser.fieldDefinitions.toSortedMap().forEach { (num, def) ->
                                DropdownMenuItem(
                                    text = { Text("F$num: ${def.name}") },
                                    onClick = {
                                        selectedFieldNum = num
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = selectedFieldValue,
                        onValueChange = { selectedFieldValue = it },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    fieldValues = fieldValues.toMutableMap().apply { put(selectedFieldNum, selectedFieldValue) }
                    selectedFieldValue = ""
                    showFieldDialog = false
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showFieldDialog = false }) { Text("Cancel") }
            }
        )
    }
}

