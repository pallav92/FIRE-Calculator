package com.finance.firecalculator.data

import android.content.Context
import android.content.SharedPreferences
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class ProfileRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fire_calculator_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_PROFILES = "saved_profiles"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_GUEST_INPUT = "guest_fire_input"
    }

    fun getAllProfiles(): List<UserProfile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserProfile>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveProfile(profile: UserProfile): List<UserProfile> {
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

    fun createProfile(name: String, input: FireInput): UserProfile {
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

    fun deleteProfile(profileId: String): List<UserProfile> {
        val updated = getAllProfiles().filterNot { it.id == profileId }
        prefs.edit().putString(KEY_PROFILES, gson.toJson(updated)).apply()
        if (getActiveProfileId() == profileId) {
            setActiveProfileId(null)
        }
        return updated
    }

    fun getActiveProfile(): UserProfile? {
        val activeId = getActiveProfileId() ?: return null
        return getAllProfiles().find { it.id == activeId }
    }

    fun getActiveProfileId(): String? {
        return prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
    }

    fun setActiveProfileId(profileId: String?) {
        if (profileId == null) {
            prefs.edit().remove(KEY_ACTIVE_PROFILE_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).apply()
        }
    }

    fun getGuestInput(): FireInput {
        val json = prefs.getString(KEY_GUEST_INPUT, null) ?: return FireInput()
        return try {
            gson.fromJson(json, FireInput::class.java) ?: FireInput()
        } catch (e: Exception) {
            FireInput()
        }
    }

    fun saveGuestInput(input: FireInput) {
        prefs.edit().putString(KEY_GUEST_INPUT, gson.toJson(input)).apply()
    }
}
