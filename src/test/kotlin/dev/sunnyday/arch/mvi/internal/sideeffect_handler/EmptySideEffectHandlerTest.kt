package dev.sunnyday.arch.mvi.internal.sideeffect_handler

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import dev.sunnyday.arch.mvi.test.collectWithScope
import dev.sunnyday.arch.mvi.test.runUnconfinedTest
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EmptySideEffectHandlerTest {

    @Test
    fun `empty side effect handler do nothing`() = runUnconfinedTest {
        val sideEffectHandler = EmptySideEffectHandler()
        val outputEvents = sideEffectHandler.outputEvents.collectWithScope()

        sideEffectHandler.onSideEffect("sideEffect")

        assertTrue(outputEvents.isEmpty())
    }
}