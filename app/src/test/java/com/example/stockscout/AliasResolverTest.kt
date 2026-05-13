package com.example.stockscout

import com.example.stockscout.domain.model.Alias
import com.example.stockscout.domain.model.AliasType
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.utils.AliasResolver
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.google.common.truth.Truth.assertThat

class AliasResolverTest {

    private lateinit var repository: ItemRepository
    private lateinit var resolver: AliasResolver

    private val itemWgt = Item(
        itemCode = "WGT-A",
        name = "Widget Alpha",
        unitOfMeasure = "EA",
        onHandQuantity = 100,
        aliases = listOf(
            Alias(AliasType.UPC_A, "012345678901"),
            Alias(AliasType.EAN_13, "0123456789012"),
            Alias(AliasType.TEXT, "SUPPLIER-SKU-01")
        )
    )

    @Before
    fun setUp() {
        repository = mock()
        resolver = AliasResolver(repository)
    }

    @Test
    fun `resolves by exact item code`() = runTest {
        whenever(repository.getItemByCode("WGT-A")).thenReturn(itemWgt)
        val result = resolver.resolve("WGT-A")
        assertThat(result).isEqualTo(itemWgt)
    }

    @Test
    fun `resolves by UPC-A alias`() = runTest {
        whenever(repository.getItemByCode("012345678901")).thenReturn(null)
        whenever(repository.findItemByAliasValue("012345678901")).thenReturn(itemWgt)
        val result = resolver.resolve("012345678901")
        assertThat(result).isEqualTo(itemWgt)
    }

    @Test
    fun `resolves UPC-A via zero-padded EAN-13 lookup`() = runTest {
        val upc = "012345678901"
        val ean = "0012345678901"
        whenever(repository.getItemByCode(upc)).thenReturn(null)
        whenever(repository.findItemByAliasValue(upc)).thenReturn(null)
        whenever(repository.findItemByAliasValue(ean)).thenReturn(itemWgt)
        val result = resolver.resolve(upc)
        assertThat(result).isEqualTo(itemWgt)
    }

    @Test
    fun `resolves by EAN-13 alias`() = runTest {
        whenever(repository.getItemByCode("0123456789012")).thenReturn(null)
        whenever(repository.findItemByAliasValue("0123456789012")).thenReturn(itemWgt)
        val result = resolver.resolve("0123456789012")
        assertThat(result).isEqualTo(itemWgt)
    }

    @Test
    fun `resolves GS1 string by extracting GTIN to EAN-13`() = runTest {
        // GS1: AI-01 + 14-digit GTIN where last 13 = EAN-13
        val gs1 = "010012345678901217250101"
        val ean13 = "012345678901" // last 13 of GTIN "00012345678901" = "0012345678901" (13 chars)
        // GTIN-14 from GS1: "00123456789012"  → last 13 = "0123456789012"
        val gs1Input = "010012345678901217250101"
        // parse: AI 01 → gtin = "00123456789012", last 13 = "0123456789012"
        val ean = "0123456789012"
        whenever(repository.getItemByCode(gs1Input.trim())).thenReturn(null)
        whenever(repository.findItemByAliasValue(gs1Input.trim())).thenReturn(null)
        whenever(repository.findItemByAliasValue(ean)).thenReturn(itemWgt)
        val result = resolver.resolve(gs1Input)
        assertThat(result).isEqualTo(itemWgt)
    }

    @Test
    fun `returns null when not found`() = runTest {
        val unknown = "UNKNOWN-999"
        whenever(repository.getItemByCode(unknown)).thenReturn(null)
        whenever(repository.findItemByAliasValue(unknown)).thenReturn(null)
        val result = resolver.resolve(unknown)
        assertThat(result).isNull()
    }

    @Test
    fun `returns null for blank input`() = runTest {
        val result = resolver.resolve("  ")
        assertThat(result).isNull()
    }
}
