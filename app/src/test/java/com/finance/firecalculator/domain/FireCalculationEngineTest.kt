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

    @Test
    fun testZeroContributionsPostRetirement() {
        val input = FireInput(
            currentAge = 30,
            retirementAge = 50,
            lifeExpectancy = 80,
            currentCorpus = 50_000_000.0, // 5 Crores funded corpus
            monthlyContribution = 50_000.0,
            monthlyWithdrawalPostRetirement = 100_000.0
        )
        val result = FireCalculationEngine.calculate(input)

        // Trajectory points during accumulation phase (age 31 to 50) must have contributions
        val accumulationPoints = result.trajectory.filter { !it.isRetired && it.yearOffset > 0 }
        assertTrue("Accumulation points exist", accumulationPoints.isNotEmpty())
        accumulationPoints.forEach { pt ->
            assertEquals("Contribution during accumulation should be 12 * 50,000", 600_000.0, pt.annualContribution, 0.01)
        }

        // Trajectory points during retirement phase (age 51 to 80) must have ZERO contributions
        val retirementPoints = result.trajectory.filter { it.isRetired }
        assertTrue("Retirement points exist", retirementPoints.isNotEmpty())
        retirementPoints.forEach { pt ->
            assertEquals("Contribution post-retirement at age ${pt.age} MUST be zero", 0.0, pt.annualContribution, 0.0)
            assertTrue("Withdrawal post-retirement should occur while funded", pt.annualWithdrawal > 0.0)
        }
    }

    @Test
    fun testUsa401kEmployerMatchAndBridgeCalculation() {
        val input = FireInput(
            country = com.finance.firecalculator.domain.model.Country.USA,
            currentAge = 35,
            retirementAge = 50, // Retiring 10 years before 60
            useSchemeBreakdown = true,
            usaSchemes = com.finance.firecalculator.domain.model.UsaSchemeInput(
                k401Balance = 200_000.0,
                k401MonthlyContribution = 1_500.0,
                k401EmployerMatchPercent = 50.0, // $750/mo match
                k401EmployerMatchLimitMonthly = 750.0,
                iraBalance = 50_000.0,
                iraMonthlyContribution = 500.0,
                taxableBrokerageBalance = 150_000.0,
                taxableBrokerageMonthlyContribution = 500.0
            )
        )
        val result = FireCalculationEngine.calculate(input)

        // Verify effective contribution includes employer match: 1500 + 750 + 500 + 500 = 3,250
        assertEquals(3_250.0, input.effectiveMonthlyContribution, 0.01)

        // Verify bridge analytics: retiring at 50 gives 10 bridge years until 60
        assertEquals(10, result.schemeAnalytics.earlyRetirementBridgeYears)
        assertTrue(result.schemeAnalytics.earlyBridgeCorpusNeeded > 0.0)
        assertTrue(result.schemeAnalytics.annual401kEmployerMatchTotal == 750.0 * 12.0)
    }

    @Test
    fun testIndiaNpsMandatoryAnnuityCalculation() {
        val input = FireInput(
            country = com.finance.firecalculator.domain.model.Country.INDIA,
            currentAge = 30,
            retirementAge = 55,
            useSchemeBreakdown = true,
            indiaSchemes = com.finance.firecalculator.domain.model.IndiaSchemeInput(
                epfBalance = 500_000.0,
                epfMonthlyContribution = 15_000.0,
                npsBalance = 300_000.0,
                npsMonthlyContribution = 5_000.0,
                mutualFundsBalance = 1_000_000.0,
                mutualFundsMonthlyContribution = 20_000.0
            )
        )
        val result = FireCalculationEngine.calculate(input)

        val analytics = result.schemeAnalytics
        assertTrue("Projected NPS should compound positively", analytics.npsProjectedCorpusAtRetirement > 300_000.0)
        // 40% annuity and 60% lump sum split
        assertEquals(analytics.npsProjectedCorpusAtRetirement * 0.40, analytics.npsMandatoryAnnuityLumpSum, 0.01)
        assertEquals(analytics.npsProjectedCorpusAtRetirement * 0.60, analytics.npsTaxFreeLumpSum, 0.01)
        assertTrue(analytics.npsMonthlyEstimatedPension > 0.0)
    }
}
