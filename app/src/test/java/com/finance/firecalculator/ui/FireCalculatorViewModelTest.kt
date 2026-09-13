package com.finance.firecalculator.ui

import com.finance.firecalculator.data.IProfileRepository
import com.finance.firecalculator.domain.model.Country
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.UserProfile
import com.finance.firecalculator.ui.navigation.AppTab
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.UUID

class FakeProfileRepository : IProfileRepository {
    private val profiles = mutableListOf<UserProfile>()
    private var activeId: String? = null
    private var guestInput = FireInput()
    private var onboardingCompleted = false
    private var selectedCountry = Country.INDIA

    override fun getAllProfiles(): List<UserProfile> = profiles.toList()

    override fun saveProfile(profile: UserProfile): List<UserProfile> {
        val idx = profiles.indexOfFirst { it.id == profile.id }
        if (idx >= 0) {
            profiles[idx] = profile
        } else {
            profiles.add(profile)
        }
        return profiles.toList()
    }

    override fun createProfile(name: String, input: FireInput): UserProfile {
        val profile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            isGuest = false,
            input = input,
            lastModified = System.currentTimeMillis()
        )
        saveProfile(profile)
        setActiveProfileId(profile.id)
        return profile
    }

    override fun deleteProfile(profileId: String): List<UserProfile> {
        profiles.removeAll { it.id == profileId }
        if (activeId == profileId) {
            activeId = null
        }
        return profiles.toList()
    }

    override fun getActiveProfile(): UserProfile? {
        return profiles.find { it.id == activeId }
    }

    override fun getActiveProfileId(): String? = activeId

    override fun setActiveProfileId(profileId: String?) {
        activeId = profileId
    }

    override fun getGuestInput(): FireInput = guestInput

    override fun saveGuestInput(input: FireInput) {
        guestInput = input
    }

    override fun isOnboardingCompleted(): Boolean = onboardingCompleted

    override fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }

    override fun getSelectedCountry(): Country = selectedCountry

    override fun setSelectedCountry(country: Country) {
        selectedCountry = country
    }
}

class FireCalculatorViewModelTest {

