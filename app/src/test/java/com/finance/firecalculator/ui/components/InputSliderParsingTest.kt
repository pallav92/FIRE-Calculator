package com.finance.firecalculator.ui.components

import org.junit.Assert.*
import org.junit.Test

class InputSliderParsingTest {

    @Test
    fun testParseCrores() {
        assertEquals(10_000_000f, parseInputNumber("1 cr") ?: 0f, 1f)
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
        assertEquals(50_000_000f, parseInputNumber("$50,000,000") ?: 0f, 1f)
        assertEquals(10_000_000f, parseInputNumber("1,00,00,000") ?: 0f, 1f)
    }

    @Test
    fun testAdaptiveSliderBoundsMath() {
        // Test -50% to +150% calculation for 10 Lakhs (1,000,000)
        val value = 1_000_000f
        val absRange = 0f..990_000_000f
        val minB = (value * 0.5f).coerceAtLeast(absRange.start)
        val maxB = (value * 2.5f).coerceAtMost(absRange.endInclusive)

        assertEquals(500_000f, minB, 0.01f)   // -50%
        assertEquals(2_500_000f, maxB, 0.01f) // +150%
    }

    @Test
    fun testAdaptiveSliderBoundsAtAbsoluteLimits() {
        val absRangeCorpus = 0f..990_000_000f

        // Near 99 Crores: should clamp to 990,000,000 max
        val highCorpus = 500_000_000f // 50 Crores
        val minHigh = (highCorpus * 0.5f).coerceAtLeast(absRangeCorpus.start)
        val maxHigh = (highCorpus * 2.5f).coerceAtMost(absRangeCorpus.endInclusive)

        assertEquals(250_000_000f, minHigh, 0.01f) // 25 Crores
        assertEquals(990_000_000f, maxHigh, 0.01f) // Clamped at 99 Crores

        // Monthly contributions: 100 to 1,000,000
        val absRangeContrib = 100f..1_000_000f
        val lowContrib = 100f
        val minContrib = (lowContrib * 0.5f).coerceAtLeast(absRangeContrib.start)
        val maxContrib = (lowContrib * 2.5f).coerceAtMost(absRangeContrib.endInclusive)

        assertEquals(100f, minContrib, 0.01f) // Clamped to min 100
        assertEquals(250f, maxContrib, 0.01f)

        val highContrib = 1_000_000f
        val minHighContrib = (highContrib * 0.5f).coerceAtLeast(absRangeContrib.start)
        val maxHighContrib = (highContrib * 2.5f).coerceAtMost(absRangeContrib.endInclusive)

        assertEquals(500_000f, minHighContrib, 0.01f)
        assertEquals(1_000_000f, maxHighContrib, 0.01f) // Clamped to max 10 Lakhs
    }
}
