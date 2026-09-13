package com.finance.firecalculator.domain

import com.finance.firecalculator.domain.model.Country
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.domain.model.SchemeAnalytics
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

        val activeCorpus = input.effectiveCurrentCorpus
        val activeMonthlyContribution = input.effectiveMonthlyContribution

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
        var runningCorpus = max(0.0, activeCorpus)
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
                runningCorpus += activeMonthlyContribution
                totalYearlyContribution += activeMonthlyContribution

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

        val fireProgressPercent = if (targetCorpusNeeded > 0.0) {
            ((activeCorpus / targetCorpusNeeded) * 100.0).coerceAtLeast(0.0)
        } else {
            100.0
        }

        val projectedFireProgressPercent = if (targetCorpusNeeded > 0.0) {
            ((projectedCorpusAtRetirement / targetCorpusNeeded) * 100.0).coerceAtLeast(0.0)
        } else {
            100.0
        }

        // Coast FIRE: existing corpus compounding until retirement without any further contributions
        val compoundedExisting = activeCorpus * (1.0 + preRoiAnnual).pow(yearsToRetire.toDouble())
        val isCoastFireAchieved = compoundedExisting >= targetCorpusNeeded && targetCorpusNeeded > 0.0
        val coastFireCurrentCorpusNeeded = if (1.0 + preRoiAnnual > 0.0) {
            (targetCorpusNeeded / (1.0 + preRoiAnnual).pow(yearsToRetire.toDouble())).coerceAtLeast(0.0)
        } else {
            targetCorpusNeeded
        }

        val leanFireCorpusNeeded = firstYearAnnualWithdrawal * 15.0
        val fatFireCorpusNeeded = firstYearAnnualWithdrawal * 33.0

        // 5. Scheme Analytics
        val schemeAnalytics = if (input.country == Country.USA) {
            val bridgeYears = max(0, 60 - safeRetirementAge)
            val bridgeCorpusNeeded = if (bridgeYears > 0) {
                var bridgeSum = 0.0
                for (bYear in 0 until bridgeYears) {
                    val inflFactor = (1.0 + inflationAnnual).pow(bYear.toDouble())
                    bridgeSum += firstYearAnnualWithdrawal * inflFactor
                }
                bridgeSum
            } else {
                0.0
            }

            var projBrokerage = input.usaSchemes.taxableBrokerageBalance
            for (y in 1..yearsToRetire) {
                for (m in 1..12) {
                    projBrokerage += input.usaSchemes.taxableBrokerageMonthlyContribution
                    projBrokerage += projBrokerage * preRoiMonthly
                }
            }

            SchemeAnalytics(
                earlyRetirementBridgeYears = bridgeYears,
                earlyBridgeCorpusNeeded = bridgeCorpusNeeded,
                projectedTaxableBrokerageAtRetirement = projBrokerage,
                isEarlyBridgeCovered = bridgeYears == 0 || projBrokerage >= bridgeCorpusNeeded,
                annual401kEmployerMatchTotal = input.usaSchemes.monthlyEmployerMatch * 12.0
            )
        } else {
            var projNps = input.indiaSchemes.npsBalance
            for (y in 1..yearsToRetire) {
                for (m in 1..12) {
                    projNps += input.indiaSchemes.npsMonthlyContribution
                    projNps += projNps * preRoiMonthly
                }
            }
            val mandatoryAnnuity = projNps * 0.40
            val taxFreeLumpSum = projNps * 0.60
            val monthlyPension = (mandatoryAnnuity * 0.06) / 12.0

            SchemeAnalytics(
                npsProjectedCorpusAtRetirement = projNps,
                npsMandatoryAnnuityLumpSum = mandatoryAnnuity,
                npsTaxFreeLumpSum = taxFreeLumpSum,
                npsMonthlyEstimatedPension = monthlyPension
            )
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
            trajectory = trajectory,
            fireProgressPercent = fireProgressPercent,
            projectedFireProgressPercent = projectedFireProgressPercent,
            isCoastFireAchieved = isCoastFireAchieved,
            coastFireCurrentCorpusNeeded = coastFireCurrentCorpusNeeded,
            leanFireCorpusNeeded = leanFireCorpusNeeded,
            fatFireCorpusNeeded = fatFireCorpusNeeded,
            schemeAnalytics = schemeAnalytics
        )
    }
}
