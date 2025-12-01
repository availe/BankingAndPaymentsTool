package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.ui.components.*
import org.emvtools.util.QrCodeParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrToolsScreen() {
    var mode by remember { mutableStateOf("Parse") }
    var qrInput by remember { mutableStateOf("") }
    var parsedQr by remember { mutableStateOf<QrCodeParser.ParsedQrCode?>(null) }
    var error by remember { mutableStateOf("") }
    
    // Generator fields
    var merchantName by remember { mutableStateOf("Test Merchant") }
    var merchantCity by remember { mutableStateOf("New York") }
    var countryCode by remember { mutableStateOf("US") }
    var currencyCode by remember { mutableStateOf("840") }
    var amount by remember { mutableStateOf("") }
    var merchantCategory by remember { mutableStateOf("5411") }
    var generatedQr by remember { mutableStateOf("") }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Input
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("QR Code Tools")
            
            // Mode selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "Parse",
                    onClick = { mode = "Parse" },
                    label = { Text("Parse QR") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                FilterChip(
                    selected = mode == "Generate",
                    onClick = { mode = "Generate" },
                    label = { Text("Generate QR") },
                    leadingIcon = { Icon(Icons.Default.QrCode2, null) }
                )
            }
            
            if (mode == "Parse") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("QR Code Data", style = MaterialTheme.typography.titleSmall)
                        
                        OutlinedTextField(
                            value = qrInput,
                            onValueChange = { qrInput = it },
                            label = { Text("Paste QR code content") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            maxLines = 10,
                            placeholder = { Text("EMVCo MPM or UPI QR data...") }
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    error = ""
                                    try {
                                        parsedQr = QrCodeParser.parse(qrInput)
                                    } catch (e: Exception) {
                                        error = e.message ?: "Error parsing QR"
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = qrInput.isNotEmpty()
                            ) {
                                Text("Parse")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    // Sample EMVCo QR
                                    qrInput = "00020101021126490014com.example.app0111123456789020208Test12340520Test Reference5204541153038405802US5913Test Merchant6008New York62070503***6304A13E"
                                }
                            ) {
                                Text("Sample")
                            }
                        }
                    }
                }
            } else {
                // Generate mode
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("EMVCo MPM Generator", style = MaterialTheme.typography.titleSmall)
                        
                        OutlinedTextField(
                            value = merchantName,
                            onValueChange = { merchantName = it.take(25) },
                            label = { Text("Merchant Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = merchantCity,
                            onValueChange = { merchantCity = it.take(15) },
                            label = { Text("Merchant City") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = countryCode,
                                onValueChange = { countryCode = it.uppercase().take(2) },
                                label = { Text("Country") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currencyCode,
                                onValueChange = { currencyCode = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Currency") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = merchantCategory,
                                onValueChange = { merchantCategory = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text("MCC") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("Amount (optional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Button(
                            onClick = {
                                error = ""
                                try {
                                    generatedQr = QrCodeParser.generateEmvcoMpm(
                                        merchantName = merchantName,
                                        merchantCity = merchantCity,
                                        countryCode = countryCode,
                                        currencyCode = currencyCode,
                                        amount = amount.ifEmpty { null },
                                        merchantCategoryCode = merchantCategory
                                    )
                                    // Also parse it to show in results
                                    parsedQr = QrCodeParser.parse(generatedQr)
                                } catch (e: Exception) {
                                    error = e.message ?: "Error generating QR"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Generate QR Data")
                        }
                    }
                }
                
                if (generatedQr.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Generated QR Data", style = MaterialTheme.typography.titleSmall)
                            SelectionContainer {
                                Text(
                                    generatedQr,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("${generatedQr.length} characters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
        
        // Right panel - Parsed Result
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Parsed QR Code")
            
            if (parsedQr != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(parsedQr!!.format.displayName, style = MaterialTheme.typography.titleMedium)
                        }
                        
                        if (parsedQr!!.format == QrCodeParser.QrFormat.EMVCO_MPM) {
                            val crcValid = QrCodeParser.validateCrc(parsedQr!!.rawData)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("CRC: ", style = MaterialTheme.typography.bodySmall)
                                Icon(
                                    if (crcValid) Icons.Default.CheckCircle else Icons.Default.Error,
                                    null,
                                    tint = if (crcValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    if (crcValid) " Valid" else " Invalid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (crcValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(parsedQr!!.fields) { field ->
                        QrFieldCard(field, 0)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.QrCode2,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Parse or generate a QR code", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrFieldCard(field: QrCodeParser.QrField, depth: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp),
        colors = CardDefaults.cardColors(
            containerColor = if (depth == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "[${field.id}] ${field.name}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (field.subFields.isEmpty()) {
                SelectionContainer {
                    Text(
                        field.value,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                Spacer(Modifier.height(8.dp))
                field.subFields.forEach { subField ->
                    QrFieldCard(subField, depth + 1)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

