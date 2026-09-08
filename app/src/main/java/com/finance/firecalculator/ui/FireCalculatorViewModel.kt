package com.finance.firecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.finance.firecalculator.data.ProfileRepository
import com.finance.firecalculator.domain.FireCalculationEngine
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FireUiState(
    val input: FireInput = FireInput(),
    val result: FireResult = FireCalculationEngine.calculate(FireInput()),
    val activeProfile: UserProfile? = null,
    val savedProfiles: List<UserProfile> = emptyList(),
    val isProfileSheetVisible: Boolean = false,
    val isSaveProfileDialogVisible: Boolean = false
) {
    val isGuest: Boolean
        get() = activeProfile == null
}

class FireCalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(application)
    private val _uiState = MutableStateFlow(FireUiState())
    val uiState: StateFlow<FireUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val profiles = repository.getAllProfiles()
        val activeProfile = repository.getActiveProfile()

        val initialInput = if (activeProfile != null) {
            activeProfile.input
        } else {
            repository.getGuestInput()
        }

        val result = FireCalculationEngine.calculate(initialInput)
        _uiState.value = FireUiState(
            input = initialInput,
            result = result,
            activeProfile = activeProfile,
            savedProfiles = profiles
        )
    }

    fun onInputChange(update: (FireInput) -> FireInput) {
        _uiState.update { state ->
            val newInput = update(state.input)
            val newResult = FireCalculationEngine.calculate(newInput)

            // If in profile mode, update active profile with new input
            val updatedProfile = state.activeProfile?.copy(input = newInput)
            if (updatedProfile != null) {
                repository.saveProfile(updatedProfile)
            } else {
                repository.saveGuestInput(newInput)
            }

            state.copy(
                input = newInput,
                result = newResult,
                activeProfile = updatedProfile,
                savedProfiles = repository.getAllProfiles()
            )
        }
    }

    fun updateCurrentAge(age: Int) = onInputChange { it.copy(currentAge = age) }

    fun updateRetirementAge(age: Int) = onInputChange { it.copy(retirementAge = age) }

    fun updateLifeExpectancy(age: Int) = onInputChange { it.copy(lifeExpectancy = age) }

    fun updateCurrentCorpus(corpus: Double) = onInputChange { it.copy(currentCorpus = corpus) }

    fun updateMonthlyContribution(contribution: Double) = onInputChange { it.copy(monthlyContribution = contribution) }

    fun updateMonthlyWithdrawal(withdrawal: Double) = onInputChange { it.copy(monthlyWithdrawalPostRetirement = withdrawal) }

    fun toggleInflation(enabled: Boolean) = onInputChange { it.copy(isInflationAdjusted = enabled) }

    fun updateInflationRate(rate: Double) = onInputChange { it.copy(inflationRatePercent = rate) }

    fun updateExpectedRoi(roi: Double) = onInputChange { it.copy(expectedRoiPercent = roi) }

    fun updatePostRetirementRoi(roi: Double) = onInputChange { it.copy(postRetirementRoiPercent = roi) }

    fun toggleCustomPostRetirementRoi(enabled: Boolean) = onInputChange { it.copy(isCustomPostRetirementRoi = enabled) }

    fun updateCurrency(currency: String) = onInputChange { it.copy(currencySymbol = currency) }

    fun setProfileSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isProfileSheetVisible = visible) }
    }

    fun setSaveProfileDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSaveProfileDialogVisible = visible) }
    }

    fun switchToGuest() {
        repository.setActiveProfileId(null)
        val guestInput = repository.getGuestInput()
        val result = FireCalculationEngine.calculate(guestInput)
        _uiState.update {
            it.copy(
                input = guestInput,
                result = result,
                activeProfile = null,
                isProfileSheetVisible = false
            )
        }
    }

    fun switchToProfile(profileId: String) {
        val profile = _uiState.value.savedProfiles.find { it.id == profileId } ?: return
        repository.setActiveProfileId(profileId)
        val result = FireCalculationEngine.calculate(profile.input)
        _uiState.update {
            it.copy(
                input = profile.input,
                result = result,
                activeProfile = profile,
                isProfileSheetVisible = false
            )
        }
    }

    fun saveCurrentAsNewProfile(name: String) {
        val newProfile = repository.createProfile(name, _uiState.value.input)
        val updatedProfiles = repository.getAllProfiles()
        _uiState.update {
            it.copy(
                activeProfile = newProfile,
                savedProfiles = updatedProfiles,
                isSaveProfileDialogVisible = false,
                isProfileSheetVisible = false
            )
        }
    }

    fun deleteProfile(profileId: String) {
        val updated = repository.deleteProfile(profileId)
        val isActiveDeleted = _uiState.value.activeProfile?.id == profileId
        if (isActiveDeleted) {
            switchToGuest()
        } else {
            _uiState.update { it.copy(savedProfiles = updated) }
        }
    }

    fun resetToDefaults() {
        val defaultInput = FireInput(currencySymbol = _uiState.value.input.currencySymbol)
        onInputChange { defaultInput }
    }
}
