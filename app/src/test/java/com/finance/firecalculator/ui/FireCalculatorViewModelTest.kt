package com.finance.firecalculator.ui

import com.finance.firecalculator.data.IProfileRepository
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
        val defaultCorpus = FireInput().currentCorpus
        assertEquals(defaultCorpus, viewModel.uiState.value.input.currentCorpus, 0.001)
    }

    @Test
    fun testProfileProtection_modificationsTriggerUnsavedChanges() {
        // Create and select a saved profile
        val initialProfileInput = FireInput(currentCorpus = 10_000_000.0, monthlyWithdrawalPostRetirement = 80_000.0)
        viewModel.saveCurrentAsNewProfile("Retirement 2040")

        assertFalse(viewModel.uiState.value.isGuest)
        assertEquals("Retirement 2040", viewModel.uiState.value.activeProfile?.name)
        assertFalse(viewModel.uiState.value.hasUnsavedChanges)

        // Modify corpus
        viewModel.updateCurrentCorpus(20_000_000.0)
        assertTrue("Modifying saved profile should flag unsaved changes", viewModel.uiState.value.hasUnsavedChanges)

        // Discard changes
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
        // Attempting to call resetToDefaults on a saved profile should be a NO-OP
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

        // Switch back to Alpha
        viewModel.switchToProfile(alphaId)
        assertEquals("Plan Alpha", viewModel.uiState.value.activeProfile?.name)

        // Switch to Guest
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
        // ViewModelProvider.AndroidViewModelFactory requires a public constructor taking exactly (Application)
        val constructor = FireCalculatorViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull("FireCalculatorViewModel must have a public constructor(Application) for ViewModelProvider", constructor)
    }
}
