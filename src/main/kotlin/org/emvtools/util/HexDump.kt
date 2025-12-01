package org.emvtools.util

/**
 * Hex dump utility with formatting options
 */
object HexDump {
    
    /**
     * Create a formatted hex dump of the input data
     */
    fun dump(
        hexData: String,
        bytesPerLine: Int = 16,
        showAscii: Boolean = true,
        showOffset: Boolean = true,
        uppercase: Boolean = true
    ): String {
        val cleanHex = hexData.replace(" ", "").replace("\n", "").replace("\r", "")
        
        if (!HexUtils.isValidHex(cleanHex)) {
            return "Invalid hex data"
        }
        
        val bytes = HexUtils.hexToBytes(cleanHex)
        val sb = StringBuilder()
        
        var offset = 0
        while (offset < bytes.size) {
            val lineBytes = bytes.copyOfRange(offset, minOf(offset + bytesPerLine, bytes.size))
            
            // Offset
            if (showOffset) {
                sb.append(String.format("%08X  ", offset))
            }
            
            // Hex bytes
            for (i in 0 until bytesPerLine) {
                if (i < lineBytes.size) {
                    val hex = String.format("%02X", lineBytes[i].toInt() and 0xFF)
                    sb.append(if (uppercase) hex else hex.lowercase())
                    sb.append(" ")
                } else {
                    sb.append("   ")
                }
                
                // Extra space in middle
                if (i == 7) sb.append(" ")
            }
            
            // ASCII representation
            if (showAscii) {
                sb.append(" |")
                for (byte in lineBytes) {
                    val char = byte.toInt() and 0xFF
                    sb.append(if (char in 0x20..0x7E) char.toChar() else '.')
                }
                // Pad if line is short
                repeat(bytesPerLine - lineBytes.size) {
                    sb.append(' ')
                }
                sb.append("|")
            }
            
            sb.appendLine()
            offset += bytesPerLine
        }
        
        return sb.toString()
    }
    
    /**
     * Create a compact hex dump (just hex with spaces)
     */
    fun dumpCompact(hexData: String, groupSize: Int = 2): String {
        val cleanHex = hexData.replace(" ", "").replace("\n", "").uppercase()
        return cleanHex.chunked(groupSize).joinToString(" ")
    }
    
