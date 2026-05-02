package com.mybutler.user.service

import com.mybutler.auth.repository.UserRepository
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.storage.StorageService
import com.mybutler.common.util.ImageUploadValidator
import com.mybutler.user.dto.*
import com.mybutler.user.entity.UserPreference
import com.mybutler.user.repository.UserPreferenceRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val storageService: StorageService,
    private val imageUploadValidator: ImageUploadValidator,
) {
    fun getMyProfile(userId: Long): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        return UserProfileResponse.from(user)
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        user.gender = request.gender
        user.ageGroup = request.ageGroup
        user.drinkingFrequency = request.drinkingFrequency

        checkOnboardingCompletion(user.id, user)

        return UserProfileResponse.from(user)
    }

    @Transactional
    fun uploadProfileImage(userId: Long, file: MultipartFile): UserProfileResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)
        imageUploadValidator.validate(file)
        val oldImageUrl = user.profileImageUrl
        val newImageUrl = storageService.upload(file, "profiles")
        user.profileImageUrl = newImageUrl
        oldImageUrl?.takeIf { it != newImageUrl }?.let(storageService::delete)
        return UserProfileResponse.from(user)
    }

    @Transactional
    fun savePreferences(userId: Long, request: SavePreferencesRequest): UserPreferenceResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        val preference = userPreferenceRepository.findByUserId(userId)
            .orElseGet { UserPreference(userId = userId) }

        preference.tastePreferences = request.tastePreferences.toMutableSet()
        preference.preferredAbv = request.preferredAbv
        preference.experienceLevel = request.experienceLevel

        val saved = userPreferenceRepository.save(preference)

        checkOnboardingCompletion(userId, user)

        return UserPreferenceResponse.from(saved)
    }

    fun getPreferences(userId: Long): UserPreferenceResponse {
        val preference = userPreferenceRepository.findByUserId(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return UserPreferenceResponse.from(preference)
    }

    @Transactional
    fun updateUsername(userId: Long, request: UpdateUsernameRequest): UsernameResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw BusinessException(ErrorCode.USER_NOT_FOUND)

        if (userRepository.existsByUsername(request.username)) {
            throw BusinessException(ErrorCode.DUPLICATE_USERNAME)
        }

        user.username = request.username
        return UsernameResponse(username = user.username)
    }

    private fun checkOnboardingCompletion(userId: Long, user: com.mybutler.auth.entity.User) {
        if (user.ageGroup != null && userPreferenceRepository.existsByUserId(userId)) {
            user.onboardingCompleted = true
        }
    }
}
