package com.finance.firecalculator.domain.model

/**
 * Supported countries with regional financial defaults.
 */
enum class Country(
    val code: String,
    val displayName: String,
    val flagEmoji: String,
    val currencySymbol: String,
    val defaultInflation: Double,
    val defaultRoi: Double,
    val defaultPostRetirementRoi: Double,
    val defaultCurrentCorpus: Double,
    val defaultMonthlyContribution: Double,
    val defaultMonthlyWithdrawal: Double,
    val corpusRange: ClosedFloatingPointRange<Float>,
    val contributionRange: ClosedFloatingPointRange<Float>,
    val withdrawalRange: ClosedFloatingPointRange<Float>,
    val corpusStep: Float,
    val contributionStep: Float,
    val withdrawalStep: Float
) {
    INDIA(
        code = "IN",
        displayName = "India",
        flagEmoji = "🇮🇳",
        currencySymbol = "₹",
        defaultInflation = 6.0,
        defaultRoi = 11.0,
        defaultPostRetirementRoi = 8.0,
        defaultCurrentCorpus = 1_500_000.0, // 15 Lakhs
        defaultMonthlyContribution = 30_000.0, // 30k/mo
        defaultMonthlyWithdrawal = 60_000.0, // 60k/mo
        corpusRange = 0f..990_000_000f, // 0 to 99 Cr
        contributionRange = 100f..1_000_000f, // 100 to 10 Lakhs
        withdrawalRange = 500f..50_000_000f, // 500 to 5 Cr
        corpusStep = 100_000f, // 1 Lakh
        contributionStep = 1_000f,
        withdrawalStep = 1_000f
    ),
    USA(
        code = "US",
        displayName = "United States",
        flagEmoji = "🇺🇸",
        currencySymbol = "$",
        defaultInflation = 3.0,
        defaultRoi = 8.5,
        defaultPostRetirementRoi = 6.5,
        defaultCurrentCorpus = 120_000.0, // $120k
        defaultMonthlyContribution = 2_000.0, // $2,000/mo
        defaultMonthlyWithdrawal = 5_000.0, // $5,000/mo
        corpusRange = 0f..25_000_000f, // $0 to $25 Million
        contributionRange = 50f..50_000f, // $50 to $50k/mo
        withdrawalRange = 200f..100_000f, // $200 to $100k/mo
        corpusStep = 10_000f, // $10k
        contributionStep = 100f, // $100
        withdrawalStep = 100f // $100
    )
}

/**
 * Breakdown of USA retirement schemes: 401(k), IRA, and Taxable Brokerage.
 */
data class UsaSchemeInput(
    val k401Balance: Double = 60_000.0,
    val k401MonthlyContribution: Double = 1_200.0, // ~$14.4k/yr (IRS limit $23,000)
    val k401EmployerMatchPercent: Double = 50.0, // 50% match
    val k401EmployerMatchLimitMonthly: Double = 600.0, // Max monthly employer match
    val iraBalance: Double = 25_000.0,
    val iraMonthlyContribution: Double = 500.0, // $6,000/yr (IRS limit $7,000)
    val taxableBrokerageBalance: Double = 35_000.0,
    val taxableBrokerageMonthlyContribution: Double = 300.0
) {
    val totalBalance: Double
        get() = k401Balance + iraBalance + taxableBrokerageBalance

    val monthlyEmployerMatch: Double
        get() = (k401MonthlyContribution * (k401EmployerMatchPercent / 100.0))
            .coerceAtMost(k401EmployerMatchLimitMonthly)

    val effectiveMonthlyContribution: Double
        get() = k401MonthlyContribution + monthlyEmployerMatch + iraMonthlyContribution + taxableBrokerageMonthlyContribution
}

/**
 * Breakdown of India retirement schemes: EPF/PPF, NPS, and Mutual Funds.
 */
