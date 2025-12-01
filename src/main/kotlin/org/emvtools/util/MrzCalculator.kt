package org.emvtools.util

import org.emvtools.model.MrzData
import org.emvtools.model.MrzDocumentType
import org.emvtools.model.MrzGender
import org.emvtools.model.MrzResult

/**
 * Machine Readable Zone (MRZ) Calculator for passports and ID cards
 */
object MrzCalculator {
    
    private val MRZ_WEIGHTS = intArrayOf(7, 3, 1)
    
    /**
     * Generate MRZ from biographical data
     */
    fun generateMrz(data: MrzData): Result<MrzResult> {
        return try {
            when (data.documentType) {
                MrzDocumentType.PASSPORT -> generateTd3Mrz(data)
                MrzDocumentType.ID_CARD -> generateTd1Mrz(data)
                MrzDocumentType.VISA -> generateTd3Mrz(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Generate TD3 format MRZ (Passport - 2 lines of 44 characters)
     */
    private fun generateTd3Mrz(data: MrzData): Result<MrzResult> {
        // Line 1: P<ISSUING_COUNTRY<SURNAME<<GIVEN_NAMES<<<...
        val line1 = buildString {
            append(data.documentType.code)
            append("<")
            append(formatCountry(data.issuingCountry))
            append(formatName(data.surname))
            append("<<")
            append(formatName(data.givenNames))
        }.padEnd(44, '<').take(44)
        
        // Line 2: DOCUMENT_NUMBER<CHECK<NATIONALITY<DOB<CHECK<SEX<EXPIRY<CHECK<OPTIONAL<CHECK<FINAL_CHECK
        val docNumber = data.documentNumber.uppercase().padEnd(9, '<').take(9)
        val docNumberCheck = calculateCheckDigit(docNumber)
        
        val nationality = formatCountry(data.nationality)
        
        val dob = data.dateOfBirth.take(6)
        val dobCheck = calculateCheckDigit(dob)
        
        val expiry = data.expiryDate.take(6)
        val expiryCheck = calculateCheckDigit(expiry)
        
        val optional = data.optionalData.uppercase().padEnd(14, '<').take(14)
        val optionalCheck = calculateCheckDigit(optional)
        
        // Calculate final check digit over specific fields
        val finalCheckData = docNumber + docNumberCheck + dob + dobCheck + expiry + expiryCheck + optional + optionalCheck
        val finalCheck = calculateCheckDigit(finalCheckData)
        
        val line2 = buildString {
            append(docNumber)
            append(docNumberCheck)
            append(nationality)
            append(dob)
            append(dobCheck)
            append(data.gender.code)
            append(expiry)
            append(expiryCheck)
            append(optional)
            append(optionalCheck)
            append(finalCheck)
        }
        
        return Result.success(MrzResult(line1, line2))
    }
    
    /**
     * Generate TD1 format MRZ (ID Card - 3 lines of 30 characters)
     */
    private fun generateTd1Mrz(data: MrzData): Result<MrzResult> {
        // Line 1: I<ISSUING_COUNTRY<DOCUMENT_NUMBER<CHECK<OPTIONAL
        val docNumber = data.documentNumber.uppercase().padEnd(9, '<').take(9)
        val docNumberCheck = calculateCheckDigit(docNumber)
        val optional1 = data.optionalData.take(15).uppercase().padEnd(15, '<')
        
        val line1 = buildString {
            append(data.documentType.code)
            append("<")
            append(formatCountry(data.issuingCountry))
            append(docNumber)
            append(docNumberCheck)
            append(optional1)
        }.take(30)
        
        // Line 2: DOB<CHECK<SEX<EXPIRY<CHECK<NATIONALITY<OPTIONAL<FINAL_CHECK
        val dob = data.dateOfBirth.take(6)
        val dobCheck = calculateCheckDigit(dob)
        
        val expiry = data.expiryDate.take(6)
        val expiryCheck = calculateCheckDigit(expiry)
        
        val optional2 = "".padEnd(11, '<')
        
        // Final check digit
        val finalCheckData = docNumber + docNumberCheck + optional1 + dob + dobCheck + expiry + expiryCheck + optional2
        val finalCheck = calculateCheckDigit(finalCheckData)
        
        val line2 = buildString {
            append(dob)
            append(dobCheck)
            append(data.gender.code)
            append(expiry)
            append(expiryCheck)
            append(formatCountry(data.nationality))
            append(optional2)
            append(finalCheck)
        }.take(30)
        
        // Line 3: SURNAME<<GIVEN_NAMES<<<...
        val line3 = buildString {
            append(formatName(data.surname))
            append("<<")
            append(formatName(data.givenNames))
        }.padEnd(30, '<').take(30)
        
        return Result.success(MrzResult(line1, line2, line3))
    }
    
    /**
     * Calculate MRZ check digit
     */
    fun calculateCheckDigit(data: String): Char {
        var sum = 0
        for (i in data.indices) {
            val char = data[i]
            val value = when {
                char in '0'..'9' -> char - '0'
                char in 'A'..'Z' -> char - 'A' + 10
                char == '<' -> 0
                else -> 0
            }
            sum += value * MRZ_WEIGHTS[i % 3]
        }
        return ('0' + (sum % 10))
    }
    
    /**
     * Validate MRZ check digit
     */
    fun validateCheckDigit(data: String, checkDigit: Char): Boolean {
        return calculateCheckDigit(data) == checkDigit
    }
    
    /**
     * Parse MRZ and extract data
     */
    fun parseMrz(mrz: String): Result<MrzData> {
        return try {
            val lines = mrz.trim().split("\n").map { it.trim() }
            
            when {
                lines.size == 2 && lines[0].length == 44 -> parseTd3Mrz(lines[0], lines[1])
                lines.size == 3 && lines[0].length == 30 -> parseTd1Mrz(lines[0], lines[1], lines[2])
                else -> Result.failure(IllegalArgumentException("Invalid MRZ format"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse TD3 (Passport) MRZ
     */
    private fun parseTd3Mrz(line1: String, line2: String): Result<MrzData> {
        val docType = when (line1[0]) {
            'P' -> MrzDocumentType.PASSPORT
            'V' -> MrzDocumentType.VISA
            else -> MrzDocumentType.PASSPORT
        }
        
        val issuingCountry = line1.substring(2, 5).replace("<", "")
        
        val namePart = line1.substring(5).trimEnd('<')
        val nameParts = namePart.split("<<")
        val surname = nameParts.getOrElse(0) { "" }.replace("<", " ").trim()
        val givenNames = nameParts.getOrElse(1) { "" }.replace("<", " ").trim()
        
        val documentNumber = line2.substring(0, 9).replace("<", "")
        val nationality = line2.substring(10, 13).replace("<", "")
        val dateOfBirth = line2.substring(13, 19)
        val gender = when (line2[20]) {
            'M' -> MrzGender.MALE
            'F' -> MrzGender.FEMALE
            else -> MrzGender.UNSPECIFIED
        }
        val expiryDate = line2.substring(21, 27)
        val optionalData = line2.substring(28, 42).trimEnd('<')
        
        return Result.success(MrzData(
            documentType = docType,
            issuingCountry = issuingCountry,
            surname = surname,
            givenNames = givenNames,
            documentNumber = documentNumber,
            nationality = nationality,
            dateOfBirth = dateOfBirth,
            gender = gender,
            expiryDate = expiryDate,
            optionalData = optionalData
        ))
    }
    
    /**
     * Parse TD1 (ID Card) MRZ
     */
    private fun parseTd1Mrz(line1: String, line2: String, line3: String): Result<MrzData> {
        val issuingCountry = line1.substring(2, 5).replace("<", "")
        val documentNumber = line1.substring(5, 14).replace("<", "")
        
        val dateOfBirth = line2.substring(0, 6)
        val gender = when (line2[7]) {
            'M' -> MrzGender.MALE
            'F' -> MrzGender.FEMALE
            else -> MrzGender.UNSPECIFIED
        }
        val expiryDate = line2.substring(8, 14)
        val nationality = line2.substring(15, 18).replace("<", "")
        
        val namePart = line3.trimEnd('<')
        val nameParts = namePart.split("<<")
        val surname = nameParts.getOrElse(0) { "" }.replace("<", " ").trim()
        val givenNames = nameParts.getOrElse(1) { "" }.replace("<", " ").trim()
        
        return Result.success(MrzData(
            documentType = MrzDocumentType.ID_CARD,
            issuingCountry = issuingCountry,
            surname = surname,
            givenNames = givenNames,
            documentNumber = documentNumber,
            nationality = nationality,
            dateOfBirth = dateOfBirth,
            gender = gender,
            expiryDate = expiryDate
        ))
    }
    
    /**
     * Format country code (3 characters)
     */
    private fun formatCountry(country: String): String {
        return country.uppercase().padEnd(3, '<').take(3)
    }
    
    /**
     * Format name for MRZ (replace spaces with <)
     */
    private fun formatName(name: String): String {
        return name.uppercase()
            .replace(" ", "<")
            .replace("-", "<")
            .replace("'", "")
            .filter { it in 'A'..'Z' || it == '<' }
    }
    
    /**
     * Validate complete MRZ
     */
    fun validateMrz(mrz: String): MrzValidationResult {
        val lines = mrz.trim().split("\n").map { it.trim() }
        val errors = mutableListOf<String>()
        
        when {
            lines.size == 2 && lines[0].length == 44 -> {
                // TD3 validation
                val line2 = lines[1]
                
                // Document number check
                val docNumber = line2.substring(0, 9)
                val docCheck = line2[9]
                if (!validateCheckDigit(docNumber, docCheck)) {
                    errors.add("Invalid document number check digit")
                }
                
                // DOB check
                val dob = line2.substring(13, 19)
                val dobCheck = line2[19]
                if (!validateCheckDigit(dob, dobCheck)) {
                    errors.add("Invalid date of birth check digit")
                }
                
                // Expiry check
                val expiry = line2.substring(21, 27)
                val expiryCheck = line2[27]
                if (!validateCheckDigit(expiry, expiryCheck)) {
                    errors.add("Invalid expiry date check digit")
                }
            }
            lines.size == 3 && lines[0].length == 30 -> {
                // TD1 validation
                val line1 = lines[0]
                val line2 = lines[1]
                
                val docNumber = line1.substring(5, 14)
                val docCheck = line1[14]
                if (!validateCheckDigit(docNumber, docCheck)) {
                    errors.add("Invalid document number check digit")
                }
                
                val dob = line2.substring(0, 6)
                val dobCheck = line2[6]
                if (!validateCheckDigit(dob, dobCheck)) {
                    errors.add("Invalid date of birth check digit")
                }
                
                val expiry = line2.substring(8, 14)
                val expiryCheck = line2[14]
                if (!validateCheckDigit(expiry, expiryCheck)) {
                    errors.add("Invalid expiry date check digit")
                }
            }
            else -> {
                errors.add("Invalid MRZ format")
            }
        }
        
        return MrzValidationResult(errors.isEmpty(), errors)
    }
}

data class MrzValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)

