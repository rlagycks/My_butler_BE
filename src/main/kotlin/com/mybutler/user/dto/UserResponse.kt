package com.mybutler.user.dto

import com.mybutler.auth.entity.AgeGroup
import com.mybutler.auth.entity.DrinkingFrequency
import com.mybutler.auth.entity.Gender
import com.mybutler.auth.entity.User
import com.mybutler.user.entity.ExperienceLevel
import com.mybutler.user.entity.PreferredAbv
import com.mybutler.user.entity.TastePreference
import com.mybutler.user.entity.UserPreference

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val username: String,
    val gender: Gender?,
    val ageGroup: AgeGroup?,
    val drinkingFrequency: DrinkingFrequency?,
    val onboardingCompleted: Boolean,
) {
    companion object {
        fun from(user: User) = UserProfileResponse(
            id = user.id,
            email = user.email,
            username = user.username,
            gender = user.gender,
            ageGroup = user.ageGroup,
            drinkingFrequency = user.drinkingFrequency,
            onboardingCompleted = user.onboardingCompleted,
        )
    }
}

data class UserPreferenceResponse(
    val tastePreferences: Set<TastePreference>,
    val preferredAbv: PreferredAbv?,
    val experienceLevel: ExperienceLevel?,
) {
    companion object {
        fun from(pref: UserPreference) = UserPreferenceResponse(
            tastePreferences = pref.tastePreferences,
            preferredAbv = pref.preferredAbv,
            experienceLevel = pref.experienceLevel,
        )
    }
}

data class UsernameResponse(
    val username: String,
)
