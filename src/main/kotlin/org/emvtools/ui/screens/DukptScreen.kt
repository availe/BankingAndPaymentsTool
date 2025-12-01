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
import org.emvtools.crypto.DukptCalculator
import org.emvtools.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DukptScreen() {
    var bdk by remember { mutableStateOf("0123456789ABCDEFFEDCBA9876543210") }
    var ksn by remember { mutableStateOf("FFFF9876543210E00001") }
    var pinBlock by remember { mutableStateOf("") }
    
    var keySet by remember { mutableStateOf<DukptCalculator.DukptKeySet?>(null) }
    var encryptedPin by remember { mutableStateOf("") }
    var decryptedPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Input
        Column(
            modifier = Modifier.weight(0.45f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("DUKPT Key Derivation")
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = bdk,
                        onValueChange = { bdk = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(32) },
                        label = { Text("BDK (Base Derivation Key)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("16 bytes (32 hex chars)") }
                    )
                    
                    OutlinedTextField(
                        value = ksn,
                        onValueChange = { ksn = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(20) },
                        label = { Text("KSN (Key Serial Number)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("10 bytes (20 hex chars)") },
                        trailingIcon = {
                            IconButton(onClick = {
                                if (ksn.length == 20) {
                                    ksn = DukptCalculator.incrementKsn(ksn)
                                }
                            }) {
                                Icon(Icons.Default.Add, "Increment KSN")
                            }
                        }
                    )
                    
                    Button(
                        onClick = {
                            error = ""
                            try {
                                keySet = DukptCalculator.getKeySet(bdk, ksn)
                            } catch (e: Exception) {
                                error = e.message ?: "Error deriving keys"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = bdk.length == 32 && ksn.length == 20
                    ) {
                        Icon(Icons.Default.Key, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Derive Keys")
                    }
                }
            }
            
            SectionHeader("PIN Block Operations")
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pinBlock,
                        onValueChange = { pinBlock = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(16) },
                        label = { Text("PIN Block (hex)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("8 bytes (16 hex chars)") }
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                error = ""
                                try {
                                    encryptedPin = DukptCalculator.encryptPinBlock(pinBlock, bdk, ksn)
                                    decryptedPin = ""
                                } catch (e: Exception) {
                                    error = e.message ?: "Error encrypting"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = pinBlock.length == 16 && bdk.length == 32 && ksn.length == 20
                        ) {
                            Text("Encrypt")
                        }
                        
                        Button(
                            onClick = {
                                error = ""
                                try {
                                    decryptedPin = DukptCalculator.decryptPinBlock(pinBlock, bdk, ksn)
                                    encryptedPin = ""
                                } catch (e: Exception) {
                                    error = e.message ?: "Error decrypting"
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = pinBlock.length == 16 && bdk.length == 32 && ksn.length == 20
                        ) {
                            Text("Decrypt")
                        }
                    }
                    
                    if (encryptedPin.isNotEmpty()) {
                        KeyValueRow("Encrypted PIN Block", encryptedPin)
                    }
                    if (decryptedPin.isNotEmpty()) {
                        KeyValueRow("Decrypted PIN Block", decryptedPin)
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
        
        // Right panel - Results
        Column(
            modifier = Modifier.weight(0.55f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Derived Keys")
            
            if (keySet != null) {
                // KSN Analysis
                val (iksn, deviceId, counter) = DukptCalculator.parseKsn(keySet!!.ksn)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("KSN Analysis", style = MaterialTheme.typography.titleSmall)
                        Divider()
                        KeyValueRow("Initial KSN", iksn)
                        KeyValueRow("Device ID", deviceId)
                        KeyValueRow("Transaction Counter", "$counter (0x${counter.toString(16).uppercase()})")
                    }
                }
                
                // Derived Keys
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("IPEK (Initial PIN Encryption Key)", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(
                                keySet!!.ipek.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current Transaction Key", style = MaterialTheme.typography.titleSmall)
                        SelectionContainer {
                            Text(
                                keySet!!.currentKey.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Variant Keys", style = MaterialTheme.typography.titleSmall)
                        Divider()
                        
                        Text("PIN Encryption Key:", style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                keySet!!.pinEncryptionKey.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text("MAC Key:", style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                keySet!!.macKey.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        
                        Text("Data Encryption Key:", style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                keySet!!.dataEncryptionKey.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.VpnKey,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Enter BDK and KSN to derive keys",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

