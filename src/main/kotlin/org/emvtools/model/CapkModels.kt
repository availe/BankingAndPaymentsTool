package org.emvtools.model

import kotlinx.serialization.Serializable

/**
 * Certification Authority Public Key (CAPK) data model
 */
@Serializable
data class Capk(
    val rid: String,                    // Registered Application Provider Identifier (5 bytes hex)
    val index: String,                  // CA Public Key Index (1 byte hex)
    val modulus: String,                // RSA Modulus (hex)
    val exponent: String,               // RSA Exponent (hex, typically 03 or 010001)
    val expiryDate: String = "",        // YYMMDD format
    val hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1,
    val publicKeyAlgorithm: PublicKeyAlgorithm = PublicKeyAlgorithm.RSA,
    val checksum: String = "",          // SHA-1 hash of RID + Index + Modulus + Exponent
    val description: String = ""
) {
    val modulusLength: Int get() = modulus.length / 2
    val keyId: String get() = "$rid$index"
}

@Serializable
enum class HashAlgorithm(val code: String, val displayName: String) {
    SHA1("01", "SHA-1"),
    SHA256("02", "SHA-256")
}

@Serializable
enum class PublicKeyAlgorithm(val code: String, val displayName: String) {
    RSA("01", "RSA")
}

/**
 * CAPK export format
 */
enum class CapkExportFormat {
    XML,
    JSON,
    TEXT,
    CSV
}

/**
 * Well-known RIDs
 */
object WellKnownRids {
    val rids = mapOf(
        "A000000003" to "Visa",
        "A000000004" to "Mastercard",
        "A000000025" to "American Express",
        "A000000065" to "JCB",
        "A000000152" to "Discover",
        "A000000333" to "UnionPay",
        "A000000384" to "Discover (Common AID)",
        "A000000324" to "Discover ZIP",
        "A000000098" to "Debit Network Alliance",
        "A000000277" to "Interac"
    )
    
    fun getName(rid: String): String = rids[rid.uppercase()] ?: "Unknown"
}

/**
 * APDU Command model
 */
@Serializable
data class ApduCommand(
    val name: String = "",
    val cla: String,                    // Class byte
    val ins: String,                    // Instruction byte
    val p1: String,                     // Parameter 1
    val p2: String,                     // Parameter 2
    val data: String = "",              // Command data (Lc + Data)
    val le: String = ""                 // Expected response length
) {
    fun toHex(): String {
        val sb = StringBuilder()
        sb.append(cla).append(ins).append(p1).append(p2)
        if (data.isNotEmpty()) {
            val dataBytes = data.length / 2
            sb.append(String.format("%02X", dataBytes))
            sb.append(data)
        }
        if (le.isNotEmpty()) {
            sb.append(le)
        }
        return sb.toString().uppercase()
    }
    
    companion object {
        fun parse(hex: String): ApduCommand? {
            val clean = hex.replace(" ", "").uppercase()
            if (clean.length < 8) return null
            
            return ApduCommand(
                cla = clean.substring(0, 2),
                ins = clean.substring(2, 4),
                p1 = clean.substring(4, 6),
                p2 = clean.substring(6, 8),
                data = if (clean.length > 10) {
                    val lc = clean.substring(8, 10).toInt(16)
                    if (clean.length >= 10 + lc * 2) clean.substring(10, 10 + lc * 2) else ""
                } else "",
                le = if (clean.length > 8 && clean.length <= 10) clean.substring(8) else ""
            )
        }
    }
}

/**
 * APDU Response model
 */
@Serializable
data class ApduResponse(
    val data: String,
    val sw1: String,
    val sw2: String
) {
    val sw: String get() = "$sw1$sw2"
    val isSuccess: Boolean get() = sw == "9000"
    
    val statusDescription: String get() = when (sw) {
        "9000" -> "Success"
        "6700" -> "Wrong length"
        "6982" -> "Security status not satisfied"
        "6983" -> "Authentication method blocked"
        "6984" -> "Reference data not usable"
        "6985" -> "Conditions of use not satisfied"
        "6986" -> "Command not allowed"
        "6A80" -> "Incorrect parameters in data field"
        "6A81" -> "Function not supported"
        "6A82" -> "File or application not found"
        "6A83" -> "Record not found"
        "6A84" -> "Not enough memory space"
        "6A86" -> "Incorrect P1-P2"
        "6A88" -> "Referenced data not found"
        "6B00" -> "Wrong parameters P1-P2"
        "6C00" -> "Wrong Le field"
        "6D00" -> "Instruction not supported"
        "6E00" -> "Class not supported"
        "6F00" -> "Unknown error"
        else -> when {
            sw1 == "61" -> "More data available (${sw2.toInt(16)} bytes)"
            sw1 == "6C" -> "Wrong Le, correct Le = ${sw2.toInt(16)}"
            sw1 == "63" -> "Warning: Counter = ${sw2[1]}"
            else -> "Unknown status"
        }
    }
    
    companion object {
        fun parse(hex: String): ApduResponse? {
            val clean = hex.replace(" ", "").uppercase()
            if (clean.length < 4) return null
            
            return ApduResponse(
                data = if (clean.length > 4) clean.substring(0, clean.length - 4) else "",
                sw1 = clean.substring(clean.length - 4, clean.length - 2),
                sw2 = clean.substring(clean.length - 2)
            )
        }
    }
}

