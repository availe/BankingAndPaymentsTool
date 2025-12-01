package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.model.*
import org.emvtools.ui.components.*
import org.emvtools.util.CapkUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapkScreen() {
    var capks by remember { mutableStateOf(CapkUtils.sampleCapks.toMutableList()) }
    var selectedCapk by remember { mutableStateOf<Capk?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - CAPK List
        Column(
            modifier = Modifier.weight(0.4f).fillMaxHeight().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CAPK List", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, "Add CAPK")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, "Export")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(capks) { capk ->
                    CapkListItem(
                        capk = capk,
                        isSelected = capk == selectedCapk,
                        onClick = { selectedCapk = capk },
                        onDelete = {
                            capks = capks.filter { it != capk }.toMutableList()
                            if (selectedCapk == capk) selectedCapk = null
                        }
                    )
                }
            }
        }
        
        VerticalDivider()
        
        // Right panel - CAPK Details
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight().padding(16.dp)
        ) {
            if (selectedCapk != null) {
                CapkDetails(capk = selectedCapk!!)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select a CAPK to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    
    // Create CAPK Dialog
    if (showCreateDialog) {
        CreateCapkDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { newCapk ->
                capks = (capks + newCapk).toMutableList()
                showCreateDialog = false
            }
        )
    }
    
    // Export Dialog
    if (showExportDialog) {
        ExportCapkDialog(
            capks = capks,
            onDismiss = { showExportDialog = false },
            onExport = { result ->
                exportResult = result
                showExportDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CapkListItem(
    capk: Capk,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "${capk.rid} - ${capk.index}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    capk.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${capk.modulusLength} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CapkDetails(capk: Capk) {
    val checksumValid = remember(capk) { CapkUtils.validateChecksum(capk) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("CAPK Details", style = MaterialTheme.typography.titleMedium)
        }
        
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeyValueRow("RID", capk.rid)
                    KeyValueRow("Index", capk.index)
                    KeyValueRow("Description", capk.description)
                    KeyValueRow("Key ID", capk.keyId)
                    KeyValueRow("Modulus Length", "${capk.modulusLength} bytes (${capk.modulusLength * 8} bits)")
                    KeyValueRow("Exponent", capk.exponent)
                    KeyValueRow("Expiry Date", capk.expiryDate.ifEmpty { "Not set" })
                    KeyValueRow("Hash Algorithm", capk.hashAlgorithm.displayName)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Checksum Valid: ", style = MaterialTheme.typography.bodyMedium)
                        Icon(
                            if (checksumValid) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (checksumValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        item {
            Text("Modulus", style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    capk.modulus.chunked(64).joinToString("\n"),
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        item {
            Text("Checksum (SHA-1)", style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    capk.checksum,
                    modifier = Modifier.padding(12.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CreateCapkDialog(
    onDismiss: () -> Unit,
    onCreate: (Capk) -> Unit
) {
    var rid by remember { mutableStateOf("") }
    var index by remember { mutableStateOf("") }
    var modulus by remember { mutableStateOf("") }
    var exponent by remember { mutableStateOf("03") }
    var expiryDate by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create CAPK") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = rid,
                    onValueChange = { rid = it.uppercase().take(10) },
                    label = { Text("RID (10 hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = index,
                    onValueChange = { index = it.uppercase().take(2) },
                    label = { Text("Index (2 hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exponent,
                    onValueChange = { exponent = it.uppercase() },
                    label = { Text("Exponent (hex)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it.take(6) },
                    label = { Text("Expiry Date (YYMMDD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = modulus,
                    onValueChange = { modulus = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                    label = { Text("Modulus (hex)") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val capk = CapkUtils.createCapk(rid, index, modulus, exponent, expiryDate, description = description)
                    onCreate(capk)
                },
                enabled = rid.length == 10 && index.length == 2 && modulus.isNotEmpty() && exponent.isNotEmpty()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportCapkDialog(
    capks: List<Capk>,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(CapkExportFormat.XML) }
    var exportedContent by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export CAPKs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select export format:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CapkExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { 
                                selectedFormat = format
                                exportedContent = CapkUtils.export(capks, format)
                            },
                            label = { Text(format.name) }
                        )
                    }
                }
                
                if (exportedContent.isNotEmpty()) {
                    OutlinedTextField(
                        value = exportedContent,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onExport(exportedContent) }) {
                Text("Copy to Clipboard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

