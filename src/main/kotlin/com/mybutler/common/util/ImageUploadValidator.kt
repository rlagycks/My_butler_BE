package com.mybutler.common.util

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.util.Locale

@Component
class ImageUploadValidator {
    private val allowedContentTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif")
    private val allowedExtensions = setOf("jpg", "jpeg", "png", "webp", "gif")

    fun validate(file: MultipartFile) {
        val contentType = file.contentType?.lowercase(Locale.ROOT)
        val extension = file.originalFilename
            ?.substringAfterLast('.', "")
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }

        if (contentType !in allowedContentTypes || extension !in allowedExtensions) {
            throw BusinessException(ErrorCode.INVALID_FILE_TYPE)
        }
    }
}
