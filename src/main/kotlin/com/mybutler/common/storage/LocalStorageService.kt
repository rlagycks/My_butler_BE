package com.mybutler.common.storage

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import com.mybutler.common.util.ImageUploadValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.util.UUID

@Service
class LocalStorageService(
    @Value("\${storage.local.upload-dir}") private val uploadDir: String,
    private val imageUploadValidator: ImageUploadValidator,
) : StorageService {

    override fun upload(file: MultipartFile, directory: String): String {
        imageUploadValidator.validate(file)

        val extension = file.originalFilename
            ?.substringAfterLast(".", "")
            ?.lowercase()
            ?: throw BusinessException(ErrorCode.INVALID_FILE_TYPE)

        val targetDir = File("$uploadDir/$directory").apply { mkdirs() }
        val fileName = "${UUID.randomUUID()}.$extension"
        val targetFile = File(targetDir, fileName)

        runCatching { file.transferTo(targetFile) }
            .onFailure { throw BusinessException(ErrorCode.FILE_UPLOAD_FAILED) }

        return "/$directory/$fileName"
    }

    override fun delete(fileUrl: String) {
        File("$uploadDir$fileUrl").delete()
    }
}
