package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.model.EmvTransactionData
import org.emvtools.ui.components.*
import org.emvtools.util.EmvSimulator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvSimulatorScreen() {
    var amount by remember { mutableStateOf("1000") }
    var transactionType by remember { mutableStateOf("00") }
    var terminalCountry by remember { mutableStateOf("0840") }
    var currency by remember { mutableStateOf("0840") }
    var terminalCapabilities by remember { mutableStateOf("E0F8C8") }
    var forceOnline by remember { mutableStateOf(false) }
    
    // Card profile
    var pan by remember { mutableStateOf("4761739001010010") }
    var panSequence by remember { mutableStateOf("00") }
    var aid by remember { mutableStateOf("A0000000031010") }
    var iacDefault by remember { mutableStateOf("DC4000A800") }
    var iacDenial by remember { mutableStateOf("0010000000") }
    var iacOnline by remember { mutableStateOf("DC4004F800") }
    
    var result by remember { mutableStateOf<EmvSimulator.TransactionResult?>(null) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Configuration
        LazyColumn(
            modifier = Modifier.weight(0.4f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader("Terminal Parameters")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter { c -> c.isDigit() } },
                            label = { Text("Amount (cents)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = transactionType,
                                onValueChange = { transactionType = it.uppercase().take(2) },
                                label = { Text("Type (9C)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = currency,
                                onValueChange = { currency = it.take(4) },
                                label = { Text("Currency") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = terminalCountry,
                                onValueChange = { terminalCountry = it.take(4) },
                                label = { Text("Country") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = terminalCapabilities,
                                onValueChange = { terminalCapabilities = it.uppercase().take(6) },
                                label = { Text("Capabilities") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = forceOnline, onCheckedChange = { forceOnline = it })
                            Text("Force Online")
                        }
                    }
                }
            }
            
            item {
                SectionHeader("Card Profile")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pan,
                            onValueChange = { pan = it.filter { c -> c.isDigit() }.take(19) },
                            label = { Text("PAN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = panSequence,
                                onValueChange = { panSequence = it.take(2) },
                                label = { Text("PSN") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = aid,
                                onValueChange = { aid = it.uppercase() },
                                label = { Text("AID") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                        }
                        
                        Text("Issuer Action Codes", style = MaterialTheme.typography.labelMedium)
                        
                        OutlinedTextField(
                            value = iacDefault,
                            onValueChange = { iacDefault = it.uppercase().take(10) },
                            label = { Text("IAC-Default") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = iacDenial,
                            onValueChange = { iacDenial = it.uppercase().take(10) },
                            label = { Text("IAC-Denial") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = iacOnline,
                            onValueChange = { iacOnline = it.uppercase().take(10) },
                            label = { Text("IAC-Online") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            item {
                Button(
                    onClick = {
                        val transactionData = EmvTransactionData(
                            amount = amount.padStart(12, '0'),
                            transactionType = transactionType,
                            terminalCountry = terminalCountry,
                            transactionCurrency = currency,
                            terminalCapabilities = terminalCapabilities
                        )
                        val cardProfile = EmvSimulator.CardProfile(
                            pan = pan,
                            panSequence = panSequence,
                            aid = aid,
                            iacDefault = iacDefault,
                            iacDenial = iacDenial,
                            iacOnline = iacOnline
                        )
                        result = EmvSimulator.simulate(transactionData, cardProfile, forceOnline)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Run Simulation")
                }
            }
        }
        
        VerticalDivider()
        
        // Right panel - Results
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight().padding(16.dp)
        ) {
            Text("Simulation Results", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (result != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val decisionColor = when (result!!.decision) {
                            EmvSimulator.TransactionDecision.APPROVED_OFFLINE -> MaterialTheme.colorScheme.primary
                            EmvSimulator.TransactionDecision.DECLINED_OFFLINE -> MaterialTheme.colorScheme.error
                            EmvSimulator.TransactionDecision.GO_ONLINE -> MaterialTheme.colorScheme.tertiary
                            EmvSimulator.TransactionDecision.REFERRAL -> MaterialTheme.colorScheme.secondary
                        }
                        
                        Text(
                            "Decision: ${result!!.decision.name}",
                            style = MaterialTheme.typography.titleLarge,
                            color = decisionColor
                        )
                        
                        Divider()
                        
                        KeyValueRow("Cryptogram Type", result!!.cryptogramType)
                        KeyValueRow("Cryptogram", result!!.cryptogram)
                        KeyValueRow("TVR", result!!.tvr)
                        KeyValueRow("CVM Result", result!!.cvmResult)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Transaction Log", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(result!!.logs) { log ->
                            Text(
                                log,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    log.startsWith("===") -> MaterialTheme.colorScheme.primary
                                    log.startsWith("---") -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
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
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Text("Configure parameters and run simulation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

