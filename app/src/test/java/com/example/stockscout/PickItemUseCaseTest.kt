package com.example.stockscout

import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.repository.ItemRepository
import com.example.stockscout.domain.usecase.PickItemUseCase
import com.example.stockscout.utils.Resource
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PickItemUseCaseTest {

    private lateinit var repository: ItemRepository
    private lateinit var useCase: PickItemUseCase

    private fun itemWithQty(q: Int) = Item(
        itemCode = "WGT-A",
        name = "Widget Alpha",
        unitOfMeasure = "EA",
        onHandQuantity = q,
        aliases = emptyList()
    )

    @Before
    fun setUp() {
        repository = mock()
        useCase = PickItemUseCase(repository)
    }

    @Test
    fun `valid pick decrements and inserts pending pick`() = runTest {
        whenever(repository.getItemByCode("WGT-A")).thenReturn(itemWithQty(10))
        whenever(repository.insertPendingPick("WGT-A", 9)).thenReturn(1L)

        val result = useCase("WGT-A")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        verify(repository).decrementItemQuantity("WGT-A")
        verify(repository).insertPendingPick("WGT-A", 9)
    }

    @Test
    fun `pick from 1 to 0 is allowed and posts newQuantity zero`() = runTest {
        whenever(repository.getItemByCode("WGT-A")).thenReturn(itemWithQty(1))
        whenever(repository.insertPendingPick("WGT-A", 0)).thenReturn(2L)

        val result = useCase("WGT-A")

        assertThat(result).isInstanceOf(Resource.Success::class.java)
        verify(repository).decrementItemQuantity("WGT-A")
        verify(repository).insertPendingPick("WGT-A", 0)
    }

    @Test
    fun `pick at zero quantity returns Error and does not mutate state`() = runTest {
        whenever(repository.getItemByCode("WGT-A")).thenReturn(itemWithQty(0))

        val result = useCase("WGT-A")

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        verify(repository, never()).decrementItemQuantity(org.mockito.kotlin.any())
        verify(repository, never()).insertPendingPick(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    @Test
    fun `missing item returns Error`() = runTest {
        whenever(repository.getItemByCode("MISSING")).thenReturn(null)

        val result = useCase("MISSING")

        assertThat(result).isInstanceOf(Resource.Error::class.java)
        verify(repository, never()).decrementItemQuantity(org.mockito.kotlin.any())
    }
}
