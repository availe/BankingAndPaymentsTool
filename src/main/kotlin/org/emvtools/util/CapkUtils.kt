package org.emvtools.util

import org.emvtools.model.Capk
import org.emvtools.model.CapkExportFormat
import org.emvtools.model.HashAlgorithm
import org.emvtools.model.WellKnownRids
import java.security.MessageDigest

/**
 * CAPK (Certification Authority Public Key) utilities
 */
object CapkUtils {

    /**
     * Calculate CAPK checksum (SHA-1 of RID + Index + Modulus + Exponent)
     */
    fun calculateChecksum(rid: String, index: String, modulus: String, exponent: String): String {
        val data = HexUtils.hexToBytes(rid + index + modulus + exponent)
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(data)
        return HexUtils.bytesToHex(hash)
    }

    /**
     * Validate CAPK checksum
     */
    fun validateChecksum(capk: Capk): Boolean {
        if (capk.checksum.isEmpty()) return true
        val calculated = calculateChecksum(capk.rid, capk.index, capk.modulus, capk.exponent)
        return calculated.equals(capk.checksum, ignoreCase = true)
    }

    /**
     * Create CAPK with calculated checksum
     */
    fun createCapk(
        rid: String,
        index: String,
        modulus: String,
        exponent: String,
        expiryDate: String = "",
        hashAlgorithm: HashAlgorithm = HashAlgorithm.SHA1,
        description: String = ""
    ): Capk {
        val checksum = calculateChecksum(rid, index, modulus, exponent)
        return Capk(
            rid = rid.uppercase(),
            index = index.uppercase(),
            modulus = modulus.uppercase(),
            exponent = exponent.uppercase(),
            expiryDate = expiryDate,
            hashAlgorithm = hashAlgorithm,
            checksum = checksum.uppercase(),
            description = description.ifEmpty { WellKnownRids.getName(rid) }
        )
    }

    /**
     * Export CAPKs to specified format
     */
    fun export(capks: List<Capk>, format: CapkExportFormat): String {
        return when (format) {
            CapkExportFormat.XML -> exportToXml(capks)
            CapkExportFormat.JSON -> exportToJson(capks)
            CapkExportFormat.TEXT -> exportToText(capks)
            CapkExportFormat.CSV -> exportToCsv(capks)
        }
    }

    private fun exportToXml(capks: List<Capk>): String {
        val sb = StringBuilder()
        sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.appendLine("<CAPKList>")
        capks.forEach { capk ->
            sb.appendLine("  <CAPK>")
            sb.appendLine("    <RID>${capk.rid}</RID>")
            sb.appendLine("    <Index>${capk.index}</Index>")
            sb.appendLine("    <Modulus>${capk.modulus}</Modulus>")
            sb.appendLine("    <Exponent>${capk.exponent}</Exponent>")
            sb.appendLine("    <ModulusLength>${capk.modulusLength}</ModulusLength>")
            sb.appendLine("    <ExpiryDate>${capk.expiryDate}</ExpiryDate>")
            sb.appendLine("    <HashAlgorithm>${capk.hashAlgorithm.code}</HashAlgorithm>")
            sb.appendLine("    <Checksum>${capk.checksum}</Checksum>")
            sb.appendLine("    <Description>${capk.description}</Description>")
            sb.appendLine("  </CAPK>")
        }
        sb.appendLine("</CAPKList>")
        return sb.toString()
    }

    private fun exportToJson(capks: List<Capk>): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("""  "capks": [""")
        capks.forEachIndexed { index, capk ->
            sb.appendLine("    {")
            sb.appendLine("""      "rid": "${capk.rid}",""")
            sb.appendLine("""      "index": "${capk.index}",""")
            sb.appendLine("""      "modulus": "${capk.modulus}",""")
            sb.appendLine("""      "exponent": "${capk.exponent}",""")
            sb.appendLine("""      "modulusLength": ${capk.modulusLength},""")
            sb.appendLine("""      "expiryDate": "${capk.expiryDate}",""")
            sb.appendLine("""      "hashAlgorithm": "${capk.hashAlgorithm.code}",""")
            sb.appendLine("""      "checksum": "${capk.checksum}",""")
            sb.appendLine("""      "description": "${capk.description}"""")
            sb.append("    }")
            if (index < capks.size - 1) sb.append(",")
            sb.appendLine()
        }
        sb.appendLine("  ]")
        sb.appendLine("}")
        return sb.toString()
    }

