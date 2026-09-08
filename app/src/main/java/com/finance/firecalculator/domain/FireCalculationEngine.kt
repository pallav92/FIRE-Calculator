package com.finance.firecalculator.domain

import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.domain.model.YearlyTrajectoryPoint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object FireCalculationEngine {

    /**
     * Calculates the complete FIRE metrics, retirement target, and year-by-year trajectory.
     */
    fun calculate(input: FireInput): FireResult {
        val safeCurrentAge = max(18, min(input.currentAge, 90))
        val safeRetirementAge = max(safeCurrentAge, min(input.retirementAge, 95))
        val safeLifeExpectancy = max(safeRetirementAge + 1, min(input.lifeExpectancy, 105))

        val yearsToRetire = safeRetirementAge - safeCurrentAge
        val yearsInRetire = safeLifeExpectancy - safeRetirementAge

        val preRoiAnnual = max(0.0, input.expectedRoiPercent) / 100.0
        val postRoiAnnual = max(0.0, input.effectivePostRetirementRoiPercent) / 100.0
        val inflationAnnual = if (input.isInflationAdjusted) max(0.0, input.inflationRatePercent) / 100.0 else 0.0

        val preRoiMonthly = preRoiAnnual / 12.0
        val postRoiMonthly = postRoiAnnual / 12.0

        // 1. Calculate adjusted monthly withdrawal at retirement
        val inflationFactorAtRetirement = (1.0 + inflationAnnual).pow(yearsToRetire.toDouble())
        val adjustedMonthlyWithdrawalAtRetirement = input.monthlyWithdrawalPostRetirement * inflationFactorAtRetirement
        val firstYearAnnualWithdrawal = adjustedMonthlyWithdrawalAtRetirement * 12.0

        // 2. Calculate target corpus needed at retirement using real return / growing annuity
        val realRateOfReturn = if (1.0 + inflationAnnual > 0.0) {
            ((1.0 + postRoiAnnual) / (1.0 + inflationAnnual)) - 1.0
        } else {
            postRoiAnnual
        }

        val targetCorpusNeeded: Double = if (yearsInRetire <= 0) {
            0.0
        } else if (kotlin.math.abs(realRateOfReturn) < 1e-6) {
            firstYearAnnualWithdrawal * yearsInRetire
        } else {
            firstYearAnnualWithdrawal * ((1.0 - (1.0 + realRateOfReturn).pow(-yearsInRetire.toDouble())) / realRateOfReturn)
        }.coerceAtLeast(0.0)

        // 3. Perpetual corpus needed (4% rule: 25x annual expenses)
        val perpetualCorpusNeeded = firstYearAnnualWithdrawal * 25.0

        // 4. Simulate trajectory year-by-year (Accumulation phase & Retirement phase)
        val trajectory = mutableListOf<YearlyTrajectoryPoint>()
        var runningCorpus = max(0.0, input.currentCorpus)
        var projectedCorpusAtRetirement = runningCorpus
        var exhaustionAge: Int? = null

        // Year 0 (Current state snapshot)
        trajectory.add(
            YearlyTrajectoryPoint(
                age = safeCurrentAge,
                yearOffset = 0,
                startCorpus = runningCorpus,
                annualContribution = 0.0,
                annualWithdrawal = 0.0,
                annualGrowth = 0.0,
                endCorpus = runningCorpus,
                isRetired = false
            )
        )

        // Accumulation Phase: from safeCurrentAge to safeRetirementAge
        for (year in 1..yearsToRetire) {
            val startYearCorpus = runningCorpus
            var totalYearlyContribution = 0.0
            var totalYearlyGrowth = 0.0

            for (month in 1..12) {
                runningCorpus += input.monthlyContribution
                totalYearlyContribution += input.monthlyContribution

                val monthlyGain = runningCorpus * preRoiMonthly
                runningCorpus += monthlyGain
                totalYearlyGrowth += monthlyGain
            }

            val currentAgeThisYear = safeCurrentAge + year
            trajectory.add(
                YearlyTrajectoryPoint(
                    age = currentAgeThisYear,
                    yearOffset = year,
                    startCorpus = startYearCorpus,
                    annualContribution = totalYearlyContribution,
                    annualWithdrawal = 0.0,
                    annualGrowth = totalYearlyGrowth,
                    endCorpus = runningCorpus,
                    isRetired = false
                )
            )
        }

        projectedCorpusAtRetirement = runningCorpus

        // Retirement Phase: from safeRetirementAge to safeLifeExpectancy
        for (retYear in 1..yearsInRetire) {
            val startYearCorpus = runningCorpus
            val yearsFromRetirementStart = retYear - 1
            val annualInflationEscalation = (1.0 + inflationAnnual).pow(yearsFromRetirementStart.toDouble())
            val yearlyWithdrawalTarget = firstYearAnnualWithdrawal * annualInflationEscalation
            val monthlyWithdrawalTarget = yearlyWithdrawalTarget / 12.0

            var totalYearlyWithdrawal = 0.0
            var totalYearlyGrowth = 0.0

            for (month in 1..12) {
                if (runningCorpus > 0.0) {
                    val actualWithdrawal = min(runningCorpus, monthlyWithdrawalTarget)
                    runningCorpus -= actualWithdrawal
                    totalYearlyWithdrawal += actualWithdrawal

                    if (runningCorpus > 0.0) {
                        val monthlyGain = runningCorpus * postRoiMonthly
                        runningCorpus += monthlyGain
                        totalYearlyGrowth += monthlyGain
                    } else if (exhaustionAge == null) {
                        exhaustionAge = safeRetirementAge + retYear
                    }
                } else if (exhaustionAge == null) {
                    exhaustionAge = safeRetirementAge + retYear
                }
            }

            val currentAgeThisYear = safeRetirementAge + retYear
            trajectory.add(
                YearlyTrajectoryPoint(
                    age = currentAgeThisYear,
                    yearOffset = yearsToRetire + retYear,
                    startCorpus = startYearCorpus,
                    annualContribution = 0.0,
                    annualWithdrawal = totalYearlyWithdrawal,
                    annualGrowth = totalYearlyGrowth,
                    endCorpus = runningCorpus,
                    isRetired = true
                )
            )
        }

        val surplusOrShortfall = projectedCorpusAtRetirement - targetCorpusNeeded
        val isFireAchieved = projectedCorpusAtRetirement >= targetCorpusNeeded && targetCorpusNeeded > 0

        val safeWithdrawalRatePercent = if (projectedCorpusAtRetirement > 0.0) {
            (firstYearAnnualWithdrawal / projectedCorpusAtRetirement) * 100.0
        } else {
            0.0
        }

        return FireResult(
            targetCorpusNeeded = targetCorpusNeeded,
            perpetualCorpusNeeded = perpetualCorpusNeeded,
            projectedCorpusAtRetirement = projectedCorpusAtRetirement,
            corpusSurplusOrShortfall = surplusOrShortfall,
            isFireAchieved = isFireAchieved,
            adjustedMonthlyWithdrawalAtRetirement = adjustedMonthlyWithdrawalAtRetirement,
            firstYearAnnualWithdrawal = firstYearAnnualWithdrawal,
            safeWithdrawalRatePercent = safeWithdrawalRatePercent,
            corpusExhaustionAge = exhaustionAge,
            yearsToRetirement = yearsToRetire,
            yearsInRetirement = yearsInRetire,
            trajectory = trajectory
        )
    }
}
