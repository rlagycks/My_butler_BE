package com.mybutler.inventory.ocr

import com.mybutler.inventory.entity.Category
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class LabelParserTest {

    private val parser = LabelParser()

    @Test
    fun `위스키 라벨 - 도수 용량 카테고리 제품명 + 숙성연수 추출`() {
        val text = "LAGAVULIN\nAGED 16 YEARS\nISLAY SINGLE MALT SCOTCH WHISKY\n43% vol  700ml"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.WHISKEY)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("43"))
        assertThat(r.capacityMl).isEqualTo(700)
        // 브랜드명에 숙성 연수 부착되어야 함
        assertThat(r.name).contains("LAGAVULIN")
        assertThat(r.name).contains("16")
        assertThat(r.confidence).isGreaterThan(0.5)
    }

    @Test
    fun `위스키 라벨 - YEARS 키워드 있으면 숙성연수 부착 (줄바꿈 포함)`() {
        // Vision OCR이 "16 YEARS"를 "16\nYEARS"로 분리해도 매칭되어야 함 (\s에 \n 포함)
        val text = "LAGAVULIN\nSINGLE ISLAY MALT WHISKY\n16\nYEARS\nSCOTCH WHISKY\n43% VOL"
        val r = parser.parse(text)

        assertThat(r.name).isEqualTo("LAGAVULIN 16")
        assertThat(r.category).isEqualTo(Category.WHISKEY)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("43"))
    }

    @Test
    fun `진 라벨 - 설립연도(ESTD 1830) 같은 숫자는 숙성연수로 인식 안 함`() {
        // Tanqueray 라벨의 "ESTD 1830"이 잘못 부착되면 안 됨
        val text = "Tanqueray\nLONDON DRY GIN\nESTD 1830\nFOUR SIGNATURE BOTANICALS"
        val r = parser.parse(text)

        assertThat(r.name).contains("Tanqueray")
        // "Tanqueray 30" 이나 "Tanqueray 18" 처럼 잘못 붙으면 안 됨
        assertThat(r.name).doesNotContain(" 30")
        assertThat(r.name).doesNotContain(" 18")
        assertThat(r.category).isEqualTo(Category.GIN)
    }

    @Test
    fun `진 라벨 - YEARS 키워드가 있어도 진은 숙성연수 부착 안 함`() {
        // 마케팅 카피에 "12 years" 들어가도 진 카테고리면 무시
        val text = "Tanqueray\nLONDON DRY GIN\nDistilled to perfection over 12 years of refinement"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.GIN)
        assertThat(r.name).doesNotContain(" 12") // 부착 안 됨
    }

    @Test
    fun `보드카 라벨 - 숙성연수 부착 안 함`() {
        val text = "ABSOLUT VODKA\nMade for 25 years\n40% ABV\n750 ML"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.VODKA)
        assertThat(r.name).doesNotContain(" 25")
    }

    @Test
    fun `부분 일치 false positive 차단 - Ginger Ale은 GIN 아님`() {
        // "Ginger Ale"의 "gin"이 진 카테고리로 잘못 잡히지 않아야 함 (단어 경계 매칭)
        val text = "Ginger Ale\nCarbonated Beverage\n5% sugar"
        val r = parser.parse(text)

        // 카테고리: 진(GIN) 키워드 false match 없이 null이거나 OTHER 등이어야 함
        assertThat(r.category).isNotEqualTo(Category.GIN)
    }

    @Test
    fun `부분 일치 false positive 차단 - rumour는 RUM 아님`() {
        val text = "Premium Rumour Bitters\nCocktail Mixer\n20% vol"
        val r = parser.parse(text)

        // "rumour"의 "rum"이 럼 카테고리로 잘못 잡히면 안 됨
        assertThat(r.category).isNotEqualTo(Category.RUM)
    }

    @Test
    fun `이미 이름에 숙성연수가 있으면 중복 부착 안 함`() {
        val text = "MACALLAN 12 SHERRY OAK\n40% vol\n700ml"
        val r = parser.parse(text)

        // "MACALLAN 12" 한 줄로 들어왔으므로 "MACALLAN 12 12" 처럼 중복되면 안 됨
        assertThat(r.name).contains("MACALLAN")
        assertThat(r.name).contains("12")
        // 12가 한 번만 나타나야 함
        assertThat(r.name!!.split("12").size - 1).isEqualTo(1)
    }

    @Test
    fun `진 라벨 - 소수점 도수 + 리터 단위`() {
        val text = "Hendrick's Gin\n41.4% ABV\n0.7L"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.GIN)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("41.4"))
        assertThat(r.capacityMl).isEqualTo(700)
    }

    @Test
    fun `보드카 라벨 - ml 대문자 + 도수 키워드`() {
        val text = "ABSOLUT VODKA\nALC 40% VOL\n750 ML"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.VODKA)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("40"))
        assertThat(r.capacityMl).isEqualTo(750)
    }

    @Test
    fun `한글 라벨 - 참이슬 브랜드 매칭`() {
        val text = "참이슬\n소주\n16.9도\n360ml"
        val r = parser.parse(text)

        assertThat(r.name).contains("참이슬")
        assertThat(r.category).isEqualTo(Category.OTHER) // 소주는 OTHER 매핑
        assertThat(r.capacityMl).isEqualTo(360)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("16.9"))
    }

    @Test
    fun `한글 라벨 - 노이즈 슬로건 무시하고 브랜드명 추출`() {
        // 실제 참이슬 fresh 라벨에서 OCR이 뽑을 법한 텍스트
        val text = """
            하이트진로
            참이슬
            fresh
            SINCE 1924
            대나무 숯 정제로 이슬같은 깨끗함
            환경성적 환경부
            360 mL
        """.trimIndent()
        val r = parser.parse(text)

        // "대나무 숯 정제로 이슬같은 깨끗함" 같은 슬로건이 아니라 참이슬이 잡혀야 함
        assertThat(r.name).contains("참이슬")
        assertThat(r.name).doesNotContain("정제로")
        assertThat(r.name).doesNotContain("환경성적")
        assertThat(r.capacityMl).isEqualTo(360)
    }

    @Test
    fun `리큐르 라벨 - 카테고리 매칭`() {
        val text = "CAMPARI\nAPERITIVO\n25% vol\n1L"
        val r = parser.parse(text)

        assertThat(r.category).isEqualTo(Category.LIQUEUR)
        assertThat(r.abv).isEqualByComparingTo(BigDecimal("25"))
        assertThat(r.capacityMl).isEqualTo(1000)
    }

    @Test
    fun `빈 텍스트 - 모두 null + confidence 0`() {
        val r = parser.parse("   ")

        assertThat(r.name).isNull()
        assertThat(r.category).isNull()
        assertThat(r.abv).isNull()
        assertThat(r.capacityMl).isNull()
        assertThat(r.confidence).isEqualTo(0.0)
    }

    @Test
    fun `숫자만 있는 줄은 제품명에서 제외`() {
        val text = "43% vol\n700ml\nMACALLAN 12"
        val r = parser.parse(text)

        assertThat(r.name).isEqualTo("MACALLAN 12")
        assertThat(r.category).isEqualTo(Category.WHISKEY)
    }

    @Test
    fun `비정상 도수값(100 초과)은 무시`() {
        val text = "SOME BRAND\n120% vol"
        val r = parser.parse(text)

        assertThat(r.abv).isNull()
    }
}
