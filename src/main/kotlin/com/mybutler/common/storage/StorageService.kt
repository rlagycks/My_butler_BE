package com.mybutler.common.storage

import org.springframework.web.multipart.MultipartFile

interface StorageService {
    fun upload(file: MultipartFile, directory: String): String
    fun delete(fileUrl: String)
}
