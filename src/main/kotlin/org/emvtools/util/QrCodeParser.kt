package org.emvtools.util

/**
 * EMVCo and UPI QR Code Parser
 */
object QrCodeParser {
    
    data class QrField(
        val id: String,
        val name: String,
        val value: String,
        val subFields: List<QrField> = emptyList()
    )
    
    data class ParsedQrCode(
        val format: QrFormat,
        val fields: List<QrField>,
        val rawData: String
    )
    
    enum class QrFormat(val displayName: String) {
        EMVCO_MPM("EMVCo Merchant Presented Mode"),
        EMVCO_CPM("EMVCo Consumer Presented Mode"),
        UPI("UPI QR Code"),
        UNKNOWN("Unknown Format")
    }
    
    // EMVCo MPM field definitions
    private val emvcoMpmFields = mapOf(
        "00" to "Payload Format Indicator",
        "01" to "Point of Initiation Method",
        "02" to "Merchant Account Information (Visa)",
        "03" to "Merchant Account Information (Visa Electron)",
        "04" to "Merchant Account Information (Mastercard)",
        "05" to "Merchant Account Information (Mastercard)",
        "06" to "Merchant Account Information (Maestro)",
        "07" to "Merchant Account Information (Maestro)",
        "08" to "Merchant Account Information (Amex)",
        "09" to "Merchant Account Information (Amex)",
        "10" to "Merchant Account Information (JCB)",
        "11" to "Merchant Account Information (JCB)",
        "12" to "Merchant Account Information (UnionPay)",
        "13" to "Merchant Account Information (UnionPay)",
        "14" to "Merchant Account Information (Discover)",
        "15" to "Merchant Account Information (Discover)",
        "26" to "Merchant Account Information (Template)",
        "27" to "Merchant Account Information (Template)",
        "28" to "Merchant Account Information (Template)",
        "29" to "Merchant Account Information (Template)",
        "30" to "Merchant Account Information (Template)",
        "31" to "Merchant Account Information (Template)",
        "32" to "Merchant Account Information (Template)",
        "33" to "Merchant Account Information (Template)",
        "34" to "Merchant Account Information (Template)",
        "35" to "Merchant Account Information (Template)",
        "36" to "Merchant Account Information (Template)",
        "37" to "Merchant Account Information (Template)",
        "38" to "Merchant Account Information (Template)",
        "39" to "Merchant Account Information (Template)",
        "40" to "Merchant Account Information (Template)",
        "41" to "Merchant Account Information (Template)",
        "42" to "Merchant Account Information (Template)",
        "43" to "Merchant Account Information (Template)",
        "44" to "Merchant Account Information (Template)",
        "45" to "Merchant Account Information (Template)",
        "51" to "Merchant Category Code",
        "52" to "Merchant Category Code",
        "53" to "Transaction Currency",
        "54" to "Transaction Amount",
        "55" to "Tip or Convenience Indicator",
        "56" to "Value of Convenience Fee Fixed",
        "57" to "Value of Convenience Fee Percentage",
        "58" to "Country Code",
        "59" to "Merchant Name",
        "60" to "Merchant City",
        "61" to "Postal Code",
        "62" to "Additional Data Field Template",
        "63" to "CRC",
        "64" to "Merchant Information Language Template"
    )
    
    // Additional Data Field Template sub-fields (ID 62)
    private val additionalDataFields = mapOf(
        "01" to "Bill Number",
        "02" to "Mobile Number",
        "03" to "Store Label",
        "04" to "Loyalty Number",
        "05" to "Reference Label",
        "06" to "Customer Label",
        "07" to "Terminal Label",
        "08" to "Purpose of Transaction",
        "09" to "Additional Consumer Data Request"
    )
    
    // UPI specific fields
    private val upiFields = mapOf(
        "pa" to "Payee VPA",
        "pn" to "Payee Name",
        "mc" to "Merchant Code",
        "tid" to "Transaction ID",
        "tr" to "Transaction Reference",
        "tn" to "Transaction Note",
        "am" to "Amount",
        "cu" to "Currency",
        "url" to "URL"
    )
    
    /**
     * Parse QR code data
     */
    fun parse(data: String): ParsedQrCode {
        val trimmed = data.trim()
        
        // Detect format
        val format = detectFormat(trimmed)
        
        val fields = when (format) {
            QrFormat.EMVCO_MPM -> parseEmvcoMpm(trimmed)
            QrFormat.UPI -> parseUpi(trimmed)
            else -> emptyList()
        }
        
        return ParsedQrCode(format, fields, trimmed)
    }
    
    /**
     * Detect QR code format
     */
    private fun detectFormat(data: String): QrFormat {
        return when {
            data.startsWith("000201") -> QrFormat.EMVCO_MPM
            data.lowercase().startsWith("upi://") -> QrFormat.UPI
            data.contains("pa=") && data.contains("pn=") -> QrFormat.UPI
            else -> QrFormat.UNKNOWN
        }
    }
    
