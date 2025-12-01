package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.crypto.PinBlockCalculator
import org.emvtools.model.PinBlockFormat
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils

@Composable
fun PinToolsScreen() {
    var pin by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var format by remember { mutableStateOf(PinBlockFormat.ISO_FORMAT_0) }
    var pinBlockResult by remember { mutableStateOf<String?>(null) }
    var pinBlockError by remember { mutableStateOf<String?>(null) }
    
    // Encryption
    var clearPinBlock by remember { mutableStateOf("") }
    var zpk by remember { mutableStateOf("") }
    var encryptedResult by remember { mutableStateOf<String?>(null) }
    var encryptError by remember { mutableStateOf<String?>(null) }
    
    // Decryption
    var encryptedPinBlock by remember { mutableStateOf("") }
    var decryptKey by remember { mutableStateOf("") }
    var decryptFormat by remember { mutableStateOf(PinBlockFormat.ISO_FORMAT_0) }
    var decryptPan by remember { mutableStateOf("") }
    var decryptedPin by remember { mutableStateOf<String?>(null) }
    var decryptError by remember { mutableStateOf<String?>(null) }
    
    // Translation
    var translatePinBlock by remember { mutableStateOf("") }
    var sourceKey by remember { mutableStateOf("") }
    var destKey by remember { mutableStateOf("") }
    var translatedResult by remember { mutableStateOf<String?>(null) }
    var translateError by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "PIN Block Tools",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left column
            Column(modifier = Modifier.weight(1f)) {
                // PIN Block Creation
                ToolCard {
                    SectionHeader("Create PIN Block")
                    
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12) },
                        label = { Text("PIN (4-12 digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = pan,
                        onValueChange = { pan = it.filter { c -> c.isDigit() }.take(19) },
                        label = { Text("PAN (for ISO-0, ISO-3)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DropdownSelector(
                        label = "PIN Block Format",
                        options = PinBlockFormat.entries,
                        selectedOption = format,
                        onOptionSelected = { format = it },
                        optionLabel = { it.name.replace("_", " ") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Create PIN Block",
                        onClick = {
                            pinBlockError = null
                            PinBlockCalculator.createPinBlock(pin, pan, format)
                                .onSuccess { pinBlockResult = HexUtils.formatHex(it) }
                                .onFailure { pinBlockError = it.message }
                        },
                        enabled = pin.length >= 4
                    )
                    
                    if (pinBlockResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Clear PIN Block", content = pinBlockResult!!)
                    }
                    
                    if (pinBlockError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = pinBlockError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // PIN Block Encryption
                ToolCard {
                    SectionHeader("Encrypt PIN Block")
                    
                    HexInputField(
                        value = clearPinBlock,
                        onValueChange = { clearPinBlock = it },
                        label = "Clear PIN Block (16 hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = zpk,
                        onValueChange = { zpk = it },
                        label = "Zone PIN Key (ZPK) - 32 hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Encrypt",
                        onClick = {
                            encryptError = null
                            PinBlockCalculator.encryptPinBlock(clearPinBlock, zpk)
                                .onSuccess { encryptedResult = HexUtils.formatHex(it) }
                                .onFailure { encryptError = it.message }
                        },
                        enabled = clearPinBlock.isNotBlank() && zpk.isNotBlank()
                    )
                    
                    if (encryptedResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Encrypted PIN Block", content = encryptedResult!!)
                    }
                    
                    if (encryptError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = encryptError!!, isError = true)
                    }
                }
            }
            
            // Right column
            Column(modifier = Modifier.weight(1f)) {
                // PIN Block Decryption
                ToolCard {
                    SectionHeader("Decrypt PIN Block")
                    
                    HexInputField(
                        value = encryptedPinBlock,
                        onValueChange = { encryptedPinBlock = it },
                        label = "Encrypted PIN Block (16 hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = decryptKey,
                        onValueChange = { decryptKey = it },
                        label = "Zone PIN Key (ZPK) - 32 hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = decryptPan,
                        onValueChange = { decryptPan = it.filter { c -> c.isDigit() }.take(19) },
                        label = { Text("PAN (for ISO-0, ISO-3)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DropdownSelector(
                        label = "PIN Block Format",
                        options = PinBlockFormat.entries,
                        selectedOption = decryptFormat,
                        onOptionSelected = { decryptFormat = it },
                        optionLabel = { it.name.replace("_", " ") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Decrypt & Extract PIN",
                        onClick = {
                            decryptError = null
                            PinBlockCalculator.decryptPinBlock(encryptedPinBlock, decryptPan, decryptFormat, decryptKey)
                                .onSuccess { decryptedPin = it }
                                .onFailure { decryptError = it.message }
                        },
                        enabled = encryptedPinBlock.isNotBlank() && decryptKey.isNotBlank()
                    )
                    
                    if (decryptedPin != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Decrypted PIN", content = decryptedPin!!)
                    }
                    
                    if (decryptError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = decryptError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // PIN Block Translation
                ToolCard {
                    SectionHeader("Translate PIN Block")
                    
                    HexInputField(
                        value = translatePinBlock,
                        onValueChange = { translatePinBlock = it },
                        label = "Encrypted PIN Block",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = sourceKey,
                        onValueChange = { sourceKey = it },
                        label = "Source Key (32 hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = destKey,
                        onValueChange = { destKey = it },
                        label = "Destination Key (32 hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Translate",
                        onClick = {
                            translateError = null
                            PinBlockCalculator.translatePinBlock(translatePinBlock, sourceKey, destKey)
                                .onSuccess { translatedResult = HexUtils.formatHex(it) }
                                .onFailure { translateError = it.message }
                        },
                        enabled = translatePinBlock.isNotBlank() && sourceKey.isNotBlank() && destKey.isNotBlank()
                    )
                    
                    if (translatedResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Translated PIN Block", content = translatedResult!!)
                    }
                    
                    if (translateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = translateError!!, isError = true)
                    }
                }
            }
        }
    }
}

