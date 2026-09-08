package com.finance.firecalculator.ui.components

import com.finance.firecalculator.ui.util.CurrencyFormatter
import org.junit.Assert.*
import org.junit.Test

class InputSliderParsingTest {

    @Test
    fun testParseCrores() {
        assertEquals(10_000_000f, parseInputNumber("1 cr") ?: 0f, 1f)
        assertEquals(12_500_000f, parseInputNumber("1.25 cr") ?: 0f, 1f)
        assertEquals(12_500_000f, parseInputNumber("1.25 crore") ?: 0f, 1f)
        assertEquals(12_500_000f, parseInputNumber("1.25 crores") ?: 0f, 1f)
        assertEquals(500_000_000f, parseInputNumber("50 cr") ?: 0f, 1f)
        assertEquals(990_000_000f, parseInputNumber("99 crore") ?: 0f, 1f)
        assertEquals(990_000_000f, parseInputNumber("99 crores") ?: 0f, 1f)
        assertEquals(25_000_000f, parseInputNumber("2.5 cr") ?: 0f, 1f)
    }

    @Test
    fun testParseLakhs() {
        assertEquals(100_000f, parseInputNumber("1 l") ?: 0f, 1f)
        assertEquals(5_000_000f, parseInputNumber("50 lakh") ?: 0f, 1f)
        assertEquals(10_000_000f, parseInputNumber("100 lakhs") ?: 0f, 1f)
        assertEquals(250_000f, parseInputNumber("2.5 lac") ?: 0f, 1f)
    }

    @Test
    fun testParseThousandsAndMillions() {
        assertEquals(10_000f, parseInputNumber("10k") ?: 0f, 1f)
        assertEquals(250_000f, parseInputNumber("250k") ?: 0f, 1f)
        assertEquals(1_000_000f, parseInputNumber("1m") ?: 0f, 1f)
        assertEquals(5_000_000f, parseInputNumber("5 million") ?: 0f, 1f)
        assertEquals(1_000_000_000f, parseInputNumber("1b") ?: 0f, 1f)
    }

    @Test
    fun testParseFormattedWithCommasAndCurrency() {
        assertEquals(100_000f, parseInputNumber("₹1,00,000") ?: 0f, 1f)
        assertEquals(12_500_000f, parseInputNumber("₹1,25,00,000") ?: 0f, 1f)
        assertEquals(50_000_000f, parseInputNumber("$50,000,000") ?: 0f, 1f)
        assertEquals(10_000_000f, parseInputNumber("1,00,00,000") ?: 0f, 1f)
    }

    @Test
    fun testSliderEndValueIsOnePointTwoFiveCrores() {
        // When value is 50 Lakhs (5,000,000):
        // -50% = 25 Lakhs (2,500,000)
        // +150% = 1.25 Crores (12,500,000)
        val value = 5_000_000f
        val absRange = 0f..990_000_000f
        val (minB, maxB) = calculateSliderBounds(value, absRange, isAdaptive = true)

        assertEquals(2_500_000f, minB, 0.01f)
        assertEquals(12_500_000f, maxB, 0.01f) // Exactly 1.25 Crores

        val formattedMin = CurrencyFormatter.formatCompact(minB.toDouble(), "₹")
        val formattedMax = CurrencyFormatter.formatCompact(maxB.toDouble(), "₹")

        assertEquals("₹25 L", formattedMin)
        assertEquals("₹1.25 Cr", formattedMax) // Cleanly formats as 1.25 Cr without extra zeros
    }

    @Test
    fun testSliderValueAtOnePointTwoFiveCrores() {
        // When value is 1.25 Crores (12,500,000):
        // -50% = 62.5 Lakhs (6,250,000)
        // +150% = 3.125 Crores (31,250,000)
        val value = 12_500_000f
        val absRange = 0f..990_000_000f
        val (minB, maxB) = calculateSliderBounds(value, absRange, isAdaptive = true)

        assertEquals(6_250_000f, minB, 0.01f)
        assertEquals(31_250_000f, maxB, 0.01f)

        val formattedMin = CurrencyFormatter.formatCompact(minB.toDouble(), "₹")
        val formattedMax = CurrencyFormatter.formatCompact(maxB.toDouble(), "₹")

        assertEquals("₹62.5 L", formattedMin)
        assertEquals("₹3.13 Cr", formattedMax)
    }

    @Test
    fun testEndValuesDoNotExceedMobileScreenWidth() {
        // Verify formatted end values stay compact (< 15 characters) even at extreme magnitudes
        val testValues = listOf(
            0.0,
            100.0,
            25_000.0,
            500_000.0,
            1_250_000.0,
            5_000_000.0,
            12_500_000.0,  // 1.25 Cr
            50_000_000.0,  // 5 Cr
            990_000_000.0  // 99 Cr
        )

        for (v in testValues) {
            val formatted = CurrencyFormatter.formatCompact(v, "₹")
            assertTrue("Formatted '$formatted' should be <= 12 characters to prevent UI overflow", formatted.length <= 12)
        }
    }

    @Test
    fun testAdaptiveSliderBoundsAtAbsoluteLimits() {
        val absRangeCorpus = 0f..990_000_000f

        // Near 99 Crores: should clamp to 990,000,000 max
        val highCorpus = 500_000_000f // 50 Crores
        val (minHigh, maxHigh) = calculateSliderBounds(highCorpus, absRangeCorpus, isAdaptive = true)

        assertEquals(250_000_000f, minHigh, 0.01f) // 25 Crores
        assertEquals(990_000_000f, maxHigh, 0.01f) // Clamped at 99 Crores

        // Monthly contributions: 100 to 1,000,000
        val absRangeContrib = 100f..1_000_000f
        val lowContrib = 100f
        val (minContrib, maxContrib) = calculateSliderBounds(lowContrib, absRangeContrib, isAdaptive = true)

        assertEquals(100f, minContrib, 0.01f) // Clamped to min 100
        assertEquals(250f, maxContrib, 0.01f)

        val highContrib = 1_000_000f
        val (minHighContrib, maxHighContrib) = calculateSliderBounds(highContrib, absRangeContrib, isAdaptive = true)

        assertEquals(500_000f, minHighContrib, 0.01f)
        assertEquals(1_000_000f, maxHighContrib, 0.01f) // Clamped to max 10 Lakhs
    }

    @Test
    fun testZeroCorpusSliderBounds() {
        val absRange = 0f..990_000_000f
        val (minB, maxB) = calculateSliderBounds(0f, absRange, isAdaptive = true, defaultZeroMax = 10_000_000f)

        assertEquals(0f, minB, 0.01f)
        assertEquals(10_000_000f, maxB, 0.01f) // 1 Crore base window from 0
        assertEquals("₹0", CurrencyFormatter.formatCompact(minB.toDouble(), "₹"))
        assertEquals("₹1 Cr", CurrencyFormatter.formatCompact(maxB.toDouble(), "₹"))
    }
}
