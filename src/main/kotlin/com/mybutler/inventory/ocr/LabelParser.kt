package com.mybutler.inventory.ocr

import com.mybutler.inventory.entity.Category
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * OCR 원시 텍스트 → 구조화 라벨 정보 추출.
 *
 * - abv / capacityMl: 정규식 (정확도 높음)
 * - category: 브랜드/키워드 사전 매핑 (라벨에 카테고리가 직접 안 적히는 경우가 많음)
 * - name: 휴리스틱 (숫자/단위가 아닌 가장 의미있는 줄)
 * - confidence: 추출 성공 필드 수 기반 0~1
 *
 * 순수 함수라 단위 테스트 용이. LLM 도입 시 이 클래스를 보강/대체.
 */
@Component
class LabelParser {

    data class ParsedLabel(
        val name: String?,
        val category: Category?,
        val abv: BigDecimal?,
        val capacityMl: Int?,
        val confidence: Double,
    )

    fun parse(rawText: String): ParsedLabel {
        val text = rawText.trim()
        if (text.isEmpty()) {
            return ParsedLabel(null, null, null, null, 0.0)
        }

        val abv = extractAbv(text)
        val capacityMl = extractCapacityMl(text)
        val category = detectCategory(text)
        // 카테고리에 따라 이름 추출 로직 분기 (숙성 연수 부착 여부)
        val name = extractName(text, category)

        // confidence: 필드별 가중치 합산
        var score = 0.0
        if (name != null) score += 0.35
        if (category != null) score += 0.30
        if (abv != null) score += 0.20
        if (capacityMl != null) score += 0.15

        return ParsedLabel(
            name = name,
            category = category,
            abv = abv,
            capacityMl = capacityMl,
            confidence = score,
        )
    }

    // ─── ABV ─────────────────────────────────────────────────────────────
    // "40%", "40% vol", "ABV 43", "alc. 40% vol", "16.9도"(한글) 등.
    // (?<!\d): "120%"에서 "20"이 잘못 잡히는 것 방지.
    private val abvPercentRegex = Regex(
        """(?<!\d)(\d{1,2}(?:\.\d)?)\s*%""",
    )
    private val abvKeywordRegex = Regex(
        """(?:abv|alc(?:\.|ohol)?|도수)\s*[:\s]?\s*(?<!\d)(\d{1,2}(?:\.\d)?)""",
        RegexOption.IGNORE_CASE,
    )
    // 한글 도수: "16.9도", "40 도" — 뒤에 다른 한글이 붙지 않은 경우만(도수/도매 등 오인 방지)
    private val abvKoreanRegex = Regex(
        """(?<!\d)(\d{1,2}(?:\.\d)?)\s*도(?![가-힣])""",
    )

    private fun extractAbv(text: String): BigDecimal? {
        val inRange = { v: String -> v.toBigDecimalOrNull()?.takeIf { it in BigDecimal.ZERO..BigDecimal(100) } }
        // 1순위: % 기호
        abvPercentRegex.find(text)?.groupValues?.get(1)?.let { inRange(it)?.let { v -> return v } }
        // 2순위: 한글 "도"
        abvKoreanRegex.find(text)?.groupValues?.get(1)?.let { inRange(it)?.let { v -> return v } }
        // 3순위: 키워드 뒤 숫자 (% 없이)
        abvKeywordRegex.find(text)?.groupValues?.get(1)?.let { inRange(it)?.let { v -> return v } }
        return null
    }

    // ─── 용량 (ml) ────────────────────────────────────────────────────────
    // "700ml", "700 mL", "0.7L", "1L", "용량 750"
    private val mlRegex = Regex("""(\d{2,4})\s*m\s*[lℓ]""", RegexOption.IGNORE_CASE)
    private val literRegex = Regex("""(\d(?:\.\d{1,2})?)\s*[lℓ](?![a-z])""", RegexOption.IGNORE_CASE)

