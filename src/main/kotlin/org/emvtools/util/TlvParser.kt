package org.emvtools.util

import org.emvtools.data.EmvTagDatabase
import org.emvtools.model.TlvData
import org.emvtools.model.TlvParseResult

/**
 * EMV TLV (Tag-Length-Value) Parser
 */
object TlvParser {
    
    /**
     * Parse TLV encoded hex string
     */
    fun parse(hexData: String): TlvParseResult {
        return try {
            val cleanHex = hexData.replace(" ", "").replace("\n", "").replace("\r", "").uppercase()
            if (!HexUtils.isValidHex(cleanHex)) {
                return TlvParseResult.Error("Invalid hex string")
            }
            
            val tlvList = mutableListOf<TlvData>()
            var position = 0
            
            while (position < cleanHex.length) {
                val result = parseTag(cleanHex, position)
                if (result == null) {
                    return TlvParseResult.Error("Failed to parse tag at position $position", position)
                }
                
                tlvList.add(result.first)
                position = result.second
            }
            
            TlvParseResult.Success(tlvList)
        } catch (e: Exception) {
            TlvParseResult.Error("Parse error: ${e.message}")
        }
    }
    
    /**
     * Parse a single TLV tag starting at the given position
     * Returns the parsed TlvData and the new position after parsing
     */
    private fun parseTag(hex: String, startPos: Int): Pair<TlvData, Int>? {
        var pos = startPos
        
        // Skip padding bytes (00 or FF)
        while (pos < hex.length - 1) {
            val byte = hex.substring(pos, pos + 2)
            if (byte != "00" && byte != "FF") break
            pos += 2
        }
        
        if (pos >= hex.length) return null
        
        // Parse tag
        val tagStart = pos
        val firstByte = hex.substring(pos, pos + 2).toInt(16)
        pos += 2
        
        // Check if tag is multi-byte (bits 1-5 of first byte are all 1s)
        if ((firstByte and 0x1F) == 0x1F) {
            // Multi-byte tag
            while (pos < hex.length) {
                val nextByte = hex.substring(pos, pos + 2).toInt(16)
                pos += 2
                // If bit 8 is 0, this is the last byte of the tag
                if ((nextByte and 0x80) == 0) break
            }
        }
        
        val tag = hex.substring(tagStart, pos)
        
        if (pos >= hex.length) return null
        
        // Parse length
        val lengthResult = parseLength(hex, pos) ?: return null
        val length = lengthResult.first
        pos = lengthResult.second
        
        // Check if we have enough data
        val valueLength = length * 2 // Convert bytes to hex chars
        if (pos + valueLength > hex.length) {
            return null
        }
        
        val value = if (length > 0) hex.substring(pos, pos + valueLength) else ""
        val rawData = hex.substring(tagStart, pos + valueLength)
        
        // Check if tag is constructed (bit 6 of first byte is 1)
        val isConstructed = (firstByte and 0x20) != 0
        
        // Get tag info from database
        val tagInfo = EmvTagDatabase.getTag(tag)
        
        // Parse children if constructed
        val children = if (isConstructed && value.isNotEmpty()) {
            when (val childResult = parse(value)) {
                is TlvParseResult.Success -> childResult.tlvList
                is TlvParseResult.Error -> emptyList()
            }
        } else {
            emptyList()
        }
        
        val tlvData = TlvData(
            tag = tag,
            length = length,
            value = value,
            rawData = rawData,
            children = children,
            isConstructed = isConstructed,
            tagInfo = tagInfo
        )
        
        return Pair(tlvData, pos + valueLength)
    }
    
    /**
     * Parse BER-TLV length field
     * Returns the length value and the new position after parsing
     */
    private fun parseLength(hex: String, startPos: Int): Pair<Int, Int>? {
        if (startPos >= hex.length) return null
        
        val firstByte = hex.substring(startPos, startPos + 2).toInt(16)
        
        return when {
            // Short form: length is in the first byte (0-127)
            firstByte <= 0x7F -> {
                Pair(firstByte, startPos + 2)
            }
            // Long form: first byte indicates number of subsequent length bytes
            firstByte == 0x81 -> {
                if (startPos + 4 > hex.length) return null
                val length = hex.substring(startPos + 2, startPos + 4).toInt(16)
                Pair(length, startPos + 4)
            }
            firstByte == 0x82 -> {
                if (startPos + 6 > hex.length) return null
                val length = hex.substring(startPos + 2, startPos + 6).toInt(16)
                Pair(length, startPos + 6)
            }
            firstByte == 0x83 -> {
                if (startPos + 8 > hex.length) return null
                val length = hex.substring(startPos + 2, startPos + 8).toInt(16)
                Pair(length, startPos + 8)
            }
            else -> null
        }
    }
    
    /**
     * Encode TLV data to hex string
     */
    fun encode(tag: String, value: String): String {
        val cleanValue = value.replace(" ", "").uppercase()
        val length = cleanValue.length / 2
        val lengthHex = encodeLength(length)
        return "${tag.uppercase()}$lengthHex$cleanValue"
    }
    
    /**
     * Encode length to BER-TLV format
     */
    fun encodeLength(length: Int): String {
        return when {
            length <= 0x7F -> String.format("%02X", length)
            length <= 0xFF -> String.format("81%02X", length)
            length <= 0xFFFF -> String.format("82%04X", length)
            else -> String.format("83%06X", length)
        }
    }
    
    /**
     * Build TLV from tag and value
     */
    fun buildTlv(tag: String, value: String): String {
        return encode(tag, value)
    }
    
    /**
     * Build constructed TLV from tag and child TLVs
     */
    fun buildConstructedTlv(tag: String, children: List<String>): String {
        val concatenatedChildren = children.joinToString("")
        return encode(tag, concatenatedChildren)
    }
    
    /**
     * Extract specific tag value from TLV data
     */
    fun extractTag(tlvList: List<TlvData>, targetTag: String): String? {
        val upperTag = targetTag.uppercase()
        for (tlv in tlvList) {
            if (tlv.tag == upperTag) {
                return tlv.value
            }
            if (tlv.isConstructed && tlv.children.isNotEmpty()) {
                val found = extractTag(tlv.children, targetTag)
                if (found != null) return found
            }
        }
        return null
    }
    
    /**
     * Format TLV data as readable string
     */
    fun formatTlv(tlvList: List<TlvData>, indent: Int = 0): String {
        val sb = StringBuilder()
        val indentStr = "  ".repeat(indent)
        
        for (tlv in tlvList) {
            val tagName = tlv.tagInfo?.name ?: "Unknown"
            sb.appendLine("${indentStr}Tag: ${tlv.tag} ($tagName)")
            sb.appendLine("${indentStr}Length: ${tlv.length}")
            
            if (tlv.isConstructed && tlv.children.isNotEmpty()) {
                sb.appendLine("${indentStr}Value: [Constructed]")
                sb.append(formatTlv(tlv.children, indent + 1))
            } else {
                sb.appendLine("${indentStr}Value: ${HexUtils.formatHex(tlv.value)}")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
}

