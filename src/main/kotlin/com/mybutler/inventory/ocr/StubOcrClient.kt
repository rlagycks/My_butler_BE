package com.mybutler.inventory.ocr

import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component

/**
 * OCR 공급자가 설정되지 않았을 때(`ocr.provider`가 google이 아님)의 폴백.
 *
 * 이미지 기반 OCR은 불가하므로 명확한 에러를 던진다.
 * 단, 클라이언트가 ocrText 폼 필드를 직접 보내는 경우(개발/테스트)에는
 * InventoryService 에서 OcrClient를 거치지 않고 파서로 직행하므로 이 빈은 호출되지 않는다.
 */
@Component
@ConditionalOnMissingBean(GoogleVisionOcrClient::class)
class StubOcrClient : OcrClient {
    override fun extractText(imageBytes: ByteArray): String {
        // 실제 OCR 공급자 미설정. ocrText 직접 전송이 아닌 이미지 OCR 요청은 실패 처리.
        throw BusinessException(ErrorCode.INVENTORY_OCR_FAILED)
    }
}
