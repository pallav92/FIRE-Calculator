package com.finance.firecalculator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.ui.components.*
import com.finance.firecalculator.ui.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireCalculatorScreen(
    viewModel: FireCalculatorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val input = uiState.input
    val result = uiState.result
    val currency = input.currencySymbol

    var showCurrencyMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "FIRE Calculator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Profile Chip
                    AssistChip(
                        onClick = { viewModel.setProfileSheetVisible(true) },
                        label = {
                            Text(
                                text = if (uiState.isGuest) "Guest" else (uiState.activeProfile?.name ?: "Profile"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.isGuest) Icons.Default.PersonOutline else Icons.Default.Person,
                                contentDescription = "Profile",
                                modifier = Modifier.size(16.dp),
                                tint = if (uiState.isGuest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Switch profile",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (uiState.isGuest) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Currency Picker dropdown
                    Box {
                        IconButton(onClick = { showCurrencyMenu = true }) {
                            Text(
                                text = currency,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showCurrencyMenu,
                            onDismissRequest = { showCurrencyMenu = false }
                        ) {
                            listOf("$", "₹", "€", "£", "¥").forEach { sym ->
                                DropdownMenuItem(
                                    text = { Text(sym) },
                                    onClick = {
                                        viewModel.updateCurrency(sym)
                                        showCurrencyMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Reset action
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Defaults"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
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
            // Guest Mode Banner if active
            if (uiState.isGuest) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Playing as Guest. Save this scenario to your local profiles anytime.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = { viewModel.setSaveProfileDialogVisible(true) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Save Plan", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero Summary Card
            SummaryHeroCard(input = input, result = result)

            // Trajectory Visualization Chart
            ProjectionChart(result = result, currencySymbol = currency)

            // 1. Timeline Section
            InputSectionCard(title = "Timeline & Retirement Age") {
                InputSliderSection(
                    title = "Current Age",
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
                    subtitle = "Age at which you want to retire (${input.retirementAge - input.currentAge} yrs left)",
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
                    subtitle = "Planning horizon for fund longevity",
                    formattedValue = "${input.lifeExpectancy} yrs",
                    value = input.lifeExpectancy.toFloat(),
                    absoluteRange = (input.retirementAge + 1).toFloat()..100f,
                    stepAmount = 1f,
                    inputSuffix = "yrs",
                    onValueChange = { viewModel.updateLifeExpectancy(it.toInt()) },
                    onStepChange = { viewModel.updateLifeExpectancy(input.lifeExpectancy + it.toInt()) }
                )
            }

            // 2. Savings & Contributions Section
            InputSectionCard(title = "Current Corpus & Monthly Savings") {
                InputSliderSection(
                    title = "Current Retirement Corpus",
                    subtitle = "Allowed: 0 to 99 Crores. Slider matches -50% to +150%",
                    formattedValue = CurrencyFormatter.formatCompact(input.currentCorpus, currency),
                    value = input.currentCorpus.toFloat(),
                    absoluteRange = 0f..990_000_000f,
                    isAdaptiveSlider = true,
                    defaultZeroMax = 10_000_000f,
                    currencySymbol = currency,
                    stepAmount = 10_000f,
                    inputSuffix = currency,
                    onValueChange = { viewModel.updateCurrentCorpus(it.toDouble()) },
                    onStepChange = { viewModel.updateCurrentCorpus(input.currentCorpus + it.toDouble()) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                InputSliderSection(
                    title = "Monthly Contribution",
                    subtitle = "Allowed: 100 to 10 Lakhs (1,000,000). Slider matches -50% to +150%",
                    formattedValue = "${CurrencyFormatter.formatCompact(input.monthlyContribution, currency)}/mo",
                    value = input.monthlyContribution.toFloat(),
                    absoluteRange = 100f..1_000_000f,
                    isAdaptiveSlider = true,
                    defaultZeroMax = 50_000f,
                    currencySymbol = currency,
                    stepAmount = 500f,
                    inputSuffix = "$currency/mo",
                    onValueChange = { viewModel.updateMonthlyContribution(it.toDouble()) },
                    onStepChange = { viewModel.updateMonthlyContribution(input.monthlyContribution + it.toDouble()) }
                )
            }

            // 3. Post-Retirement Living & Inflation Section
            InputSectionCard(title = "Post-Retirement Living & Inflation") {
                InputSliderSection(
                    title = "Monthly Withdrawal Needed",
                    subtitle = "In today's purchasing power (Slider matches -50% to +150%)",
                    formattedValue = "${CurrencyFormatter.formatCompact(input.monthlyWithdrawalPostRetirement, currency)}/mo",
                    value = input.monthlyWithdrawalPostRetirement.toFloat(),
                    absoluteRange = 500f..50_000_000f,
                    isAdaptiveSlider = true,
                    defaultZeroMax = 100_000f,
                    currencySymbol = currency,
                    stepAmount = 1_000f,
                    inputSuffix = "$currency/mo",
                    onValueChange = { viewModel.updateMonthlyWithdrawal(it.toDouble()) },
                    onStepChange = { viewModel.updateMonthlyWithdrawal(input.monthlyWithdrawalPostRetirement + it.toDouble()) }
                )

                Spacer(modifier = Modifier.height(6.dp))

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
                            text = if (input.isInflationAdjusted) {
                                "Escalates post-retirement withdrawals annually"
                            } else {
                                "Assumes flat expenses in today's currency"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = input.isInflationAdjusted,
                        onCheckedChange = { viewModel.toggleInflation(it) }
                    )
                }

                AnimatedVisibility(visible = input.isInflationAdjusted) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        InputSliderSection(
                            title = "Expected Inflation Rate",
                            subtitle = "Annual cost of living increase",
                            formattedValue = CurrencyFormatter.formatPercent(input.inflationRatePercent),
                            value = input.inflationRatePercent.toFloat(),
                            absoluteRange = 1f..15f,
                            stepAmount = 0.5f,
                            inputSuffix = "%",
                            onValueChange = { viewModel.updateInflationRate(it.toDouble()) },
                            onStepChange = { viewModel.updateInflationRate(input.inflationRatePercent + it.toDouble()) }
                        )
                    }
                }
            }

            // 4. Expected ROI (Return on Investment) Section
            InputSectionCard(title = "Expected Return on Investment (ROI)") {
                InputSliderSection(
                    title = "Expected Annual ROI",
                    subtitle = "Average portfolio growth rate",
                    formattedValue = CurrencyFormatter.formatPercent(input.expectedRoiPercent),
                    value = input.expectedRoiPercent.toFloat(),
                    absoluteRange = 1f..25f,
                    stepAmount = 0.5f,
                    inputSuffix = "%",
                    onValueChange = { viewModel.updateExpectedRoi(it.toDouble()) },
                    onStepChange = { viewModel.updateExpectedRoi(input.expectedRoiPercent + it.toDouble()) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Custom Post-Retirement ROI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Switch to more conservative asset allocation after retirement",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = input.isCustomPostRetirementRoi,
                        onCheckedChange = { viewModel.toggleCustomPostRetirementRoi(it) }
                    )
                }

                AnimatedVisibility(visible = input.isCustomPostRetirementRoi) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        InputSliderSection(
                            title = "Post-Retirement ROI",
                            subtitle = "Return on corpus while in retirement",
                            formattedValue = CurrencyFormatter.formatPercent(input.postRetirementRoiPercent),
                            value = input.postRetirementRoiPercent.toFloat(),
                            absoluteRange = 1f..20f,
                            stepAmount = 0.5f,
                            inputSuffix = "%",
                            onValueChange = { viewModel.updatePostRetirementRoi(it.toDouble()) },
                            onStepChange = { viewModel.updatePostRetirementRoi(input.postRetirementRoiPercent + it.toDouble()) }
                        )
                    }
                }
            }

            // 5. FIRE Benchmark Rules
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "FIRE Benchmarks & Safe Withdrawal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Perpetual 4% Rule",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCompact(result.perpetualCorpusNeeded, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = "Implied SWR",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatPercent(result.safeWithdrawalRatePercent),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (result.safeWithdrawalRatePercent <= 4.0 && result.safeWithdrawalRatePercent > 0.0) {
                                    Color(0xFF198754)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        Column {
                            Text(
                                text = "1st Yr Annual Expense",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCompact(result.firstYearAnnualWithdrawal, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Profile Selection Bottom Sheet
    if (uiState.isProfileSheetVisible) {
        ProfileSelectionSheet(
            activeProfile = uiState.activeProfile,
            savedProfiles = uiState.savedProfiles,
            onSelectGuest = { viewModel.switchToGuest() },
            onSelectProfile = { viewModel.switchToProfile(it) },
            onSaveNewProfileClick = {
                viewModel.setProfileSheetVisible(false)
                viewModel.setSaveProfileDialogVisible(true)
            },
            onDeleteProfile = { viewModel.deleteProfile(it) },
            onDismiss = { viewModel.setProfileSheetVisible(false) }
        )
    }

    // Save As Profile Dialog
    if (uiState.isSaveProfileDialogVisible) {
        SaveProfileDialog(
            onDismiss = { viewModel.setSaveProfileDialogVisible(false) },
            onSave = { viewModel.saveCurrentAsNewProfile(it) }
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
