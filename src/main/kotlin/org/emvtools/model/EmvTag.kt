package org.emvtools.model

/**
 * Represents an EMV tag with its metadata
 */
data class EmvTag(
    val tag: String,
    val name: String,
    val description: String,
    val source: TagSource,
    val format: TagFormat,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val template: String? = null
)

enum class TagSource {
    ICC,        // Card
    TERMINAL,   // Terminal
    ISSUER,     // Issuer
    KERNEL,     // Kernel specific
    PROPRIETARY // Proprietary
}

enum class TagFormat {
    BINARY,
    NUMERIC,
    COMPRESSED_NUMERIC,
    ALPHANUMERIC,
    ALPHANUMERIC_SPECIAL,
    VARIABLE,
    CONSTRUCTED,
    UNKNOWN
}

/**
 * Represents a parsed TLV (Tag-Length-Value) structure
 */
data class TlvData(
    val tag: String,
    val length: Int,
    val value: String,
    val rawData: String,
    val children: List<TlvData> = emptyList(),
    val isConstructed: Boolean = false,
    val tagInfo: EmvTag? = null
)

/**
 * Result of TLV parsing operation
 */
sealed class TlvParseResult {
    data class Success(val tlvList: List<TlvData>) : TlvParseResult()
    data class Error(val message: String, val position: Int? = null) : TlvParseResult()
}

/**
 * PIN Block formats
 */
enum class PinBlockFormat(val description: String) {
    ISO_FORMAT_0("ISO 9564-1 Format 0 (ISO-0)"),
    ISO_FORMAT_1("ISO 9564-1 Format 1 (ISO-1)"),
    ISO_FORMAT_2("ISO 9564-1 Format 2 (ISO-2)"),
    ISO_FORMAT_3("ISO 9564-1 Format 3 (ISO-3)"),
    ISO_FORMAT_4("ISO 9564-1 Format 4 (ISO-4)"),
    ANSI_X9_8("ANSI X9.8"),
    VISA_FORMAT_1("VISA Format 1"),
    VISA_FORMAT_3("VISA Format 3 / ECI Format 1"),
    VISA_FORMAT_4("VISA Format 4")
}

/**
 * Represents a PIN block
 */
data class PinBlock(
    val format: PinBlockFormat,
    val clearPinBlock: String,
    val encryptedPinBlock: String? = null
)

/**
 * Key component for key share generation
 */
data class KeyComponent(
    val index: Int,
    val value: String,
    val kcv: String
)

/**
 * Complete key with components
 */
data class KeyShare(
    val fullKey: String,
    val fullKeyKcv: String,
    val components: List<KeyComponent>
)

/**
 * Cryptogram types
 */
enum class CryptogramType(val description: String) {
    ARQC("Authorization Request Cryptogram"),
    TC("Transaction Certificate"),
    AAC("Application Authentication Cryptogram"),
    ARPC("Authorization Response Cryptogram")
}

/**
 * Cryptogram calculation result
 */
data class CryptogramResult(
    val type: CryptogramType,
    val cryptogram: String,
    val inputData: String,
    val key: String
)

/**
 * DES operation mode
 */
enum class DesMode {
    ECB,
    CBC
}

/**
 * DES operation type
 */
enum class DesOperation {
    ENCRYPT,
    DECRYPT
}

/**
 * DES key type
 */
enum class DesKeyType {
    SINGLE_DES,
    TRIPLE_DES_2KEY,
    TRIPLE_DES_3KEY
}

/**
 * Character encoding types
 */
enum class CharEncoding {
    ASCII,
    EBCDIC,
    HEX
}

/**
 * MRZ document type
 */
enum class MrzDocumentType(val code: String, val description: String) {
    PASSPORT("P", "Passport"),
    ID_CARD("I", "ID Card"),
    VISA("V", "Visa")
}

/**
 * MRZ gender
 */
enum class MrzGender(val code: String) {
    MALE("M"),
    FEMALE("F"),
    UNSPECIFIED("X")
}

/**
 * MRZ data for passport/ID
 */
data class MrzData(
    val documentType: MrzDocumentType,
    val issuingCountry: String,
    val surname: String,
    val givenNames: String,
    val documentNumber: String,
    val nationality: String,
    val dateOfBirth: String, // YYMMDD
    val gender: MrzGender,
    val expiryDate: String,  // YYMMDD
    val optionalData: String = ""
)

/**
 * Generated MRZ result
 */
data class MrzResult(
    val line1: String,
    val line2: String,
    val line3: String? = null // For TD1 format (ID cards)
)

