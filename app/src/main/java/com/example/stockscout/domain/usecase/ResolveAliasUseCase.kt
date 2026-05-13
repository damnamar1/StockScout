package com.example.stockscout.domain.usecase

import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.AliasResolver
import javax.inject.Inject

class ResolveAliasUseCase @Inject constructor(
    private val repository: ItemRepository,
    private val aliasResolver: AliasResolver
) {
    suspend operator fun invoke(input: String): Item? =
        aliasResolver.resolve(input)
}