    private lateinit var fakeRepository: FakeProfileRepository
    private lateinit var viewModel: FireCalculatorViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeProfileRepository()
        viewModel = FireCalculatorViewModel(fakeRepository)
    }

    @Test
    fun testInitialState_isGuestWhenNoActiveProfile() {
        val state = viewModel.uiState.value
        assertTrue("Initial state without profiles should be guest mode", state.isGuest)
        assertNull(state.activeProfile)
        assertEquals(AppTab.Visualizer, state.selectedTab)
        assertFalse(state.hasUnsavedChanges)
        assertFalse(state.isOnboardingCompleted)
    }

    @Test
    fun testCompleteOnboarding_setsCountryAndDefaults() {
        assertFalse(viewModel.uiState.value.isOnboardingCompleted)

        viewModel.completeOnboarding(Country.USA)

        val state = viewModel.uiState.value
        assertTrue(state.isOnboardingCompleted)
        assertEquals(Country.USA, state.input.country)
        assertEquals("$", state.input.currencySymbol)
        assertEquals(3.0, state.input.inflationRatePercent, 0.01)
        assertEquals(8.5, state.input.expectedRoiPercent, 0.01)
        assertTrue(fakeRepository.isOnboardingCompleted())
        assertEquals(Country.USA, fakeRepository.getSelectedCountry())
    }

    @Test
    fun testSwitchCountry_updatesCurrencyAndDefaults() {
        viewModel.completeOnboarding(Country.INDIA)
        assertEquals(Country.INDIA, viewModel.uiState.value.input.country)
        assertEquals("₹", viewModel.uiState.value.input.currencySymbol)

        viewModel.switchCountry(Country.USA)
        assertEquals(Country.USA, viewModel.uiState.value.input.country)
        assertEquals("$", viewModel.uiState.value.input.currencySymbol)
        assertEquals(3.0, viewModel.uiState.value.input.inflationRatePercent, 0.01)
    }

    @Test
    fun testUsaSchemes_aggregationAndBridgeAnalytics() {
        viewModel.completeOnboarding(Country.USA)
        viewModel.toggleUseSchemeBreakdown(true)

        viewModel.updateUsaSchemes {
            it.copy(
                k401MonthlyContribution = 1_500.0,
                k401EmployerMatchPercent = 50.0,
                k401EmployerMatchLimitMonthly = 750.0,
                iraMonthlyContribution = 500.0,
                taxableBrokerageMonthlyContribution = 400.0
            )
        }

        val state = viewModel.uiState.value
        assertTrue(state.input.useSchemeBreakdown)
        // 1500 + 750 (50% match) + 500 + 400 = 3150
        assertEquals(3_150.0, state.input.effectiveMonthlyContribution, 0.01)
        assertTrue(state.result.schemeAnalytics.annual401kEmployerMatchTotal > 0.0)
    }

    @Test
    fun testIndiaSchemes_npsAnnuityAnalytics() {
        viewModel.completeOnboarding(Country.INDIA)
        viewModel.toggleUseSchemeBreakdown(true)

        viewModel.updateIndiaSchemes {
            it.copy(
                npsBalance = 500_000.0,
                npsMonthlyContribution = 5_000.0
            )
        }

        val state = viewModel.uiState.value
        assertTrue(state.input.useSchemeBreakdown)
        val npsAnalytics = state.result.schemeAnalytics
        assertTrue("Projected NPS at retirement should be greater than 0", npsAnalytics.npsProjectedCorpusAtRetirement > 0.0)
        assertEquals(npsAnalytics.npsProjectedCorpusAtRetirement * 0.40, npsAnalytics.npsMandatoryAnnuityLumpSum, 0.01)
        assertEquals(npsAnalytics.npsProjectedCorpusAtRetirement * 0.60, npsAnalytics.npsTaxFreeLumpSum, 0.01)
        assertTrue("Estimated monthly pension should be positive", npsAnalytics.npsMonthlyEstimatedPension > 0.0)
    }

    @Test
    fun testSelectTab_updatesSelectedTab() {
        assertEquals(AppTab.Visualizer, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(AppTab.Calculator)
        assertEquals(AppTab.Calculator, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(AppTab.Profiles)
        assertEquals(AppTab.Profiles, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun testGuestMode_modificationsDoNotFlagUnsavedChanges() {
        assertTrue(viewModel.uiState.value.isGuest)

        viewModel.updateCurrentCorpus(15_000_000.0) // 1.5 Cr
        assertFalse("Guest modifications are saved automatically to guest input", viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(15_000_000.0, viewModel.uiState.value.input.currentCorpus, 0.001)
    }

    @Test
    fun testGuestMode_resetToDefaultsResetsInputs() {
        assertTrue(viewModel.uiState.value.isGuest)

        viewModel.updateCurrentCorpus(50_000_000.0)
        assertEquals(50_000_000.0, viewModel.uiState.value.input.currentCorpus, 0.001)

        viewModel.resetToDefaults()
        val defaultCorpus = FireInput.defaultForCountry(viewModel.uiState.value.input.country).currentCorpus
        assertEquals(defaultCorpus, viewModel.uiState.value.input.currentCorpus, 0.001)
    }

    @Test
    fun testProfileProtection_modificationsTriggerUnsavedChanges() {
        viewModel.saveCurrentAsNewProfile("Retirement 2040")

        assertFalse(viewModel.uiState.value.isGuest)
        assertEquals("Retirement 2040", viewModel.uiState.value.activeProfile?.name)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.updateCurrentCorpus(20_000_000.0)
        assertTrue("Modifying saved profile should flag unsaved changes", viewModel.uiState.value.hasUnsavedChanges)

        viewModel.discardProfileChanges()
        assertFalse("Discarding changes should clear unsaved flag", viewModel.uiState.value.hasUnsavedChanges)
        assertNotEquals(20_000_000.0, viewModel.uiState.value.input.currentCorpus, 0.001)
    }

    @Test
    fun testProfileProtection_saveChangesUpdatesActiveProfile() {
        viewModel.saveCurrentAsNewProfile("Primary Plan")
        val originalCorpus = viewModel.uiState.value.input.currentCorpus

        viewModel.updateCurrentCorpus(originalCorpus + 5_000_000.0)
        assertTrue(viewModel.uiState.value.hasUnsavedChanges)

        viewModel.saveActiveProfileChanges()
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)
        assertEquals(originalCorpus + 5_000_000.0, viewModel.uiState.value.activeProfile?.input?.currentCorpus ?: 0.0, 0.001)
    }

    @Test
    fun testProfileProtection_resetToDefaultsDoesNotResetSavedProfile() {
        viewModel.saveCurrentAsNewProfile("Protected Plan")
        val initialAge = viewModel.uiState.value.input.retirementAge

        viewModel.updateRetirementAge(52)
        viewModel.resetToDefaults()

        assertEquals("Saved profile must not be wiped by resetToDefaults", 52, viewModel.uiState.value.input.retirementAge)
    }

    @Test
    fun testProfileSwitchingAndGuestSwitching() {
        viewModel.saveCurrentAsNewProfile("Plan Alpha")
        val alphaId = viewModel.uiState.value.activeProfile!!.id

        viewModel.updateCurrentCorpus(75_000_000.0)
        viewModel.saveCurrentAsNewProfile("Plan Beta")
        val betaId = viewModel.uiState.value.activeProfile!!.id

        assertEquals(2, viewModel.uiState.value.savedProfiles.size)
        assertEquals(betaId, viewModel.uiState.value.activeProfile?.id)

        viewModel.switchToProfile(alphaId)
        assertEquals("Plan Alpha", viewModel.uiState.value.activeProfile?.name)

        viewModel.switchToGuest()
        assertTrue(viewModel.uiState.value.isGuest)
        assertNull(viewModel.uiState.value.activeProfile)
    }

    @Test
    fun testDuplicateProfile() {
        viewModel.saveCurrentAsNewProfile("Base Scenario")
        val baseId = viewModel.uiState.value.activeProfile!!.id

        viewModel.duplicateProfile(baseId, "Aggressive Scenario")
        val profiles = viewModel.uiState.value.savedProfiles

        assertEquals(2, profiles.size)
        assertTrue(profiles.any { it.name == "Aggressive Scenario" })
    }

    @Test
    fun testDeleteActiveProfile_switchesToGuest() {
        viewModel.saveCurrentAsNewProfile("Doomed Profile")
        val profileId = viewModel.uiState.value.activeProfile!!.id
        assertFalse(viewModel.uiState.value.isGuest)

        viewModel.deleteProfile(profileId)
        assertTrue("Deleting active profile should fallback to Guest Mode", viewModel.uiState.value.isGuest)
        assertNull(viewModel.uiState.value.activeProfile)
    }

    @Test
    fun testApplicationConstructorExistsForAndroidViewModelFactory() {
        val constructor = FireCalculatorViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull("FireCalculatorViewModel must have a public constructor(Application) for ViewModelProvider", constructor)
    }
}
