package com.mybutler.user.dto

import com.mybutler.auth.entity.AgeGroup
import com.mybutler.auth.entity.DrinkingFrequency
import com.mybutler.auth.entity.Gender
import com.mybutler.user.entity.ExperienceLevel
import com.mybutler.user.entity.PreferredAbv
import com.mybutler.user.entity.TastePreference
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateProfileRequest(
    val gender: Gender?,
    val ageGroup: AgeGroup?,
    val drinkingFrequency: DrinkingFrequency?,
    val birthDate: LocalDate?,
)

data class SavePreferencesRequest(
    @field:NotEmpty
    val tastePreferences: Set<TastePreference>,

    val preferredAbv: PreferredAbv?,
    val experienceLevel: ExperienceLevel?,
)

data class UpdateUsernameRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 20)
    @field:Pattern(regexp = "^[a-zA-Z0-9가-힣_]+$", message = "아이디는 영문, 숫자, 한글, 언더스코어만 사용할 수 있습니다.")
    val username: String,
)
