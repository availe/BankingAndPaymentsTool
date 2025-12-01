package org.emvtools.crypto

import org.emvtools.model.DesKeyType
import org.emvtools.model.DesOperation
import org.emvtools.model.DesMode
import org.emvtools.model.PinBlock
import org.emvtools.model.PinBlockFormat
import org.emvtools.util.HexUtils

/**
 * PIN Block Calculator for various formats
 */
object PinBlockCalculator {
    
    /**
     * Create PIN block in specified format
     */
    fun createPinBlock(
        pin: String,
        pan: String,
        format: PinBlockFormat
    ): Result<String> {
        return try {
            // Validate PIN (4-12 digits)
            if (!pin.all { it.isDigit() } || pin.length < 4 || pin.length > 12) {
                return Result.failure(IllegalArgumentException("PIN must be 4-12 digits"))
            }
            
            val pinBlock = when (format) {
                PinBlockFormat.ISO_FORMAT_0, PinBlockFormat.ANSI_X9_8 -> createIsoFormat0(pin, pan)
                PinBlockFormat.ISO_FORMAT_1 -> createIsoFormat1(pin)
                PinBlockFormat.ISO_FORMAT_2 -> createIsoFormat2(pin)
                PinBlockFormat.ISO_FORMAT_3 -> createIsoFormat3(pin, pan)
                PinBlockFormat.ISO_FORMAT_4 -> createIsoFormat4(pin, pan)
                PinBlockFormat.VISA_FORMAT_1 -> createVisaFormat1(pin)
                PinBlockFormat.VISA_FORMAT_3 -> createVisaFormat3(pin, pan)
                PinBlockFormat.VISA_FORMAT_4 -> createVisaFormat4(pin, pan)
            }
            
            Result.success(pinBlock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * ISO Format 0 (ISO-0) / ANSI X9.8
     * PIN Block = PIN Field XOR PAN Field
     * PIN Field: 0 + PIN Length + PIN + F padding
     * PAN Field: 0000 + 12 rightmost PAN digits (excluding check digit)
     */
    private fun createIsoFormat0(pin: String, pan: String): String {
        val pinField = "0${pin.length.toString(16).uppercase()}${pin}${"F".repeat(14 - pin.length)}"
        val panField = "0000${extractPanDigits(pan)}"
        return HexUtils.xorHex(pinField, panField)
    }
    
    /**
     * ISO Format 1 (ISO-1)
     * PIN Block = 1 + PIN Length + PIN + Random padding
     */
    private fun createIsoFormat1(pin: String): String {
        val randomPadding = (1..(14 - pin.length)).map {
            "0123456789ABCDEF".random()
        }.joinToString("")
        return "1${pin.length.toString(16).uppercase()}${pin}$randomPadding"
    }
    
    /**
     * ISO Format 2 (ISO-2) - Used for ICC
     * PIN Block = 2 + PIN Length + PIN + F padding
     */
    private fun createIsoFormat2(pin: String): String {
        return "2${pin.length.toString(16).uppercase()}${pin}${"F".repeat(14 - pin.length)}"
    }
    
    /**
     * ISO Format 3 (ISO-3)
     * PIN Block = PIN Field XOR PAN Field
     * PIN Field: 3 + PIN Length + PIN + Random padding (A-F only)
     * PAN Field: 0000 + 12 rightmost PAN digits (excluding check digit)
     */
    private fun createIsoFormat3(pin: String, pan: String): String {
        val randomPadding = (1..(14 - pin.length)).map {
            "ABCDEF".random()
        }.joinToString("")
        val pinField = "3${pin.length.toString(16).uppercase()}${pin}$randomPadding"
        val panField = "0000${extractPanDigits(pan)}"
        return HexUtils.xorHex(pinField, panField)
    }
    
    /**
     * ISO Format 4 (ISO-4) - AES based
     * Simplified implementation - actual ISO-4 uses AES
     */
    private fun createIsoFormat4(pin: String, pan: String): String {
        // ISO Format 4 is AES-based and more complex
        // This is a simplified placeholder
        val pinField = "4${pin.length.toString(16).uppercase()}${pin}${"A".repeat(14 - pin.length)}"
        val panField = "0000${extractPanDigits(pan)}"
        return HexUtils.xorHex(pinField, panField)
    }
    
    /**
     * VISA Format 1 (VISA PIN Block Format 1)
     * Same as ISO Format 1
     */
    private fun createVisaFormat1(pin: String): String {
        return createIsoFormat1(pin)
    }
    
    /**
     * VISA Format 3 / ECI Format 1
     * Same as ISO Format 0
     */
    private fun createVisaFormat3(pin: String, pan: String): String {
        return createIsoFormat0(pin, pan)
    }
    
    /**
     * VISA Format 4
     * Same as ISO Format 0 but with different padding
     */
    private fun createVisaFormat4(pin: String, pan: String): String {
        return createIsoFormat0(pin, pan)
    }
    
    /**
     * Extract 12 rightmost PAN digits (excluding check digit)
     */
    private fun extractPanDigits(pan: String): String {
        val cleanPan = pan.replace(" ", "").filter { it.isDigit() }
        return if (cleanPan.length > 13) {
            cleanPan.substring(cleanPan.length - 13, cleanPan.length - 1)
        } else {
            cleanPan.dropLast(1).padStart(12, '0')
        }
    }
    
    /**
     * Decrypt PIN block and extract PIN
     */
    fun decryptPinBlock(
        encryptedPinBlock: String,
        pan: String,
        format: PinBlockFormat,
        key: String,
        keyType: DesKeyType = DesKeyType.TRIPLE_DES_2KEY
    ): Result<String> {
        return try {
            // Decrypt the PIN block
            val clearPinBlock = DesCalculator.calculate(
                encryptedPinBlock,
                key,
                DesOperation.DECRYPT,
                DesMode.ECB,
                keyType
            ).getOrThrow()
            
            // Extract PIN from clear PIN block
            extractPinFromBlock(clearPinBlock, pan, format)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Extract PIN from clear PIN block
     */
    fun extractPinFromBlock(
        clearPinBlock: String,
        pan: String,
        format: PinBlockFormat
    ): Result<String> {
        return try {
            val pinField = when (format) {
                PinBlockFormat.ISO_FORMAT_0, PinBlockFormat.ANSI_X9_8,
                PinBlockFormat.VISA_FORMAT_3, PinBlockFormat.VISA_FORMAT_4 -> {
                    val panField = "0000${extractPanDigits(pan)}"
                    HexUtils.xorHex(clearPinBlock, panField)
                }
                PinBlockFormat.ISO_FORMAT_3 -> {
                    val panField = "0000${extractPanDigits(pan)}"
                    HexUtils.xorHex(clearPinBlock, panField)
                }
                else -> clearPinBlock
            }
            
            val pinLength = pinField[1].digitToInt(16)
            val pin = pinField.substring(2, 2 + pinLength)
            
            Result.success(pin)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Encrypt PIN block
     */
    fun encryptPinBlock(
        clearPinBlock: String,
        key: String,
        keyType: DesKeyType = DesKeyType.TRIPLE_DES_2KEY
    ): Result<String> {
        return DesCalculator.calculate(
            clearPinBlock,
            key,
            DesOperation.ENCRYPT,
            DesMode.ECB,
            keyType
        )
    }
    
    /**
     * Translate PIN block from one key to another
     */
    fun translatePinBlock(
        encryptedPinBlock: String,
        sourceKey: String,
        destinationKey: String,
        keyType: DesKeyType = DesKeyType.TRIPLE_DES_2KEY
    ): Result<String> {
        return try {
            // Decrypt with source key
            val clearPinBlock = DesCalculator.calculate(
                encryptedPinBlock,
                sourceKey,
                DesOperation.DECRYPT,
                DesMode.ECB,
                keyType
            ).getOrThrow()
            
            // Encrypt with destination key
            DesCalculator.calculate(
                clearPinBlock,
                destinationKey,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                keyType
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

