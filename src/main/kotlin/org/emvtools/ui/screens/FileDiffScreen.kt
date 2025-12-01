package org.emvtools.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.ui.components.*
import org.emvtools.util.HexUtils
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDiffScreen() {
    var mode by remember { mutableStateOf("Diff") }
    var leftText by remember { mutableStateOf("") }
    var rightText by remember { mutableStateOf("") }
    var diffResult by remember { mutableStateOf<List<DiffLine>>(emptyList()) }
    
    // Base64 converter
    var base64Input by remember { mutableStateOf("") }
    var base64Output by remember { mutableStateOf("") }
    var base64Mode by remember { mutableStateOf("Encode") }
    
    Row(modifier = Modifier.fillMaxSize()) {
        // Left panel - Input
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionHeader("Hex Utilities")
            
            // Mode selection
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == "Diff",
                    onClick = { mode = "Diff" },
                    label = { Text("File Diff") },
                    leadingIcon = { Icon(Icons.Default.Compare, null) }
                )
                FilterChip(
                    selected = mode == "Base64",
                    onClick = { mode = "Base64" },
                    label = { Text("Base64") },
                    leadingIcon = { Icon(Icons.Default.Transform, null) }
                )
            }
            
            if (mode == "Diff") {
                // Diff mode
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Left (Original)", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = leftText,
                            onValueChange = { leftText = it },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            placeholder = { Text("Paste hex data or text...") }
                        )
                    }
                }
                
                Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Right (Modified)", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = rightText,
                            onValueChange = { rightText = it },
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            placeholder = { Text("Paste hex data or text...") }
                        )
                    }
                }
                
                Button(
                    onClick = {
                        diffResult = computeDiff(leftText, rightText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = leftText.isNotEmpty() || rightText.isNotEmpty()
                ) {
                    Icon(Icons.Default.Compare, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compare")
                }
            } else {
                // Base64 mode
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = base64Mode == "Encode",
                                onClick = { base64Mode = "Encode" },
                                label = { Text("Encode") }
                            )
                            FilterChip(
                                selected = base64Mode == "Decode",
                                onClick = { base64Mode = "Decode" },
                                label = { Text("Decode") }
                            )
                            FilterChip(
                                selected = base64Mode == "Hex→Base64",
                                onClick = { base64Mode = "Hex→Base64" },
                                label = { Text("Hex→Base64") }
                            )
                            FilterChip(
                                selected = base64Mode == "Base64→Hex",
                                onClick = { base64Mode = "Base64→Hex" },
                                label = { Text("Base64→Hex") }
                            )
                        }
                        
                        OutlinedTextField(
                            value = base64Input,
                            onValueChange = { base64Input = it },
                            label = { Text("Input") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5,
                            maxLines = 10,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        
                        Button(
                            onClick = {
                                base64Output = try {
                                    when (base64Mode) {
                                        "Encode" -> Base64.getEncoder().encodeToString(base64Input.toByteArray())
                                        "Decode" -> String(Base64.getDecoder().decode(base64Input.trim()))
                                        "Hex→Base64" -> Base64.getEncoder().encodeToString(HexUtils.hexToBytes(base64Input.replace(" ", "")))
                                        "Base64→Hex" -> HexUtils.bytesToHex(Base64.getDecoder().decode(base64Input.trim()))
                                        else -> ""
                                    }
                                } catch (e: Exception) {
                                    "Error: ${e.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Convert")
                        }
                        
                        if (base64Output.isNotEmpty()) {
                            Divider()
                            Text("Output", style = MaterialTheme.typography.titleSmall)
                            SelectionContainer {
                                Text(
                                    base64Output,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                // Quick conversions reference
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Base64 Reference", style = MaterialTheme.typography.titleSmall)
                        Divider()
                        Text("• Encode: Text → Base64", style = MaterialTheme.typography.bodySmall)
                        Text("• Decode: Base64 → Text", style = MaterialTheme.typography.bodySmall)
                        Text("• Hex→Base64: Hex bytes → Base64", style = MaterialTheme.typography.bodySmall)
                        Text("• Base64→Hex: Base64 → Hex bytes", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        
        VerticalDivider()
        
        // Right panel - Results
        Column(
            modifier = Modifier.weight(0.5f).fillMaxHeight().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (mode == "Diff") {
                SectionHeader("Diff Result")
                
                if (diffResult.isNotEmpty()) {
                    // Stats
                    val added = diffResult.count { it.type == DiffType.ADDED }
                    val removed = diffResult.count { it.type == DiffType.REMOVED }
                    val unchanged = diffResult.count { it.type == DiffType.UNCHANGED }
                    
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("+$added", color = Color(0xFF4CAF50), style = MaterialTheme.typography.titleMedium)
                                Text("Added", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("-$removed", color = Color(0xFFF44336), style = MaterialTheme.typography.titleMedium)
                                Text("Removed", style = MaterialTheme.typography.bodySmall)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$unchanged", style = MaterialTheme.typography.titleMedium)
                                Text("Unchanged", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        LazyColumn(modifier = Modifier.padding(8.dp).horizontalScroll(rememberScrollState())) {
                            itemsIndexed(diffResult) { index, line ->
                                val bgColor = when (line.type) {
                                    DiffType.ADDED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                    DiffType.REMOVED -> Color(0xFFF44336).copy(alpha = 0.2f)
                                    DiffType.UNCHANGED -> Color.Transparent
                                }
                                val prefix = when (line.type) {
                                    DiffType.ADDED -> "+"
                                    DiffType.REMOVED -> "-"
                                    DiffType.UNCHANGED -> " "
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(bgColor).padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        String.format("%4d", index + 1),
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(40.dp)
                                    )
                                    Text(
                                        prefix,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when (line.type) {
                                            DiffType.ADDED -> Color(0xFF4CAF50)
                                            DiffType.REMOVED -> Color(0xFFF44336)
                                            DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
                                        },
                                        modifier = Modifier.width(16.dp)
                                    )
                                    Text(
                                        line.content,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
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
                                    Icons.Default.Compare,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Enter text in both panels and click Compare", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                // Base64 mode - show encoding table
                SectionHeader("Base64 Encoding Table")
                
                Card(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        item {
                            Text("Standard Base64 Alphabet", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                        }
                        
                        item {
                            val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                            alphabet.chunked(16).forEachIndexed { rowIndex, chunk ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    chunk.forEachIndexed { colIndex, char ->
                                        val index = rowIndex * 16 + colIndex
                                        Column(
                                            modifier = Modifier.weight(1f).padding(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "$index",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "$char",
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            Spacer(Modifier.height(16.dp))
                            Text("Padding character: = (equals sign)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

enum class DiffType { ADDED, REMOVED, UNCHANGED }
data class DiffLine(val type: DiffType, val content: String)

private fun computeDiff(left: String, right: String): List<DiffLine> {
    val leftLines = left.lines()
    val rightLines = right.lines()
    val result = mutableListOf<DiffLine>()
    
    // Simple line-by-line diff (LCS-based diff would be more sophisticated)
    val maxLines = maxOf(leftLines.size, rightLines.size)
    
    var leftIndex = 0
    var rightIndex = 0
    
    while (leftIndex < leftLines.size || rightIndex < rightLines.size) {
        val leftLine = leftLines.getOrNull(leftIndex)
        val rightLine = rightLines.getOrNull(rightIndex)
        
        when {
            leftLine == rightLine -> {
                result.add(DiffLine(DiffType.UNCHANGED, leftLine ?: ""))
                leftIndex++
                rightIndex++
            }
            leftLine != null && (rightLine == null || !rightLines.drop(rightIndex).contains(leftLine)) -> {
                result.add(DiffLine(DiffType.REMOVED, leftLine))
                leftIndex++
            }
            rightLine != null && (leftLine == null || !leftLines.drop(leftIndex).contains(rightLine)) -> {
                result.add(DiffLine(DiffType.ADDED, rightLine))
                rightIndex++
            }
            else -> {
                if (leftLine != null) {
                    result.add(DiffLine(DiffType.REMOVED, leftLine))
                    leftIndex++
                }
                if (rightLine != null) {
                    result.add(DiffLine(DiffType.ADDED, rightLine))
                    rightIndex++
                }
            }
        }
    }
    
    return result
}

