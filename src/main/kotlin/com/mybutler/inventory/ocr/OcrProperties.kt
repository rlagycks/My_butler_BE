package com.mybutler.inventory.ocr

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * OCR 관련 설정. application.yml:
 *   ocr:
 *     provider: stub        # stub | google
 *     google:
 *       api-key: ${GOOGLE_VISION_API_KEY:}
 */
@ConfigurationProperties(prefix = "ocr")
data class OcrProperties(
    val provider: String = "stub",
    val google: Google = Google(),
) {
    data class Google(
        val apiKey: String? = null,
    )
}
