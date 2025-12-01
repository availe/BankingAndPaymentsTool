package org.emvtools.util

import org.emvtools.model.EmvTransactionData
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * EMV Transaction Simulator
 */
object EmvSimulator {
    
    /**
     * Transaction result
     */
    data class TransactionResult(
        val decision: TransactionDecision,
        val cryptogramType: String,
        val cryptogram: String,
        val tvr: String,
        val cvmResult: String,
        val logs: List<String>
    )
    
    enum class TransactionDecision {
        APPROVED_OFFLINE,
        DECLINED_OFFLINE,
        GO_ONLINE,
        REFERRAL
    }
    
    /**
     * Card profile for simulation
     */
    data class CardProfile(
        val pan: String = "4761739001010010",
        val panSequence: String = "00",
        val expiryDate: String = "2512",
        val applicationLabel: String = "VISA CREDIT",
        val aid: String = "A0000000031010",
        val aip: String = "1800",                    // Application Interchange Profile
        val afl: String = "08010100",                // Application File Locator
        val cdol1: String = "9F02069F03069F1A0295055F2A029A039C019F37049F35019F45029F4C089F3403",
        val cdol2: String = "8A029F02069F03069F1A0295055F2A029A039C019F3704",
        val iacDefault: String = "DC4000A800",
        val iacDenial: String = "0010000000",
        val iacOnline: String = "DC4004F800",
        val cvm: String = "410342031E031F00",        // CVM List
        val floorLimit: Long = 0,
        val atc: Int = 1
    )
    
    /**
     * Simulate EMV transaction
     */
    fun simulate(
        transactionData: EmvTransactionData,
        cardProfile: CardProfile,
        forceOnline: Boolean = false
    ): TransactionResult {
        val logs = mutableListOf<String>()
        var tvr = "0000000000"
        var cvmResult = "000000"
        
        logs.add("=== EMV Transaction Simulation ===")
        logs.add("Transaction Date: ${transactionData.transactionDate.ifEmpty { getCurrentDate() }}")
        logs.add("Amount: ${formatAmount(transactionData.amount)}")
        logs.add("Currency: ${transactionData.transactionCurrency}")
        logs.add("")
        
        // Step 1: Application Selection
        logs.add("--- Step 1: Application Selection ---")
        logs.add("Selected AID: ${cardProfile.aid}")
        logs.add("Application Label: ${cardProfile.applicationLabel}")
        logs.add("")
        
        // Step 2: Initiate Application Processing
        logs.add("--- Step 2: Initiate Application Processing ---")
        logs.add("AIP: ${cardProfile.aip}")
        logs.add("AFL: ${cardProfile.afl}")
        logs.add("")
        
        // Step 3: Read Application Data
        logs.add("--- Step 3: Read Application Data ---")
        logs.add("PAN: ${maskPan(cardProfile.pan)}")
        logs.add("Expiry: ${cardProfile.expiryDate}")
        logs.add("ATC: ${String.format("%04X", cardProfile.atc)}")
        logs.add("")
        
        // Step 4: Offline Data Authentication
        logs.add("--- Step 4: Offline Data Authentication ---")
        val aip = cardProfile.aip.toInt(16)
        val sdaSupported = (aip and 0x40) != 0
        val ddaSupported = (aip and 0x20) != 0
        val cdaSupported = (aip and 0x01) != 0
        
        when {
            cdaSupported -> logs.add("CDA supported - Combined DDA/AC Generation")
            ddaSupported -> logs.add("DDA supported - Dynamic Data Authentication")
            sdaSupported -> logs.add("SDA supported - Static Data Authentication")
            else -> {
                logs.add("No ODA supported - TVR bit set")
                tvr = setTvrBit(tvr, 0, 7) // Offline data authentication not performed
            }
        }
        logs.add("")
        
        // Step 5: Processing Restrictions
        logs.add("--- Step 5: Processing Restrictions ---")
        logs.add("Checking application version, usage control, effective/expiry dates...")
        logs.add("All checks passed")
        logs.add("")
        
        // Step 6: Cardholder Verification
        logs.add("--- Step 6: Cardholder Verification ---")
        val amount = transactionData.amount.toLongOrNull() ?: 0
        cvmResult = performCvm(cardProfile.cvm, amount, logs)
        logs.add("CVM Result: $cvmResult")
        logs.add("")
        
        // Step 7: Terminal Risk Management
        logs.add("--- Step 7: Terminal Risk Management ---")
        logs.add("Floor Limit: ${cardProfile.floorLimit}")
        if (amount > cardProfile.floorLimit) {
            logs.add("Amount exceeds floor limit - Transaction exceeds floor limit bit set")
            tvr = setTvrBit(tvr, 3, 7)
        }
        
        // Random selection for online
        if (!forceOnline && Math.random() < 0.1) {
            logs.add("Random selection triggered")
            tvr = setTvrBit(tvr, 3, 6)
        }
        logs.add("")
        
        // Step 8: Terminal Action Analysis
        logs.add("--- Step 8: Terminal Action Analysis ---")
        val decision = analyzeTerminalAction(tvr, cardProfile, forceOnline, logs)
        logs.add("Terminal Decision: $decision")
        logs.add("")
        
        // Step 9: Card Action Analysis (Generate AC)
        logs.add("--- Step 9: Card Action Analysis ---")
        val (cryptogramType, cryptogram) = generateCryptogram(decision, transactionData, cardProfile, logs)
        logs.add("Cryptogram Type: $cryptogramType")
        logs.add("Cryptogram: $cryptogram")
        logs.add("")
        
        logs.add("=== Transaction Complete ===")
        logs.add("Final TVR: $tvr")
        logs.add("Final Decision: $decision")
        
        return TransactionResult(
            decision = decision,
            cryptogramType = cryptogramType,
            cryptogram = cryptogram,
            tvr = tvr,
            cvmResult = cvmResult,
            logs = logs
        )
    }
    
