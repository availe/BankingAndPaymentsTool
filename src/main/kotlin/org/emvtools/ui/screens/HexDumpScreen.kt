package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.ui.components.*
import org.emvtools.util.HexDump

@Composable
fun HexDumpScreen() {
    var inputHex by remember { mutableStateOf("") }
    var bytesPerLine by remember { mutableStateOf(16) }
    var showAscii by remember { mutableStateOf(true) }
    var showOffset by remember { mutableStateOf(true) }
    var uppercase by remember { mutableStateOf(true) }
    var dumpResult by remember { mutableStateOf("") }
    var analysisResult by remember { mutableStateOf<String?>(null) }
    
    // Compare
    var hex1 by remember { mutableStateOf("") }
    var hex2 by remember { mutableStateOf("") }
    var compareResult by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Hex Dump Viewer",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Hex Dump
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Hex Dump")
                    
                    HexInputField(
                        value = inputHex,
                        onValueChange = { inputHex = it },
                        label = "Hex Data",
                        minLines = 4,
                        maxLines = 6
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row {
                            Checkbox(
                                checked = showOffset,
                                onCheckedChange = { showOffset = it }
                            )
                            Text("Show Offset", modifier = Modifier.padding(top = 12.dp))
                        }
                        
                        Row {
                            Checkbox(
                                checked = showAscii,
                                onCheckedChange = { showAscii = it }
                            )
                            Text("Show ASCII", modifier = Modifier.padding(top = 12.dp))
                        }
                        
                        Row {
                            Checkbox(
                                checked = uppercase,
                                onCheckedChange = { uppercase = it }
                            )
                            Text("Uppercase", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DropdownSelector(
                        label = "Bytes per line",
                        options = listOf(8, 16, 32),
                        selectedOption = bytesPerLine,
                        onOptionSelected = { bytesPerLine = it },
                        optionLabel = { "$it" }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Generate Dump",
                            onClick = {
                                dumpResult = HexDump.dump(
                                    inputHex,
                                    bytesPerLine,
                                    showAscii,
                                    showOffset,
                                    uppercase
                                )
                            },
                            enabled = inputHex.isNotBlank()
                        )
                        
                        OutlinedButton(
                            onClick = {
                                val analysis = HexDump.analyze(inputHex)
                                analysisResult = buildString {
                                    appendLine("Total bytes: ${analysis.totalBytes}")
                                    appendLine("Unique bytes: ${analysis.uniqueBytes}")
                                    appendLine("Entropy: %.2f bits".format(analysis.entropy))
                                    appendLine("Printable ASCII: %.1f%%".format(analysis.printableAsciiPercent))
                                    appendLine()
                                    appendLine("Most common bytes:")
                                    analysis.mostCommonBytes.take(5).forEach { (byte, count) ->
                                        appendLine("  %02X: %d occurrences".format(byte, count))
                                    }
                                    if (analysis.patterns.isNotEmpty()) {
                                        appendLine()
                                        appendLine("Patterns detected:")
                                        analysis.patterns.forEach { appendLine("  • $it") }
                                    }
                                }
                            },
                            enabled = inputHex.isNotBlank()
                        ) {
                            Text("Analyze")
                        }
                    }
                    
                    if (dumpResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBlock(code = dumpResult)
                    }
                    
                    if (analysisResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Analysis", content = analysisResult!!)
                    }
                }
            }
            
            // Right panel - Compare
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Compare Hex Data")
                    
                    HexInputField(
                        value = hex1,
                        onValueChange = { hex1 = it },
                        label = "First Hex Data",
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = hex2,
                        onValueChange = { hex2 = it },
                        label = "Second Hex Data",
                        minLines = 3,
                        maxLines = 5
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Compare",
                        onClick = {
                            compareResult = HexDump.compare(hex1, hex2)
                        },
                        enabled = hex1.isNotBlank() && hex2.isNotBlank()
                    )
                    
                    if (compareResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CodeBlock(code = compareResult!!)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick format
                ToolCard {
                    SectionHeader("Quick Format")
                    
                    var quickInput by remember { mutableStateOf("") }
                    var quickOutput by remember { mutableStateOf("") }
                    
                    HexInputField(
                        value = quickInput,
                        onValueChange = { quickInput = it },
                        label = "Input Hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                quickOutput = HexDump.dumpCompact(quickInput, 2)
                            },
                            enabled = quickInput.isNotBlank()
                        ) {
                            Text("Space (2)")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                quickOutput = HexDump.dumpCompact(quickInput, 4)
                            },
                            enabled = quickInput.isNotBlank()
                        ) {
                            Text("Space (4)")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                quickOutput = quickInput.replace(" ", "").uppercase()
                            },
                            enabled = quickInput.isNotBlank()
                        ) {
                            Text("Remove Spaces")
                        }
                    }
                    
                    if (quickOutput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Formatted", content = quickOutput)
                    }
                }
            }
        }
    }
}

