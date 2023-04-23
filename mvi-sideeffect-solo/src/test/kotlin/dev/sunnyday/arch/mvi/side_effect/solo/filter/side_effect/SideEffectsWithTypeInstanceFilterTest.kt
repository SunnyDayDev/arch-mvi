package dev.sunnyday.arch.mvi.side_effect.solo.filter.side_effect

import dev.sunnyday.arch.mvi.primitive.ObservableEvent
import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.SoloSideEffect
import dev.sunnyday.arch.mvi.side_effect.solo.SoloSideEffectHandler
import dev.sunnyday.arch.mvi.side_effect.solo.test.TestFilterExecutingSideEffect
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SideEffectsWithTypeInstanceFilterTest {

    @Test
    fun `by default SideEffectsWithTypeInstanceFilter allow super types`() {
        assertEquals(
            SideEffectsWithTypeInstanceFilter(Int::class),
            SideEffectsWithTypeInstanceFilter(Int::class, allowSupertype = true),
        )
    }

    @Test
    fun `filter side effects of specified type (allow supertype)`() {
        val sideEffects = listOf(
            TestFilterExecutingSideEffect(Super()),
            TestFilterExecutingSideEffect(Nested()),
            TestFilterExecutingSideEffect("string"),
        )

        val filter = SideEffectsWithTypeInstanceFilter.of<Super>()
        val filteredSideEffect = sideEffects.filter(filter::accept)

        assertEquals(
            listOf(
                TestFilterExecutingSideEffect(Super()),
                TestFilterExecutingSideEffect(Nested()),
            ),
            filteredSideEffect,
        )
    }

    @Test
    fun `filter side effects of specified type (disallow supertype)`() {
        val sideEffects = listOf(
            TestFilterExecutingSideEffect(Super()),
            TestFilterExecutingSideEffect(Nested()),
            TestFilterExecutingSideEffect("string"),
        )

        val filter = SideEffectsWithTypeInstanceFilter.of<Super>(allowSupertype = false)
        val filteredSideEffect = sideEffects.filter(filter::accept)

        assertEquals(
            listOf(
                TestFilterExecutingSideEffect(Super()),
            ),
            filteredSideEffect,
        )
    }

    @Test
    fun `SideEffectsWithTypeInstanceFilter casts filtered item to expected side effect`() {
        val anyExecutingSideEffect: ExecutingSideEffect<Any> = TestFilterExecutingSideEffect(Nested())

        val intExecutingSizeEffect: ExecutingSideEffect<Super> = SideEffectsWithTypeInstanceFilter.of<Super>()
            .get(anyExecutingSideEffect)

        assertSame(anyExecutingSideEffect, intExecutingSizeEffect)
    }

    @Test
    fun `extensions just creates SideEffectsWithTypeInstanceFilter`() {
        assertEquals(
            SideEffectsWithTypeInstanceFilter(Int::class),
            sideEffectsWithType<Int>(),
        )

        assertEquals(
            SideEffectsWithTypeInstanceFilter(Int::class),
            SideEffectsWithTypeInstanceFilter.of<Int>(),
        )

        assertEquals(
            SideEffectsWithTypeInstanceFilter(Int::class, allowSupertype = false),
            sideEffectsWithType<Int>(allowSupertype = false),
        )

        assertEquals(
            SideEffectsWithTypeInstanceFilter(Int::class, allowSupertype = false),
            SideEffectsWithTypeInstanceFilter.of<Int>(allowSupertype = false),
        )
    }

    @Test
    fun `sideEffectsWithSameType creates SideEffectsWithTypeInstanceFilter with receiver type`() {
        assertEquals(
            SideEffectsWithTypeInstanceFilter(SoloSuper::class),
            SoloSuper().sideEffectsWithSameType(),
        )

        assertEquals(
            SideEffectsWithTypeInstanceFilter(SoloSuper::class, allowSupertype = false),
            SoloSuper().sideEffectsWithSameType(allowSupertype = false),
        )
    }

    private open class Super {
        override fun equals(other: Any?): Boolean {
            return other is Super
        }
    }

    private class Nested : Super() {
        override fun equals(other: Any?): Boolean {
            return other is Nested
        }
    }

    class SoloSuper : SoloSideEffect<Any, Super, Any> {
        override fun execute(dependency: Any): Flow<Any> {
            TODO("Not yet implemented")
        }
    }
}