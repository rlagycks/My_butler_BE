package com.mybutler.inventory.ocr

/**
 * 이미지 바이트 → 원시 텍스트 추출 추상화.
 * 구현체: GoogleVisionOcrClient(실제), StubOcrClient(키 미설정 시 폴백).
 */
interface OcrClient {
    fun extractText(imageBytes: ByteArray): String
}
