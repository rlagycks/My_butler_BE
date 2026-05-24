package com.mybutler.inventory.ocr

import com.fasterxml.jackson.databind.JsonNode
import com.mybutler.common.exception.BusinessException
import com.mybutler.common.exception.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.Base64

/**
 * Google Cloud Vision REST API (DOCUMENT_TEXT_DETECTION) 기반 OCR.
 *
 * `ocr.provider=google` 이고 `ocr.google.api-key` 가 설정된 경우에만 빈으로 등록된다.
 * 키가 없으면 StubOcrClient 가 대신 동작.
 *
 * 요청: POST https://vision.googleapis.com/v1/images:annotate?key=...
 *   { requests: [{ image: { content: <base64> }, features: [{ type: DOCUMENT_TEXT_DETECTION }] }] }
 * 응답: responses[0].fullTextAnnotation.text
 */
@Component
@ConditionalOnProperty(name = ["ocr.provider"], havingValue = "google")
class GoogleVisionOcrClient(
    restClientBuilder: RestClient.Builder,
    private val props: OcrProperties,
) : OcrClient {

    private val log = LoggerFactory.getLogger(javaClass)
    // Spring 자동설정(RestClientCustomizer 등) 적용을 위해 Builder 주입 후 빌드.
    private val restClient: RestClient = restClientBuilder.build()

    override fun extractText(imageBytes: ByteArray): String {
        val apiKey = props.google.apiKey
        if (apiKey.isNullOrBlank()) {
            log.error("Google Vision API 키가 설정되지 않았습니다.")
            throw BusinessException(ErrorCode.INVENTORY_OCR_FAILED)
        }

        val base64 = Base64.getEncoder().encodeToString(imageBytes)
        val body = mapOf(
            "requests" to listOf(
                mapOf(
                    "image" to mapOf("content" to base64),
                    // DOCUMENT_TEXT_DETECTION: 조밀/작은 텍스트(예: "43% VOL")에 더 강함.
                    // 라벨처럼 폰트 크기 편차 큰 이미지에 적합.
                    "features" to listOf(mapOf("type" to "DOCUMENT_TEXT_DETECTION")),
                    // 한글+영문 혼재 라벨 힌트
                    "imageContext" to mapOf("languageHints" to listOf("ko", "en")),
                ),
            ),
        )

        return try {
            // JsonNode로 직접 받아 추가 변환 단계 제거.
            val response = restClient.post()
                .uri("https://vision.googleapis.com/v1/images:annotate?key={key}", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw BusinessException(ErrorCode.INVENTORY_OCR_FAILED)

            val text = parseFullText(response)
            // 디버그: OCR이 실제로 추출한 원시 텍스트 로깅 (파서 튜닝 시 매우 유용)
            log.info("Google Vision OCR raw_text ({} chars):\n---\n{}\n---", text.length, text)
            text
        } catch (e: BusinessException) {
            throw e
        } catch (e: Exception) {
            log.error("Google Vision OCR 호출 실패", e)
            throw BusinessException(ErrorCode.INVENTORY_OCR_FAILED)
        }
    }

    /**
     * 응답 JsonNode → 추출된 텍스트. 모든 path 접근은 누락 시 MissingNode를 반환하므로
     * NPE 없이 빈 문자열로 안전하게 fallthrough.
     */
    private fun parseFullText(root: JsonNode): String {
        val first = root.path("responses").path(0)
        // fullTextAnnotation.text 우선, 없으면 textAnnotations[0].description
        val full = first.path("fullTextAnnotation").path("text").asText("")
        if (full.isNotBlank()) return full
        return first.path("textAnnotations").path(0).path("description").asText("")
    }
}
