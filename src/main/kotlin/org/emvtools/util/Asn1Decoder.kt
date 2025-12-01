package org.emvtools.util

/**
 * ASN.1 DER Decoder
 */
object Asn1Decoder {
    
    /**
     * Decode ASN.1 DER encoded data
     */
    fun decode(hexData: String): Result<Asn1Node> {
        return try {
            val cleanHex = hexData.replace(" ", "").replace("\n", "").uppercase()
            if (!HexUtils.isValidHex(cleanHex)) {
                return Result.failure(IllegalArgumentException("Invalid hex string"))
            }
            
            val bytes = HexUtils.hexToBytes(cleanHex)
            val result = parseNode(bytes, 0)
            Result.success(result.first)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse a single ASN.1 node
     */
    private fun parseNode(bytes: ByteArray, offset: Int): Pair<Asn1Node, Int> {
        var pos = offset
        
        // Parse tag
        val tagByte = bytes[pos].toInt() and 0xFF
        val tagClass = Asn1TagClass.fromByte(tagByte)
        val isConstructed = (tagByte and 0x20) != 0
        
        var tag = tagByte
        pos++
        
        // Multi-byte tag
        if ((tagByte and 0x1F) == 0x1F) {
            tag = tagByte
            while (pos < bytes.size) {
                val nextByte = bytes[pos].toInt() and 0xFF
                tag = (tag shl 8) or nextByte
                pos++
                if ((nextByte and 0x80) == 0) break
            }
        }
        
        // Parse length
        val lengthResult = parseLength(bytes, pos)
        val length = lengthResult.first
        pos = lengthResult.second
        
        // Parse value
        val valueBytes = bytes.copyOfRange(pos, pos + length)
        val value = HexUtils.bytesToHex(valueBytes)
        
        // Parse children if constructed
        val children = if (isConstructed && length > 0) {
            val childList = mutableListOf<Asn1Node>()
            var childPos = 0
            while (childPos < valueBytes.size) {
                val childResult = parseNode(valueBytes, childPos)
                childList.add(childResult.first)
                childPos = childResult.second
            }
            childList
        } else {
            emptyList()
        }
        
        val tagType = getTagType(tag, tagClass)
        val decodedValue = if (!isConstructed) decodeValue(valueBytes, tagType) else null
        
        val node = Asn1Node(
            tag = tag,
            tagHex = String.format("%02X", tag),
            tagClass = tagClass,
            tagType = tagType,
            isConstructed = isConstructed,
            length = length,
            value = value,
            decodedValue = decodedValue,
            children = children
        )
        
        return Pair(node, pos + length)
    }
    
    /**
     * Parse BER length
     */
    private fun parseLength(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        val firstByte = bytes[offset].toInt() and 0xFF
        
        return when {
            firstByte <= 0x7F -> Pair(firstByte, offset + 1)
            firstByte == 0x81 -> Pair(bytes[offset + 1].toInt() and 0xFF, offset + 2)
            firstByte == 0x82 -> {
                val len = ((bytes[offset + 1].toInt() and 0xFF) shl 8) or (bytes[offset + 2].toInt() and 0xFF)
                Pair(len, offset + 3)
            }
            firstByte == 0x83 -> {
                val len = ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                        (bytes[offset + 3].toInt() and 0xFF)
                Pair(len, offset + 4)
            }
            else -> Pair(0, offset + 1)
        }
    }
    
    /**
     * Get tag type name
     */
    private fun getTagType(tag: Int, tagClass: Asn1TagClass): String {
        if (tagClass != Asn1TagClass.UNIVERSAL) {
            return "[$tag]"
        }
        
        return when (tag) {
            0x01 -> "BOOLEAN"
            0x02 -> "INTEGER"
            0x03 -> "BIT STRING"
            0x04 -> "OCTET STRING"
            0x05 -> "NULL"
            0x06 -> "OBJECT IDENTIFIER"
            0x07 -> "ObjectDescriptor"
            0x08 -> "EXTERNAL"
            0x09 -> "REAL"
            0x0A -> "ENUMERATED"
            0x0B -> "EMBEDDED PDV"
            0x0C -> "UTF8String"
            0x0D -> "RELATIVE-OID"
            0x10 -> "SEQUENCE"
            0x11 -> "SET"
            0x12 -> "NumericString"
            0x13 -> "PrintableString"
            0x14 -> "T61String"
            0x15 -> "VideotexString"
            0x16 -> "IA5String"
            0x17 -> "UTCTime"
            0x18 -> "GeneralizedTime"
            0x19 -> "GraphicString"
            0x1A -> "VisibleString"
            0x1B -> "GeneralString"
            0x1C -> "UniversalString"
            0x1D -> "CHARACTER STRING"
            0x1E -> "BMPString"
            0x30 -> "SEQUENCE"
            0x31 -> "SET"
            else -> "UNKNOWN ($tag)"
        }
    }
    
    /**
     * Decode value based on tag type
     */
    private fun decodeValue(bytes: ByteArray, tagType: String): String? {
        return try {
            when (tagType) {
                "BOOLEAN" -> if (bytes.isNotEmpty() && bytes[0].toInt() != 0) "TRUE" else "FALSE"
                "INTEGER" -> {
                    if (bytes.size <= 8) {
                        var value = 0L
                        for (b in bytes) {
                            value = (value shl 8) or (b.toLong() and 0xFF)
                        }
                        // Handle negative numbers
                        if (bytes.isNotEmpty() && (bytes[0].toInt() and 0x80) != 0) {
                            value = value - (1L shl (bytes.size * 8))
                        }
                        value.toString()
                    } else {
                        HexUtils.bytesToHex(bytes)
                    }
                }
                "BIT STRING" -> {
                    if (bytes.isNotEmpty()) {
                        val unusedBits = bytes[0].toInt() and 0xFF
                        "($unusedBits unused) ${HexUtils.bytesToHex(bytes.copyOfRange(1, bytes.size))}"
                    } else null
                }
                "OCTET STRING" -> HexUtils.bytesToHex(bytes)
                "NULL" -> "NULL"
                "OBJECT IDENTIFIER" -> decodeOid(bytes)
                "UTF8String", "PrintableString", "IA5String", "VisibleString" -> String(bytes, Charsets.UTF_8)
                "UTCTime" -> String(bytes, Charsets.US_ASCII)
                "GeneralizedTime" -> String(bytes, Charsets.US_ASCII)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Decode OID
     */
    private fun decodeOid(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        
        val components = mutableListOf<Long>()
        
        // First byte encodes first two components
        val firstByte = bytes[0].toInt() and 0xFF
        components.add((firstByte / 40).toLong())
        components.add((firstByte % 40).toLong())
        
        // Remaining bytes
        var value = 0L
        for (i in 1 until bytes.size) {
            val b = bytes[i].toInt() and 0xFF
            value = (value shl 7) or (b.toLong() and 0x7F)
            if ((b and 0x80) == 0) {
                components.add(value)
                value = 0
            }
        }
        
        return components.joinToString(".")
    }
    
    /**
     * Format ASN.1 tree as string
     */
    fun format(node: Asn1Node, indent: Int = 0): String {
        val sb = StringBuilder()
        val indentStr = "  ".repeat(indent)
        
        sb.append("${indentStr}${node.tagType}")
        if (node.tagClass != Asn1TagClass.UNIVERSAL) {
            sb.append(" [${node.tagClass}]")
        }
        sb.append(" (${node.length} bytes)")
        
        if (node.isConstructed) {
            sb.appendLine()
            for (child in node.children) {
                sb.append(format(child, indent + 1))
            }
        } else {
            if (node.decodedValue != null) {
                sb.appendLine(": ${node.decodedValue}")
            } else {
                sb.appendLine(": ${HexUtils.formatHex(node.value)}")
            }
        }
        
        return sb.toString()
    }
}

data class Asn1Node(
    val tag: Int,
    val tagHex: String,
    val tagClass: Asn1TagClass,
    val tagType: String,
    val isConstructed: Boolean,
    val length: Int,
    val value: String,
    val decodedValue: String?,
    val children: List<Asn1Node>
)

enum class Asn1TagClass {
    UNIVERSAL,
    APPLICATION,
    CONTEXT_SPECIFIC,
    PRIVATE;
    
    companion object {
        fun fromByte(byte: Int): Asn1TagClass {
            return when ((byte shr 6) and 0x03) {
                0 -> UNIVERSAL
                1 -> APPLICATION
                2 -> CONTEXT_SPECIFIC
                3 -> PRIVATE
                else -> UNIVERSAL
            }
        }
    }
}