data class IndiaSchemeInput(
    val epfBalance: Double = 600_000.0, // 6 Lakhs
    val epfMonthlyContribution: Double = 12_000.0,
    val npsBalance: Double = 300_000.0, // 3 Lakhs
    val npsMonthlyContribution: Double = 5_000.0, // ~60k/yr
    val mutualFundsBalance: Double = 600_000.0, // 6 Lakhs
    val mutualFundsMonthlyContribution: Double = 13_000.0
) {
    val totalBalance: Double
        get() = epfBalance + npsBalance + mutualFundsBalance

    val effectiveMonthlyContribution: Double
        get() = epfMonthlyContribution + npsMonthlyContribution + mutualFundsMonthlyContribution
}

/**
 * Country-specific scheme analytics and milestone insights.
 */
data class SchemeAnalytics(
    // USA Insights
    val earlyRetirementBridgeYears: Int = 0,
    val earlyBridgeCorpusNeeded: Double = 0.0,
    val projectedTaxableBrokerageAtRetirement: Double = 0.0,
    val isEarlyBridgeCovered: Boolean = true,
    val annual401kEmployerMatchTotal: Double = 0.0,

    // India Insights
    val npsProjectedCorpusAtRetirement: Double = 0.0,
    val npsMandatoryAnnuityLumpSum: Double = 0.0,
    val npsTaxFreeLumpSum: Double = 0.0,
    val npsMonthlyEstimatedPension: Double = 0.0
)

/**
 * User inputs for FIRE scenario calculation.
 */
data class FireInput(
    val country: Country = Country.INDIA,
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
    val currencySymbol: String = "₹",
    val useSchemeBreakdown: Boolean = false,
    val usaSchemes: UsaSchemeInput = UsaSchemeInput(),
    val indiaSchemes: IndiaSchemeInput = IndiaSchemeInput()
) {
    val effectiveCurrentCorpus: Double
        get() = if (useSchemeBreakdown) {
            if (country == Country.USA) usaSchemes.totalBalance else indiaSchemes.totalBalance
        } else {
            currentCorpus
        }

    val effectiveMonthlyContribution: Double
        get() = if (useSchemeBreakdown) {
            if (country == Country.USA) usaSchemes.effectiveMonthlyContribution else indiaSchemes.effectiveMonthlyContribution
        } else {
            monthlyContribution
        }

    val effectivePostRetirementRoiPercent: Double
        get() = if (isCustomPostRetirementRoi) postRetirementRoiPercent else expectedRoiPercent

    companion object {
        fun defaultForCountry(country: Country): FireInput {
            return FireInput(
                country = country,
                currentAge = 30,
                retirementAge = 50,
                lifeExpectancy = 85,
                currentCorpus = country.defaultCurrentCorpus,
                monthlyContribution = country.defaultMonthlyContribution,
                monthlyWithdrawalPostRetirement = country.defaultMonthlyWithdrawal,
                isInflationAdjusted = true,
                inflationRatePercent = country.defaultInflation,
                expectedRoiPercent = country.defaultRoi,
                postRetirementRoiPercent = country.defaultPostRetirementRoi,
                isCustomPostRetirementRoi = false,
                currencySymbol = country.currencySymbol,
                useSchemeBreakdown = false,
                usaSchemes = UsaSchemeInput(),
                indiaSchemes = IndiaSchemeInput()
            )
        }
    }
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
    val trajectory: List<YearlyTrajectoryPoint>,
    val fireProgressPercent: Double = 0.0,
    val projectedFireProgressPercent: Double = 0.0,
    val isCoastFireAchieved: Boolean = false,
    val coastFireCurrentCorpusNeeded: Double = 0.0,
    val leanFireCorpusNeeded: Double = perpetualCorpusNeeded * 0.6,
    val fatFireCorpusNeeded: Double = perpetualCorpusNeeded * 1.32,
    val schemeAnalytics: SchemeAnalytics = SchemeAnalytics()
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