    /**
     * Create hex dump with annotations for specific byte ranges
     */
    fun dumpAnnotated(
        hexData: String,
        annotations: List<HexAnnotation>,
        bytesPerLine: Int = 16
    ): String {
        val cleanHex = hexData.replace(" ", "").replace("\n", "").uppercase()
        val bytes = HexUtils.hexToBytes(cleanHex)
        val sb = StringBuilder()
        
        // First, create the standard dump
        sb.appendLine(dump(cleanHex, bytesPerLine))
        sb.appendLine()
        sb.appendLine("Annotations:")
        sb.appendLine("-".repeat(60))
        
        // Add annotations
        for (annotation in annotations.sortedBy { it.startByte }) {
            val startHex = String.format("%08X", annotation.startByte)
            val endHex = String.format("%08X", annotation.endByte)
            val length = annotation.endByte - annotation.startByte + 1
            
            sb.appendLine("$startHex - $endHex ($length bytes): ${annotation.description}")
            
            if (annotation.startByte < bytes.size) {
                val endIndex = minOf(annotation.endByte + 1, bytes.size)
                val annotatedBytes = bytes.copyOfRange(annotation.startByte, endIndex)
                sb.appendLine("  Value: ${HexUtils.bytesToHex(annotatedBytes)}")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Analyze hex data and provide statistics
     */
    fun analyze(hexData: String): HexAnalysis {
        val cleanHex = hexData.replace(" ", "").replace("\n", "").uppercase()
        val bytes = HexUtils.hexToBytes(cleanHex)
        
        // Calculate byte frequency
        val frequency = IntArray(256)
        for (byte in bytes) {
            frequency[byte.toInt() and 0xFF]++
        }
        
        // Find most common bytes
        val mostCommon = frequency.indices
            .sortedByDescending { frequency[it] }
            .take(10)
            .filter { frequency[it] > 0 }
            .map { ByteFrequency(it, frequency[it]) }
        
        // Calculate entropy
        val entropy = calculateEntropy(bytes)
        
        // Count printable ASCII
        val printableCount = bytes.count { (it.toInt() and 0xFF) in 0x20..0x7E }
        
        // Detect patterns
        val patterns = detectPatterns(bytes)
        
        return HexAnalysis(
            totalBytes = bytes.size,
            uniqueBytes = frequency.count { it > 0 },
            entropy = entropy,
            printableAsciiPercent = if (bytes.isNotEmpty()) (printableCount * 100.0 / bytes.size) else 0.0,
            mostCommonBytes = mostCommon,
            patterns = patterns
        )
    }
    
    /**
     * Calculate Shannon entropy
     */
    private fun calculateEntropy(bytes: ByteArray): Double {
        if (bytes.isEmpty()) return 0.0
        
        val frequency = IntArray(256)
        for (byte in bytes) {
            frequency[byte.toInt() and 0xFF]++
        }
        
        var entropy = 0.0
        val total = bytes.size.toDouble()
        
        for (count in frequency) {
            if (count > 0) {
                val probability = count / total
                entropy -= probability * kotlin.math.log2(probability)
            }
        }
        
        return entropy
    }
    
    /**
     * Detect common patterns in data
     */
    private fun detectPatterns(bytes: ByteArray): List<String> {
        val patterns = mutableListOf<String>()
        
        // Check for all zeros
        if (bytes.all { it.toInt() == 0 }) {
            patterns.add("All zeros")
        }
        
        // Check for all FFs
        if (bytes.all { (it.toInt() and 0xFF) == 0xFF }) {
            patterns.add("All 0xFF")
        }
        
        // Check for repeating pattern
        if (bytes.size >= 4) {
            val first = bytes[0]
            if (bytes.all { it == first }) {
                patterns.add("Single byte repeated: ${String.format("%02X", first.toInt() and 0xFF)}")
            }
        }
        
        // Check for incrementing sequence
        if (bytes.size >= 4) {
            var isIncrementing = true
            for (i in 1 until bytes.size) {
                if ((bytes[i].toInt() and 0xFF) != ((bytes[i-1].toInt() and 0xFF) + 1) % 256) {
                    isIncrementing = false
                    break
                }
            }
            if (isIncrementing) {
                patterns.add("Incrementing sequence")
            }
        }
        
        return patterns
    }
    
    /**
     * Compare two hex strings and show differences
     */
    fun compare(hex1: String, hex2: String): String {
        val clean1 = hex1.replace(" ", "").uppercase()
        val clean2 = hex2.replace(" ", "").uppercase()
        
        val bytes1 = HexUtils.hexToBytes(clean1)
        val bytes2 = HexUtils.hexToBytes(clean2)
        
        val sb = StringBuilder()
        val maxLen = maxOf(bytes1.size, bytes2.size)
        
        sb.appendLine("Comparison (${bytes1.size} bytes vs ${bytes2.size} bytes):")
        sb.appendLine("-".repeat(70))
        
        var diffCount = 0
        for (i in 0 until maxLen) {
            val b1 = if (i < bytes1.size) bytes1[i] else null
            val b2 = if (i < bytes2.size) bytes2[i] else null
            
            if (b1 != b2) {
                diffCount++
                val hex1Str = b1?.let { String.format("%02X", it.toInt() and 0xFF) } ?: "--"
                val hex2Str = b2?.let { String.format("%02X", it.toInt() and 0xFF) } ?: "--"
                sb.appendLine(String.format("Offset %08X: %s != %s", i, hex1Str, hex2Str))
            }
        }
        
        if (diffCount == 0) {
            sb.appendLine("Data is identical")
        } else {
            sb.appendLine("-".repeat(70))
            sb.appendLine("Total differences: $diffCount bytes")
        }
        
        return sb.toString()
    }
}

data class HexAnnotation(
    val startByte: Int,
    val endByte: Int,
    val description: String
)

data class ByteFrequency(
    val byteValue: Int,
    val count: Int
)

data class HexAnalysis(
    val totalBytes: Int,
    val uniqueBytes: Int,
    val entropy: Double,
    val printableAsciiPercent: Double,
    val mostCommonBytes: List<ByteFrequency>,
    val patterns: List<String>
)

