package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.crypto.KeyShareGenerator
import org.emvtools.model.DesKeyType
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils

@Composable
fun KeySharesScreen() {
    var keyType by remember { mutableStateOf(DesKeyType.TRIPLE_DES_2KEY) }
    var numComponents by remember { mutableStateOf(3) }
    var generatedKey by remember { mutableStateOf<String?>(null) }
    var generatedComponents by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var generatedKcv by remember { mutableStateOf<String?>(null) }
    var generateError by remember { mutableStateOf<String?>(null) }
    
    // Combine components
    var componentInputs by remember { mutableStateOf(listOf("", "", "")) }
    var combinedKey by remember { mutableStateOf<String?>(null) }
    var combinedKcv by remember { mutableStateOf<String?>(null) }
    var combineError by remember { mutableStateOf<String?>(null) }
    
    // KCV Calculator
    var kcvKey by remember { mutableStateOf("") }
    var kcvKeyType by remember { mutableStateOf(DesKeyType.TRIPLE_DES_2KEY) }
    var calculatedKcv by remember { mutableStateOf<String?>(null) }
    var kcvError by remember { mutableStateOf<String?>(null) }
    
    // Parity check
    var parityKey by remember { mutableStateOf("") }
    var parityResult by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Key Share Generator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left column - Generate
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Generate Key & Components")
                    
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
                            modifier = Modifier.weight(2f)
                        )
                        
                        DropdownSelector(
                            label = "Components",
                            options = listOf(2, 3, 4, 5),
                            selectedOption = numComponents,
                            onOptionSelected = { numComponents = it },
                            optionLabel = { "$it" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Generate Key",
                        onClick = {
                            generateError = null
                            KeyShareGenerator.generateKey(keyType)
                                .onSuccess { key ->
                                    generatedKey = HexUtils.formatHex(key)
                                    generatedKcv = KeyShareGenerator.calculateKeyKcv(key)

                                    KeyShareGenerator.splitKey(key, numComponents)
                                        .onSuccess { keyShare ->
                                            generatedComponents = keyShare.components.map { comp ->
                                                HexUtils.formatHex(comp.value) to comp.kcv
                                            }
                                        }
                                        .onFailure { generateError = it.message }
                                }
                                .onFailure { generateError = it.message }
                        }
                    )
                    
                    if (generatedKey != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        KeyValueRow(key = "Full Key", value = generatedKey!!)
                        KeyValueRow(key = "KCV", value = generatedKcv ?: "")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Key Components:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        
                        generatedComponents.forEachIndexed { index, (component, kcv) ->
                            KeyValueRow(key = "Component ${index + 1}", value = component)
                            KeyValueRow(key = "KCV ${index + 1}", value = kcv)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                    
                    if (generateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = generateError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // KCV Calculator
                ToolCard {
                    SectionHeader("KCV Calculator")
                    
                    HexInputField(
                        value = kcvKey,
                        onValueChange = { kcvKey = it },
                        label = "Key (hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DropdownSelector(
                        label = "Key Type",
                        options = DesKeyType.entries,
                        selectedOption = kcvKeyType,
                        onOptionSelected = { kcvKeyType = it },
                        optionLabel = { it.name.replace("_", " ") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Calculate KCV",
                        onClick = {
                            kcvError = null
                            try {
                                calculatedKcv = KeyShareGenerator.calculateKeyKcv(kcvKey)
                            } catch (e: Exception) {
                                kcvError = e.message
                            }
                        },
                        enabled = kcvKey.isNotBlank()
                    )
                    
                    if (calculatedKcv != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "KCV", content = calculatedKcv!!)
                    }
                    
                    if (kcvError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = kcvError!!, isError = true)
                    }
                }
            }
            
            // Right column - Combine
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Combine Key Components")
                    
                    componentInputs.forEachIndexed { index, value ->
                        HexInputField(
                            value = value,
                            onValueChange = { newValue ->
                                componentInputs = componentInputs.toMutableList().also { it[index] = newValue }
                            },
                            label = "Component ${index + 1}",
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                componentInputs = componentInputs + ""
                            }
                        ) {
                            Text("Add Component")
                        }
                        
                        if (componentInputs.size > 2) {
                            OutlinedButton(
                                onClick = {
                                    componentInputs = componentInputs.dropLast(1)
                                }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Combine Components",
                        onClick = {
                            combineError = null
                            val components = componentInputs.filter { it.isNotBlank() }
                            KeyShareGenerator.combineComponents(components)
                                .onSuccess { keyShare ->
                                    combinedKey = HexUtils.formatHex(keyShare.fullKey)
                                    combinedKcv = keyShare.fullKeyKcv
                                }
                                .onFailure { combineError = it.message }
                        },
                        enabled = componentInputs.count { it.isNotBlank() } >= 2
                    )
                    
                    if (combinedKey != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Combined Key", content = combinedKey!!)
                        KeyValueRow(key = "KCV", value = combinedKcv ?: "")
                    }
                    
                    if (combineError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = combineError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Parity Check
                ToolCard {
                    SectionHeader("Parity Check & Fix")
                    
                    HexInputField(
                        value = parityKey,
                        onValueChange = { parityKey = it },
                        label = "Key (hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Check Parity",
                            onClick = {
                                val isValid = KeyShareGenerator.checkParity(parityKey)
                                parityResult = if (isValid) "✓ Parity is correct" else "✗ Parity is incorrect"
                            },
                            enabled = parityKey.isNotBlank()
                        )
                        
                        OutlinedButton(
                            onClick = {
                                parityKey = KeyShareGenerator.fixParity(parityKey)
                                parityResult = "Parity fixed"
                            },
                            enabled = parityKey.isNotBlank()
                        ) {
                            Text("Fix Parity")
                        }
                    }
                    
                    if (parityResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parityResult!!,
                            color = if (parityResult!!.startsWith("✓")) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

