package org.emvtools.util

/**
 * ISO8583 Message Parser and Builder
 */
object Iso8583Parser {
    
    data class Iso8583Message(
        val mti: String,
        val bitmap: String,
        val fields: Map<Int, String>
    ) {
        fun toHex(): String {
            val sb = StringBuilder()
            sb.append(mti)
            sb.append(bitmap)
            
            // Sort fields and append
            fields.toSortedMap().forEach { (fieldNum, value) ->
                val fieldDef = fieldDefinitions[fieldNum]
                if (fieldDef != null) {
                    when (fieldDef.lengthType) {
                        LengthType.FIXED -> sb.append(value)
                        LengthType.LLVAR -> {
                            sb.append(String.format("%02d", value.length / 2))
                            sb.append(value)
                        }
                        LengthType.LLLVAR -> {
                            sb.append(String.format("%03d", value.length / 2))
                            sb.append(value)
                        }
                    }
                }
            }
            
            return sb.toString()
        }
        
        fun toJson(): String {
            val sb = StringBuilder()
            sb.appendLine("{")
            sb.appendLine("""  "mti": "$mti",""")
            sb.appendLine("""  "bitmap": "$bitmap",""")
            sb.appendLine("""  "fields": {""")
            
            fields.toSortedMap().entries.forEachIndexed { index, (fieldNum, value) ->
                val fieldDef = fieldDefinitions[fieldNum]
                val name = fieldDef?.name ?: "Field $fieldNum"
                val comma = if (index < fields.size - 1) "," else ""
                sb.appendLine("""    "$fieldNum": { "name": "$name", "value": "$value" }$comma""")
            }
            
            sb.appendLine("  }")
            sb.appendLine("}")
            return sb.toString()
        }
    }
    
    enum class LengthType { FIXED, LLVAR, LLLVAR }
    enum class DataType { N, AN, ANS, B, Z }
    
    data class FieldDefinition(
        val number: Int,
        val name: String,
        val lengthType: LengthType,
        val maxLength: Int,
        val dataType: DataType
    )
    
    val fieldDefinitions = mapOf(
        2 to FieldDefinition(2, "Primary Account Number", LengthType.LLVAR, 19, DataType.N),
        3 to FieldDefinition(3, "Processing Code", LengthType.FIXED, 6, DataType.N),
        4 to FieldDefinition(4, "Transaction Amount", LengthType.FIXED, 12, DataType.N),
        7 to FieldDefinition(7, "Transmission Date/Time", LengthType.FIXED, 10, DataType.N),
        11 to FieldDefinition(11, "System Trace Audit Number", LengthType.FIXED, 6, DataType.N),
        12 to FieldDefinition(12, "Local Transaction Time", LengthType.FIXED, 6, DataType.N),
        13 to FieldDefinition(13, "Local Transaction Date", LengthType.FIXED, 4, DataType.N),
        14 to FieldDefinition(14, "Expiration Date", LengthType.FIXED, 4, DataType.N),
        18 to FieldDefinition(18, "Merchant Category Code", LengthType.FIXED, 4, DataType.N),
        22 to FieldDefinition(22, "POS Entry Mode", LengthType.FIXED, 3, DataType.N),
        23 to FieldDefinition(23, "Card Sequence Number", LengthType.FIXED, 3, DataType.N),
        25 to FieldDefinition(25, "POS Condition Code", LengthType.FIXED, 2, DataType.N),
        26 to FieldDefinition(26, "POS PIN Capture Code", LengthType.FIXED, 2, DataType.N),
        32 to FieldDefinition(32, "Acquiring Institution ID", LengthType.LLVAR, 11, DataType.N),
        35 to FieldDefinition(35, "Track 2 Data", LengthType.LLVAR, 37, DataType.Z),
        37 to FieldDefinition(37, "Retrieval Reference Number", LengthType.FIXED, 12, DataType.AN),
        38 to FieldDefinition(38, "Authorization ID Response", LengthType.FIXED, 6, DataType.AN),
        39 to FieldDefinition(39, "Response Code", LengthType.FIXED, 2, DataType.AN),
        41 to FieldDefinition(41, "Card Acceptor Terminal ID", LengthType.FIXED, 8, DataType.ANS),
        42 to FieldDefinition(42, "Card Acceptor ID", LengthType.FIXED, 15, DataType.ANS),
        43 to FieldDefinition(43, "Card Acceptor Name/Location", LengthType.FIXED, 40, DataType.ANS),
        48 to FieldDefinition(48, "Additional Data", LengthType.LLLVAR, 999, DataType.ANS),
        49 to FieldDefinition(49, "Transaction Currency Code", LengthType.FIXED, 3, DataType.N),
        52 to FieldDefinition(52, "PIN Data", LengthType.FIXED, 8, DataType.B),
        53 to FieldDefinition(53, "Security Related Control Info", LengthType.FIXED, 16, DataType.N),
        55 to FieldDefinition(55, "ICC Data (EMV)", LengthType.LLLVAR, 999, DataType.B),
        60 to FieldDefinition(60, "Reserved (National)", LengthType.LLLVAR, 999, DataType.ANS),
        61 to FieldDefinition(61, "Reserved (National)", LengthType.LLLVAR, 999, DataType.ANS),
        62 to FieldDefinition(62, "Reserved (Private)", LengthType.LLLVAR, 999, DataType.ANS),
        63 to FieldDefinition(63, "Reserved (Private)", LengthType.LLLVAR, 999, DataType.ANS),
        64 to FieldDefinition(64, "MAC", LengthType.FIXED, 8, DataType.B),
        70 to FieldDefinition(70, "Network Management Info Code", LengthType.FIXED, 3, DataType.N),
        90 to FieldDefinition(90, "Original Data Elements", LengthType.FIXED, 42, DataType.N),
        95 to FieldDefinition(95, "Replacement Amounts", LengthType.FIXED, 42, DataType.AN),
        100 to FieldDefinition(100, "Receiving Institution ID", LengthType.LLVAR, 11, DataType.N),
        102 to FieldDefinition(102, "Account ID 1", LengthType.LLVAR, 28, DataType.ANS),
        103 to FieldDefinition(103, "Account ID 2", LengthType.LLVAR, 28, DataType.ANS),
        128 to FieldDefinition(128, "MAC 2", LengthType.FIXED, 8, DataType.B)
    )
    
