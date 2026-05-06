package com.mybutler.common.util

import com.mybutler.common.storage.StorageService
import org.slf4j.Logger

fun deleteFromStorageQuietly(
    storageService: StorageService,
    fileUrl: String,
    logger: Logger,
    resourceLabel: String,
) {
    runCatching { storageService.delete(fileUrl) }
        .onFailure { ex -> logger.warn("Failed to delete {}: {}", resourceLabel, fileUrl, ex) }
}
