package org.emvtools.crypto

import org.emvtools.model.DesKeyType
import org.emvtools.model.KeyComponent
import org.emvtools.model.KeyShare
import org.emvtools.util.HexUtils
import java.security.SecureRandom

/**
 * Key Share Generator for splitting keys into components
 */
object KeyShareGenerator {
    
    private val secureRandom = SecureRandom()
    
    /**
     * Generate a random key of specified length
     */
    fun generateKey(keyType: DesKeyType): Result<String> {
        return try {
            val keyLength = when (keyType) {
                DesKeyType.SINGLE_DES -> 8
                DesKeyType.TRIPLE_DES_2KEY -> 16
                DesKeyType.TRIPLE_DES_3KEY -> 24
            }
            
            val keyBytes = ByteArray(keyLength)
            secureRandom.nextBytes(keyBytes)
            
            // Apply odd parity to each byte
            for (i in keyBytes.indices) {
                keyBytes[i] = applyOddParity(keyBytes[i])
            }
            
            Result.success(HexUtils.bytesToHex(keyBytes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Split a key into specified number of components
     */
    fun splitKey(key: String, numComponents: Int): Result<KeyShare> {
        return try {
            val cleanKey = key.replace(" ", "").uppercase()
            
            if (!HexUtils.isValidHex(cleanKey)) {
                return Result.failure(IllegalArgumentException("Invalid key hex string"))
            }
            
            if (numComponents < 2 || numComponents > 9) {
                return Result.failure(IllegalArgumentException("Number of components must be between 2 and 9"))
            }
            
            val keyBytes = HexUtils.hexToBytes(cleanKey)
            val components = mutableListOf<KeyComponent>()
            
            // Generate n-1 random components
            var xorResult = keyBytes.clone()
            
            for (i in 1 until numComponents) {
                val componentBytes = ByteArray(keyBytes.size)
                secureRandom.nextBytes(componentBytes)
                
                // Apply odd parity
                for (j in componentBytes.indices) {
                    componentBytes[j] = applyOddParity(componentBytes[j])
                }
                
                val componentHex = HexUtils.bytesToHex(componentBytes)
                val kcv = calculateComponentKcv(componentHex, keyBytes.size)
                
                components.add(KeyComponent(i, componentHex, kcv))
                
                // XOR with running result
                for (j in xorResult.indices) {
                    xorResult[j] = (xorResult[j].toInt() xor componentBytes[j].toInt()).toByte()
                }
            }
            
            // Last component is the XOR result
            val lastComponentHex = HexUtils.bytesToHex(xorResult)
            val lastKcv = calculateComponentKcv(lastComponentHex, keyBytes.size)
            components.add(KeyComponent(numComponents, lastComponentHex, lastKcv))
            
            // Calculate full key KCV
            val fullKeyKcv = calculateKeyKcv(cleanKey)
            
            Result.success(KeyShare(cleanKey, fullKeyKcv, components))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Combine key components back into full key
     */
    fun combineComponents(components: List<String>): Result<KeyShare> {
        return try {
            if (components.isEmpty()) {
                return Result.failure(IllegalArgumentException("At least one component required"))
            }
            
            val cleanComponents = components.map { it.replace(" ", "").uppercase() }
            
            // Validate all components have same length
            val length = cleanComponents.first().length
            if (!cleanComponents.all { it.length == length }) {
                return Result.failure(IllegalArgumentException("All components must have same length"))
            }
            
            // XOR all components together
            var result = HexUtils.hexToBytes(cleanComponents.first())
            
            for (i in 1 until cleanComponents.size) {
                val componentBytes = HexUtils.hexToBytes(cleanComponents[i])
                for (j in result.indices) {
                    result[j] = (result[j].toInt() xor componentBytes[j].toInt()).toByte()
                }
            }
            
            val fullKey = HexUtils.bytesToHex(result)
            val fullKeyKcv = calculateKeyKcv(fullKey)
            
            val keyComponents = cleanComponents.mapIndexed { index, component ->
                KeyComponent(index + 1, component, calculateComponentKcv(component, result.size))
            }
            
            Result.success(KeyShare(fullKey, fullKeyKcv, keyComponents))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate KCV for a key
     */
    fun calculateKeyKcv(key: String): String {
        val keyType = when (key.length) {
            16 -> DesKeyType.SINGLE_DES
            32 -> DesKeyType.TRIPLE_DES_2KEY
            48 -> DesKeyType.TRIPLE_DES_3KEY
            else -> DesKeyType.TRIPLE_DES_2KEY
        }
        
        return DesCalculator.calculateKcv(key, keyType).getOrElse { "??????" }
    }
    
    /**
     * Calculate KCV for a component (treating it as a key)
     */
    private fun calculateComponentKcv(component: String, keySize: Int): String {
        val keyType = when (keySize) {
            8 -> DesKeyType.SINGLE_DES
            16 -> DesKeyType.TRIPLE_DES_2KEY
            24 -> DesKeyType.TRIPLE_DES_3KEY
            else -> DesKeyType.TRIPLE_DES_2KEY
        }
        
        return DesCalculator.calculateKcv(component, keyType).getOrElse { "??????" }
    }
    
    /**
     * Apply odd parity to a byte
     */
    private fun applyOddParity(byte: Byte): Byte {
        var b = byte.toInt() and 0xFE // Clear LSB
        var parity = 0
        var temp = b
        for (i in 0 until 7) {
            parity = parity xor (temp and 1)
            temp = temp shr 1
        }
        // Set LSB to make odd parity
        if (parity == 1) {
            return b.toByte()
        } else {
            return (b or 1).toByte()
        }
    }
    
    /**
     * Check if key has valid odd parity
     */
    fun checkParity(key: String): Boolean {
        val keyBytes = HexUtils.hexToBytes(key.replace(" ", "").uppercase())
        return keyBytes.all { byte ->
            var parity = 0
            var temp = byte.toInt() and 0xFF
            for (i in 0 until 8) {
                parity = parity xor (temp and 1)
                temp = temp shr 1
            }
            parity == 1
        }
    }
    
    /**
     * Fix parity of a key
     */
    fun fixParity(key: String): String {
        val keyBytes = HexUtils.hexToBytes(key.replace(" ", "").uppercase())
        for (i in keyBytes.indices) {
            keyBytes[i] = applyOddParity(keyBytes[i])
        }
        return HexUtils.bytesToHex(keyBytes)
    }
}

