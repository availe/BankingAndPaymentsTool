package org.emvtools.util

/**
 * HSM Command Templates for Thales payShield and similar HSMs
 */
object HsmCommands {
    
    data class HsmCommand(
        val code: String,
        val name: String,
        val description: String,
        val category: String,
        val parameters: List<HsmParameter>,
        val responseFields: List<String>
    )
    
    data class HsmParameter(
        val name: String,
        val length: String,
        val description: String,
        val required: Boolean = true,
        val defaultValue: String = ""
    )
    
    enum class HsmType(val displayName: String) {
        THALES_PAYSHIELD("Thales payShield"),
        THALES_HSM9000("Thales HSM 9000"),
        FUTUREX("Futurex"),
        UTIMACO("Utimaco")
    }
    
    // Thales payShield commands
    val thalesCommands = listOf(
        HsmCommand(
            code = "A0",
            name = "Generate Key",
            description = "Generate a random key and encrypt under ZMK or LMK",
            category = "Key Management",
            parameters = listOf(
                HsmParameter("Mode", "1", "0=Generate, 1=Generate and print"),
                HsmParameter("Key Type", "3", "Key type code (000=ZMK, 001=ZPK, 002=TMK, etc.)"),
                HsmParameter("Key Scheme LMK", "1", "Key scheme under LMK (U=Double, T=Triple)"),
                HsmParameter("ZMK/TMK", "32-48H", "Optional ZMK for export", false),
                HsmParameter("Key Scheme ZMK", "1", "Key scheme under ZMK", false)
            ),
            responseFields = listOf("Response Code", "Key under LMK", "Key under ZMK", "Key Check Value")
        ),
        HsmCommand(
            code = "A6",
            name = "Import Key",
            description = "Import a key encrypted under ZMK",
            category = "Key Management",
            parameters = listOf(
                HsmParameter("Key Type", "3", "Key type code"),
                HsmParameter("ZMK", "32-48H", "Zone Master Key"),
                HsmParameter("Key", "32-48H", "Key encrypted under ZMK"),
                HsmParameter("Key Scheme LMK", "1", "Key scheme under LMK")
            ),
            responseFields = listOf("Response Code", "Key under LMK", "Key Check Value")
        ),
        HsmCommand(
            code = "A8",
            name = "Export Key",
            description = "Export a key encrypted under ZMK",
            category = "Key Management",
            parameters = listOf(
                HsmParameter("Key Type", "3", "Key type code"),
                HsmParameter("ZMK", "32-48H", "Zone Master Key"),
                HsmParameter("Key", "32-48H", "Key encrypted under LMK"),
                HsmParameter("Key Scheme ZMK", "1", "Key scheme under ZMK")
            ),
            responseFields = listOf("Response Code", "Key under ZMK", "Key Check Value")
        ),
        HsmCommand(
            code = "BU",
            name = "Generate KCV",
            description = "Generate Key Check Value for a key",
            category = "Key Management",
            parameters = listOf(
                HsmParameter("Key Type", "3", "Key type code"),
                HsmParameter("Key Length", "1", "Key length flag"),
                HsmParameter("Key", "32-48H", "Key encrypted under LMK")
            ),
            responseFields = listOf("Response Code", "Key Check Value")
        ),
        HsmCommand(
            code = "CA",
            name = "Translate PIN ZPK to LMK",
            description = "Translate PIN block from ZPK to LMK encryption",
            category = "PIN Operations",
            parameters = listOf(
                HsmParameter("ZPK", "32-48H", "Zone PIN Key"),
                HsmParameter("PIN Block", "16H", "Encrypted PIN block"),
                HsmParameter("PIN Block Format", "2", "PIN block format code"),
                HsmParameter("Account Number", "12N", "Account number (rightmost 12 digits)")
            ),
            responseFields = listOf("Response Code", "PIN under LMK")
        ),
        HsmCommand(
            code = "CC",
            name = "Translate PIN LMK to ZPK",
            description = "Translate PIN block from LMK to ZPK encryption",
            category = "PIN Operations",
            parameters = listOf(
                HsmParameter("ZPK", "32-48H", "Zone PIN Key"),
                HsmParameter("PIN Block Format", "2", "PIN block format code"),
                HsmParameter("Account Number", "12N", "Account number"),
                HsmParameter("PIN under LMK", "Variable", "PIN encrypted under LMK")
            ),
            responseFields = listOf("Response Code", "PIN Block Length", "PIN Block")
        ),
        HsmCommand(
            code = "DC",
            name = "Verify PIN IBM",
            description = "Verify PIN using IBM 3624 algorithm",
            category = "PIN Operations",
            parameters = listOf(
                HsmParameter("ZPK", "32-48H", "Zone PIN Key"),
                HsmParameter("PVK Pair", "32-48H", "PIN Verification Key pair"),
                HsmParameter("PIN Block", "16H", "Encrypted PIN block"),
                HsmParameter("PIN Block Format", "2", "PIN block format code"),
                HsmParameter("Check Length", "2N", "PIN check length"),
                HsmParameter("Account Number", "12N", "Account number"),
                HsmParameter("Decimalization Table", "16H", "Decimalization table"),
                HsmParameter("PIN Validation Data", "12N", "PIN validation data"),
                HsmParameter("Offset", "12N", "PIN offset")
            ),
            responseFields = listOf("Response Code")
        ),
        HsmCommand(
            code = "EC",
            name = "Verify PIN VISA PVV",
            description = "Verify PIN using VISA PVV algorithm",
            category = "PIN Operations",
            parameters = listOf(
                HsmParameter("ZPK", "32-48H", "Zone PIN Key"),
                HsmParameter("PVK Pair", "32-48H", "PIN Verification Key pair"),
                HsmParameter("PIN Block", "16H", "Encrypted PIN block"),
                HsmParameter("PIN Block Format", "2", "PIN block format code"),
                HsmParameter("Account Number", "12N", "Account number"),
                HsmParameter("PVKI", "1N", "PVV Key Index"),
                HsmParameter("PVV", "4N", "PIN Verification Value")
            ),
            responseFields = listOf("Response Code")
        ),
        HsmCommand(
            code = "KQ",
            name = "Generate ARQC/Verify ARQC",
            description = "Generate or verify EMV ARQC cryptogram",
            category = "EMV Operations",
            parameters = listOf(
                HsmParameter("Mode", "1", "0=Verify ARQC, 1=Verify ARQC and generate ARPC"),
                HsmParameter("Scheme ID", "1", "Key derivation scheme"),
                HsmParameter("MK-AC", "32-48H", "Master Key for AC"),
                HsmParameter("PAN", "16N", "Primary Account Number"),
                HsmParameter("PAN Sequence", "2N", "PAN Sequence Number"),
                HsmParameter("ARC", "4H", "Authorization Response Code", false),
                HsmParameter("ATC", "4H", "Application Transaction Counter"),
                HsmParameter("UN", "8H", "Unpredictable Number"),
                HsmParameter("Transaction Data", "Variable", "Transaction data for cryptogram")
            ),
            responseFields = listOf("Response Code", "ARPC")
        ),
        HsmCommand(
            code = "NC",
            name = "Diagnostics",
            description = "Perform HSM diagnostics",
            category = "System",
            parameters = listOf(),
            responseFields = listOf("Response Code", "LMK Check Value", "Firmware Version")
        ),
        HsmCommand(
            code = "NO",
            name = "HSM Status",
            description = "Get HSM status information",
            category = "System",
            parameters = listOf(),
            responseFields = listOf("Response Code", "Status Info")
        )
    )
    