    /**
     * Parse EMVCo MPM format
     */
    private fun parseEmvcoMpm(data: String): List<QrField> {
        val fields = mutableListOf<QrField>()
        var pos = 0
        
        while (pos < data.length - 4) {
            try {
                val id = data.substring(pos, pos + 2)
                val length = data.substring(pos + 2, pos + 4).toInt()
                val value = data.substring(pos + 4, pos + 4 + length)
                
                val name = emvcoMpmFields[id] ?: "Unknown Field"
                
                // Parse sub-fields for templates
                val subFields = if (id in listOf("26", "27", "28", "29", "62", "64") || id.toIntOrNull() in 2..45) {
                    parseEmvcoMpm(value)
                } else {
                    emptyList()
                }
                
                fields.add(QrField(id, name, value, subFields))
                pos += 4 + length
            } catch (e: Exception) {
                break
            }
        }
        
        return fields
    }
    
    /**
     * Parse UPI QR format
     */
    private fun parseUpi(data: String): List<QrField> {
        val fields = mutableListOf<QrField>()
        
        // Remove upi:// prefix if present
        val params = data.removePrefix("upi://").removePrefix("pay?")
        
        // Parse URL parameters
        params.split("&").forEach { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                val key = parts[0].lowercase()
                val value = java.net.URLDecoder.decode(parts[1], "UTF-8")
                val name = upiFields[key] ?: key
                fields.add(QrField(key, name, value))
            }
        }
        
        return fields
    }
    
    /**
     * Generate EMVCo MPM QR code data
     */
    fun generateEmvcoMpm(
        merchantName: String,
        merchantCity: String,
        countryCode: String = "US",
        currencyCode: String = "840",
        amount: String? = null,
        merchantCategoryCode: String = "5411",
        merchantAccountInfo: String? = null,
        additionalData: Map<String, String>? = null
    ): String {
        val sb = StringBuilder()
        
        // Payload Format Indicator
        sb.append("000201")
        
        // Point of Initiation Method (11 = static, 12 = dynamic)
        sb.append("0102").append(if (amount != null) "12" else "11")
        
        // Merchant Account Information
        if (merchantAccountInfo != null) {
            sb.append("26").append(String.format("%02d", merchantAccountInfo.length)).append(merchantAccountInfo)
        }
        
        // Merchant Category Code
        sb.append("52").append(String.format("%02d", merchantCategoryCode.length)).append(merchantCategoryCode)
        
        // Transaction Currency
        sb.append("53").append(String.format("%02d", currencyCode.length)).append(currencyCode)
        
        // Transaction Amount
        if (amount != null) {
            sb.append("54").append(String.format("%02d", amount.length)).append(amount)
        }
        
        // Country Code
        sb.append("58").append(String.format("%02d", countryCode.length)).append(countryCode)
        
        // Merchant Name
        sb.append("59").append(String.format("%02d", merchantName.length)).append(merchantName)
        
        // Merchant City
        sb.append("60").append(String.format("%02d", merchantCity.length)).append(merchantCity)
        
        // Additional Data
        if (additionalData != null && additionalData.isNotEmpty()) {
            val additionalDataStr = buildAdditionalData(additionalData)
            sb.append("62").append(String.format("%02d", additionalDataStr.length)).append(additionalDataStr)
        }
        
        // CRC placeholder
        sb.append("6304")
        
        // Calculate and append CRC
        val crc = calculateCrc16(sb.toString())
        sb.append(String.format("%04X", crc))
        
        return sb.toString().dropLast(4) + String.format("%04X", calculateCrc16(sb.toString().dropLast(4)))
    }
    
    private fun buildAdditionalData(data: Map<String, String>): String {
        val sb = StringBuilder()
        data.forEach { (key, value) ->
            val id = when (key.lowercase()) {
                "billnumber", "bill" -> "01"
                "mobile" -> "02"
                "store" -> "03"
                "loyalty" -> "04"
                "reference" -> "05"
                "customer" -> "06"
                "terminal" -> "07"
                "purpose" -> "08"
                else -> return@forEach
            }
            sb.append(id).append(String.format("%02d", value.length)).append(value)
        }
        return sb.toString()
    }
    
    /**
     * Calculate CRC-16 CCITT
     */
    private fun calculateCrc16(data: String): Int {
        var crc = 0xFFFF
        val polynomial = 0x1021
        
        data.forEach { char ->
            var b = char.code
            for (i in 0 until 8) {
                val bit = ((b shr (7 - i)) and 1) == 1
                val c15 = ((crc shr 15) and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        
        return crc and 0xFFFF
    }
    
    /**
     * Validate CRC
     */
    fun validateCrc(data: String): Boolean {
        if (data.length < 8) return false
        val withoutCrc = data.dropLast(4)
        val providedCrc = data.takeLast(4)
        val calculatedCrc = String.format("%04X", calculateCrc16(withoutCrc + "6304"))
        return providedCrc.equals(calculatedCrc, ignoreCase = true)
    }
}

