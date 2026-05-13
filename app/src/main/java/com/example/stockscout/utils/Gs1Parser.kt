package com.example.stockscout.utils

/**
 * Parses GS1 barcode strings using Application Identifiers (AIs).
 *
 * GS1 strings encode data fields using fixed or variable-length AIs.
 * Variable-length AIs are delimited by FNC1 (ASCII 29 / GS char).
 * This parser covers the most common AIs used in warehouse contexts.
 */
object Gs1Parser {

    // FNC1 separator character (ASCII 29 = Group Separator)
    private const val FNC1 = ''

    data class Gs1Data(
        val gtin: String?,      // AI 01: GTIN-14
        val lot: String?,       // AI 10: batch/lot number
        val expiry: String?,    // AI 17: expiry date YYMMDD
        val serial: String?,    // AI 21: serial number
        val quantity: String?,  // AI 30: variable quantity
        val raw: String
    )

    // Map of AI prefix -> fixed length (-1 = variable, terminated by FNC1)
    private val AI_LENGTHS: Map<String, Int> = mapOf(
        "00" to 18,  // SSCC
        "01" to 14,  // GTIN-14
        "02" to 14,  // GTIN-14 of contained trade items
        "10" to -1,  // Batch/Lot (variable, max 20)
        "11" to 6,   // Production date YYMMDD
        "13" to 6,   // Packaging date YYMMDD
        "15" to 6,   // Best before date YYMMDD
        "17" to 6,   // Expiry date YYMMDD
        "20" to 2,   // Internal product variant
        "21" to -1,  // Serial number (variable, max 20)
        "22" to -1,  // Consumer product variant (variable)
        "30" to -1,  // Variable count (variable, max 8)
        "37" to -1,  // Count of trade items (variable)
        "310" to 6,  // Net weight, kg
        "390" to -1, // Applicable amount payable (variable)
        "400" to -1, // Customer PO number (variable)
        "401" to -1, // GINC (variable)
        "402" to 17, // GSIN
        "403" to -1, // Routing code (variable)
        "410" to 13, // Ship-to, GLN
        "412" to 13, // Purchased-from, GLN
        "420" to -1  // Ship-to, postal code (variable)
    )

    fun parse(input: String): Gs1Data {
        // Normalize: strip scanner-mode prefix, replace literal FNC1 marker variants
        val normalized = input.removePrefix("]C1")

        var gtin: String? = null
        var lot: String? = null
        var expiry: String? = null
        var serial: String? = null
        var quantity: String? = null

        var pos = 0
        while (pos < normalized.length) {
            // Skip FNC1 separators
            if (normalized[pos] == FNC1) { pos++; continue }

            // Try 3-digit AI first, then 2-digit
            val ai3 = if (pos + 3 <= normalized.length) normalized.substring(pos, pos + 3) else null
            val ai2 = if (pos + 2 <= normalized.length) normalized.substring(pos, pos + 2) else break

            val (ai, length) = when {
                ai3 != null && AI_LENGTHS.containsKey(ai3) -> Pair(ai3, AI_LENGTHS[ai3]!!)
                AI_LENGTHS.containsKey(ai2) -> Pair(ai2, AI_LENGTHS[ai2]!!)
                else -> { pos++; continue }
            }

            val dataStart = pos + ai.length

            val value: String
            if (length == -1) {
                val fnc1Idx = normalized.indexOf(FNC1, dataStart)
                value = if (fnc1Idx == -1) normalized.substring(dataStart)
                        else normalized.substring(dataStart, fnc1Idx)
                pos = if (fnc1Idx == -1) normalized.length else fnc1Idx + 1
            } else {
                val end = dataStart + length
                if (end > normalized.length) break
                value = normalized.substring(dataStart, end)
                pos = end
            }

            when (ai) {
                "01" -> gtin = value
                "10" -> lot = value
                "17" -> expiry = value
                "21" -> serial = value
                "30" -> quantity = value
            }
        }

        return Gs1Data(gtin = gtin, lot = lot, expiry = expiry, serial = serial, quantity = quantity, raw = input)
    }

    /** Returns true if the string looks like a GS1-encoded barcode. */
    fun isGs1(input: String): Boolean {
        val stripped = input.removePrefix("]C1")
        // Contains FNC1 separator, or starts with AI "01" followed by 14 digits
        return stripped.contains(FNC1) ||
            (stripped.length >= 16 && stripped.startsWith("01") && stripped.substring(2, 16).all { it.isDigit() })
    }
}
