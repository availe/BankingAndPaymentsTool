package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.model.CharEncoding
import org.emvtools.ui.components.*
import org.emvtools.util.CharConverter
import org.emvtools.util.HexUtils

@Composable
fun CharConverterScreen() {
    var inputText by remember { mutableStateOf("") }
    var fromEncoding by remember { mutableStateOf(CharEncoding.ASCII) }
    var toEncoding by remember { mutableStateOf(CharEncoding.HEX) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Number conversion
    var decimalInput by remember { mutableStateOf("") }
    var hexInput by remember { mutableStateOf("") }
    var binaryInput by remember { mutableStateOf("") }
    
    // EBCDIC
    var asciiForEbcdic by remember { mutableStateOf("") }
    var ebcdicResult by remember { mutableStateOf<String?>(null) }
    var ebcdicHexInput by remember { mutableStateOf("") }
    var asciiFromEbcdic by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Character Converter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Main converter
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Encoding Converter")
                    
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Input") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownSelector(
                            label = "From",
                            options = CharEncoding.entries,
                            selectedOption = fromEncoding,
                            onOptionSelected = { fromEncoding = it },
                            optionLabel = { it.name },
                            modifier = Modifier.weight(1f)
                        )
                        
                        DropdownSelector(
                            label = "To",
                            options = CharEncoding.entries,
                            selectedOption = toEncoding,
                            onOptionSelected = { toEncoding = it },
                            optionLabel = { it.name },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Convert",
                        onClick = {
                            error = null
                            CharConverter.convert(inputText, fromEncoding, toEncoding)
                                .onSuccess { result = it }
                                .onFailure { error = it.message }
                        },
                        enabled = inputText.isNotBlank()
                    )
                    
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
                
                // Number conversion
                ToolCard {
                    SectionHeader("Number Conversion")
                    
                    OutlinedTextField(
                        value = decimalInput,
                        onValueChange = { 
                            decimalInput = it.filter { c -> c.isDigit() }
                            if (decimalInput.isNotBlank()) {
                                CharConverter.decimalToHex(decimalInput)
                                    .onSuccess { hexInput = it }
                                hexInput.toLongOrNull(16)?.let { value ->
                                    binaryInput = value.toString(2)
                                }
                            }
                        },
                        label = { Text("Decimal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { 
                            hexInput = it.uppercase().filter { c -> c in '0'..'9' || c in 'A'..'F' }
                            if (hexInput.isNotBlank()) {
                                CharConverter.hexToDecimal(hexInput)
                                    .onSuccess { decimalInput = it }
                                hexInput.toLongOrNull(16)?.let { value ->
                                    binaryInput = value.toString(2)
                                }
                            }
                        },
                        label = { Text("Hexadecimal") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = binaryInput,
                        onValueChange = { 
                            binaryInput = it.filter { c -> c == '0' || c == '1' }
                            if (binaryInput.isNotBlank()) {
                                binaryInput.toLongOrNull(2)?.let { value ->
                                    decimalInput = value.toString()
                                    hexInput = value.toString(16).uppercase()
                                }
                            }
                        },
                        label = { Text("Binary") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            // Right panel - EBCDIC and utilities
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("ASCII ↔ EBCDIC")
                    
                    OutlinedTextField(
                        value = asciiForEbcdic,
                        onValueChange = { asciiForEbcdic = it },
                        label = { Text("ASCII Text") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Convert to EBCDIC Hex",
                        onClick = {
                            ebcdicResult = CharConverter.asciiToEbcdicHex(asciiForEbcdic)
                        },
                        enabled = asciiForEbcdic.isNotBlank()
                    )
                    
                    if (ebcdicResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "EBCDIC Hex", content = HexUtils.formatHex(ebcdicResult!!))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    HexInputField(
                        value = ebcdicHexInput,
                        onValueChange = { ebcdicHexInput = it },
                        label = "EBCDIC Hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Convert to ASCII",
                        onClick = {
                            asciiFromEbcdic = CharConverter.ebcdicHexToAscii(ebcdicHexInput)
                        },
                        enabled = ebcdicHexInput.isNotBlank()
                    )
                    
                    if (asciiFromEbcdic != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "ASCII", content = asciiFromEbcdic!!)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick conversions
                ToolCard {
                    SectionHeader("Quick Conversions")
                    
                    var quickHex by remember { mutableStateOf("") }
                    var quickAscii by remember { mutableStateOf("") }
                    var quickBinary by remember { mutableStateOf("") }
                    
                    HexInputField(
                        value = quickHex,
                        onValueChange = { 
                            quickHex = it
                            if (it.isNotBlank()) {
                                quickAscii = CharConverter.hexToPrintableAscii(it)
                                quickBinary = CharConverter.hexToBinary(it)
                            }
                        },
                        label = "Hex Input",
                        singleLine = true
                    )
                    
                    if (quickAscii.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        KeyValueRow(key = "ASCII", value = quickAscii)
                        KeyValueRow(key = "Binary", value = quickBinary.chunked(8).joinToString(" "))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info
                ToolCard {
                    SectionHeader("Information")
                    
                    Text(
                        text = """
                            Supported encodings:
                            • ASCII - 7-bit character encoding
                            • EBCDIC - IBM mainframe encoding
                            • HEX - Hexadecimal representation
                            
                            EBCDIC is commonly used in:
                            • IBM mainframes
                            • Banking systems
                            • Legacy payment networks
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

