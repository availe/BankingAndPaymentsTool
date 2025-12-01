package org.emvtools.crypto

import org.emvtools.util.HexUtils
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * DUKPT (Derived Unique Key Per Transaction) Calculator
 * Implements ANSI X9.24-1 key derivation
 */
object DukptCalculator {
    
    // Masks for key derivation
    private const val KEY_REGISTER_BITMASK = "C0C0C0C000000000C0C0C0C000000000"
    private const val DATA_ENCRYPTION_REQUEST_MASK = "0000000000FF00000000000000FF0000"
    private const val MAC_REQUEST_MASK = "000000000000FF00000000000000FF00"
    private const val PIN_ENCRYPTION_MASK = "00000000000000FF00000000000000FF"
    
    data class DukptKeySet(
        val ipek: String,
        val ksn: String,
        val currentKey: String,
        val pinEncryptionKey: String,
        val macKey: String,
        val dataEncryptionKey: String
    )
    
    /**
     * Derive IPEK (Initial PIN Encryption Key) from BDK and KSN
     */
    fun deriveIpek(bdk: String, ksn: String): String {
        val ksnBytes = HexUtils.hexToBytes(ksn)
        // Clear counter (last 21 bits)
        val maskedKsn = ksnBytes.copyOf()
        maskedKsn[7] = (maskedKsn[7].toInt() and 0xE0).toByte()
        maskedKsn[8] = 0
        maskedKsn[9] = 0
        
        val maskedKsnHex = HexUtils.bytesToHex(maskedKsn.take(8).toByteArray())
        
        // Left half of IPEK
        val leftIpek = tripleDes(bdk, maskedKsnHex)
        
        // Right half of IPEK (XOR BDK with mask, then encrypt)
        val bdkXored = xorHex(bdk, KEY_REGISTER_BITMASK)
        val rightIpek = tripleDes(bdkXored, maskedKsnHex)
        
        return leftIpek + rightIpek
    }
    