    private fun getCurrentDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
    }
    
    private fun formatAmount(amount: String): String {
        val value = amount.toLongOrNull() ?: 0
        return String.format("%.2f", value / 100.0)
    }
    
    private fun maskPan(pan: String): String {
        if (pan.length < 8) return pan
        return pan.take(6) + "*".repeat(pan.length - 10) + pan.takeLast(4)
    }
    
    private fun setTvrBit(tvr: String, byteIndex: Int, bitIndex: Int): String {
        val bytes = HexUtils.hexToBytes(tvr).toMutableList()
        if (byteIndex < bytes.size) {
            bytes[byteIndex] = (bytes[byteIndex].toInt() or (1 shl bitIndex)).toByte()
        }
        return HexUtils.bytesToHex(bytes.toByteArray())
    }
    
    private fun performCvm(cvmList: String, amount: Long, logs: MutableList<String>): String {
        logs.add("CVM List: $cvmList")
        // Simplified CVM processing
        // In real implementation, would parse CVM list and apply rules
        return if (amount > 2500) {
            logs.add("Amount > 25.00 - PIN required")
            "020000" // Enciphered PIN verified online
        } else {
            logs.add("Amount <= 25.00 - No CVM required")
            "1F0000" // No CVM required
        }
    }
    
    private fun analyzeTerminalAction(
        tvr: String,
        cardProfile: CardProfile,
        forceOnline: Boolean,
        logs: MutableList<String>
    ): TransactionDecision {
        if (forceOnline) {
            logs.add("Force online flag set")
            return TransactionDecision.GO_ONLINE
        }
        
        val tvrBytes = HexUtils.hexToBytes(tvr)
        val iacDenial = HexUtils.hexToBytes(cardProfile.iacDenial)
        val iacOnline = HexUtils.hexToBytes(cardProfile.iacOnline)
        val iacDefault = HexUtils.hexToBytes(cardProfile.iacDefault)
        
        // Check denial conditions
        for (i in tvrBytes.indices) {
            if ((tvrBytes[i].toInt() and iacDenial[i].toInt()) != 0) {
                logs.add("IAC-Denial condition met at byte $i")
                return TransactionDecision.DECLINED_OFFLINE
            }
        }
        
        // Check online conditions
        for (i in tvrBytes.indices) {
            if ((tvrBytes[i].toInt() and iacOnline[i].toInt()) != 0) {
                logs.add("IAC-Online condition met at byte $i")
                return TransactionDecision.GO_ONLINE
            }
        }
        
        logs.add("No denial or online conditions - Approved offline")
        return TransactionDecision.APPROVED_OFFLINE
    }
    
    private fun generateCryptogram(
        decision: TransactionDecision,
        transactionData: EmvTransactionData,
        cardProfile: CardProfile,
        logs: MutableList<String>
    ): Pair<String, String> {
        val cryptogramType = when (decision) {
            TransactionDecision.APPROVED_OFFLINE -> "TC"
            TransactionDecision.DECLINED_OFFLINE -> "AAC"
            TransactionDecision.GO_ONLINE -> "ARQC"
            TransactionDecision.REFERRAL -> "AAC"
        }
        
        // Generate simulated cryptogram (in real implementation would use actual key derivation)
        val un = transactionData.unpredictableNumber.ifEmpty { 
            String.format("%08X", (Math.random() * 0xFFFFFFFF).toLong())
        }
        logs.add("Unpredictable Number: $un")
        
        // Simulated cryptogram
        val cryptogram = HexUtils.bytesToHex(
            java.security.MessageDigest.getInstance("SHA-256")
                .digest((cardProfile.pan + cardProfile.atc + un).toByteArray())
        ).take(16)
        
        return Pair(cryptogramType, cryptogram)
    }
}

