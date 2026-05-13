package com.example.stockscout.utils

import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.repository.ItemRepository
import javax.inject.Inject

/**
 * Resolves any user input (scan or text) to an Item.
 *
 * Resolution order:
 *  1. Exact item code match
 *  2. Exact alias value match (stored in DB index)
 *  3. Barcode-normalized lookup (UPC-A → EAN-13, GS1 → GTIN last 13 digits)
 */
class AliasResolver @Inject constructor(
    private val repository: ItemRepository
) {
    suspend fun resolve(input: String): Item? {
        if (input.isBlank()) return null

        // 1. Exact item code
        repository.getItemByCode(input.trim())?.let { return it }

        // 2. Exact alias match
        repository.findItemByAliasValue(input.trim())?.let { return it }

        // 3. Normalized barcode lookup
        return when (val resolved = BarcodeResolver.resolve(input)) {
            is BarcodeResolver.ResolvedInput.UpcA -> {
                // Try raw UPC-A, then zero-padded as EAN-13
                repository.findItemByAliasValue(resolved.value)
                    ?: repository.findItemByAliasValue(resolved.asEan13)
            }
            is BarcodeResolver.ResolvedInput.Ean13 ->
                repository.findItemByAliasValue(resolved.value)
            is BarcodeResolver.ResolvedInput.Gs1Input -> {
                resolved.lookupEan?.let { repository.findItemByAliasValue(it) }
            }
            is BarcodeResolver.ResolvedInput.TextInput -> null
        }
    }
}