    /**
     * Derive current transaction key from IPEK and KSN
     */
    fun deriveCurrentKey(ipek: String, ksn: String): String {
        val ksnBytes = HexUtils.hexToBytes(ksn)
        val counter = ((ksnBytes[7].toInt() and 0x1F) shl 16) or
                     ((ksnBytes[8].toInt() and 0xFF) shl 8) or
                     (ksnBytes[9].toInt() and 0xFF)
        
        // Clear counter from KSN
        val baseKsn = ksnBytes.copyOf()
        baseKsn[7] = (baseKsn[7].toInt() and 0xE0).toByte()
        baseKsn[8] = 0
        baseKsn[9] = 0
        
        var currentKey = ipek
        var currentKsn = baseKsn.copyOf()
        
        // Process each bit of counter
        var shiftReg = 0x100000
        while (shiftReg > 0) {
            if ((counter and shiftReg) != 0) {
                // Set bit in KSN
                val bitPos = 21 - Integer.numberOfTrailingZeros(Integer.highestOneBit(shiftReg))
                val byteIndex = 7 + (bitPos / 8)
                val bitIndex = 7 - (bitPos % 8)
                if (byteIndex < currentKsn.size) {
                    currentKsn[byteIndex] = (currentKsn[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
                
                // Derive new key
                currentKey = deriveKey(currentKey, HexUtils.bytesToHex(currentKsn.take(8).toByteArray()))
            }
            shiftReg = shiftReg shr 1
        }
        
        return currentKey
    }
    
    /**
     * Derive PIN encryption key from current key
     */
    fun derivePinKey(currentKey: String): String {
        val variant = xorHex(currentKey, PIN_ENCRYPTION_MASK)
        return variant
    }
    
    /**
     * Derive MAC key from current key
     */
    fun deriveMacKey(currentKey: String): String {
        val variant = xorHex(currentKey, MAC_REQUEST_MASK)
        return variant
    }
    
    /**
     * Derive data encryption key from current key
     */
    fun deriveDataKey(currentKey: String): String {
        val variant = xorHex(currentKey, DATA_ENCRYPTION_REQUEST_MASK)
        // Encrypt variant with itself for data key
        val leftKey = variant.take(16)
        val rightKey = variant.drop(16)
        val encLeft = tripleDes(variant, leftKey)
        val encRight = tripleDes(variant, rightKey)
        return encLeft + encRight
    }
    
    /**
     * Get complete DUKPT key set
     */
    fun getKeySet(bdk: String, ksn: String): DukptKeySet {
        val ipek = deriveIpek(bdk, ksn)
        val currentKey = deriveCurrentKey(ipek, ksn)
        
        return DukptKeySet(
            ipek = ipek,
            ksn = ksn,
            currentKey = currentKey,
            pinEncryptionKey = derivePinKey(currentKey),
            macKey = deriveMacKey(currentKey),
            dataEncryptionKey = deriveDataKey(currentKey)
        )
    }
    
    /**
     * Encrypt PIN block using DUKPT
     */
    fun encryptPinBlock(pinBlock: String, bdk: String, ksn: String): String {
        val keySet = getKeySet(bdk, ksn)
        return tripleDes(keySet.pinEncryptionKey, pinBlock)
    }
    
    /**
     * Decrypt PIN block using DUKPT
     */
    fun decryptPinBlock(encryptedPinBlock: String, bdk: String, ksn: String): String {
        val keySet = getKeySet(bdk, ksn)
        return tripleDesDecrypt(keySet.pinEncryptionKey, encryptedPinBlock)
    }
    
    /**
     * Increment KSN counter
     */
    fun incrementKsn(ksn: String): String {
        val ksnBytes = HexUtils.hexToBytes(ksn).toMutableList()
        
        // Increment last 21 bits
        var carry = 1
        for (i in 9 downTo 7) {
            val mask = if (i == 7) 0x1F else 0xFF
            val value = (ksnBytes[i].toInt() and mask) + carry
            ksnBytes[i] = ((ksnBytes[i].toInt() and mask.inv()) or (value and mask)).toByte()
            carry = if (value > mask) 1 else 0
        }
        
        return HexUtils.bytesToHex(ksnBytes.toByteArray())
    }
    
    /**
     * Parse KSN components
     */
    fun parseKsn(ksn: String): Triple<String, String, Int> {
        val ksnBytes = HexUtils.hexToBytes(ksn)
        val iksn = HexUtils.bytesToHex(ksnBytes.take(5).toByteArray()) // Initial Key Serial Number
        val deviceId = HexUtils.bytesToHex(ksnBytes.slice(5..6).toByteArray())
        val counter = ((ksnBytes[7].toInt() and 0x1F) shl 16) or
                     ((ksnBytes[8].toInt() and 0xFF) shl 8) or
                     (ksnBytes[9].toInt() and 0xFF)
        return Triple(iksn, deviceId, counter)
    }
    
    // Helper functions
    
    private fun deriveKey(key: String, data: String): String {
        val keyXored = xorHex(key, KEY_REGISTER_BITMASK)
        
        // Encrypt right half of data with key
        val rightData = data.takeLast(16).padStart(16, '0')
        val leftKey = key.take(16)
        val rightKey = key.drop(16).take(16)
        
        // Left half
        val curKeyLeft = xorHex(rightData, rightKey)
        val encLeft = des(leftKey, curKeyLeft)
        val newLeft = xorHex(encLeft, rightKey)
        
        // Right half (with XORed key)
        val leftKeyXored = keyXored.take(16)
        val rightKeyXored = keyXored.drop(16).take(16)
        val curKeyRight = xorHex(rightData, rightKeyXored)
        val encRight = des(leftKeyXored, curKeyRight)
        val newRight = xorHex(encRight, rightKeyXored)
        
        return newLeft + newRight
    }
    
    private fun des(key: String, data: String): String {
        val keyBytes = HexUtils.hexToBytes(key.take(16))
        val dataBytes = HexUtils.hexToBytes(data.take(16))
        
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "DES"))
        
        return HexUtils.bytesToHex(cipher.doFinal(dataBytes))
    }
    
    private fun tripleDes(key: String, data: String): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(data.take(16))
        
        val fullKey = if (keyBytes.size == 16) {
            keyBytes + keyBytes.take(8)
        } else {
            keyBytes
        }
        
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fullKey, "DESede"))
        
        return HexUtils.bytesToHex(cipher.doFinal(dataBytes))
    }
    
    private fun tripleDesDecrypt(key: String, data: String): String {
        val keyBytes = HexUtils.hexToBytes(key)
        val dataBytes = HexUtils.hexToBytes(data.take(16))
        
        val fullKey = if (keyBytes.size == 16) {
            keyBytes + keyBytes.take(8)
        } else {
            keyBytes
        }
        
        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(fullKey, "DESede"))
        
        return HexUtils.bytesToHex(cipher.doFinal(dataBytes))
    }
    
    private fun xorHex(a: String, b: String): String {
        val aBytes = HexUtils.hexToBytes(a)
        val bBytes = HexUtils.hexToBytes(b)
        val result = ByteArray(maxOf(aBytes.size, bBytes.size))
        
        for (i in result.indices) {
            val av = if (i < aBytes.size) aBytes[i].toInt() and 0xFF else 0
            val bv = if (i < bBytes.size) bBytes[i].toInt() and 0xFF else 0
            result[i] = (av xor bv).toByte()
        }
        
        return HexUtils.bytesToHex(result)
    }
}

