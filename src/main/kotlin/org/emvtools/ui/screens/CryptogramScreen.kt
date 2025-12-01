package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.emvtools.crypto.CryptogramCalculator
import org.emvtools.crypto.KeyDerivationMethod
import org.emvtools.model.CryptogramType
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils

@Composable
fun CryptogramScreen() {
    var pan by remember { mutableStateOf("") }
    var psn by remember { mutableStateOf("00") }
    var atc by remember { mutableStateOf("") }
    var imk by remember { mutableStateOf("") }
    var unpredictableNumber by remember { mutableStateOf("") }
    var transactionData by remember { mutableStateOf("") }
    var cryptogramType by remember { mutableStateOf(CryptogramType.ARQC) }
    var derivationMethod by remember { mutableStateOf(KeyDerivationMethod.EMV_COMMON) }
    
    var result by remember { mutableStateOf<Map<String, String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // ARPC calculation
    var arqcForArpc by remember { mutableStateOf("") }
    var arc by remember { mutableStateOf("3030") }
    var arpcResult by remember { mutableStateOf<String?>(null) }
    var arpcError by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Cryptogram Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - ARQC/TC/AAC
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("ARQC / TC / AAC Calculator")
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pan,
                            onValueChange = { pan = it.filter { c -> c.isDigit() }.take(19) },
                            label = { Text("PAN") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = psn,
                            onValueChange = { psn = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("PSN") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = imk,
                        onValueChange = { imk = it },
                        label = "Issuer Master Key (IMK) - 16 or 32 hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = atc,
                        onValueChange = { atc = it },
                        label = "Application Transaction Counter (ATC) - 4 hex",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = unpredictableNumber,
                        onValueChange = { unpredictableNumber = it },
                        label = "Unpredictable Number (UN) - 8 hex",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HexInputField(
                        value = transactionData,
                        onValueChange = { transactionData = it },
                        label = "Transaction Data (hex)",
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DropdownSelector(
                            label = "Cryptogram Type",
                            options = CryptogramType.entries.filter { it != CryptogramType.ARPC },
                            selectedOption = cryptogramType,
                            onOptionSelected = { cryptogramType = it },
                            optionLabel = { it.name },
                            modifier = Modifier.weight(1f)
                        )
                        
                        DropdownSelector(
                            label = "Key Derivation",
                            options = KeyDerivationMethod.entries,
                            selectedOption = derivationMethod,
                            onOptionSelected = { derivationMethod = it },
                            optionLabel = { it.name.replace("_", " ") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Calculate ${cryptogramType.name}",
                        onClick = {
                            error = null
                            // First derive UDK
                            CryptogramCalculator.deriveUdk(imk, pan, psn)
                                .onSuccess { udk ->
                                    // Then derive session key
                                    CryptogramCalculator.deriveSessionKey(imk, pan, psn, atc, derivationMethod)
                                        .onSuccess { sessionKey ->
                                            // Then calculate cryptogram
                                            CryptogramCalculator.calculateCryptogram(
                                                type = cryptogramType,
                                                pan = pan,
                                                panSequence = psn,
                                                atc = atc,
                                                unpredictableNumber = unpredictableNumber,
                                                transactionData = transactionData,
                                                masterKey = imk,
                                                derivationMethod = derivationMethod
                                            ).onSuccess { cryptResult ->
                                                result = mapOf(
                                                    "UDK" to HexUtils.formatHex(udk),
                                                    "Session Key" to HexUtils.formatHex(sessionKey),
                                                    cryptogramType.name to HexUtils.formatHex(cryptResult.cryptogram)
                                                )
                                            }.onFailure { error = it.message }
                                        }.onFailure { error = it.message }
                                }.onFailure { error = it.message }
                        },
                        enabled = pan.isNotBlank() && imk.isNotBlank() && atc.isNotBlank()
                    )
                    
                    if (result != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        result!!.forEach { (key, value) ->
                            KeyValueRow(key = key, value = value)
                        }
                    }
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = error!!, isError = true)
                    }
                }
            }
            
            // Right panel - ARPC
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("ARPC Calculator (Method 1)")
                    
                    Text(
                        text = "ARPC = 3DES(ARQC XOR ARC, Session Key)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = arqcForArpc,
                        onValueChange = { arqcForArpc = it },
                        label = "ARQC (8 bytes hex)",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HexInputField(
                        value = arc,
                        onValueChange = { arc = it },
                        label = "Authorization Response Code (ARC) - 2 bytes",
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Uses Session Key from ARQC calculation above",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Calculate ARPC",
                        onClick = {
                            arpcError = null
                            val sessionKey = result?.get("Session Key")?.replace(" ", "")
                            if (sessionKey == null) {
                                arpcError = "Calculate ARQC first to derive session key"
                                return@ActionButton
                            }
                            CryptogramCalculator.calculateArpcMethod1(arqcForArpc, arc, sessionKey)
                                .onSuccess { arpcResult = HexUtils.formatHex(it) }
                                .onFailure { arpcError = it.message }
                        },
                        enabled = arqcForArpc.isNotBlank() && arc.isNotBlank() && result != null
                    )
                    
                    if (arpcResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "ARPC", content = arpcResult!!)
                    }
                    
                    if (arpcError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = arpcError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info card
                ToolCard {
                    SectionHeader("Information")
                    
                    Text(
                        text = """
                            Key Derivation Methods:
                            • EMV Common: Standard EMV derivation
                            • VISA: CVN 10/17/18 derivation
                            • Mastercard: M/Chip derivation
                            
                            Cryptogram Types:
                            • ARQC: Authorization Request
                            • TC: Transaction Certificate
                            • AAC: Application Authentication
                            • ARPC: Authorization Response
                            
                            Transaction Data typically includes:
                            Amount, Currency, Date, ATC, UN, etc.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