/**
 * Common EMV APDU commands
 */
object CommonApduCommands {
    val SELECT_PSE = ApduCommand("SELECT PSE", "00", "A4", "04", "00", "315041592E5359532E4444463031", "00")
    val SELECT_PPSE = ApduCommand("SELECT PPSE", "00", "A4", "04", "00", "325041592E5359532E4444463031", "00")
    val GET_PROCESSING_OPTIONS = ApduCommand("GET PROCESSING OPTIONS", "80", "A8", "00", "00", "8300", "00")
    val READ_RECORD_SFI1_REC1 = ApduCommand("READ RECORD SFI1 REC1", "00", "B2", "01", "0C", "", "00")
    val GET_DATA_ATC = ApduCommand("GET DATA ATC", "80", "CA", "9F", "36", "", "00")
    val GET_DATA_PIN_TRY = ApduCommand("GET DATA PIN Try Counter", "80", "CA", "9F", "17", "", "00")
    val VERIFY_PIN = ApduCommand("VERIFY PIN", "00", "20", "00", "80", "", "")
    val GENERATE_AC = ApduCommand("GENERATE AC", "80", "AE", "00", "00", "", "00")
    val INTERNAL_AUTHENTICATE = ApduCommand("INTERNAL AUTHENTICATE", "00", "88", "00", "00", "", "00")
    val EXTERNAL_AUTHENTICATE = ApduCommand("EXTERNAL AUTHENTICATE", "00", "82", "00", "00", "", "")
    
    val all = listOf(
        SELECT_PSE, SELECT_PPSE, GET_PROCESSING_OPTIONS, READ_RECORD_SFI1_REC1,
        GET_DATA_ATC, GET_DATA_PIN_TRY, VERIFY_PIN, GENERATE_AC,
        INTERNAL_AUTHENTICATE, EXTERNAL_AUTHENTICATE
    )
}

/**
 * EMV Transaction data for simulator
 */
@Serializable
data class EmvTransactionData(
    val amount: String = "000000001000",           // 9F02 - Amount Authorized (12 digits)
    val amountOther: String = "000000000000",      // 9F03 - Amount Other
    val terminalCountry: String = "0840",          // 9F1A - Terminal Country Code
    val transactionCurrency: String = "0840",     // 5F2A - Transaction Currency Code
    val transactionDate: String = "",              // 9A - Transaction Date YYMMDD
    val transactionType: String = "00",            // 9C - Transaction Type
    val unpredictableNumber: String = "",          // 9F37 - Unpredictable Number
    val terminalType: String = "22",               // 9F35 - Terminal Type
    val terminalCapabilities: String = "E0F8C8",  // 9F33 - Terminal Capabilities
    val additionalTerminalCapabilities: String = "FF00F0A001", // 9F40
    val cvmResults: String = "000000",             // 9F34 - CVM Results
    val tvr: String = "0000000000",                // 95 - Terminal Verification Results
    val iacDefault: String = "0000000000",
    val iacDenial: String = "0000000000",
    val iacOnline: String = "0000000000"
)

/**
 * CVM (Cardholder Verification Method) types
 */
enum class CvmType(val code: String, val description: String) {
    FAIL_CVM("00", "Fail CVM processing"),
    PLAINTEXT_PIN_ICC("01", "Plaintext PIN verification by ICC"),
    ENCIPHERED_PIN_ONLINE("02", "Enciphered PIN verified online"),
    PLAINTEXT_PIN_ICC_SIGNATURE("03", "Plaintext PIN by ICC and signature"),
    ENCIPHERED_PIN_ICC("04", "Enciphered PIN verification by ICC"),
    ENCIPHERED_PIN_ICC_SIGNATURE("05", "Enciphered PIN by ICC and signature"),
    SIGNATURE("1E", "Signature"),
    NO_CVM("1F", "No CVM required"),
    NA("3F", "N/A")
}

