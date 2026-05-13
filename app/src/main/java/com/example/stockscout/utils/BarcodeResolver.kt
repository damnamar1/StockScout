package com.example.stockscout.utils

/**
 * Detects the format of a scanned/typed input and normalizes it for alias lookup.
 */
object BarcodeResolver {

    sealed class ResolvedInput {
        /** Raw input matched against item codes and TEXT aliases directly. */
        data class TextInput(val value: String) : ResolvedInput()
        /** 12-digit UPC-A. Also produce zero-padded EAN-13 for cross-lookup. */
        data class UpcA(val value: String, val asEan13: String) : ResolvedInput()
        /** 13-digit EAN-13. */
        data class Ean13(val value: String) : ResolvedInput()
        /** GS1 string — parsed GTIN-14 extracted and reduced to last 13 digits for lookup. */
        data class Gs1Input(val raw: String, val parsedData: Gs1Parser.Gs1Data, val lookupEan: String?) : ResolvedInput()
    }

    fun resolve(input: String): ResolvedInput {
        val trimmed = input.trim()

        return when {
            Gs1Parser.isGs1(trimmed) -> {
                val data = Gs1Parser.parse(trimmed)
                // Reduce GTIN-14 to last 13 digits (EAN-13 equivalent)
                val lookupEan = data.gtin?.takeIf { it.length >= 13 }?.takeLast(13)
                ResolvedInput.Gs1Input(raw = trimmed, parsedData = data, lookupEan = lookupEan)
            }
            trimmed.length == 13 && trimmed.all { it.isDigit() } ->
                ResolvedInput.Ean13(trimmed)
            trimmed.length == 12 && trimmed.all { it.isDigit() } ->
                ResolvedInput.UpcA(value = trimmed, asEan13 = "0$trimmed")
            else ->
                ResolvedInput.TextInput(trimmed)
        }
    }
}
