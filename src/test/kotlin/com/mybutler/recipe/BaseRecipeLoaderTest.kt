package com.mybutler.recipe

import com.mybutler.common.config.CacheConfig
import com.mybutler.recipe.entity.Recipe
import com.mybutler.recipe.entity.RecipeCategory
import com.mybutler.recipe.repository.RecipeRepository
import com.mybutler.recipe.service.BaseRecipeLoader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.times

@SpringBootTest(classes = [BaseRecipeLoader::class])
@Import(CacheConfig::class)
class BaseRecipeLoaderTest {

    @MockitoBean
    lateinit var recipeRepository: RecipeRepository

    @Autowired
    lateinit var baseRecipeLoader: BaseRecipeLoader

    @Autowired
    lateinit var cacheManager: CacheManager

    @Test
    fun `loadAll - 두 번 호출해도 repository는 한 번만 조회`() {
        val recipes = listOf(recipe(id = 1L), recipe(id = 2L))
        given(recipeRepository.findByIsCustomFalse(any<Pageable>())).willReturn(PageImpl(recipes))

        cacheManager.getCache("baseRecipes")?.clear()

        val first = baseRecipeLoader.loadAll()
        val second = baseRecipeLoader.loadAll()

        verify(recipeRepository, times(1)).findByIsCustomFalse(any<Pageable>())
        assertThat(first).isSameAs(second)
    }

    private fun recipe(id: Long) = Recipe(id = id, name = "Test $id", category = RecipeCategory.CLASSIC)
}
