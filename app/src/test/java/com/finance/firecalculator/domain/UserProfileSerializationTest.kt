package com.finance.firecalculator.domain

import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class UserProfileSerializationTest {

    private val gson = Gson()

    @Test
    fun testUserProfileJsonRoundTrip() {
        val originalProfile = UserProfile(
            id = UUID.randomUUID().toString(),
            name = "Early Retirement Scenario",
            isGuest = false,
            input = FireInput(
                currentAge = 28,
                retirementAge = 45,
                lifeExpectancy = 90,
                currentCorpus = 75_000.0,
                monthlyContribution = 2_500.0,
                monthlyWithdrawalPostRetirement = 4_000.0,
                isInflationAdjusted = true,
                inflationRatePercent = 7.0,
                expectedRoiPercent = 12.0,
                currencySymbol = "₹"
            ),
            lastModified = System.currentTimeMillis()
        )

        val json = gson.toJson(originalProfile)
        assertNotNull(json)
        assertTrue(json.contains("Early Retirement Scenario"))

        val deserialized = gson.fromJson(json, UserProfile::class.java)
        assertEquals(originalProfile.id, deserialized.id)
        assertEquals(originalProfile.name, deserialized.name)
        assertEquals(originalProfile.input.currentAge, deserialized.input.currentAge)
        assertEquals(originalProfile.input.retirementAge, deserialized.input.retirementAge)
        assertEquals(originalProfile.input.currencySymbol, deserialized.input.currencySymbol)
        assertEquals(originalProfile.input.inflationRatePercent, deserialized.input.inflationRatePercent, 0.001)
    }

    @Test
    fun testProfileListSerialization() {
        val profiles = listOf(
            UserProfile(id = "1", name = "Plan A"),
            UserProfile(id = "2", name = "Plan B")
        )

        val json = gson.toJson(profiles)
        val type = object : TypeToken<List<UserProfile>>() {}.type
        val parsed: List<UserProfile> = gson.fromJson(json, type)

        assertEquals(2, parsed.size)
        assertEquals("Plan A", parsed[0].name)
        assertEquals("Plan B", parsed[1].name)
    }
}
