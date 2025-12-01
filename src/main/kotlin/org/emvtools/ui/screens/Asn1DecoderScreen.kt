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
import org.emvtools.ui.components.*
import org.emvtools.util.Asn1Decoder
import org.emvtools.util.Asn1Node
import org.emvtools.util.Asn1TagClass

@Composable
fun Asn1DecoderScreen() {
    var inputHex by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<Asn1Node?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var formattedOutput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "ASN.1 Decoder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Input
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Input")
                    
                    HexInputField(
                        value = inputHex,
                        onValueChange = { inputHex = it },
                        label = "ASN.1 DER Encoded Data (hex)",
                        minLines = 6,
                        maxLines = 10
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Decode",
                            onClick = {
                                error = null
                                Asn1Decoder.decode(inputHex)
                                    .onSuccess { 
                                        result = it
                                        formattedOutput = Asn1Decoder.format(it)
                                    }
                                    .onFailure { error = it.message }
                            },
                            enabled = inputHex.isNotBlank()
                        )
                        
                        OutlinedButton(onClick = {
                            inputHex = ""
                            result = null
                            error = null
                            formattedOutput = ""
                        }) {
                            Text("Clear")
                        }
                    }
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = error!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sample data
                ToolCard {
                    SectionHeader("Sample ASN.1 Data")
                    
                    val samples = listOf(
                        "X.509 Certificate (partial)" to "30820122300D06092A864886F70D01010B0500",
                        "RSA Public Key" to "30819F300D06092A864886F70D010101050003818D0030818902818100",
                        "Simple Sequence" to "3009020103020104020105",
                        "OID Example" to "06092A864886F70D010101"
                    )
                    
                    samples.forEach { (name, data) ->
                        TextButton(onClick = { inputHex = data }) {
                            Text(name)
                        }
                    }
                }
            }
            
            // Right panel - Output
            Column(modifier = Modifier.weight(1f)) {
                if (result != null) {
                    ToolCard {
                        SectionHeader("Decoded Structure")
                        
                        LazyColumn {
                            item {
                                Asn1NodeCard(result!!, 0)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ToolCard {
                        SectionHeader("Text Output")
                        CodeBlock(code = formattedOutput)
                    }
                } else {
                    ToolCard {
                        SectionHeader("Information")
                        
                        Text(
                            text = """
                                ASN.1 (Abstract Syntax Notation One) is a standard interface description language for defining data structures.
                                
                                DER (Distinguished Encoding Rules) is a binary encoding format for ASN.1.
                                
                                Common ASN.1 types:
                                • SEQUENCE (0x30) - Ordered collection
                                • SET (0x31) - Unordered collection
                                • INTEGER (0x02) - Whole number
                                • BIT STRING (0x03) - Bit sequence
                                • OCTET STRING (0x04) - Byte sequence
                                • NULL (0x05) - Empty value
                                • OBJECT IDENTIFIER (0x06) - OID
                                • UTF8String (0x0C) - UTF-8 text
                                • PrintableString (0x13) - ASCII text
                                • UTCTime (0x17) - Date/time
                                
                                Tag classes:
                                • Universal (00) - Standard types
                                • Application (01) - Application-specific
                                • Context-specific (10) - Context-dependent
                                • Private (11) - Private use
                            """.trimIndent(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Asn1NodeCard(node: Asn1Node, indent: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indent * 12).dp, top = 4.dp, bottom = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (node.isConstructed)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text(
                        text = node.tagHex,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.tagType,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "${node.length} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (node.tagClass != Asn1TagClass.UNIVERSAL) {
                Text(
                    text = "[${node.tagClass}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            if (node.isConstructed) {
                Spacer(modifier = Modifier.height(4.dp))
                node.children.forEach { child ->
                    Asn1NodeCard(child, indent + 1)
                }
            } else if (node.decodedValue != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.decodedValue,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = node.value,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

