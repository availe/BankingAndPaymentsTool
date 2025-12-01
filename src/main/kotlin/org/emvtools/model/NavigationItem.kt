package org.emvtools.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation items for the application
 */
enum class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val category: NavigationCategory
) {
    // EMV Tools
    EMV_TAGS("EMV Tags", Icons.Default.Search, NavigationCategory.EMV),
    TLV_DECODER("TLV Decoder", Icons.Default.Code, NavigationCategory.EMV),
    CRYPTOGRAM_CALC("Cryptogram Calc", Icons.Default.Calculate, NavigationCategory.EMV),
    CAPK_MANAGEMENT("CAPK Manager", Icons.Default.VerifiedUser, NavigationCategory.EMV),
    EMV_SIMULATOR("EMV Simulator", Icons.Default.PlayArrow, NavigationCategory.EMV),
    APDU_SENDER("APDU Sender", Icons.Default.Send, NavigationCategory.EMV),

    // Crypto Tools
    DES_CALCULATOR("DES Calculator", Icons.Default.Lock, NavigationCategory.CRYPTO),
    AES_CALCULATOR("AES Calculator", Icons.Default.Security, NavigationCategory.CRYPTO),
    ASN1_DECODER("ASN.1 Decoder", Icons.Default.DataObject, NavigationCategory.CRYPTO),

    // Banking Tools
    PIN_TOOLS("PIN Tools", Icons.Default.Pin, NavigationCategory.BANKING),
    KEY_SHARES("Key Shares", Icons.Default.Key, NavigationCategory.BANKING),
    DUKPT_TOOLS("DUKPT Tools", Icons.Default.VpnKey, NavigationCategory.BANKING),
    ISO8583_BUILDER("ISO8583 Builder", Icons.Default.Message, NavigationCategory.BANKING),
    HSM_TESTER("HSM Tester", Icons.Default.Memory, NavigationCategory.BANKING),

    // Misc Tools
    HEX_DUMP("Hex Dump", Icons.Default.GridOn, NavigationCategory.MISC),
    CHAR_CONVERTER("Char Converter", Icons.Default.SwapHoriz, NavigationCategory.MISC),
    MRZ_CALCULATOR("MRZ Calculator", Icons.Default.CreditCard, NavigationCategory.MISC),
    QR_TOOLS("QR Tools", Icons.Default.QrCode2, NavigationCategory.MISC),
    FILE_DIFF("File Diff", Icons.Default.Compare, NavigationCategory.MISC)
}

enum class NavigationCategory(val title: String) {
    EMV("EMV"),
    CRYPTO("Crypto"),
    BANKING("Banking"),
    MISC("Misc")
}