    val responseCodeDescriptions = mapOf(
        "00" to "No error",
        "01" to "Verification failure",
        "02" to "Key inappropriate length",
        "03" to "Invalid message header",
        "04" to "Unknown command code",
        "05" to "Invalid key type code",
        "10" to "Source key parity error",
        "11" to "Destination key parity error",
        "12" to "Key not found",
        "13" to "Invalid LMK scheme",
        "14" to "Invalid key scheme",
        "15" to "Invalid key check value",
        "20" to "PIN block format error",
        "21" to "Invalid PIN length",
        "22" to "Invalid decimalization table",
        "23" to "Invalid PIN validation data",
        "24" to "Invalid offset",
        "25" to "Invalid PVV",
        "26" to "Invalid PIN block",
        "27" to "Invalid account number",
        "68" to "Command disabled",
        "90" to "Data parity error",
        "91" to "LMK parity error",
        "92" to "LMK ID mismatch"
    )
    
    /**
     * Build HSM command string
     */
    fun buildCommand(command: HsmCommand, paramValues: Map<String, String>, header: String = ""): String {
        val sb = StringBuilder()
        sb.append(header)
        sb.append(command.code)
        
        command.parameters.forEach { param ->
            val value = paramValues[param.name] ?: param.defaultValue
            if (param.required || value.isNotEmpty()) {
                sb.append(value)
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Parse HSM response
     */
    fun parseResponse(response: String, command: HsmCommand): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        if (response.length >= 4) {
            result["Response Code"] = response.substring(2, 4)
            result["Response Description"] = responseCodeDescriptions[response.substring(2, 4)] ?: "Unknown"
        }
        
        return result
    }
}

