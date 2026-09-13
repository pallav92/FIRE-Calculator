package com.finance.firecalculator.data

import android.content.Context
import android.content.SharedPreferences
import com.finance.firecalculator.domain.model.Country
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

interface IProfileRepository {
    fun getAllProfiles(): List<UserProfile>
    fun saveProfile(profile: UserProfile): List<UserProfile>
    fun createProfile(name: String, input: FireInput): UserProfile
    fun deleteProfile(profileId: String): List<UserProfile>
    fun getActiveProfile(): UserProfile?
    fun getActiveProfileId(): String?
    fun setActiveProfileId(profileId: String?)
    fun getGuestInput(): FireInput
    fun saveGuestInput(input: FireInput)
    fun isOnboardingCompleted(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    fun getSelectedCountry(): Country
    fun setSelectedCountry(country: Country)
}

class ProfileRepository(context: Context) : IProfileRepository {
    private val prefs: SharedPreferences = context.getSharedPreferences("fire_calculator_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PROFILES = "saved_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_GUEST_INPUT = "guest_fire_input"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_SELECTED_COUNTRY = "selected_country"
    }

    override fun getAllProfiles(): List<UserProfile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun saveProfile(profile: UserProfile): List<UserProfile> {
        val currentProfiles = getAllProfiles().toMutableList()
        val existingIndex = currentProfiles.indexOfFirst { it.id == profile.id }
        val updatedProfile = profile.copy(lastModified = System.currentTimeMillis())
        if (existingIndex >= 0) {
            currentProfiles[existingIndex] = updatedProfile
        } else {
            currentProfiles.add(updatedProfile)
        }
        prefs.edit().putString(KEY_PROFILES, gson.toJson(currentProfiles)).apply()
        return currentProfiles
    }

    override fun createProfile(name: String, input: FireInput): UserProfile {
        val newProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "My Plan" },
            isGuest = false,
            input = input,
            lastModified = System.currentTimeMillis()
        )
        saveProfile(newProfile)
        setActiveProfileId(newProfile.id)
        return newProfile
    }

    override fun deleteProfile(profileId: String): List<UserProfile> {
        val updated = getAllProfiles().filterNot { it.id == profileId }
        prefs.edit().putString(KEY_PROFILES, gson.toJson(updated)).apply()
        if (getActiveProfileId() == profileId) {
            setActiveProfileId(null)
        }
        return updated
    }

    override fun getActiveProfile(): UserProfile? {
        val activeId = getActiveProfileId() ?: return null
        return getAllProfiles().find { it.id == activeId }
    }

    override fun getActiveProfileId(): String? {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
    }

    override fun setActiveProfileId(profileId: String?) {
        if (profileId == null) {
            prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
        }
    }

    override fun getGuestInput(): FireInput {
        val json = prefs.getString(KEY_GUEST_INPUT, null)
        if (json == null) {
            return FireInput.defaultForCountry(getSelectedCountry())
        }
        return try {
            gson.fromJson(json, FireInput::class.java) ?: FireInput.defaultForCountry(getSelectedCountry())
        } catch (e: Exception) {
            FireInput.defaultForCountry(getSelectedCountry())
        }
    }

    override fun saveGuestInput(input: FireInput) {
        prefs.edit().putString(KEY_GUEST_INPUT, gson.toJson(input)).apply()
    }

    override fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override fun getSelectedCountry(): Country {
        val code = prefs.getString(KEY_SELECTED_COUNTRY, Country.INDIA.code)
        return Country.entries.find { it.code == code } ?: Country.INDIA
    }

    override fun setSelectedCountry(country: Country) {
        prefs.edit().putString(KEY_SELECTED_COUNTRY, country.code).apply()
    }
}
