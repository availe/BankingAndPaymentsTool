package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.crypto.DesCalculator
import org.emvtools.model.DesKeyType
import org.emvtools.model.DesMode
import org.emvtools.model.DesOperation
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils

@Composable
fun DesCalculatorScreen() {
    var data by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var iv by remember { mutableStateOf("0000000000000000") }
    var operation by remember { mutableStateOf(DesOperation.ENCRYPT) }
    var mode by remember { mutableStateOf(DesMode.ECB) }
    var keyType by remember { mutableStateOf(DesKeyType.TRIPLE_DES_2KEY) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var kcvResult by remember { mutableStateOf<String?>(null) }
    
    // MAC calculation
    var macData by remember { mutableStateOf("") }
    var macKey by remember { mutableStateOf("") }
    var macResult by remember { mutableStateOf<String?>(null) }
    var macError by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DES/3DES Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Encrypt/Decrypt
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Encrypt / Decrypt")
                    
                    HexInputField(
                        value = data,
                        onValueChange = { data = it },
                        label = "Data (hex, multiple of 8 bytes)",
                        minLines = 2
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = key,
                        onValueChange = { key = it },
                        label = "Key (hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownSelector(
                            label = "Key Type",
                            options = DesKeyType.entries,
                            selectedOption = keyType,
                            onOptionSelected = { keyType = it },
                            optionLabel = { 
                                when (it) {
                                    DesKeyType.SINGLE_DES -> "Single DES (8 bytes)"
                                    DesKeyType.TRIPLE_DES_2KEY -> "3DES 2-Key (16 bytes)"
                                    DesKeyType.TRIPLE_DES_3KEY -> "3DES 3-Key (24 bytes)"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        
                        DropdownSelector(
                            label = "Mode",
                            options = DesMode.entries,
                            selectedOption = mode,
                            onOptionSelected = { mode = it },
                            optionLabel = { it.name },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (mode == DesMode.CBC) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HexInputField(
                            value = iv,
                            onValueChange = { iv = it },
                            label = "IV (8 bytes hex)",
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Encrypt",
                            onClick = {
                                error = null
                                DesCalculator.calculate(data, key, DesOperation.ENCRYPT, mode, keyType, iv)
                                    .onSuccess { result = HexUtils.formatHex(it) }
                                    .onFailure { error = it.message }
                            },
                            enabled = data.isNotBlank() && key.isNotBlank()
                        )
                        
                        OutlinedButton(
                            onClick = {
                                error = null
                                DesCalculator.calculate(data, key, DesOperation.DECRYPT, mode, keyType, iv)
                                    .onSuccess { result = HexUtils.formatHex(it) }
                                    .onFailure { error = it.message }
                            },
                            enabled = data.isNotBlank() && key.isNotBlank()
                        ) {
                            Text("Decrypt")
                        }
                    }
                    
                    if (result != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Result", content = result!!)
                    }
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = error!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // KCV Calculator
                ToolCard {
                    SectionHeader("Key Check Value (KCV)")
                    
                    Text(
                        text = "Calculate KCV by encrypting zeros with the key",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Calculate KCV",
                        onClick = {
                            DesCalculator.calculateKcv(key, keyType)
                                .onSuccess { kcvResult = it }
                                .onFailure { kcvResult = "Error: ${it.message}" }
                        },
                        enabled = key.isNotBlank()
                    )
                    
                    if (kcvResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "KCV", content = kcvResult!!)
                    }
                }
            }
            
            // Right panel - MAC Calculator
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("MAC Calculator")
                    
                    HexInputField(
                        value = macData,
                        onValueChange = { macData = it },
                        label = "Data (hex)",
                        minLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = macKey,
                        onValueChange = { macKey = it },
                        label = "Key (16 bytes hex for Retail MAC)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Calculate MAC",
                            onClick = {
                                macError = null
                                DesCalculator.calculateMac(macData, macKey, DesKeyType.TRIPLE_DES_2KEY)
                                    .onSuccess { macResult = HexUtils.formatHex(it) }
                                    .onFailure { macError = it.message }
                            },
                            enabled = macData.isNotBlank() && macKey.isNotBlank()
                        )
                        
                        OutlinedButton(
                            onClick = {
                                macError = null
                                DesCalculator.calculateRetailMac(macData, macKey)
                                    .onSuccess { macResult = HexUtils.formatHex(it) }
                                    .onFailure { macError = it.message }
                            },
                            enabled = macData.isNotBlank() && macKey.isNotBlank()
                        ) {
                            Text("Retail MAC")
                        }
                    }
                    
                    if (macResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "MAC", content = macResult!!)
                    }
                    
                    if (macError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = macError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info card
                ToolCard {
                    SectionHeader("Information")
                    
                    Text(
                        text = """
                            • Single DES: 8-byte key (64 bits, 56 effective)
                            • 3DES 2-Key: 16-byte key (K1-K2-K1)
                            • 3DES 3-Key: 24-byte key (K1-K2-K3)
                            • ECB: Electronic Codebook (no IV)
                            • CBC: Cipher Block Chaining (requires IV)
                            • Data must be multiple of 8 bytes
                            • Retail MAC: ISO 9797-1 Algorithm 3
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

