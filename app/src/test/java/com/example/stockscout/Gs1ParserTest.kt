package com.example.stockscout

import com.example.stockscout.utils.Gs1Parser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class Gs1ParserTest {

    private val FNC1 = '\u001D'

    @Test
    fun `parses GTIN from packed AI-01 string`() {
        val input = "010012345678905217250101"
        val data = Gs1Parser.parse(input)
        assertThat(data.gtin).isEqualTo("00123456789052")
    }

    @Test
    fun `parses expiry from AI-17 following AI-01`() {
        val input = "010012345678905217250630"
        val data = Gs1Parser.parse(input)
        assertThat(data.expiry).isEqualTo("250630")
    }

    @Test
    fun `parses lot from AI-10 after FNC1`() {
        val input = "0100012345678905${FNC1}10LOT456"
        val data = Gs1Parser.parse(input)
        assertThat(data.gtin).isEqualTo("00012345678905")
        assertThat(data.lot).isEqualTo("LOT456")
    }

    @Test
    fun `parses serial from AI-21 after FNC1`() {
        val input = "010012345678905221SERIAL99${FNC1}"
        val data = Gs1Parser.parse(input)
        assertThat(data.serial).isEqualTo("SERIAL99")
    }

    @Test
    fun `handles multiple AIs separated by FNC1`() {
        val input = "0100012345678905${FNC1}10BATCH01${FNC1}17260101${FNC1}21SN001"
        val data = Gs1Parser.parse(input)
        assertThat(data.gtin).isEqualTo("00012345678905")
        assertThat(data.lot).isEqualTo("BATCH01")
        assertThat(data.expiry).isEqualTo("260101")
        assertThat(data.serial).isEqualTo("SN001")
    }

    @Test
    fun `isGs1 returns true for AI-01 prefixed string`() {
        assertThat(Gs1Parser.isGs1("010012345678905217250101")).isTrue()
    }

    @Test
    fun `isGs1 returns false for plain EAN-13`() {
        assertThat(Gs1Parser.isGs1("0123456789012")).isFalse()
    }

    @Test
    fun `raw is preserved unchanged`() {
        val input = "010012345678905217250101"
        assertThat(Gs1Parser.parse(input).raw).isEqualTo(input)
    }
}
