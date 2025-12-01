package org.emvtools.data

import org.emvtools.model.EmvTag
import org.emvtools.model.TagFormat
import org.emvtools.model.TagSource

/**
 * Database of EMV tags with their definitions
 */
object EmvTagDatabase {
    
    private val tags: Map<String, EmvTag> = buildMap {
        // Template tags
        put("6F", EmvTag("6F", "FCI Template", "File Control Information Template", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("70", EmvTag("70", "EMV Proprietary Template", "EMV Proprietary Template", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("71", EmvTag("71", "Issuer Script Template 1", "Contains proprietary issuer data for transmission to the ICC before the second GENERATE AC command", TagSource.ISSUER, TagFormat.CONSTRUCTED))
        put("72", EmvTag("72", "Issuer Script Template 2", "Contains proprietary issuer data for transmission to the ICC after the second GENERATE AC command", TagSource.ISSUER, TagFormat.CONSTRUCTED))
        put("73", EmvTag("73", "Directory Discretionary Template", "Issuer discretionary part of the directory according to ISO/IEC 7816-5", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("77", EmvTag("77", "Response Message Template Format 2", "Contains the data objects (with tags and lengths) returned by the ICC in response to a command", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("80", EmvTag("80", "Response Message Template Format 1", "Contains the data objects (without tags and lengths) returned by the ICC in response to a command", TagSource.ICC, TagFormat.BINARY))
        put("A5", EmvTag("A5", "FCI Proprietary Template", "Identifies the data object proprietary to this specification in the FCI template according to ISO/IEC 7816-4", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("E1", EmvTag("E1", "Application Template", "Contains one or more data objects relevant to an application directory entry according to ISO/IEC 7816-5", TagSource.ICC, TagFormat.CONSTRUCTED))
        put("61", EmvTag("61", "Application Template", "Contains one or more data objects relevant to an application directory entry according to ISO/IEC 7816-5", TagSource.ICC, TagFormat.CONSTRUCTED))
        
        // Common EMV tags
        put("42", EmvTag("42", "IIN", "Issuer Identification Number", TagSource.ICC, TagFormat.NUMERIC, 3, 3))
        put("4F", EmvTag("4F", "ADF Name", "Application Identifier (AID) - identifies the application as described in ISO/IEC 7816-5", TagSource.ICC, TagFormat.BINARY, 5, 16))
        put("50", EmvTag("50", "Application Label", "Mnemonic associated with the AID according to ISO/IEC 7816-5", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL, 1, 16))
        put("56", EmvTag("56", "Track 1 Data", "Track 1 Data contains the data objects of the track 1 according to ISO/IEC 7813", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL))
        put("57", EmvTag("57", "Track 2 Equivalent Data", "Contains the data elements of track 2 according to ISO/IEC 7813", TagSource.ICC, TagFormat.BINARY))
        put("5A", EmvTag("5A", "PAN", "Primary Account Number", TagSource.ICC, TagFormat.COMPRESSED_NUMERIC, 0, 10))
        put("5F20", EmvTag("5F20", "Cardholder Name", "Indicates cardholder name according to ISO 7813", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL, 2, 26))
        put("5F24", EmvTag("5F24", "Application Expiration Date", "Date after which application expires", TagSource.ICC, TagFormat.NUMERIC, 3, 3))
        put("5F25", EmvTag("5F25", "Application Effective Date", "Date from which the application may be used", TagSource.ICC, TagFormat.NUMERIC, 3, 3))
        put("5F28", EmvTag("5F28", "Issuer Country Code", "Indicates the country of the issuer according to ISO 3166", TagSource.ICC, TagFormat.NUMERIC, 2, 2))
        put("5F2A", EmvTag("5F2A", "Transaction Currency Code", "Indicates the currency code of the transaction according to ISO 4217", TagSource.TERMINAL, TagFormat.NUMERIC, 2, 2))
        put("5F2D", EmvTag("5F2D", "Language Preference", "1-4 languages stored in order of preference", TagSource.ICC, TagFormat.ALPHANUMERIC, 2, 8))
        put("5F34", EmvTag("5F34", "PAN Sequence Number", "Identifies and differentiates cards with the same PAN", TagSource.ICC, TagFormat.NUMERIC, 1, 1))
        put("5F36", EmvTag("5F36", "Transaction Currency Exponent", "Indicates the implied position of the decimal point from the right of the transaction amount", TagSource.TERMINAL, TagFormat.NUMERIC, 1, 1))
        put("5F50", EmvTag("5F50", "Issuer URL", "The URL provides the location of the Issuer's Library Server on the Internet", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL))
        put("5F53", EmvTag("5F53", "IBAN", "International Bank Account Number", TagSource.ICC, TagFormat.BINARY, 0, 34))
        put("5F54", EmvTag("5F54", "Bank Identifier Code", "Uniquely identifies a bank as defined in ISO 9362", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL, 8, 11))
        put("5F55", EmvTag("5F55", "Issuer Country Code (alpha2)", "Indicates the country of the issuer as defined in ISO 3166 (using a 2 character alphabetic code)", TagSource.ICC, TagFormat.ALPHANUMERIC, 2, 2))
        put("5F56", EmvTag("5F56", "Issuer Country Code (alpha3)", "Indicates the country of the issuer as defined in ISO 3166 (using a 3 character alphabetic code)", TagSource.ICC, TagFormat.ALPHANUMERIC, 3, 3))
        
        // 9F tags
        put("9F01", EmvTag("9F01", "Acquirer Identifier", "Uniquely identifies the acquirer within each payment system", TagSource.TERMINAL, TagFormat.NUMERIC, 6, 6))
        put("9F02", EmvTag("9F02", "Amount, Authorised (Numeric)", "Authorised amount of the transaction (excluding adjustments)", TagSource.TERMINAL, TagFormat.NUMERIC, 6, 6))
        put("9F03", EmvTag("9F03", "Amount, Other (Numeric)", "Secondary amount associated with the transaction representing a cashback amount", TagSource.TERMINAL, TagFormat.NUMERIC, 6, 6))
        put("9F04", EmvTag("9F04", "Amount, Other (Binary)", "Secondary amount associated with the transaction representing a cashback amount", TagSource.TERMINAL, TagFormat.BINARY, 4, 4))
        put("9F05", EmvTag("9F05", "Application Discretionary Data", "Issuer or payment system specified data relating to the application", TagSource.ICC, TagFormat.BINARY, 1, 32))
        put("9F06", EmvTag("9F06", "AID - Terminal", "Identifies the application as described in ISO/IEC 7816-5", TagSource.TERMINAL, TagFormat.BINARY, 5, 16))
        put("9F07", EmvTag("9F07", "Application Usage Control", "Indicates issuer's specified restrictions on the geographic usage and services allowed for the application", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F08", EmvTag("9F08", "Application Version Number (ICC)", "Version number assigned by the payment system for the application", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F09", EmvTag("9F09", "Application Version Number (Terminal)", "Version number assigned by the payment system for the application", TagSource.TERMINAL, TagFormat.BINARY, 2, 2))
        put("9F0B", EmvTag("9F0B", "Cardholder Name Extended", "Indicates the whole cardholder name when greater than 26 characters using the same coding convention as in ISO 7813", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL, 27, 45))
        put("9F0D", EmvTag("9F0D", "IAC - Default", "Issuer Action Code - Default", TagSource.ICC, TagFormat.BINARY, 5, 5))
        put("9F0E", EmvTag("9F0E", "IAC - Denial", "Issuer Action Code - Denial", TagSource.ICC, TagFormat.BINARY, 5, 5))
        put("9F0F", EmvTag("9F0F", "IAC - Online", "Issuer Action Code - Online", TagSource.ICC, TagFormat.BINARY, 5, 5))
        put("9F10", EmvTag("9F10", "Issuer Application Data", "Contains proprietary application data for transmission to the issuer in an online transaction", TagSource.ICC, TagFormat.BINARY, 0, 32))
        put("9F11", EmvTag("9F11", "Issuer Code Table Index", "Indicates the code table according to ISO/IEC 8859 for displaying the Application Preferred Name", TagSource.ICC, TagFormat.NUMERIC, 1, 1))
        put("9F12", EmvTag("9F12", "Application Preferred Name", "Preferred mnemonic associated with the AID", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL, 1, 16))
        put("9F13", EmvTag("9F13", "Last Online ATC Register", "ATC value of the last transaction that went online", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F14", EmvTag("9F14", "Lower Consecutive Offline Limit", "Issuer-specified preference for the maximum number of consecutive offline transactions for this ICC application allowed in a terminal with online capability", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("9F15", EmvTag("9F15", "Merchant Category Code", "Classifies the type of business being done by the merchant", TagSource.TERMINAL, TagFormat.NUMERIC, 2, 2))
        put("9F16", EmvTag("9F16", "Merchant Identifier", "When concatenated with the Acquirer Identifier, uniquely identifies a given merchant", TagSource.TERMINAL, TagFormat.ALPHANUMERIC_SPECIAL, 15, 15))
        put("9F17", EmvTag("9F17", "PIN Try Counter", "Number of PIN tries remaining", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("9F18", EmvTag("9F18", "Issuer Script Identifier", "Identification of the Issuer Script", TagSource.ISSUER, TagFormat.BINARY, 4, 4))
        put("9F1A", EmvTag("9F1A", "Terminal Country Code", "Indicates the country of the terminal", TagSource.TERMINAL, TagFormat.NUMERIC, 2, 2))
        put("9F1B", EmvTag("9F1B", "Terminal Floor Limit", "Indicates the floor limit in the terminal in conjunction with the AID", TagSource.TERMINAL, TagFormat.BINARY, 4, 4))
        put("9F1C", EmvTag("9F1C", "Terminal Identification", "Designates the unique location of a terminal at a merchant", TagSource.TERMINAL, TagFormat.ALPHANUMERIC_SPECIAL, 8, 8))
        put("9F1D", EmvTag("9F1D", "Terminal Risk Management Data", "Application-specific value used by the card for risk management purposes", TagSource.TERMINAL, TagFormat.BINARY, 1, 8))
        put("9F1E", EmvTag("9F1E", "IFD Serial Number", "Unique and permanent serial number assigned to the IFD by the manufacturer", TagSource.TERMINAL, TagFormat.ALPHANUMERIC_SPECIAL, 8, 8))
        put("9F1F", EmvTag("9F1F", "Track 1 Discretionary Data", "Discretionary part of track 1 according to ISO/IEC 7813", TagSource.ICC, TagFormat.ALPHANUMERIC_SPECIAL))
        put("9F20", EmvTag("9F20", "Track 2 Discretionary Data", "Discretionary part of track 2 according to ISO/IEC 7813", TagSource.ICC, TagFormat.COMPRESSED_NUMERIC))
        put("9F21", EmvTag("9F21", "Transaction Time", "Local time that the transaction was authorised", TagSource.TERMINAL, TagFormat.NUMERIC, 3, 3))
        put("9F22", EmvTag("9F22", "CA Public Key Index (Terminal)", "Identifies the certification authority's public key in conjunction with the RID", TagSource.TERMINAL, TagFormat.BINARY, 1, 1))
        put("9F23", EmvTag("9F23", "Upper Consecutive Offline Limit", "Issuer-specified preference for the maximum number of consecutive offline transactions for this ICC application allowed in a terminal without online capability", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("9F26", EmvTag("9F26", "Application Cryptogram", "Cryptogram returned by the ICC in response of the GENERATE AC command", TagSource.ICC, TagFormat.BINARY, 8, 8))
        put("9F27", EmvTag("9F27", "Cryptogram Information Data", "Indicates the type of cryptogram and the actions to be performed by the terminal", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("9F32", EmvTag("9F32", "Issuer Public Key Exponent", "Issuer public key exponent used for the verification of the Signed Static Application Data and the ICC Public Key Certificate", TagSource.ICC, TagFormat.BINARY, 1, 3))
        put("9F33", EmvTag("9F33", "Terminal Capabilities", "Indicates the card data input, CVM, and security capabilities of the terminal", TagSource.TERMINAL, TagFormat.BINARY, 3, 3))
        put("9F34", EmvTag("9F34", "CVM Results", "Indicates the results of the last CVM performed", TagSource.TERMINAL, TagFormat.BINARY, 3, 3))
        put("9F35", EmvTag("9F35", "Terminal Type", "Indicates the environment of the terminal, its communications capability, and its operational control", TagSource.TERMINAL, TagFormat.NUMERIC, 1, 1))
        put("9F36", EmvTag("9F36", "Application Transaction Counter", "Counter maintained by the application in the ICC", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F37", EmvTag("9F37", "Unpredictable Number", "Value to provide variability and uniqueness to the generation of a cryptogram", TagSource.TERMINAL, TagFormat.BINARY, 4, 4))
        put("9F38", EmvTag("9F38", "PDOL", "Processing Options Data Object List", TagSource.ICC, TagFormat.BINARY))
        put("9F39", EmvTag("9F39", "POS Entry Mode", "Indicates the method by which the PAN was entered", TagSource.TERMINAL, TagFormat.NUMERIC, 1, 1))
        put("9F3A", EmvTag("9F3A", "Amount, Reference Currency", "Authorised amount expressed in the reference currency", TagSource.TERMINAL, TagFormat.BINARY, 4, 4))
        put("9F3B", EmvTag("9F3B", "Application Reference Currency", "1-4 currency codes used between the terminal and the ICC when the Transaction Currency Code is different from the Application Currency Code", TagSource.ICC, TagFormat.NUMERIC, 2, 8))
        put("9F3C", EmvTag("9F3C", "Transaction Reference Currency Code", "Code defining the common currency used by the terminal in case the Transaction Currency Code is different from the Application Currency Code", TagSource.TERMINAL, TagFormat.NUMERIC, 2, 2))
        put("9F3D", EmvTag("9F3D", "Transaction Reference Currency Exponent", "Indicates the implied position of the decimal point from the right of the transaction amount, with the Transaction Reference Currency Code", TagSource.TERMINAL, TagFormat.NUMERIC, 1, 1))
        put("9F40", EmvTag("9F40", "Additional Terminal Capabilities", "Indicates the data input and output capabilities of the terminal", TagSource.TERMINAL, TagFormat.BINARY, 5, 5))
        put("9F41", EmvTag("9F41", "Transaction Sequence Counter", "Counter maintained by the terminal that is incremented by one for each transaction", TagSource.TERMINAL, TagFormat.NUMERIC, 2, 4))
        put("9F42", EmvTag("9F42", "Application Currency Code", "Indicates the currency in which the account is managed according to ISO 4217", TagSource.ICC, TagFormat.NUMERIC, 2, 2))
        put("9F43", EmvTag("9F43", "Application Reference Currency Exponent", "Indicates the implied position of the decimal point from the right of the amount, for each of the 1-4 reference currencies represented according to ISO 4217", TagSource.ICC, TagFormat.NUMERIC, 1, 4))
        put("9F44", EmvTag("9F44", "Application Currency Exponent", "Indicates the implied position of the decimal point from the right of the amount represented according to ISO 4217", TagSource.ICC, TagFormat.NUMERIC, 1, 1))
        put("9F45", EmvTag("9F45", "Data Authentication Code", "An issuer assigned value that is retained by the terminal during the verification process of the Signed Static Application Data", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F46", EmvTag("9F46", "ICC Public Key Certificate", "ICC Public Key certified by the Issuer Public Key", TagSource.ICC, TagFormat.BINARY))
        put("9F47", EmvTag("9F47", "ICC Public Key Exponent", "ICC Public Key Exponent used for the verification of the Signed Dynamic Application Data", TagSource.ICC, TagFormat.BINARY, 1, 3))
        put("9F48", EmvTag("9F48", "ICC Public Key Remainder", "Remaining digits of the ICC Public Key Modulus", TagSource.ICC, TagFormat.BINARY))
        put("9F49", EmvTag("9F49", "DDOL", "Dynamic Data Authentication Data Object List", TagSource.ICC, TagFormat.BINARY))
        put("9F4A", EmvTag("9F4A", "SDA Tag List", "List of tags of primitive data objects defined in this specification whose value fields are to be included in the Signed Static or Dynamic Application Data", TagSource.ICC, TagFormat.BINARY))
        put("9F4B", EmvTag("9F4B", "Signed Dynamic Application Data", "Digital signature on critical application parameters for DDA or CDA", TagSource.ICC, TagFormat.BINARY))
        put("9F4C", EmvTag("9F4C", "ICC Dynamic Number", "Time-variant number generated by the ICC, to be captured by the terminal", TagSource.ICC, TagFormat.BINARY, 2, 8))
        put("9F4D", EmvTag("9F4D", "Log Entry", "Provides the SFI of the Transaction Log file and its number of records", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("9F4E", EmvTag("9F4E", "Merchant Name and Location", "Indicates the name and location of the merchant", TagSource.TERMINAL, TagFormat.ALPHANUMERIC_SPECIAL))
        put("9F4F", EmvTag("9F4F", "Log Format", "List (in tag and length format) of data objects representing the logged data elements that are passed to the terminal when a transaction log record is read", TagSource.ICC, TagFormat.BINARY))
        
        // Additional important tags
        put("82", EmvTag("82", "AIP", "Application Interchange Profile - Indicates the capabilities of the card to support specific functions in the application", TagSource.ICC, TagFormat.BINARY, 2, 2))
        put("84", EmvTag("84", "DF Name", "Dedicated File Name", TagSource.ICC, TagFormat.BINARY, 5, 16))
        put("87", EmvTag("87", "Application Priority Indicator", "Indicates the priority of a given application or group of applications in a directory", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("88", EmvTag("88", "SFI", "Short File Identifier", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("8A", EmvTag("8A", "Authorisation Response Code", "Code that defines the disposition of a message", TagSource.ISSUER, TagFormat.ALPHANUMERIC, 2, 2))
        put("8C", EmvTag("8C", "CDOL1", "Card Risk Management Data Object List 1", TagSource.ICC, TagFormat.BINARY))
        put("8D", EmvTag("8D", "CDOL2", "Card Risk Management Data Object List 2", TagSource.ICC, TagFormat.BINARY))
        put("8E", EmvTag("8E", "CVM List", "Cardholder Verification Method List", TagSource.ICC, TagFormat.BINARY))
        put("8F", EmvTag("8F", "CA Public Key Index (ICC)", "Identifies the certification authority's public key in conjunction with the RID", TagSource.ICC, TagFormat.BINARY, 1, 1))
        put("90", EmvTag("90", "Issuer Public Key Certificate", "Issuer public key certified by a certification authority", TagSource.ICC, TagFormat.BINARY))
        put("91", EmvTag("91", "Issuer Authentication Data", "Data sent to the ICC for online issuer authentication", TagSource.ISSUER, TagFormat.BINARY, 8, 16))
        put("92", EmvTag("92", "Issuer Public Key Remainder", "Remaining digits of the Issuer Public Key Modulus", TagSource.ICC, TagFormat.BINARY))
        put("93", EmvTag("93", "Signed Static Application Data", "Digital signature on critical application parameters for SDA", TagSource.ICC, TagFormat.BINARY))
        put("94", EmvTag("94", "AFL", "Application File Locator - Indicates the location (SFI, range of records) of the AEFs related to a given application", TagSource.ICC, TagFormat.BINARY))
        put("95", EmvTag("95", "TVR", "Terminal Verification Results", TagSource.TERMINAL, TagFormat.BINARY, 5, 5))
        put("97", EmvTag("97", "TDOL", "Transaction Certificate Data Object List", TagSource.ICC, TagFormat.BINARY))
        put("98", EmvTag("98", "TC Hash Value", "Result of a hash function specified in Book 2, Annex B3.1", TagSource.TERMINAL, TagFormat.BINARY, 20, 20))
        put("99", EmvTag("99", "Transaction PIN Data", "Data entered by the cardholder for the purpose of the PIN verification", TagSource.TERMINAL, TagFormat.BINARY))
        put("9A", EmvTag("9A", "Transaction Date", "Local date that the transaction was authorised", TagSource.TERMINAL, TagFormat.NUMERIC, 3, 3))
        put("9B", EmvTag("9B", "TSI", "Transaction Status Information", TagSource.TERMINAL, TagFormat.BINARY, 2, 2))
        put("9C", EmvTag("9C", "Transaction Type", "Indicates the type of financial transaction", TagSource.TERMINAL, TagFormat.NUMERIC, 1, 1))
        put("9D", EmvTag("9D", "DDF Name", "Directory Definition File Name", TagSource.ICC, TagFormat.BINARY, 5, 16))
    }
    
    fun getTag(tag: String): EmvTag? = tags[tag.uppercase()]
    
    fun searchTags(query: String): List<EmvTag> {
        val upperQuery = query.uppercase()
        return tags.values.filter { tag ->
            tag.tag.contains(upperQuery) ||
            tag.name.uppercase().contains(upperQuery) ||
            tag.description.uppercase().contains(upperQuery)
        }.sortedBy { it.tag }
    }
    
    fun getAllTags(): List<EmvTag> = tags.values.sortedBy { it.tag }
    
    fun getTagsBySource(source: TagSource): List<EmvTag> = 
        tags.values.filter { it.source == source }.sortedBy { it.tag }
}

