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
import org.emvtools.model.*
import org.emvtools.ui.components.*
import org.emvtools.util.TlvParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApduSenderScreen() {
    var cla by remember { mutableStateOf("00") }
    var ins by remember { mutableStateOf("A4") }
    var p1 by remember { mutableStateOf("04") }
    var p2 by remember { mutableStateOf("00") }
    var data by remember { mutableStateOf("") }
    var le by remember { mutableStateOf("00") }
    
    var commandHistory by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var selectedCommand by remember { mutableStateOf<ApduCommand?>(null) }
    var simulatedResponse by remember { mutableStateOf<ApduResponse?>(null) }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Command Builder
        LazyColumn(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader("APDU Command Builder")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cla,
                                onValueChange = { cla = it.uppercase().take(2) },
                                label = { Text("CLA") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ins,
                                onValueChange = { ins = it.uppercase().take(2) },
                                label = { Text("INS") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = p1,
                                onValueChange = { p1 = it.uppercase().take(2) },
                                label = { Text("P1") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = p2,
                                onValueChange = { p2 = it.uppercase().take(2) },
                                label = { Text("P2") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        OutlinedTextField(
                            value = data,
                            onValueChange = { data = it.uppercase().filter { c -> c.isLetterOrDigit() } },
                            label = { Text("Data (hex)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        OutlinedTextField(
                            value = le,
                            onValueChange = { le = it.uppercase().take(2) },
                            label = { Text("Le (expected length)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        val command = ApduCommand("", cla, ins, p1, p2, data, le)
                        Text(
                            "Command: ${command.toHex()}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    // Simulate response (in real app would use PC/SC)
                                    simulatedResponse = simulateApduResponse(command)
                                    commandHistory = commandHistory + (command.toHex() to (simulatedResponse?.let { "${it.data}${it.sw}" } ?: ""))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Send, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Send (Simulated)")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    cla = "00"; ins = "A4"; p1 = "04"; p2 = "00"
                                    data = ""; le = "00"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
            
            item {
                SectionHeader("Common Commands")
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        CommonApduCommands.all.forEach { cmd ->
                            TextButton(
                                onClick = {
                                    cla = cmd.cla
                                    ins = cmd.ins
                                    p1 = cmd.p1
                                    p2 = cmd.p2
                                    data = cmd.data
                                    le = cmd.le
                                    selectedCommand = cmd
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(cmd.name)
                                    Text(
                                        "${cmd.cla}${cmd.ins}",
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("PC/SC Reader", style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Real card communication requires PC/SC reader support. " +
                            "This simulator provides mock responses for testing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
        
        VerticalDivider()
        
        // Right panel - Response & History
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp)
        ) {
            Text("Response", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (simulatedResponse != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (simulatedResponse!!.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                null,
                                tint = if (simulatedResponse!!.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "SW: ${simulatedResponse!!.sw} - ${simulatedResponse!!.statusDescription}",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        
                        if (simulatedResponse!!.data.isNotEmpty()) {
                            Divider()
                            Text("Response Data:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                simulatedResponse!!.data.chunked(32).joinToString("\n"),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                            
                            // Try to parse as TLV
                            val tlvResult = try {
                                TlvParser.parse(simulatedResponse!!.data)
                            } catch (e: Exception) { null }

                            if (tlvResult is org.emvtools.model.TlvParseResult.Success && tlvResult.tlvList.isNotEmpty()) {
                                Divider()
                                Text("Parsed TLV:", style = MaterialTheme.typography.labelMedium)
                                tlvResult.tlvList.forEach { tlv ->
                                    Text(
                                        "${tlv.tag}: ${tlv.value}",
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Command History", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (commandHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No commands sent yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(commandHistory.reversed()) { (cmd, resp) ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    ">> $cmd",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "<< $resp",
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simulate APDU response for testing
 */
private fun simulateApduResponse(command: ApduCommand): ApduResponse {
    val cmdHex = command.toHex()
    
    return when {
        // SELECT PSE
        cmdHex.startsWith("00A404") && command.data.contains("315041592E") -> {
            ApduResponse(
                data = "6F1A840E315041592E5359532E4444463031A5088801025F2D02656E",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // SELECT PPSE
        cmdHex.startsWith("00A404") && command.data.contains("325041592E") -> {
            ApduResponse(
                data = "6F23840E325041592E5359532E4444463031A511BF0C0E61" +
                       "0C4F07A0000000031010870101",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // SELECT by AID
        cmdHex.startsWith("00A404") -> {
            ApduResponse(
                data = "6F1E8407A0000000031010A5139F38039F1A029F3501229F" +
                       "40056000B0A001BF0C059F4D020B0A",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // GET PROCESSING OPTIONS
        cmdHex.startsWith("80A8") -> {
            ApduResponse(
                data = "770E82021980940808010100100101011801",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // READ RECORD
        cmdHex.startsWith("00B2") -> {
            ApduResponse(
                data = "70375A0847617390010100105F24032512315F25031901015F280208405F3401009F0702FF00",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // GET DATA
        cmdHex.startsWith("80CA") -> {
            ApduResponse(
                data = "9F360200${String.format("%02X", (Math.random() * 255).toInt())}",
                sw1 = "90",
                sw2 = "00"
            )
        }
        // Unknown command
        else -> {
            ApduResponse(data = "", sw1 = "6D", sw2 = "00")
        }
    }
}

