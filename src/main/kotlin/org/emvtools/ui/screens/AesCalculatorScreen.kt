package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.crypto.AesCalculator
import org.emvtools.crypto.AesCalculator.AesKeySize
import org.emvtools.crypto.AesCalculator.AesMode
import org.emvtools.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AesCalculatorScreen() {
    var key by remember { mutableStateOf("") }
    var data by remember { mutableStateOf("") }
    var iv by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    
    var selectedMode by remember { mutableStateOf(AesMode.ECB) }
    var selectedOperation by remember { mutableStateOf("Encrypt") }
    var selectedKeySize by remember { mutableStateOf(AesKeySize.AES_128) }
    
    val operations = listOf("Encrypt", "Decrypt", "CMAC", "CBC-MAC", "KCV")
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Input
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("AES Calculator")
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Operation selection
                    Text("Operation", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        operations.forEach { op ->
                            FilterChip(
                                selected = selectedOperation == op,
                                onClick = { selectedOperation = op },
                                label = { Text(op) }
                            )
                        }
                    }
                    
                    // Mode selection (for encrypt/decrypt)
                    if (selectedOperation in listOf("Encrypt", "Decrypt")) {
                        Text("Mode", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AesMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = selectedMode == mode,
                                    onClick = { selectedMode = mode },
                                    label = { Text(mode.displayName) }
                                )
                            }
                        }
                    }
                    
                    Divider()
                    
                    // Key input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = key,
                            onValueChange = { key = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                            label = { Text("Key (hex)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                val keySize = AesCalculator.validateKeyLength(key)
                                Text(
                                    if (keySize != null) "${keySize.bits}-bit key" 
                                    else "Need 16/24/32 bytes (${key.length/2} bytes)"
                                )
                            }
                        )
                        
                        Column {
                            AesKeySize.entries.forEach { size ->
                                TextButton(
                                    onClick = {
                                        selectedKeySize = size
                                        key = AesCalculator.generateKey(size)
                                    }
                                ) {
                                    Text("Gen ${size.bits}")
                                }
                            }
                        }
                    }
                    
                    // IV input (for modes that need it)
                    if (selectedMode != AesMode.ECB && selectedOperation in listOf("Encrypt", "Decrypt", "CBC-MAC")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = iv,
                                onValueChange = { iv = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(32) },
                                label = { Text("IV (hex, 16 bytes)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { iv = AesCalculator.generateIv() }) {
                                Icon(Icons.Default.Refresh, "Generate IV")
                            }
                        }
                    }
                    
                    // Data input
                    if (selectedOperation != "KCV") {
                        OutlinedTextField(
                            value = data,
                            onValueChange = { data = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                            label = { Text("Data (hex)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            supportingText = { Text("${data.length / 2} bytes") }
                        )
                    }
                    
                    // Calculate button
                    Button(
                        onClick = {
                            error = ""
                            result = try {
                                when (selectedOperation) {
                                    "Encrypt" -> AesCalculator.encrypt(key, data, selectedMode, iv)
                                    "Decrypt" -> AesCalculator.decrypt(key, data, selectedMode, iv)
                                    "CMAC" -> AesCalculator.calculateCmac(key, data)
                                    "CBC-MAC" -> AesCalculator.calculateCbcMac(key, data, iv)
                                    "KCV" -> AesCalculator.calculateKcv(key)
                                    else -> ""
                                }
                            } catch (e: Exception) {
                                error = e.message ?: "Error"
                                ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = key.isNotEmpty() && (selectedOperation == "KCV" || data.isNotEmpty())
                    ) {
                        Icon(Icons.Default.Calculate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Calculate")
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
            SectionHeader("Result")
            
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
            
            if (result.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            when (selectedOperation) {
                                "Encrypt" -> "Ciphertext"
                                "Decrypt" -> "Plaintext"
                                "CMAC" -> "CMAC"
                                "CBC-MAC" -> "CBC-MAC"
                                "KCV" -> "Key Check Value"
                                else -> "Result"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        SelectionContainer {
                            Text(
                                result.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            "${result.length / 2} bytes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // Reference card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("AES Reference", style = MaterialTheme.typography.titleSmall)
                    Divider()
                    Text("• AES-128: 16-byte key (32 hex chars)", style = MaterialTheme.typography.bodySmall)
                    Text("• AES-192: 24-byte key (48 hex chars)", style = MaterialTheme.typography.bodySmall)
                    Text("• AES-256: 32-byte key (64 hex chars)", style = MaterialTheme.typography.bodySmall)
                    Text("• Block size: 16 bytes (128 bits)", style = MaterialTheme.typography.bodySmall)
                    Text("• IV: 16 bytes for CBC/CFB/OFB/CTR", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

