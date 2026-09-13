package com.finance.firecalculator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.firecalculator.domain.model.Country
import com.finance.firecalculator.ui.FireCalculatorViewModel
import com.finance.firecalculator.ui.components.InputSliderSection
import com.finance.firecalculator.ui.components.SaveProfileDialog
import com.finance.firecalculator.ui.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorStudioScreen(
    viewModel: FireCalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val input = uiState.input
    val result = uiState.result
    val country = input.country
    val currency = input.currencySymbol

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Plan Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (uiState.isGuest) "Guest Mode (${country.displayName})" else "Editing: ${uiState.activeProfile?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isGuest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Country Switcher Pill Selector
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { viewModel.setShowCountrySelectionDialog(true) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = country.flagEmoji,
                                fontSize = 15.sp
                            )
                            Text(
                                text = country.code,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Country",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Reset is ONLY available for Guest Mode. Saved profiles are protected!
                    if (uiState.isGuest) {
                        TextButton(onClick = { showResetConfirmDialog = true }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                        TextButton(onClick = { viewModel.setSaveProfileDialogVisible(true) }) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save Plan")
                        }
                    }
                }
            )
        },
        bottomBar = {
            // Profile Protection Sticky Action Bar:
            // Visible ONLY for Saved Profiles when there are unsaved modifications!
            AnimatedVisibility(
                visible = !uiState.isGuest && uiState.hasUnsavedChanges,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Text(
                                    text = "Unsaved Changes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(onClick = { viewModel.setSaveProfileDialogVisible(true) }) {
                                Text("Save as New...", fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.discardProfileChanges() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Discard")
                            }

                            Button(
                                onClick = { viewModel.saveActiveProfileChanges() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Save Changes")
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Inflation Reality Check Banner
            if (input.isInflationAdjusted) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                text = "Inflation Reality Check (${input.inflationRatePercent}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            val multiplier = result.adjustedMonthlyWithdrawalAtRetirement / (input.monthlyWithdrawalPostRetirement.coerceAtLeast(1.0))
                            Text(
                                text = "At ${input.inflationRatePercent}% inflation, ${CurrencyFormatter.formatCompact(input.monthlyWithdrawalPostRetirement, currency)}/mo today will require ${CurrencyFormatter.formatCompact(result.adjustedMonthlyWithdrawalAtRetirement, currency)}/mo (${String.format("%.1f", multiplier)}x) at retirement in ${result.yearsToRetirement} years.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // 1. Timeline Section
            InputSectionCard(title = "Timeline & Age Milestones") {
                InputSliderSection(
                    title = "Current Age",
                    subtitle = "Your current age today",
                    formattedValue = "${input.currentAge} yrs",
                    value = input.currentAge.toFloat(),
                    absoluteRange = 18f..80f,
                    stepAmount = 1f,
                    inputSuffix = "yrs",
                    onValueChange = { viewModel.updateCurrentAge(it.toInt()) },
                    onStepChange = { viewModel.updateCurrentAge(input.currentAge + it.toInt()) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                InputSliderSection(
                    title = "Retirement Target Age",
                    subtitle = "Target age to stop working (${input.retirementAge - input.currentAge} yrs to go)",
                    formattedValue = "${input.retirementAge} yrs",
                    value = input.retirementAge.toFloat(),
                    absoluteRange = input.currentAge.toFloat()..85f,
                    stepAmount = 1f,
                    inputSuffix = "yrs",
                    onValueChange = { viewModel.updateRetirementAge(it.toInt()) },
                    onStepChange = { viewModel.updateRetirementAge(input.retirementAge + it.toInt()) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                InputSliderSection(
                    title = "Life Expectancy",
                    subtitle = "Planning horizon for longevity",
                    formattedValue = "${input.lifeExpectancy} yrs",
                    value = input.lifeExpectancy.toFloat(),
                    absoluteRange = (input.retirementAge + 1).toFloat()..100f,
                    stepAmount = 1f,
                    inputSuffix = "yrs",
                    onValueChange = { viewModel.updateLifeExpectancy(it.toInt()) },
                    onStepChange = { viewModel.updateLifeExpectancy(input.lifeExpectancy + it.toInt()) }
                )
            }

            // 2. Wealth & Monthly Contributions Section
            InputSectionCard(title = "Wealth & Monthly Contributions") {
                // Plan Mode Segmented Pill Selector
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Segment 1: Simple Total Plan
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!input.useSchemeBreakdown) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (!input.useSchemeBreakdown) 2.dp else 0.dp,
                            border = if (!input.useSchemeBreakdown) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { if (input.useSchemeBreakdown) viewModel.toggleUseSchemeBreakdown(false) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📊 Simple Plan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (!input.useSchemeBreakdown) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!input.useSchemeBreakdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Segment 2: Country Schemes Breakdown
                        val schemesTabLabel = if (country == Country.USA) "🏛️ 401(k) & IRA" else "🏛️ NPS & EPF"
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (input.useSchemeBreakdown) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (input.useSchemeBreakdown) 2.dp else 0.dp,
                            border = if (input.useSchemeBreakdown) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { if (!input.useSchemeBreakdown) viewModel.toggleUseSchemeBreakdown(true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = schemesTabLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (input.useSchemeBreakdown) FontWeight.Bold else FontWeight.Medium,
                                    color = if (input.useSchemeBreakdown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Text(
                    text = if (input.useSchemeBreakdown) {
                        if (country == Country.USA) "Split portfolio across 401(k), IRA, and Taxable Brokerage"
                        else "Split portfolio across NPS Tier 1, EPF/PPF, and Equity Mutual Funds"
                    } else {
                        "Single consolidated corpus and monthly investment"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (!input.useSchemeBreakdown) {
                    // Simple Single Corpus & Monthly Contribution
                    val defaultCorpusMax = if (country == Country.USA) 1_000_000f else 10_000_000f
                    val defaultMonthlyMax = if (country == Country.USA) 5_000f else 50_000f

                    InputSliderSection(
                        title = "Current Retirement Corpus",
                        subtitle = "Total existing savings & investments",
                        formattedValue = CurrencyFormatter.formatCompact(input.currentCorpus, currency),
                        value = input.currentCorpus.toFloat(),
                        absoluteRange = country.corpusRange,
                        isAdaptiveSlider = true,
                        defaultZeroMax = defaultCorpusMax,
                        currencySymbol = currency,
                        stepAmount = country.corpusStep,
                        inputSuffix = currency,
                        onValueChange = { viewModel.updateCurrentCorpus(it.toDouble()) },
                        onStepChange = { viewModel.updateCurrentCorpus(input.currentCorpus + it.toDouble()) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    InputSliderSection(
                        title = "Monthly Contribution",
                        subtitle = "Invested monthly until retirement",
                        formattedValue = "${CurrencyFormatter.formatCompact(input.monthlyContribution, currency)}/mo",
                        value = input.monthlyContribution.toFloat(),
                        absoluteRange = country.contributionRange,
                        isAdaptiveSlider = true,
                        defaultZeroMax = defaultMonthlyMax,
                        currencySymbol = currency,
                        stepAmount = country.contributionStep,
                        inputSuffix = "$currency/mo",
                        onValueChange = { viewModel.updateMonthlyContribution(it.toDouble()) },
                        onStepChange = { viewModel.updateMonthlyContribution(input.monthlyContribution + it.toDouble()) }
                    )
                } else {
                    // Scheme Breakdown View
                    if (country == Country.USA) {
                        UsaSchemesEditor(
                            input = input,
                            currency = currency,
                            viewModel = viewModel
                        )
                    } else {
                        IndiaSchemesEditor(
                            input = input,
                            currency = currency,
                            viewModel = viewModel
                        )
                    }
                }
            }

            // 3. Post-Retirement Living & Inflation Section
            InputSectionCard(title = "Post-Retirement Living & Inflation") {
                val defaultWithdrawalMax = if (country == Country.USA) 10_000f else 100_000f

                InputSliderSection(
                    title = "Monthly Withdrawal Needed",
                    subtitle = "In today's purchasing power",
                    formattedValue = "${CurrencyFormatter.formatCompact(input.monthlyWithdrawalPostRetirement, currency)}/mo",
                    value = input.monthlyWithdrawalPostRetirement.toFloat(),
                    absoluteRange = country.withdrawalRange,
                    isAdaptiveSlider = true,
                    defaultZeroMax = defaultWithdrawalMax,
                    currencySymbol = currency,
                    stepAmount = country.withdrawalStep,
                    inputSuffix = "$currency/mo",
                    onValueChange = { viewModel.updateMonthlyWithdrawal(it.toDouble()) },
                    onStepChange = { viewModel.updateMonthlyWithdrawal(input.monthlyWithdrawalPostRetirement + it.toDouble()) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Inflation Adjustment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (input.isInflationAdjusted) "Escalates annual withdrawal by inflation" else "Assumes flat expenses in today's currency",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = input.isInflationAdjusted,
                        onCheckedChange = { viewModel.toggleInflation(it) }
                    )
                }

                if (input.isInflationAdjusted) {
                    InputSliderSection(
                        title = "Annual Inflation Rate",
                        subtitle = "Expected long-term inflation",
                        formattedValue = "${String.format("%.1f", input.inflationRatePercent)}%",
                        value = input.inflationRatePercent.toFloat(),
                        absoluteRange = 1f..15f,
                        stepAmount = 0.5f,
                        inputSuffix = "%",
                        onValueChange = { viewModel.updateInflationRate(it.toDouble()) },
                        onStepChange = { viewModel.updateInflationRate(input.inflationRatePercent + it.toDouble()) }
                    )
                }
            }

            // 4. Return on Investment (ROI) Section
            InputSectionCard(title = "Investment Returns & Portfolio Yield") {
                InputSliderSection(
                    title = "Expected ROI (Pre-Retirement)",
                    subtitle = "Average annual portfolio growth during accumulation",
                    formattedValue = "${String.format("%.1f", input.expectedRoiPercent)}%",
                    value = input.expectedRoiPercent.toFloat(),
                    absoluteRange = 3f..25f,
                    stepAmount = 0.5f,
                    inputSuffix = "%",
                    onValueChange = { viewModel.updateExpectedRoi(it.toDouble()) },
                    onStepChange = { viewModel.updateExpectedRoi(input.expectedRoiPercent + it.toDouble()) }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Conservative Post-Retirement ROI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (input.isCustomPostRetirementRoi) "De-risks asset allocation into bonds/debt" else "Uses same ${input.expectedRoiPercent}% ROI throughout retirement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = input.isCustomPostRetirementRoi,
                        onCheckedChange = { viewModel.toggleCustomPostRetirementRoi(it) }
                    )
                }

                if (input.isCustomPostRetirementRoi) {
                    InputSliderSection(
                        title = "Post-Retirement ROI",
                        subtitle = "Lower, safer return assumption once retired",
                        formattedValue = "${String.format("%.1f", input.postRetirementRoiPercent)}%",
                        value = input.postRetirementRoiPercent.toFloat(),
                        absoluteRange = 2f..20f,
                        stepAmount = 0.5f,
                        inputSuffix = "%",
                        onValueChange = { viewModel.updatePostRetirementRoi(it.toDouble()) },
                        onStepChange = { viewModel.updatePostRetirementRoi(input.postRetirementRoiPercent + it.toDouble()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Reset Confirmation Dialog (Guest Mode Only)
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Defaults?") },
            text = { Text("Are you sure you want to reset all numbers to default ${country.displayName} values? Any temporary adjustments will be restored.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Save Profile Dialog
    if (uiState.isSaveProfileDialogVisible) {
        SaveProfileDialog(
            onDismiss = { viewModel.setSaveProfileDialogVisible(false) },
            onSave = { name -> viewModel.saveCurrentAsNewProfile(name) }
        )
    }
}

@Composable
private fun UsaSchemesEditor(
    input: com.finance.firecalculator.domain.model.FireInput,
    currency: String,
    viewModel: FireCalculatorViewModel
) {
    val usa = input.usaSchemes

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Aggregation Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Portfolio: ${CurrencyFormatter.formatCompact(usa.totalBalance, currency)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total/mo: ${CurrencyFormatter.formatCompact(usa.effectiveMonthlyContribution, currency)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (usa.monthlyEmployerMatch > 0) {
                    Text(
                        text = "Includes ${CurrencyFormatter.formatCompact(usa.monthlyEmployerMatch, currency)}/mo employer match (${CurrencyFormatter.formatCompact(usa.monthlyEmployerMatch * 12.0, currency)}/yr free money)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF198754),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 1. 401(k) Card
        InputSliderSection(
            title = "401(k) Employee Contribution",
            subtitle = "Annual IRS Limit: $23,000 (~$1,916/mo)",
            formattedValue = "${CurrencyFormatter.formatCompact(usa.k401MonthlyContribution, currency)}/mo",
            value = usa.k401MonthlyContribution.toFloat(),
            absoluteRange = 0f..5_000f,
            stepAmount = 50f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(k401MonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(k401MonthlyContribution = s.k401MonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "401(k) Employer Match %",
            subtitle = "Match percentage from your employer",
            formattedValue = "${usa.k401EmployerMatchPercent.toInt()}%",
            value = usa.k401EmployerMatchPercent.toFloat(),
            absoluteRange = 0f..100f,
            stepAmount = 5f,
            inputSuffix = "%",
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(k401EmployerMatchPercent = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(k401EmployerMatchPercent = s.k401EmployerMatchPercent + it.toDouble()) } }
        )

        InputSliderSection(
            title = "401(k) Current Balance",
            subtitle = "Locked until age 59½",
            formattedValue = CurrencyFormatter.formatCompact(usa.k401Balance, currency),
            value = usa.k401Balance.toFloat(),
            absoluteRange = 0f..5_000_000f,
            stepAmount = 5_000f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(k401Balance = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(k401Balance = s.k401Balance + it.toDouble()) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 2. IRA Card
        InputSliderSection(
            title = "Traditional / Roth IRA Contribution",
            subtitle = "Annual IRS Limit: $7,000 (~$583/mo)",
            formattedValue = "${CurrencyFormatter.formatCompact(usa.iraMonthlyContribution, currency)}/mo",
            value = usa.iraMonthlyContribution.toFloat(),
            absoluteRange = 0f..1_500f,
            stepAmount = 25f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(iraMonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(iraMonthlyContribution = s.iraMonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "IRA Current Balance",
            subtitle = "Existing IRA / Roth balance",
            formattedValue = CurrencyFormatter.formatCompact(usa.iraBalance, currency),
            value = usa.iraBalance.toFloat(),
            absoluteRange = 0f..2_000_000f,
            stepAmount = 2_500f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(iraBalance = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(iraBalance = s.iraBalance + it.toDouble()) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 3. Taxable Brokerage Card
        InputSliderSection(
            title = "Taxable Brokerage Monthly Savings",
            subtitle = "Penalty-free bridge for early retirement",
            formattedValue = "${CurrencyFormatter.formatCompact(usa.taxableBrokerageMonthlyContribution, currency)}/mo",
            value = usa.taxableBrokerageMonthlyContribution.toFloat(),
            absoluteRange = 0f..20_000f,
            stepAmount = 100f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(taxableBrokerageMonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(taxableBrokerageMonthlyContribution = s.taxableBrokerageMonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "Taxable Brokerage Current Balance",
            subtitle = "Immediately accessible liquid funds",
            formattedValue = CurrencyFormatter.formatCompact(usa.taxableBrokerageBalance, currency),
            value = usa.taxableBrokerageBalance.toFloat(),
            absoluteRange = 0f..10_000_000f,
            stepAmount = 10_000f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateUsaSchemes { s -> s.copy(taxableBrokerageBalance = it.toDouble()) } },
            onStepChange = { viewModel.updateUsaSchemes { s -> s.copy(taxableBrokerageBalance = s.taxableBrokerageBalance + it.toDouble()) } }
        )
    }
}

@Composable
private fun IndiaSchemesEditor(
    input: com.finance.firecalculator.domain.model.FireInput,
    currency: String,
    viewModel: FireCalculatorViewModel
) {
    val india = input.indiaSchemes

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Live Aggregation Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Portfolio: ${CurrencyFormatter.formatCompact(india.totalBalance, currency)}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Total SIP: ${CurrencyFormatter.formatCompact(india.effectiveMonthlyContribution, currency)}/mo",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // 1. EPF / PPF Card
        InputSliderSection(
            title = "EPF / PPF Monthly Contribution",
            subtitle = "Employee + Employer EPF deduction / PPF deposit",
            formattedValue = "${CurrencyFormatter.formatCompact(india.epfMonthlyContribution, currency)}/mo",
            value = india.epfMonthlyContribution.toFloat(),
            absoluteRange = 0f..100_000f,
            stepAmount = 1_000f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(epfMonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(epfMonthlyContribution = s.epfMonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "EPF / PPF Current Balance",
            subtitle = "Accumulated provident fund balance",
            formattedValue = CurrencyFormatter.formatCompact(india.epfBalance, currency),
            value = india.epfBalance.toFloat(),
            absoluteRange = 0f..20_000_000f,
            stepAmount = 50_000f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(epfBalance = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(epfBalance = s.epfBalance + it.toDouble()) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 2. NPS Card
        InputSliderSection(
            title = "NPS Tier 1 Monthly Contribution",
            subtitle = "Sec 80CCD tax deduction • 40% annuity at 60",
            formattedValue = "${CurrencyFormatter.formatCompact(india.npsMonthlyContribution, currency)}/mo",
            value = india.npsMonthlyContribution.toFloat(),
            absoluteRange = 0f..50_000f,
            stepAmount = 500f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(npsMonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(npsMonthlyContribution = s.npsMonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "NPS Current Balance",
            subtitle = "National Pension System tier-1 pool",
            formattedValue = CurrencyFormatter.formatCompact(india.npsBalance, currency),
            value = india.npsBalance.toFloat(),
            absoluteRange = 0f..20_000_000f,
            stepAmount = 25_000f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(npsBalance = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(npsBalance = s.npsBalance + it.toDouble()) } }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 3. Mutual Funds & Equity
        InputSliderSection(
            title = "Equity Mutual Funds & Stocks SIP",
            subtitle = "Flexible wealth accumulation (Index & Flexicap)",
            formattedValue = "${CurrencyFormatter.formatCompact(india.mutualFundsMonthlyContribution, currency)}/mo",
            value = india.mutualFundsMonthlyContribution.toFloat(),
            absoluteRange = 0f..500_000f,
            stepAmount = 1_000f,
            inputSuffix = "$currency/mo",
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(mutualFundsMonthlyContribution = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(mutualFundsMonthlyContribution = s.mutualFundsMonthlyContribution + it.toDouble()) } }
        )

        InputSliderSection(
            title = "Mutual Funds & Stocks Balance",
            subtitle = "Directly accessible equity wealth",
            formattedValue = CurrencyFormatter.formatCompact(india.mutualFundsBalance, currency),
            value = india.mutualFundsBalance.toFloat(),
            absoluteRange = 0f..100_000_000f,
            stepAmount = 100_000f,
            inputSuffix = currency,
            onValueChange = { viewModel.updateIndiaSchemes { s -> s.copy(mutualFundsBalance = it.toDouble()) } },
            onStepChange = { viewModel.updateIndiaSchemes { s -> s.copy(mutualFundsBalance = s.mutualFundsBalance + it.toDouble()) } }
        )
    }
}

@Composable
private fun InputSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
