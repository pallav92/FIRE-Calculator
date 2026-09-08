package com.finance.firecalculator.domain.model

/**
 * User inputs for FIRE scenario calculation.
 */
data class FireInput(
    val currentAge: Int = 30,
    val retirementAge: Int = 50,
    val lifeExpectancy: Int = 85,
    val currentCorpus: Double = 1_000_000.0,
    val monthlyContribution: Double = 25_000.0,
    val monthlyWithdrawalPostRetirement: Double = 50_000.0,
    val isInflationAdjusted: Boolean = true,
    val inflationRatePercent: Double = 6.0,
    val expectedRoiPercent: Double = 10.0,
    val postRetirementRoiPercent: Double = 8.0,
    val isCustomPostRetirementRoi: Boolean = false,
    val currencySymbol: String = "₹"
) {
    val effectivePostRetirementRoiPercent: Double
        get() = if (isCustomPostRetirementRoi) postRetirementRoiPercent else expectedRoiPercent
}

/**
 * Trajectory point representing one year in the projection.
 */
data class YearlyTrajectoryPoint(
    val age: Int,
    val yearOffset: Int,
    val startCorpus: Double,
    val annualContribution: Double,
    val annualWithdrawal: Double,
    val annualGrowth: Double,
    val endCorpus: Double,
    val isRetired: Boolean
)

/**
 * Calculated results and simulation metrics.
 */
data class FireResult(
    val targetCorpusNeeded: Double,
    val perpetualCorpusNeeded: Double,
    val projectedCorpusAtRetirement: Double,
    val corpusSurplusOrShortfall: Double,
    val isFireAchieved: Boolean,
    val adjustedMonthlyWithdrawalAtRetirement: Double,
    val firstYearAnnualWithdrawal: Double,
    val safeWithdrawalRatePercent: Double,
    val corpusExhaustionAge: Int?,
    val yearsToRetirement: Int,
    val yearsInRetirement: Int,
    val trajectory: List<YearlyTrajectoryPoint>
)

/**
 * Saved local user profile entity.
 */
data class UserProfile(
    val id: String,
    val name: String,
    val isGuest: Boolean = false,
    val input: FireInput = FireInput(),
    val lastModified: Long = System.currentTimeMillis()
)
