package dev.sunnyday.arch.mvi.side_effect.solo.internal

import dev.sunnyday.arch.mvi.side_effect.solo.ExecutingSideEffect
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InternalExecutingSideEffectTest {

    @Test
    fun `starts as active`() = runUnconfinedTest {
        val executingSideEffect = InternalExecutingSideEffect(
            id = ExecutingSideEffect.Id.Custom("custom.id"),
            sideEffect = mockk(),
            coroutineScope = this,
        )

        assertTrue(executingSideEffect.isActiveFlow.first())
    }

    @Test
    fun `on cancel set side effect innactive`() = runUnconfinedTest {
        val executingSideEffect = InternalExecutingSideEffect(
            id = ExecutingSideEffect.Id.Custom("custom.id"),
            sideEffect = mockk(),
            coroutineScope = this,
        )

        executingSideEffect.cancel()

        assertFalse(executingSideEffect.isActiveFlow.first())
    }

    @Test
    fun `toString() writes ExecutingSideEffectTest with id`() {
        val executingSideEffect = InternalExecutingSideEffect(
            id = ExecutingSideEffect.Id.Custom("custom.id"),
            sideEffect = mockk(),
            coroutineScope = TestScope(),
        )

        assertEquals(
            "ExecutingSideEffect(custom.id)",
            executingSideEffect.toString(),
        )
    }
}