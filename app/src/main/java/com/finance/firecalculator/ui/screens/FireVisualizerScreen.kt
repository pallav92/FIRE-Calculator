package com.finance.firecalculator.ui.screens

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
import com.finance.firecalculator.ui.components.FireGaugeCard
import com.finance.firecalculator.ui.components.ProjectionChart
import com.finance.firecalculator.ui.navigation.AppTab
import com.finance.firecalculator.ui.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireVisualizerScreen(
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
                    Text(
                        text = "FIRE Visualizer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    // Profile chip navigating to Scenarios tab
                    AssistChip(
                        onClick = { viewModel.selectTab(AppTab.Profiles) },
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
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (uiState.isGuest) {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            }
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Currency picker menu
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
                            listOf("₹", "$", "€", "£", "¥").forEach { sym ->
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
                }
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
            // 1. FIRE Gauge Loader Card
            FireGaugeCard(input = input, result = result)

            // 2. Milestone Countdown & Sustainability Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Milestone Horizon",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${result.yearsToRetirement} Years to FIRE",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "Target: Age ${input.retirementAge}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Monthly Pace",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${CurrencyFormatter.formatCompact(input.monthlyContribution, currency)}/mo",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Projected at Retirement",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatCompact(result.projectedCorpusAtRetirement, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (result.isFireAchieved) Color(0xFF198754) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fund Longevity",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (result.corpusExhaustionAge == null) "Age ${input.lifeExpectancy}+" else "Age ${result.corpusExhaustionAge}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (result.corpusExhaustionAge == null) Color(0xFF198754) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // 3. CTA to Tune Plan
            Button(
                onClick = { viewModel.selectTab(AppTab.Calculator) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tune Plan & Assumptions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. FIRE Tiers & Benchmarks
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "FIRE Tiers & Milestones",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Coast FIRE Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (result.isCoastFireAchieved) Color(0xFF198754).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Coast FIRE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Today's corpus needed: ${CurrencyFormatter.formatCompact(result.coastFireCurrentCorpusNeeded, currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (result.isCoastFireAchieved) Color(0xFF198754) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (result.isCoastFireAchieved) "Achieved! 🏖️" else "In Progress",
                                    color = if (result.isCoastFireAchieved) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Lean, Standard, Fat FIRE Rows
                    TierRow(
                        name = "Lean FIRE",
                        desc = "15x Annual Expenses (Basic)",
                        amount = CurrencyFormatter.formatCompact(result.leanFireCorpusNeeded, currency)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TierRow(
                        name = "Standard FIRE",
                        desc = "25x Annual Expenses (4% Rule)",
                        amount = CurrencyFormatter.formatCompact(result.perpetualCorpusNeeded, currency),
                        isHighlight = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TierRow(
                        name = "Fat FIRE",
                        desc = "33x Annual Expenses (Abundance)",
                        amount = CurrencyFormatter.formatCompact(result.fatFireCorpusNeeded, currency)
                    )
                }
            }

            // 5. Portfolio Trajectory Snapshot
            ProjectionChart(result = result, currencySymbol = currency)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TierRow(
    name: String,
    desc: String,
    amount: String,
    isHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
