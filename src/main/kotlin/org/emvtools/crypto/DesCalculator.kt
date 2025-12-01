package org.emvtools.crypto

import org.emvtools.model.DesKeyType
import org.emvtools.model.DesMode
import org.emvtools.model.DesOperation
import org.emvtools.util.HexUtils
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DES and Triple DES Calculator
 */
object DesCalculator {
    
    /**
     * Perform DES/3DES operation
     */
    fun calculate(
        data: String,
        key: String,
        operation: DesOperation,
        mode: DesMode,
        keyType: DesKeyType,
        iv: String = "0000000000000000"
    ): Result<String> {
        return try {
            val cleanData = data.replace(" ", "").uppercase()
            val cleanKey = key.replace(" ", "").uppercase()
            val cleanIv = iv.replace(" ", "").uppercase()
            
            // Validate inputs
            if (!HexUtils.isValidHex(cleanData)) {
                return Result.failure(IllegalArgumentException("Invalid data hex string"))
            }
            if (!HexUtils.isValidHex(cleanKey)) {
                return Result.failure(IllegalArgumentException("Invalid key hex string"))
            }
            if (!HexUtils.isValidHex(cleanIv)) {
                return Result.failure(IllegalArgumentException("Invalid IV hex string"))
            }
            
            // Validate key length
            val expectedKeyLength = when (keyType) {
                DesKeyType.SINGLE_DES -> 16
                DesKeyType.TRIPLE_DES_2KEY -> 32
                DesKeyType.TRIPLE_DES_3KEY -> 48
            }
            if (cleanKey.length != expectedKeyLength) {
                return Result.failure(IllegalArgumentException("Key must be $expectedKeyLength hex characters for $keyType"))
            }
            
            // Validate data length (must be multiple of 8 bytes / 16 hex chars)
            if (cleanData.length % 16 != 0) {
                return Result.failure(IllegalArgumentException("Data must be multiple of 8 bytes (16 hex characters)"))
            }
            
            val dataBytes = HexUtils.hexToBytes(cleanData)
            val keyBytes = prepareKey(cleanKey, keyType)
            val ivBytes = HexUtils.hexToBytes(cleanIv)
            
            val algorithm = if (keyType == DesKeyType.SINGLE_DES) "DES" else "DESede"
            val cipherMode = when (mode) {
                DesMode.ECB -> "$algorithm/ECB/NoPadding"
                DesMode.CBC -> "$algorithm/CBC/NoPadding"
            }
            
            val secretKey: SecretKey = SecretKeySpec(keyBytes, algorithm)
            val cipher = Cipher.getInstance(cipherMode)
            
            val cipherOperation = when (operation) {
                DesOperation.ENCRYPT -> Cipher.ENCRYPT_MODE
                DesOperation.DECRYPT -> Cipher.DECRYPT_MODE
            }
            
            if (mode == DesMode.CBC) {
                cipher.init(cipherOperation, secretKey, IvParameterSpec(ivBytes))
            } else {
                cipher.init(cipherOperation, secretKey)
            }
            
            val result = cipher.doFinal(dataBytes)
            Result.success(HexUtils.bytesToHex(result))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Prepare key bytes for the cipher
     */
    private fun prepareKey(key: String, keyType: DesKeyType): ByteArray {
        val keyBytes = HexUtils.hexToBytes(key)
        return when (keyType) {
            DesKeyType.SINGLE_DES -> keyBytes
            DesKeyType.TRIPLE_DES_2KEY -> {
                // For 2-key 3DES, duplicate first 8 bytes: K1 K2 K1
                ByteArray(24).also {
                    System.arraycopy(keyBytes, 0, it, 0, 16)
                    System.arraycopy(keyBytes, 0, it, 16, 8)
                }
            }
            DesKeyType.TRIPLE_DES_3KEY -> keyBytes
        }
    }
    
    /**
     * Calculate Key Check Value (KCV)
     */
    fun calculateKcv(key: String, keyType: DesKeyType): Result<String> {
        val zeroBlock = "0000000000000000"
        return calculate(zeroBlock, key, DesOperation.ENCRYPT, DesMode.ECB, keyType)
            .map { it.take(6) }
    }
    
    /**
     * Calculate MAC (Message Authentication Code) using DES/3DES
     */
    fun calculateMac(
        data: String,
        key: String,
        keyType: DesKeyType,
        iv: String = "0000000000000000"
    ): Result<String> {
        return try {
            val cleanData = data.replace(" ", "").uppercase()
            val cleanKey = key.replace(" ", "").uppercase()
            
            // Pad data to multiple of 8 bytes if needed
            val paddedData = if (cleanData.length % 16 != 0) {
                val padLength = 16 - (cleanData.length % 16)
                cleanData + "0".repeat(padLength)
            } else {
                cleanData
            }
            
            // Split into 8-byte blocks
            val blocks = paddedData.chunked(16)
            var result = iv.replace(" ", "").uppercase()
            
            for (block in blocks) {
                val xored = HexUtils.xorHex(result, block)
                result = calculate(xored, cleanKey, DesOperation.ENCRYPT, DesMode.ECB, keyType)
                    .getOrThrow()
            }
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate Retail MAC (ISO 9797-1 Algorithm 3)
     * Uses single DES for all blocks except the last, which uses 3DES
     */
    fun calculateRetailMac(
        data: String,
        key: String,
        iv: String = "0000000000000000"
    ): Result<String> {
        return try {
            val cleanData = data.replace(" ", "").uppercase()
            val cleanKey = key.replace(" ", "").uppercase()
            
            if (cleanKey.length != 32) {
                return Result.failure(IllegalArgumentException("Key must be 32 hex characters (16 bytes) for Retail MAC"))
            }
            
            val keyLeft = cleanKey.substring(0, 16)
            val keyRight = cleanKey.substring(16, 32)
            
            // Pad data to multiple of 8 bytes if needed
            val paddedData = if (cleanData.length % 16 != 0) {
                val padLength = 16 - (cleanData.length % 16)
                cleanData + "0".repeat(padLength)
            } else {
                cleanData
            }
            
            // Split into 8-byte blocks
            val blocks = paddedData.chunked(16)
            var result = iv.replace(" ", "").uppercase()
            
            // Process all blocks except last with single DES
            for (i in 0 until blocks.size - 1) {
                val xored = HexUtils.xorHex(result, blocks[i])
                result = calculate(xored, keyLeft, DesOperation.ENCRYPT, DesMode.ECB, DesKeyType.SINGLE_DES)
                    .getOrThrow()
            }
            
            // Process last block with 3DES
            val lastXored = HexUtils.xorHex(result, blocks.last())
            result = calculate(lastXored, cleanKey, DesOperation.ENCRYPT, DesMode.ECB, DesKeyType.TRIPLE_DES_2KEY)
                .getOrThrow()
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