    private fun exportToText(capks: List<Capk>): String {
        val sb = StringBuilder()
        sb.appendLine("=" .repeat(80))
        sb.appendLine("CAPK LIST")
        sb.appendLine("=".repeat(80))
        capks.forEach { capk ->
            sb.appendLine()
            sb.appendLine("RID:            ${capk.rid}")
            sb.appendLine("Index:          ${capk.index}")
            sb.appendLine("Description:    ${capk.description}")
            sb.appendLine("Modulus Length: ${capk.modulusLength} bytes")
            sb.appendLine("Exponent:       ${capk.exponent}")
            sb.appendLine("Expiry Date:    ${capk.expiryDate}")
            sb.appendLine("Hash Algorithm: ${capk.hashAlgorithm.displayName}")
            sb.appendLine("Checksum:       ${capk.checksum}")
            sb.appendLine("Modulus:")
            capk.modulus.chunked(64).forEach { sb.appendLine("  $it") }
            sb.appendLine("-".repeat(80))
        }
        return sb.toString()
    }

    private fun exportToCsv(capks: List<Capk>): String {
        val sb = StringBuilder()
        sb.appendLine("RID,Index,ModulusLength,Exponent,ExpiryDate,HashAlgorithm,Checksum,Description,Modulus")
        capks.forEach { capk ->
            sb.appendLine("${capk.rid},${capk.index},${capk.modulusLength},${capk.exponent},${capk.expiryDate},${capk.hashAlgorithm.code},${capk.checksum},\"${capk.description}\",${capk.modulus}")
        }
        return sb.toString()
    }

    /**
     * Parse CAPK from XML string
     */
    fun parseFromXml(xml: String): List<Capk> {
        val capks = mutableListOf<Capk>()
        val capkPattern = Regex("""<CAPK>(.*?)</CAPK>""", RegexOption.DOT_MATCHES_ALL)

        capkPattern.findAll(xml).forEach { match ->
            val content = match.groupValues[1]
            val rid = extractXmlValue(content, "RID") ?: return@forEach
            val index = extractXmlValue(content, "Index") ?: return@forEach
            val modulus = extractXmlValue(content, "Modulus") ?: return@forEach
            val exponent = extractXmlValue(content, "Exponent") ?: return@forEach

            capks.add(Capk(
                rid = rid,
                index = index,
                modulus = modulus,
                exponent = exponent,
                expiryDate = extractXmlValue(content, "ExpiryDate") ?: "",
                checksum = extractXmlValue(content, "Checksum") ?: "",
                description = extractXmlValue(content, "Description") ?: ""
            ))
        }
        return capks
    }

    private fun extractXmlValue(content: String, tag: String): String? {
        val pattern = Regex("""<$tag>(.*?)</$tag>""", RegexOption.DOT_MATCHES_ALL)
        return pattern.find(content)?.groupValues?.get(1)?.trim()
    }

    /**
     * Sample test CAPKs (Visa test keys)
     */
    val sampleCapks = listOf(
        Capk(
            rid = "A000000003",
            index = "92",
            modulus = "996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9F",
            exponent = "03",
            expiryDate = "311231",
            checksum = "429C954A3859CEF91295F663C963E582ED6EB253",
            description = "Visa Test CAPK 92"
        ),
        Capk(
            rid = "A000000004",
            index = "EF",
            modulus = "A191CB87473F29349B5D60A88B3EAEE0973AA6F1A082F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A13ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB651AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3B",
            exponent = "03",
            expiryDate = "311231",
            checksum = "21766EBB0EE122AFB65D7845B73DB46BAB65427A",
            description = "Mastercard Test CAPK EF"
        )
    )
}

