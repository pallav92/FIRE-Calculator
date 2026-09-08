package com.finance.firecalculator.domain

import com.finance.firecalculator.domain.model.FireInput
import org.junit.Assert.*
import org.junit.Test

class FireCalculationEngineTest {

    @Test
    fun testInflationToggleEffects() {
        val inputWithInflation = FireInput(
            currentAge = 30,
            retirementAge = 50,
            monthlyWithdrawalPostRetirement = 3_000.0,
            isInflationAdjusted = true,
            inflationRatePercent = 6.0
        )
        val resultWithInflation = FireCalculationEngine.calculate(inputWithInflation)

        val inputWithoutInflation = FireInput(
            currentAge = 30,
            retirementAge = 50,
            monthlyWithdrawalPostRetirement = 3_000.0,
            isInflationAdjusted = false,
            inflationRatePercent = 6.0
        )
        val resultWithoutInflation = FireCalculationEngine.calculate(inputWithoutInflation)

        // With 6% inflation over 20 years, monthly expense increases by ~3.2x
        assertTrue(resultWithInflation.adjustedMonthlyWithdrawalAtRetirement > 3_000.0 * 3.0)
        assertEquals(3_000.0, resultWithoutInflation.adjustedMonthlyWithdrawalAtRetirement, 0.01)

        // Target corpus needed should be significantly higher with inflation
        assertTrue(resultWithInflation.targetCorpusNeeded > resultWithoutInflation.targetCorpusNeeded)
    }

    @Test
    fun testAccumulationPhaseCompoundGrowth() {
        val input = FireInput(
            currentAge = 30,
            retirementAge = 40, // 10 years
            currentCorpus = 100_000.0,
            monthlyContribution = 1_000.0,
            expectedRoiPercent = 10.0
        )
        val result = FireCalculationEngine.calculate(input)

        // 100k compounded at 10% over 10 years is > 250k, plus 1k/month SIP is > 200k
        assertTrue("Projected corpus should exceed 450k", result.projectedCorpusAtRetirement > 450_000.0)
        assertEquals(10, result.yearsToRetirement)
        assertEquals(11, result.trajectory.filter { !it.isRetired }.size) // age 30 to 40 inclusive
    }

    @Test
    fun testCorpusExhaustionDetection() {
        // High monthly withdrawal, small initial savings -> funds should exhaust early
        val input = FireInput(
            currentAge = 30,
            retirementAge = 35,
            lifeExpectancy = 85,
            currentCorpus = 10_000.0,
            monthlyContribution = 100.0,
            monthlyWithdrawalPostRetirement = 10_000.0, // High withdrawal
            expectedRoiPercent = 5.0
        )
        val result = FireCalculationEngine.calculate(input)

        assertNotNull("Corpus exhaustion should be detected", result.corpusExhaustionAge)
        assertTrue(result.corpusExhaustionAge!! < input.lifeExpectancy)
        assertFalse(result.isFireAchieved)
        assertTrue(result.corpusSurplusOrShortfall < 0.0)
    }

    @Test
    fun testRetirementAgeEqualsCurrentAge() {
        // Retiring immediately
        val input = FireInput(
            currentAge = 40,
            retirementAge = 40,
            currentCorpus = 1_000_000.0,
            monthlyContribution = 0.0,
            monthlyWithdrawalPostRetirement = 2_000.0
        )
        val result = FireCalculationEngine.calculate(input)

        assertEquals(0, result.yearsToRetirement)
        assertEquals(1_000_000.0, result.projectedCorpusAtRetirement, 0.01)
        assertEquals(2_000.0, result.adjustedMonthlyWithdrawalAtRetirement, 0.01)
    }

    @Test
    fun testPerpetualCorpusFormula() {
        val input = FireInput(
            currentAge = 30,
            retirementAge = 30,
            monthlyWithdrawalPostRetirement = 4_000.0,
            isInflationAdjusted = false
        )
        val result = FireCalculationEngine.calculate(input)

        // Annual expense = 4000 * 12 = 48,000. 25x = 1,200,000
        assertEquals(48_000.0, result.firstYearAnnualWithdrawal, 0.01)
        assertEquals(1_200_000.0, result.perpetualCorpusNeeded, 0.01)
    }
}
