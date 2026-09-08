package com.finance.firecalculator.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                            text = if (uiState.isGuest) "Guest Mode (Temporary Exploration)" else "Editing Profile: ${uiState.activeProfile?.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (uiState.isGuest) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
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
                                text = "Inflation Reality Check",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "At ${input.inflationRatePercent}% annual inflation, ${CurrencyFormatter.formatCompact(input.monthlyWithdrawalPostRetirement, currency)}/mo today will equal ${CurrencyFormatter.formatCompact(result.adjustedMonthlyWithdrawalAtRetirement, currency)}/mo when you retire in ${result.yearsToRetirement} years.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

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

            // 2. Wealth & Savings Section
            InputSectionCard(title = "Wealth & Monthly Contributions") {
                InputSliderSection(
                    title = "Current Retirement Corpus",
                    subtitle = "Existing investments (0 to 99 Cr)",
                    formattedValue = CurrencyFormatter.formatCompact(input.currentCorpus, currency),
                    value = input.currentCorpus.toFloat(),
                    absoluteRange = 0f..990_000_000f,
                    isAdaptiveSlider = true,
                    defaultZeroMax = 10_000_000f,
                    currencySymbol = currency,
                    stepAmount = 100_000f, // ₹100,000 stepper step!
                    inputSuffix = currency,
                    onValueChange = { viewModel.updateCurrentCorpus(it.toDouble()) },
                    onStepChange = { viewModel.updateCurrentCorpus(input.currentCorpus + it.toDouble()) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                InputSliderSection(
                    title = "Monthly Contribution",
                    subtitle = "Invested monthly until retirement (100 to 10 Lakhs)",
                    formattedValue = "${CurrencyFormatter.formatCompact(input.monthlyContribution, currency)}/mo",
                    value = input.monthlyContribution.toFloat(),
                    absoluteRange = 100f..1_000_000f,
                    isAdaptiveSlider = true,
                    defaultZeroMax = 50_000f,
                    currencySymbol = currency,
                    stepAmount = 1_000f, // ₹1,000 stepper step!
                    inputSuffix = "$currency/mo",
                    onValueChange = { viewModel.updateMonthlyContribution(it.toDouble()) },
                    onStepChange = { viewModel.updateMonthlyContribution(input.monthlyContribution + it.toDouble()) }
                )
            }

            // 3. Post-Retirement Living & Inflation Section
            InputSectionCard(title = "Post-Retirement Living & Inflation") {
                InputSliderSection(
                    title = "Monthly Withdrawal Needed",
                    subtitle = "In today's purchasing power",
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

                AnimatedVisibility(visible = input.isInflationAdjusted) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        InputSliderSection(
                            title = "Expected Inflation Rate",
                            subtitle = "Annual cost of living escalation",
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

            // 4. Return on Investment (ROI) Section
            InputSectionCard(title = "Expected Return on Investment (ROI)") {
                InputSliderSection(
                    title = "Expected Annual ROI",
                    subtitle = "Pre-retirement portfolio compounding",
                    formattedValue = CurrencyFormatter.formatPercent(input.expectedRoiPercent),
                    value = input.expectedRoiPercent.toFloat(),
                    absoluteRange = 1f..25f,
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
                            text = "Custom Post-Retirement ROI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Switch to more conservative allocation after retiring",
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
                            subtitle = "Compounding rate during retirement withdrawals",
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

            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom action bar
        }
    }

    // Reset Confirmation Dialog (Guest Mode only)
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset to Defaults?") },
            text = { Text("This will reset all slider inputs back to default template numbers.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDefaults()
                        showResetConfirmDialog = false
                    }
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

    // Save as Profile Dialog
    if (uiState.isSaveProfileDialogVisible) {
        SaveProfileDialog(
            onDismiss = { viewModel.setSaveProfileDialogVisible(false) },
            onSave = { name -> viewModel.saveCurrentAsNewProfile(name) }
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
