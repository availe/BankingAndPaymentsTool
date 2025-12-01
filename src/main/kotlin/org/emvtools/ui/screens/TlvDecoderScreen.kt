package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.emvtools.model.TlvData
import org.emvtools.model.TlvParseResult
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils
import org.emvtools.util.TlvParser

@Composable
fun TlvDecoderScreen() {
    var inputHex by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<TlvParseResult?>(null) }
    var buildTag by remember { mutableStateOf("") }
    var buildValue by remember { mutableStateOf("") }
    var builtTlv by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "TLV Decoder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Decoder
            Column(
                modifier = Modifier.weight(1f)
            ) {
                ToolCard {
                    SectionHeader("Decode TLV Data")
                    
                    HexInputField(
                        value = inputHex,
                        onValueChange = { inputHex = it },
                        label = "TLV Hex Data",
                        minLines = 4,
                        maxLines = 6
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Decode",
                            onClick = { parseResult = TlvParser.parse(inputHex) },
                            enabled = inputHex.isNotBlank()
                        )
                        OutlinedButton(onClick = { 
                            inputHex = ""
                            parseResult = null
                        }) {
                            Text("Clear")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Results
                when (val result = parseResult) {
                    is TlvParseResult.Success -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(result.tlvList) { tlv ->
                                TlvCard(tlv, 0)
                            }
                        }
                    }
                    is TlvParseResult.Error -> {
                        ResultDisplay(
                            title = "Error",
                            content = result.message + (result.position?.let { " at position $it" } ?: ""),
                            isError = true
                        )
                    }
                    null -> {}
                }
            }
            
            // Right panel - Builder
            Column(
                modifier = Modifier.weight(1f)
            ) {
                ToolCard {
                    SectionHeader("Build TLV")
                    
                    HexInputField(
                        value = buildTag,
                        onValueChange = { buildTag = it },
                        label = "Tag (e.g., 9F26)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = buildValue,
                        onValueChange = { buildValue = it },
                        label = "Value (hex)",
                        minLines = 2,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Build TLV",
                        onClick = {
                            builtTlv = TlvParser.buildTlv(buildTag, buildValue)
                        },
                        enabled = buildTag.isNotBlank()
                    )
                    
                    if (builtTlv.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(
                            title = "Built TLV",
                            content = HexUtils.formatHex(builtTlv)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sample data
                ToolCard {
                    SectionHeader("Sample TLV Data")
                    
                    Text(
                        text = "Click to load sample data:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val samples = listOf(
                        "FCI Template" to "6F1A840E315041592E5359532E4444463031A5088801025F2D02656E",
                        "EMV Record" to "70819F9F2701809F100706010A03A4B8009F3704000000009F3602001C9F2608A1B2C3D4E5F6A7B8",
                        "Simple Tags" to "9F2608AABBCCDD112233449F2701809F3602001C"
                    )
                    
                    samples.forEach { (name, data) ->
                        TextButton(
                            onClick = { inputHex = data }
                        ) {
                            Text(name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TlvCard(tlv: TlvData, indent: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 16).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (tlv.isConstructed) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text(
                        text = tlv.tag,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tlv.tagInfo?.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "${tlv.length} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (tlv.isConstructed) {
                Text(
                    text = "[Constructed]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                tlv.children.forEach { child ->
                    TlvCard(child, indent + 1)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = HexUtils.formatHex(tlv.value),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

