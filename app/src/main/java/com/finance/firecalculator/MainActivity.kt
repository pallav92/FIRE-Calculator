package com.finance.firecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finance.firecalculator.ui.FireCalculatorViewModel
import com.finance.firecalculator.ui.MainAppScaffold
import com.finance.firecalculator.ui.screens.CountrySelectionScreen
import com.finance.firecalculator.ui.theme.FIRECalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FIRECalculatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: FireCalculatorViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsState()

                    Crossfade(targetState = uiState.isOnboardingCompleted, label = "onboarding_crossfade") { isCompleted ->
                        if (!isCompleted) {
                            CountrySelectionScreen(
                                onCountrySelected = { country ->
                                    viewModel.completeOnboarding(country)
                                }
                            )
                        } else {
                            MainAppScaffold(viewModel = viewModel)
                        }
                    }

                    if (uiState.showCountrySelectionDialog) {
                        Dialog(
                            onDismissRequest = { viewModel.setShowCountrySelectionDialog(false) },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            CountrySelectionScreen(
                                initialCountry = uiState.input.country,
                                isSettingsDialog = true,
                                onCountrySelected = { newCountry ->
                                    viewModel.switchCountry(newCountry)
                                },
                                onDismissDialog = {
                                    viewModel.setShowCountrySelectionDialog(false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}