    private fun extractCapacityMl(text: String): Int? {
        mlRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 20..5000) return it
        }
        literRegex.find(text)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
            val ml = (it * 1000).toInt()
            if (ml in 20..5000) return ml
        }
        return null
    }

    // ─── 카테고리 (브랜드/키워드 매핑) ──────────────────────────────────────
    private val categoryKeywords: List<Pair<Category, List<String>>> = listOf(
        Category.WHISKEY to listOf(
            "whisky", "whiskey", "bourbon", "scotch", "위스키", "버번", "스카치",
            "lagavulin", "macallan", "glenfiddich", "glenlivet", "jameson", "chivas",
            "johnnie walker", "jack daniel", "jim beam", "bulleit", "ardbeg", "laphroaig",
            "balvenie", "talisker", "hibiki", "yamazaki", "hakushu", "highland",
        ),
        Category.GIN to listOf(
            "gin", "진", "hendrick", "tanqueray", "bombay", "beefeater", "gordon",
            "monkey 47", "roku", "sipsmith", "plymouth",
        ),
        Category.VODKA to listOf(
            "vodka", "보드카", "absolut", "smirnoff", "grey goose", "belvedere",
            "ketel one", "ciroc", "stolichnaya",
        ),
        Category.RUM to listOf(
            "rum", "럼", "bacardi", "captain morgan", "havana", "kraken", "plantation",
            "appleton", "diplomatico",
        ),
        Category.TEQUILA to listOf(
            "tequila", "테킬라", "jose cuervo", "patron", "don julio", "olmeca",
            "espolon", "mezcal", "메즈칼",
        ),
        Category.LIQUEUR to listOf(
            "liqueur", "리큐르", "campari", "aperol", "cointreau", "baileys", "kahlua",
            "chartreuse", "drambuie", "amaretto", "triple sec", "vermouth", "베르무트",
        ),
        // 한국 소주/맥주는 BE Category enum에 별도 항목이 없어 OTHER로 매핑.
        Category.OTHER to listOf(
            // 소주
            "참이슬", "처음처럼", "진로", "한라산", "좋은데이", "맛있는참", "잎새주", "soju",
            // 맥주
            "하이트", "카스", "테라", "맥스", "필라이트", "클라우드",
            "asahi", "아사히", "heineken", "하이네켄", "tsingtao", "guinness", "기네스", "beer", "맥주",
            // 막걸리
            "막걸리", "장수", "느린마을",
        ),
    )

    /**
     * 키워드가 텍스트 내에 "단어 단위"로 등장하는지 검사.
     * 단순 contains 사용 시 "Ginger Ale"의 "gin", "rumour"의 "rum" 같은 부분 일치로
     * 카테고리 오판정이 발생할 수 있어 단어 경계(영문) / 한글 인접성으로 방어한다.
     *
     * 영문 키워드: 앞뒤가 알파벳/숫자가 아닌 경계여야 매칭 (Regex word boundary `\b`).
     * 한글 키워드: 한글 부분 일치는 의미상 자연스러움 ("참이슬"이 "참이슬후레쉬" 안에 있어도 OK).
     *   다만 부분 일치 폭주 방지 위해 키워드 길이 2자 이상만 매칭.
     */
    private fun textContainsKeyword(lowerText: String, keyword: String): Boolean {
        val kw = keyword.lowercase()
        // 한글이 포함된 키워드 → contains
        val hasKorean = kw.any { it in '가'..'힣' }
        if (hasKorean) {
            return kw.length >= 2 && lowerText.contains(kw)
        }
        // 영문/숫자/공백 키워드 → 단어 경계
        val escaped = Regex.escape(kw)
        return Regex("""(?<![a-z0-9])$escaped(?![a-z0-9])""", RegexOption.IGNORE_CASE)
            .containsMatchIn(lowerText)
    }

    private fun detectCategory(text: String): Category? {
        val lower = text.lowercase()
        // 키워드 매칭 수가 가장 많은 카테고리 선택 (단어 경계 매칭으로 false positive 차단)
        return categoryKeywords
            .map { (cat, kws) -> cat to kws.count { textContainsKeyword(lower, it) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    // ─── 제품명 추출 ───────────────────────────────────────────────────────
    // 전략:
    //  1) 브랜드 사전에서 매칭된 키워드를 우선 사용 (예: "참이슬"). 가장 긴 매칭 키워드를
    //     포함한 OCR 줄을 찾아 정제 후 반환. 짧은 한글 브랜드명도 안정적으로 잡힘.
    //  2) 매칭 없으면 노이즈 필터 후 가장 긴 줄로 폴백.

    /** 숫자/단위/특수문자만 있는 줄 — 의미 없는 noise */
    private val numericOnlyRegex = Regex("""^[\d\s.%mlℓLvol°×x()\-]+$""", RegexOption.IGNORE_CASE)

    /**
     * 제품명으로 부적합한 줄을 가려내는 패턴.
     *  - 법적 고지, 회사명, 환경 마크, 배치 코드, 슬로건 문구 등
     *  - URL/email/긴 알파벳숫자 시리얼
     */
    private val noisePhrases = listOf(
        // 환경/법적 표시
        "환경성적", "환경부", "재활용", "분리배출", "유기농인증",
        "임산부", "주류", "건강", "주의", "성분",
        // 회사/주소 류
        "(주)", "주식회사", "유한회사", "co.,", "ltd", "inc.",
        "www.", "http", ".com", ".kr",
        // 슬로건/마케팅 문구
        "since", "premium", "원료", "정제", "깨끗", "natural", "100%",
        "정제로", "이슬같", "best", "smooth",
        // 배치/날짜성
        "best before", "유통기한", "제조일",
    )

    private fun isNoiseLine(line: String): Boolean {
        val lower = line.lowercase()
        if (numericOnlyRegex.matches(line)) return true
        // 줄이 너무 길면(>40) 마케팅 슬로건일 확률 높음 — name 후보 제외
        if (line.length > 40) return true
        // 노이즈 키워드 포함
        if (noisePhrases.any { lower.contains(it) }) return true
        // 알파벳/한글 비율이 너무 낮으면(주로 일련번호/날짜)
        val letterCount = line.count { it.isLetter() }
        if (letterCount < 2) return true
        // 한 줄이 거의 숫자/심볼이면 제외 (50% 미만 글자)
        if (letterCount * 2 < line.length) return true
        return false
    }

    /**
     * 텍스트 전체에서 매칭된 브랜드 키워드 중 가장 긴 것 반환.
     * 가장 긴 = 가장 특정적 (예: "lagavulin"이 "whisky"보다 우선).
     */
    private fun findMatchedBrand(text: String): String? {
        val lower = text.lowercase()
        // 일반 명사("whisky", "gin" 등)는 brand로 쓰면 안 됨 → 길이 4 이상 + 고유명사스러운 것만
        val genericTerms = setOf(
            "whisky", "whiskey", "vodka", "gin", "rum", "tequila", "liqueur", "beer", "soju",
            "위스키", "보드카", "럼", "진", "테킬라", "리큐르", "맥주", "소주", "버번", "스카치",
            "bourbon", "scotch", "mezcal", "메즈칼", "베르무트", "vermouth", "amaretto",
        )
        return categoryKeywords
            .flatMap { it.second }
            .filter { it !in genericTerms && it.length >= 3 }
            .filter { textContainsKeyword(lower, it) }
            .maxByOrNull { it.length }
    }

    /**
     * 숙성 연수 표기가 흔한 카테고리.
     * 위스키 — 12/16/18/21년 등 표기 일반적
     * 럼 — Plantation/Diplomatico 등 일부 프리미엄 럼
     *
     * 진/보드카/리큐르/맥주/소주는 숙성 표기가 거의 없어 false positive 방지를 위해 제외.
     * (라벨에 "12 years" 같은 텍스트가 마케팅 문구로 들어가도 부착 안 함)
     */
    private val agedCategories = setOf(Category.WHISKEY, Category.RUM)

    private fun extractName(text: String, category: Category?): String? {
        // 0) 줄 단위 정제
        val lines = text
            .split('\n')
            .map { it.trim() }
            .filter { it.length in 2..60 }

        // 1) 브랜드 우선 매칭
        val brand = findMatchedBrand(text)
        val baseName: String? = if (brand != null) {
            // 브랜드가 등장한 줄을 찾아서 (가장 짧은 = 가장 정제된 줄)
            val brandLine = lines
                .filter { it.lowercase().contains(brand) && !isNoiseLine(it) }
                .minByOrNull { it.length }
            brandLine ?: run {
                // 브랜드는 매칭됐는데 그 줄이 전부 노이즈로 걸러진 경우 — 브랜드 자체 + 인접 짧은 줄 결합
                val brandIndex = lines.indexOfFirst { it.lowercase().contains(brand) }
                if (brandIndex >= 0) {
                    val core = lines[brandIndex]
                    val next = lines.getOrNull(brandIndex + 1)
                        ?.takeIf { it.length in 2..20 && !isNoiseLine(it) }
                    if (next != null) "$core $next" else core
                } else {
                    // 텍스트 내엔 있는데 줄로 못 잡으면 brand 자체 반환 (영문 첫글자 대문자)
                    brand.replaceFirstChar { it.uppercaseChar() }
                }
            }
        } else {
            // 2) 폴백: 노이즈 필터 후 가장 긴 줄
            lines.filterNot { isNoiseLine(it) }.maxByOrNull { it.length }
        }

        if (baseName == null) return null

        // 3) 숙성 연수 부착은 위스키/럼 카테고리만. 그 외는 base 그대로.
        return if (category in agedCategories) appendAgeIfPresent(baseName, text) else baseName
    }

    // ─── 숙성 연수(age statement) 추출 ────────────────────────────────────
    // 위스키/브랜디 라벨의 핵심 표기 — "AGED 16 YEARS", "12 Years Old", "12년".
    // 브랜드명 옆에 붙여 더 정확한 제품명을 만든다 ("LAGAVULIN" → "LAGAVULIN 16").
    //
    // ⚠️ 반드시 "years"/"year"/"년" 키워드가 있을 때만 인식.
    //   단독 숫자(예: "1830", "30")는 설립연도/배치번호 등일 수 있어 부착하지 않는다.
    //   (Vision OCR이 \s에 줄바꿈 포함하므로 "16\nYEARS"도 매칭됨)
    private val ageStatementRegex = Regex(
        """(?:aged\s+)?(\d{1,2})\s*years?(?:\s+old)?""",
        RegexOption.IGNORE_CASE,
    )
    private val ageKoreanRegex = Regex(
        """(\d{1,2})\s*년(?:산|짜리)?(?![가-힣])""",
    )

    private fun extractAge(text: String): String? {
        // 영문 "N YEARS" 패턴
        ageStatementRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 3..50) return it.toString()
        }
        // 한글 "N년" / "N년산" 패턴 (산속/임진강 같이 다른 한글 뒤따르는 경우 제외)
        ageKoreanRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()?.let {
            if (it in 3..50) return it.toString()
        }
        return null
    }

    private fun appendAgeIfPresent(baseName: String, fullText: String): String {
        val age = extractAge(fullText) ?: return baseName
        // 이미 이름에 숙성 연수가 포함됐으면 중복 부착 X
        if (Regex("""\b$age\b""").containsMatchIn(baseName)) return baseName
        return "$baseName $age"
    }
}
