package org.emvtools.crypto

import org.emvtools.model.CryptogramResult
import org.emvtools.model.CryptogramType
import org.emvtools.model.DesKeyType
import org.emvtools.model.DesMode
import org.emvtools.model.DesOperation
import org.emvtools.util.HexUtils

/**
 * EMV Cryptogram Calculator for ARQC, ARPC, TC, AAC
 */
object CryptogramCalculator {
    
    /**
     * Calculate Application Cryptogram (ARQC/TC/AAC)
     * Using EMV Common Session Key derivation and MAC calculation
     */
    fun calculateCryptogram(
        type: CryptogramType,
        pan: String,
        panSequence: String,
        atc: String,
        unpredictableNumber: String,
        transactionData: String,
        masterKey: String,
        derivationMethod: KeyDerivationMethod = KeyDerivationMethod.EMV_COMMON
    ): Result<CryptogramResult> {
        return try {
            // Derive session key
            val sessionKey = deriveSessionKey(masterKey, pan, panSequence, atc, derivationMethod)
                .getOrThrow()
            
            // Build cryptogram input data
            val inputData = buildCryptogramInput(transactionData, atc, unpredictableNumber)
            
            // Calculate MAC
            val cryptogram = DesCalculator.calculateRetailMac(inputData, sessionKey)
                .getOrThrow()
            
            Result.success(CryptogramResult(
                type = type,
                cryptogram = cryptogram,
                inputData = inputData,
                key = sessionKey
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate ARPC (Authorization Response Cryptogram)
     * Method 1: ARPC = 3DES(ARQC XOR ARC, Session Key)
     */
    fun calculateArpcMethod1(
        arqc: String,
        arc: String,
        sessionKey: String
    ): Result<String> {
        return try {
            val cleanArqc = arqc.replace(" ", "").uppercase()
            val cleanArc = arc.replace(" ", "").uppercase().padEnd(16, '0')
            
            // XOR ARQC with ARC (padded to 8 bytes)
            val xored = HexUtils.xorHex(cleanArqc, cleanArc)
            
            // Encrypt with session key
            DesCalculator.calculate(
                xored,
                sessionKey,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Calculate ARPC Method 2
     * ARPC = MAC over (ARQC || CSU || Proprietary Auth Data)
     */
    fun calculateArpcMethod2(
        arqc: String,
        csu: String,
        proprietaryData: String,
        sessionKey: String
    ): Result<String> {
        return try {
            val inputData = "${arqc.replace(" ", "")}${csu.replace(" ", "")}${proprietaryData.replace(" ", "")}"
            
            // Pad to multiple of 8 bytes
            val paddedData = if (inputData.length % 16 != 0) {
                val padLength = 16 - (inputData.length % 16)
                inputData + "8" + "0".repeat(padLength - 1)
            } else {
                inputData
            }
            
            DesCalculator.calculateRetailMac(paddedData, sessionKey)
                .map { it.take(8) } // ARPC is 4 bytes
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Derive session key from master key
     */
    fun deriveSessionKey(
        masterKey: String,
        pan: String,
        panSequence: String,
        atc: String,
        method: KeyDerivationMethod = KeyDerivationMethod.EMV_COMMON
    ): Result<String> {
        return when (method) {
            KeyDerivationMethod.EMV_COMMON -> deriveEmvCommonSessionKey(masterKey, pan, panSequence, atc)
            KeyDerivationMethod.VISA -> deriveVisaSessionKey(masterKey, pan, panSequence, atc)
            KeyDerivationMethod.MASTERCARD -> deriveMastercardSessionKey(masterKey, pan, panSequence, atc)
        }
    }
    
    /**
     * EMV Common Session Key Derivation
     */
    private fun deriveEmvCommonSessionKey(
        masterKey: String,
        pan: String,
        panSequence: String,
        atc: String
    ): Result<String> {
        return try {
            // First derive UDK (Unique DEA Key) from IMK (Issuer Master Key)
            val udk = deriveUdk(masterKey, pan, panSequence).getOrThrow()
            
            // Then derive session key from UDK using ATC
            val cleanAtc = atc.replace(" ", "").uppercase().padStart(4, '0')
            
            // Session key derivation data
            val leftData = "${cleanAtc}F00000000000"
            val rightData = "${cleanAtc}0F0000000000"
            
            val leftKey = DesCalculator.calculate(
                leftData,
                udk,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            val rightKey = DesCalculator.calculate(
                rightData,
                udk,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            Result.success(leftKey + rightKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Visa Session Key Derivation (CVN 10, 17, 18)
     */
    private fun deriveVisaSessionKey(
        masterKey: String,
        pan: String,
        panSequence: String,
        atc: String
    ): Result<String> {
        // Visa uses similar derivation to EMV Common
        return deriveEmvCommonSessionKey(masterKey, pan, panSequence, atc)
    }
    
    /**
     * Mastercard Session Key Derivation
     */
    private fun deriveMastercardSessionKey(
        masterKey: String,
        pan: String,
        panSequence: String,
        atc: String
    ): Result<String> {
        return try {
            val udk = deriveUdk(masterKey, pan, panSequence).getOrThrow()
            val cleanAtc = atc.replace(" ", "").uppercase().padStart(4, '0')
            
            // Mastercard specific derivation
            val derivationData = cleanAtc + "000000000000"
            
            val sessionKey = DesCalculator.calculate(
                derivationData,
                udk,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            // For double-length key, derive second half
            val derivationData2 = cleanAtc + "000000000001"
            val sessionKey2 = DesCalculator.calculate(
                derivationData2,
                udk,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            Result.success(sessionKey + sessionKey2)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Derive UDK (Unique DEA Key) from IMK using PAN and PSN
     */
    fun deriveUdk(
        masterKey: String,
        pan: String,
        panSequence: String
    ): Result<String> {
        return try {
            val cleanPan = pan.replace(" ", "").filter { it.isDigit() }
            val cleanPsn = panSequence.replace(" ", "").padStart(2, '0')
            
            // Build derivation data: rightmost 16 digits of (PAN || PSN)
            val panPsn = cleanPan + cleanPsn
            val derivationData = if (panPsn.length >= 16) {
                panPsn.takeLast(16)
            } else {
                panPsn.padStart(16, '0')
            }
            
            // Encrypt derivation data with master key for left half
            val leftHalf = DesCalculator.calculate(
                derivationData,
                masterKey,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            // XOR derivation data with FFFFFFFFFFFFFFFF for right half
            val xoredData = HexUtils.xorHex(derivationData, "FFFFFFFFFFFFFFFF")
            val rightHalf = DesCalculator.calculate(
                xoredData,
                masterKey,
                DesOperation.ENCRYPT,
                DesMode.ECB,
                DesKeyType.TRIPLE_DES_2KEY
            ).getOrThrow()
            
            Result.success(leftHalf + rightHalf)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Build cryptogram input data
     */
    private fun buildCryptogramInput(
        transactionData: String,
        atc: String,
        unpredictableNumber: String
    ): String {
        val cleanData = transactionData.replace(" ", "").uppercase()
        val cleanAtc = atc.replace(" ", "").uppercase()
        val cleanUn = unpredictableNumber.replace(" ", "").uppercase()
        
        return cleanData + cleanAtc + cleanUn
    }
    
    /**
     * Verify ARQC
     */
    fun verifyArqc(
        expectedArqc: String,
        pan: String,
        panSequence: String,
        atc: String,
        unpredictableNumber: String,
        transactionData: String,
        masterKey: String
    ): Result<Boolean> {
        return try {
            val calculated = calculateCryptogram(
                CryptogramType.ARQC,
                pan, panSequence, atc, unpredictableNumber, transactionData, masterKey
            ).getOrThrow()
            
            Result.success(calculated.cryptogram.equals(expectedArqc.replace(" ", ""), ignoreCase = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

enum class KeyDerivationMethod {
    EMV_COMMON,
    VISA,
    MASTERCARD
}