    val mtiDescriptions = mapOf(
        "0100" to "Authorization Request",
        "0110" to "Authorization Response",
        "0120" to "Authorization Advice",
        "0130" to "Authorization Advice Response",
        "0200" to "Financial Request",
        "0210" to "Financial Response",
        "0220" to "Financial Advice",
        "0230" to "Financial Advice Response",
        "0400" to "Reversal Request",
        "0410" to "Reversal Response",
        "0420" to "Reversal Advice",
        "0430" to "Reversal Advice Response",
        "0500" to "Batch Settlement Request",
        "0510" to "Batch Settlement Response",
        "0800" to "Network Management Request",
        "0810" to "Network Management Response"
    )
    
    /**
     * Parse ISO8583 message from hex string
     */
    fun parse(hex: String): Iso8583Message {
        var pos = 0
        
        // MTI (4 bytes = 8 hex chars for BCD, or 4 chars for ASCII)
        val mti = hex.substring(pos, pos + 4)
        pos += 4
        
        // Primary bitmap (8 bytes = 16 hex chars)
        val primaryBitmap = hex.substring(pos, pos + 16)
        pos += 16
        
        // Check for secondary bitmap
        val hasSecondaryBitmap = (primaryBitmap[0].digitToInt(16) and 0x8) != 0
        val secondaryBitmap = if (hasSecondaryBitmap) {
            val sb = hex.substring(pos, pos + 16)
            pos += 16
            sb
        } else ""
        
        val fullBitmap = primaryBitmap + secondaryBitmap
        val fields = mutableMapOf<Int, String>()
        
        // Parse fields based on bitmap
        for (fieldNum in 2..128) {
            if (isBitSet(fullBitmap, fieldNum)) {
                val fieldDef = fieldDefinitions[fieldNum]
                if (fieldDef != null) {
                    val (value, newPos) = parseField(hex, pos, fieldDef)
                    fields[fieldNum] = value
                    pos = newPos
                }
            }
        }
        
        return Iso8583Message(mti, fullBitmap, fields)
    }
    
    /**
     * Build ISO8583 message
     */
    fun build(mti: String, fields: Map<Int, String>): Iso8583Message {
        // Calculate bitmap
        val bitmap = calculateBitmap(fields.keys)
        return Iso8583Message(mti, bitmap, fields)
    }
    
    private fun isBitSet(bitmap: String, bitNum: Int): Boolean {
        if (bitNum < 1 || bitNum > bitmap.length * 4) return false
        val byteIndex = (bitNum - 1) / 4
        val bitIndex = 3 - ((bitNum - 1) % 4)
        if (byteIndex >= bitmap.length) return false
        val nibble = bitmap[byteIndex].digitToInt(16)
        return (nibble and (1 shl bitIndex)) != 0
    }
    
    private fun parseField(hex: String, pos: Int, fieldDef: FieldDefinition): Pair<String, Int> {
        var currentPos = pos
        
        val length = when (fieldDef.lengthType) {
            LengthType.FIXED -> fieldDef.maxLength
            LengthType.LLVAR -> {
                val len = hex.substring(currentPos, currentPos + 2).toInt()
                currentPos += 2
                len
            }
            LengthType.LLLVAR -> {
                val len = hex.substring(currentPos, currentPos + 3).toInt()
                currentPos += 3
                len
            }
        }
        
        val dataLength = if (fieldDef.dataType == DataType.B) length * 2 else length
        val value = hex.substring(currentPos, minOf(currentPos + dataLength, hex.length))
        
        return Pair(value, currentPos + dataLength)
    }
    
    private fun calculateBitmap(fieldNumbers: Set<Int>): String {
        val hasSecondary = fieldNumbers.any { it > 64 }
        val bitmapBytes = if (hasSecondary) 16 else 8
        val bitmap = ByteArray(bitmapBytes)
        
        if (hasSecondary) {
            bitmap[0] = (bitmap[0].toInt() or 0x80).toByte() // Set bit 1 for secondary bitmap
        }
        
        fieldNumbers.forEach { fieldNum ->
            if (fieldNum in 2..128) {
                val byteIndex = (fieldNum - 1) / 8
                val bitIndex = 7 - ((fieldNum - 1) % 8)
                if (byteIndex < bitmap.size) {
                    bitmap[byteIndex] = (bitmap[byteIndex].toInt() or (1 shl bitIndex)).toByte()
                }
            }
        }
        
        return HexUtils.bytesToHex(bitmap)
    }
}

