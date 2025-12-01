package org.emvtools.crypto

import org.emvtools.util.HexUtils
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES Calculator for encryption, decryption, and MAC operations
 */
object AesCalculator {
    
    enum class AesMode(val displayName: String, val cipherMode: String) {
        ECB("ECB", "AES/ECB/NoPadding"),
        CBC("CBC", "AES/CBC/NoPadding"),
        CFB("CFB", "AES/CFB/NoPadding"),
        OFB("OFB", "AES/OFB/NoPadding"),
        CTR("CTR", "AES/CTR/NoPadding")
    }
    
    enum class AesKeySize(val bits: Int, val bytes: Int) {
        AES_128(128, 16),
        AES_192(192, 24),
        AES_256(256, 32)
    }
    
    /**
     * Encrypt data using AES
     */
    fun encrypt(
        key: String,
        data: String,
        mode: AesMode = AesMode.ECB,
        iv: String = ""
    ): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(padData(data))
        
        val cipher = Cipher.getInstance(mode.cipherMode)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        
        if (mode == AesMode.ECB) {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        } else {
            val ivBytes = if (iv.isNotEmpty()) HexUtils.hexToBytes(iv) else ByteArray(16)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
        }
        
        val encrypted = cipher.doFinal(dataBytes)
        return HexUtils.bytesToHex(encrypted)
    }
    
    /**
     * Decrypt data using AES
     */
    fun decrypt(
        key: String,
        data: String,
        mode: AesMode = AesMode.ECB,
        iv: String = ""
    ): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(data)
        
        val cipher = Cipher.getInstance(mode.cipherMode)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        
        if (mode == AesMode.ECB) {
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
        } else {
            val ivBytes = if (iv.isNotEmpty()) HexUtils.hexToBytes(iv) else ByteArray(16)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(ivBytes))
        }
        
        val decrypted = cipher.doFinal(dataBytes)
        return HexUtils.bytesToHex(decrypted)
    }
    
    /**
     * Calculate CMAC (Cipher-based MAC) using AES
     */
    fun calculateCmac(key: String, data: String): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(data)
        
        // Generate subkeys
        val (k1, k2) = generateSubkeys(keyBytes)
        
        // Pad and XOR with subkey
        val n = (dataBytes.size + 15) / 16
        val lastBlockComplete = dataBytes.size > 0 && dataBytes.size % 16 == 0
        
        val blocks = mutableListOf<ByteArray>()
        for (i in 0 until n) {
            val start = i * 16
            val end = minOf(start + 16, dataBytes.size)
            blocks.add(dataBytes.copyOfRange(start, end))
        }
        
        if (blocks.isEmpty()) {
            blocks.add(ByteArray(0))
        }
        
        // Process last block
        val lastBlock = blocks.last()
        val processedLastBlock = if (lastBlockComplete && lastBlock.size == 16) {
            xorBytes(lastBlock, k1)
        } else {
            val padded = padBlock(lastBlock)
            xorBytes(padded, k2)
        }
        blocks[blocks.size - 1] = processedLastBlock
        
        // CBC-MAC
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"))
        
        var x = ByteArray(16)
        for (block in blocks) {
            val y = xorBytes(x, block)
            x = cipher.doFinal(y)
        }
        
        return HexUtils.bytesToHex(x)
    }
    
    /**
     * Calculate AES-CBC MAC
     */
    fun calculateCbcMac(key: String, data: String, iv: String = ""): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(padData(data))
        val ivBytes = if (iv.isNotEmpty()) HexUtils.hexToBytes(iv) else ByteArray(16)
        
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
        
        val encrypted = cipher.doFinal(dataBytes)
        // Return last block as MAC
        return HexUtils.bytesToHex(encrypted.copyOfRange(encrypted.size - 16, encrypted.size))
    }
    
    /**
     * Calculate Key Check Value (KCV) for AES key
     */
    fun calculateKcv(key: String): String {
        val encrypted = encrypt(key, "00000000000000000000000000000000", AesMode.ECB)
        return encrypted.take(6)
    }
    
    /**
     * Validate AES key length
     */
    fun validateKeyLength(key: String): AesKeySize? {
        val bytes = key.length / 2
        return AesKeySize.entries.find { it.bytes == bytes }
    }
    
    /**
     * Generate random AES key
     */
    fun generateKey(keySize: AesKeySize = AesKeySize.AES_128): String {
        val random = java.security.SecureRandom()
        val keyBytes = ByteArray(keySize.bytes)
        random.nextBytes(keyBytes)
        return HexUtils.bytesToHex(keyBytes)
    }
    
    /**
     * Generate random IV
     */
    fun generateIv(): String {
        val random = java.security.SecureRandom()
        val ivBytes = ByteArray(16)
        random.nextBytes(ivBytes)
        return HexUtils.bytesToHex(ivBytes)
    }
    
    // Helper functions
    
    private fun padData(data: String): String {
        val bytes = data.length / 2
        val remainder = bytes % 16
        return if (remainder == 0 && bytes > 0) {
            data
        } else {
            data + "00".repeat(16 - remainder)
        }
    }
    
    private fun padBlock(block: ByteArray): ByteArray {
        val padded = ByteArray(16)
        block.copyInto(padded)
        if (block.size < 16) {
            padded[block.size] = 0x80.toByte()
        }
        return padded
    }
    
    private fun generateSubkeys(key: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        
        val l = cipher.doFinal(ByteArray(16))
        val k1 = shiftLeft(l)
        if ((l[0].toInt() and 0x80) != 0) {
            k1[15] = (k1[15].toInt() xor 0x87).toByte()
        }
        
        val k2 = shiftLeft(k1)
        if ((k1[0].toInt() and 0x80) != 0) {
            k2[15] = (k2[15].toInt() xor 0x87).toByte()
        }
        
        return Pair(k1, k2)
    }
    
    private fun shiftLeft(data: ByteArray): ByteArray {
        val result = ByteArray(data.size)
        var carry = 0
        for (i in data.indices.reversed()) {
            val b = data[i].toInt() and 0xFF
            result[i] = ((b shl 1) or carry).toByte()
            carry = if ((b and 0x80) != 0) 1 else 0
        }
        return result
    }
    
    private fun xorBytes(a: ByteArray, b: ByteArray): ByteArray {
        val result = ByteArray(maxOf(a.size, b.size))
        for (i in result.indices) {
            val av = if (i < a.size) a[i].toInt() and 0xFF else 0
            val bv = if (i < b.size) b[i].toInt() and 0xFF else 0
            result[i] = (av xor bv).toByte()
        }
        return result
    }
}

