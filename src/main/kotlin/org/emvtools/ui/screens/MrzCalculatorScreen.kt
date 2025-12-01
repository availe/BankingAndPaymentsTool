package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.emvtools.model.MrzData
import org.emvtools.model.MrzDocumentType
import org.emvtools.model.MrzGender
import org.emvtools.model.MrzResult
import org.emvtools.ui.components.*
import org.emvtools.util.MrzCalculator

@Composable
fun MrzCalculatorScreen() {
    var documentType by remember { mutableStateOf(MrzDocumentType.PASSPORT) }
    var surname by remember { mutableStateOf("") }
    var givenNames by remember { mutableStateOf("") }
    var documentNumber by remember { mutableStateOf("") }
    var nationality by remember { mutableStateOf("") }
    var issuingCountry by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(MrzGender.MALE) }
    var expiryDate by remember { mutableStateOf("") }
    var optionalData by remember { mutableStateOf("") }
    
    var mrzResult by remember { mutableStateOf<MrzResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Parse MRZ
    var mrzInput by remember { mutableStateOf("") }
    var parsedData by remember { mutableStateOf<MrzData?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var validationResult by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "MRZ Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left panel - Generate MRZ
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Generate MRZ")
                    
                    DropdownSelector(
                        label = "Document Type",
                        options = MrzDocumentType.entries,
                        selectedOption = documentType,
                        onOptionSelected = { documentType = it },
                        optionLabel = { it.name.replace("_", " ") }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = surname,
                            onValueChange = { surname = it.uppercase() },
                            label = { Text("Surname") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = givenNames,
                            onValueChange = { givenNames = it.uppercase() },
                            label = { Text("Given Names") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = documentNumber,
                            onValueChange = { documentNumber = it.uppercase().take(9) },
                            label = { Text("Document Number") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        DropdownSelector(
                            label = "Gender",
                            options = MrzGender.entries,
                            selectedOption = gender,
                            onOptionSelected = { gender = it },
                            optionLabel = { it.name },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = issuingCountry,
                            onValueChange = { issuingCountry = it.uppercase().take(3) },
                            label = { Text("Issuing Country (3-letter)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = nationality,
                            onValueChange = { nationality = it.uppercase().take(3) },
                            label = { Text("Nationality (3-letter)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Date of Birth (YYMMDD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = { expiryDate = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Expiry Date (YYMMDD)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = optionalData,
                        onValueChange = { optionalData = it.uppercase() },
                        label = { Text("Optional Data (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    ActionButton(
                        text = "Generate MRZ",
                        onClick = {
                            error = null
                            val data = MrzData(
                                documentType = documentType,
                                issuingCountry = issuingCountry,
                                surname = surname,
                                givenNames = givenNames,
                                documentNumber = documentNumber,
                                nationality = nationality,
                                dateOfBirth = dateOfBirth,
                                gender = gender,
                                expiryDate = expiryDate,
                                optionalData = optionalData
                            )
                            MrzCalculator.generateMrz(data)
                                .onSuccess { mrzResult = it }
                                .onFailure { error = it.message }
                        },
                        enabled = surname.isNotBlank() && documentNumber.isNotBlank() && 
                                 dateOfBirth.length == 6 && expiryDate.length == 6
                    )
                    
                    if (mrzResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = mrzResult!!.line1,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mrzResult!!.line2,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                mrzResult!!.line3?.let {
                                    Text(
                                        text = it,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = error!!, isError = true)
                    }
                }
            }
            
            // Right panel - Parse MRZ
            Column(modifier = Modifier.weight(1f)) {
                ToolCard {
                    SectionHeader("Parse & Validate MRZ")
                    
                    OutlinedTextField(
                        value = mrzInput,
                        onValueChange = { mrzInput = it.uppercase() },
                        label = { Text("MRZ (2 or 3 lines)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 4
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton(
                            text = "Parse",
                            onClick = {
                                parseError = null
                                MrzCalculator.parseMrz(mrzInput)
                                    .onSuccess { parsedData = it }
                                    .onFailure { parseError = it.message }
                            },
                            enabled = mrzInput.isNotBlank()
                        )
                        
                        OutlinedButton(
                            onClick = {
                                val result = MrzCalculator.validateMrz(mrzInput)
                                validationResult = if (result.isValid) {
                                    "✓ MRZ is valid"
                                } else {
                                    "✗ Invalid:\n" + result.errors.joinToString("\n")
                                }
                            },
                            enabled = mrzInput.isNotBlank()
                        ) {
                            Text("Validate")
                        }
                    }
                    
                    if (parsedData != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                KeyValueRow("Document Type", parsedData!!.documentType.name)
                                KeyValueRow("Surname", parsedData!!.surname)
                                KeyValueRow("Given Names", parsedData!!.givenNames)
                                KeyValueRow("Document Number", parsedData!!.documentNumber)
                                KeyValueRow("Nationality", parsedData!!.nationality)
                                KeyValueRow("Issuing Country", parsedData!!.issuingCountry)
                                KeyValueRow("Date of Birth", parsedData!!.dateOfBirth)
                                KeyValueRow("Gender", parsedData!!.gender.name)
                                KeyValueRow("Expiry Date", parsedData!!.expiryDate)
                                if (parsedData!!.optionalData.isNotBlank()) {
                                    KeyValueRow("Optional Data", parsedData!!.optionalData)
                                }
                            }
                        }
                    }
                    
                    if (validationResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(
                            title = "Validation",
                            content = validationResult!!,
                            isError = !validationResult!!.startsWith("✓")
                        )
                    }
                    
                    if (parseError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ResultDisplay(title = "Error", content = parseError!!, isError = true)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Info
                ToolCard {
                    SectionHeader("Information")
                    
                    Text(
                        text = """
                            MRZ Formats:
                            • TD3 (Passport): 2 lines × 44 characters
                            • TD1 (ID Card): 3 lines × 30 characters
                            
                            Date format: YYMMDD
                            Country codes: ISO 3166-1 alpha-3
                            
                            Check digits are calculated using:
                            weights 7, 3, 1 (repeating)
                            A-Z = 10-35, 0-9 = 0-9, < = 0
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

