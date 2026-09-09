package com.finance.firecalculator.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.finance.firecalculator.data.IProfileRepository
import com.finance.firecalculator.data.ProfileRepository
import com.finance.firecalculator.domain.FireCalculationEngine
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.domain.model.UserProfile
import com.finance.firecalculator.ui.navigation.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FireUiState(
    val selectedTab: AppTab = AppTab.Visualizer,
    val input: FireInput = FireInput(),
    val result: FireResult = FireCalculationEngine.calculate(FireInput()),
    val activeProfile: UserProfile? = null,
    val savedProfiles: List<UserProfile> = emptyList(),
    val hasUnsavedChanges: Boolean = false,
    val isProfileSheetVisible: Boolean = false,
    val isSaveProfileDialogVisible: Boolean = false,
    val isCreateNewProfileDialogVisible: Boolean = false,
    val profileToRename: UserProfile? = null,
    val comparisonProfile: UserProfile? = null
) {
    val isGuest: Boolean
        get() = activeProfile == null
}

class FireCalculatorViewModel(
    application: Application,
    private val repository: IProfileRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, ProfileRepository(application))

    constructor(repository: IProfileRepository) : this(Application(), repository)

    private val _uiState = MutableStateFlow(FireUiState())
    val uiState: StateFlow<FireUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val profiles = repository.getAllProfiles()
        val activeProfile = repository.getActiveProfile()

        val initialInput = activeProfile?.input ?: repository.getGuestInput()
        val result = FireCalculationEngine.calculate(initialInput)

        // Default secondary comparison profile (first profile that isn't the active one)
        val comparison = profiles.firstOrNull { it.id != activeProfile?.id }

        _uiState.value = FireUiState(
            selectedTab = AppTab.Visualizer,
            input = initialInput,
            result = result,
            activeProfile = activeProfile,
            savedProfiles = profiles,
            hasUnsavedChanges = false,
            comparisonProfile = comparison
        )
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onInputChange(update: (FireInput) -> FireInput) {
        _uiState.update { state ->
            val newInput = update(state.input)
            val newResult = FireCalculationEngine.calculate(newInput)

            val hasChanges = if (state.activeProfile != null) {
                newInput != state.activeProfile.input
            } else {
                repository.saveGuestInput(newInput)
                false
            }

            state.copy(
                input = newInput,
                result = newResult,
                hasUnsavedChanges = hasChanges
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

    fun saveActiveProfileChanges() {
        val active = _uiState.value.activeProfile ?: return
        val updated = active.copy(input = _uiState.value.input)
        val allProfiles = repository.saveProfile(updated)
        _uiState.update {
            it.copy(
                activeProfile = updated,
                savedProfiles = allProfiles,
                hasUnsavedChanges = false
            )
        }
    }

    fun discardProfileChanges() {
        val active = _uiState.value.activeProfile ?: return
        val originalInput = active.input
        val originalResult = FireCalculationEngine.calculate(originalInput)
        _uiState.update {
            it.copy(
                input = originalInput,
                result = originalResult,
                hasUnsavedChanges = false
            )
        }
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
                hasUnsavedChanges = false,
                isProfileSheetVisible = false
            )
        }
    }

    fun switchToProfile(profileId: String) {
        val profile = _uiState.value.savedProfiles.find { it.id == profileId } ?: return
        repository.setActiveProfileId(profileId)
        val result = FireCalculationEngine.calculate(profile.input)
        val otherComparison = _uiState.value.savedProfiles.firstOrNull { it.id != profileId }
        _uiState.update {
            it.copy(
                input = profile.input,
                result = result,
                activeProfile = profile,
                hasUnsavedChanges = false,
                isProfileSheetVisible = false,
                comparisonProfile = otherComparison
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
                hasUnsavedChanges = false,
                isSaveProfileDialogVisible = false,
                isCreateNewProfileDialogVisible = false
            )
        }
    }

    fun duplicateProfile(profileId: String, newName: String) {
        val source = _uiState.value.savedProfiles.find { it.id == profileId } ?: return
        val duplicated = repository.createProfile(newName, source.input)
        val updatedProfiles = repository.getAllProfiles()
        _uiState.update {
            it.copy(savedProfiles = updatedProfiles)
        }
    }

    fun deleteProfile(profileId: String) {
        val updated = repository.deleteProfile(profileId)
        val isActiveDeleted = _uiState.value.activeProfile?.id == profileId
        if (isActiveDeleted) {
            switchToGuest()
        } else {
            val nextComp = updated.firstOrNull { it.id != _uiState.value.activeProfile?.id }
            _uiState.update {
                it.copy(
                    savedProfiles = updated,
                    comparisonProfile = if (it.comparisonProfile?.id == profileId) nextComp else it.comparisonProfile
                )
            }
        }
    }

    fun setComparisonProfile(profileId: String?) {
        val comp = _uiState.value.savedProfiles.find { it.id == profileId }
        _uiState.update { it.copy(comparisonProfile = comp) }
    }

    fun resetToDefaults() {
        // Only for guest mode
        if (_uiState.value.isGuest) {
            val defaultInput = FireInput(currencySymbol = _uiState.value.input.currencySymbol)
            onInputChange { defaultInput }
        }
    }

    fun setSaveProfileDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isSaveProfileDialogVisible = visible) }
    }

    fun setCreateNewProfileDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isCreateNewProfileDialogVisible = visible) }
    }

    fun setProfileSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isProfileSheetVisible = visible) }
    }
}
