package dev.sunnyday.arch.mvi.side_effect.particle.filter.side_effect

import dev.sunnyday.arch.mvi.side_effect.particle.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.particle.test.TestFilterExecutingSideEffect
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SideEffectsWithTypeInstanceFilterTest {

    @Test
    fun `extension creates proper SideEffectsWithTypeInstanceFilter`() {
        val filter = sideEffectsWithType<Int>()
        assertEquals(SideEffectsWithTypeInstanceFilter.of<Int>(), filter)
    }

    @Test
    fun `filter side effects with specified type`() {
        val sideEffects = listOf(
            TestFilterExecutingSideEffect(1),
            TestFilterExecutingSideEffect("string"),
        )

        val filter = SideEffectsWithTypeInstanceFilter.of<Int>()
        val filteredSideEffect = sideEffects.filter(filter::accept)

        assertEquals(listOf(TestFilterExecutingSideEffect(1)), filteredSideEffect)
    }

    @Test
    fun `SideEffectsWithTypeInstanceFilter casts filtered item to expected side effect`() {
        val anyExecutingSideEffect: ExecutingSideEffect<Any> = TestFilterExecutingSideEffect(1)

        val intExecutingSizeEffect: ExecutingSideEffect<Int> = SideEffectsWithTypeInstanceFilter.of<Int>()
            .get(anyExecutingSideEffect)

        assertSame(anyExecutingSideEffect, intExecutingSizeEffect)
    }
}