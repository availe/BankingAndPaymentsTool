package org.emvtools.util

/**
 * Utility functions for hex string manipulation
 */
object HexUtils {
    
    private val HEX_CHARS = "0123456789ABCDEF".toCharArray()
    
    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray): String {
        val result = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val i = byte.toInt()
            result.append(HEX_CHARS[(i shr 4) and 0x0F])
            result.append(HEX_CHARS[i and 0x0F])
        }
        return result.toString()
    }
    
    /**
     * Convert hex string to byte array
     */
    fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        require(cleanHex.length % 2 == 0) { "Hex string must have even length" }
        require(cleanHex.all { it in '0'..'9' || it in 'A'..'F' }) { "Invalid hex character" }
        
        return ByteArray(cleanHex.length / 2) { i ->
            val index = i * 2
            ((Character.digit(cleanHex[index], 16) shl 4) + Character.digit(cleanHex[index + 1], 16)).toByte()
        }
    }
    
    /**
     * Check if string is valid hex
     */
    fun isValidHex(hex: String): Boolean {
        val cleanHex = hex.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
        return cleanHex.length % 2 == 0 && cleanHex.all { it in '0'..'9' || it in 'A'..'F' }
    }
    
    /**
     * Format hex string with spaces every 2 characters
     */
    fun formatHex(hex: String, groupSize: Int = 2): String {
        val cleanHex = hex.replace(" ", "").uppercase()
        return cleanHex.chunked(groupSize).joinToString(" ")
    }
    
    /**
     * XOR two hex strings
     */
    fun xorHex(hex1: String, hex2: String): String {
        val bytes1 = hexToBytes(hex1)
        val bytes2 = hexToBytes(hex2)
        require(bytes1.size == bytes2.size) { "Hex strings must have same length" }
        
        return bytesToHex(ByteArray(bytes1.size) { i ->
            (bytes1[i].toInt() xor bytes2[i].toInt()).toByte()
        })
    }
    
    /**
     * Pad hex string to specified length (in bytes)
     */
    fun padHex(hex: String, length: Int, padChar: Char = '0', padRight: Boolean = true): String {
        val cleanHex = hex.replace(" ", "").uppercase()
        val targetLength = length * 2
        return if (cleanHex.length >= targetLength) {
            cleanHex.take(targetLength)
        } else {
            val padding = padChar.toString().repeat(targetLength - cleanHex.length)
            if (padRight) cleanHex + padding else padding + cleanHex
        }
    }
    
    /**
     * Convert ASCII string to hex
     */
    fun asciiToHex(ascii: String): String {
        return bytesToHex(ascii.toByteArray(Charsets.US_ASCII))
    }
    
    /**
     * Convert hex to ASCII string
     */
    fun hexToAscii(hex: String): String {
        return String(hexToBytes(hex), Charsets.US_ASCII)
    }
    
    /**
     * Convert hex to binary string
     */
    fun hexToBinary(hex: String): String {
        val cleanHex = hex.replace(" ", "").uppercase()
        return cleanHex.map { char ->
            Integer.toBinaryString(Character.digit(char, 16)).padStart(4, '0')
        }.joinToString("")
    }
    
    /**
     * Convert binary string to hex
     */
    fun binaryToHex(binary: String): String {
        val cleanBinary = binary.replace(" ", "")
        require(cleanBinary.length % 4 == 0) { "Binary string length must be multiple of 4" }
        return cleanBinary.chunked(4).map { chunk ->
            Integer.parseInt(chunk, 2).toString(16).uppercase()
        }.joinToString("")
    }
    
    /**
     * Reverse byte order of hex string
     */
    fun reverseBytes(hex: String): String {
        val cleanHex = hex.replace(" ", "").uppercase()
        return cleanHex.chunked(2).reversed().joinToString("")
    }
    
    /**
     * Calculate simple checksum (XOR of all bytes)
     */
    fun calculateXorChecksum(hex: String): String {
        val bytes = hexToBytes(hex)
        var checksum = 0
        for (byte in bytes) {
            checksum = checksum xor (byte.toInt() and 0xFF)
        }
        return String.format("%02X", checksum)
    }
    
    /**
     * Calculate LRC (Longitudinal Redundancy Check)
     */
    fun calculateLrc(hex: String): String {
        return calculateXorChecksum(hex)
    }
}

