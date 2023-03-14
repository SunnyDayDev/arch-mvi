package dev.sunnyday.arch.mvi.internal.sideeffect_handler

import dev.sunnyday.arch.mvi.internal.sideeffect_handler.EmptySideEffectHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import dev.sunnyday.arch.mvi.test.collectWithScope

@OptIn(ExperimentalCoroutinesApi::class)
class EmptySideEffectHandlerTest {

    @Test
    fun `empty side effect handler do nothing`() = runTest(UnconfinedTestDispatcher()) {
        val sideEffectHandler = EmptySideEffectHandler()
        val outputEvents = sideEffectHandler.outputEvents.collectWithScope()

        sideEffectHandler.onSideEffect("sideEffect")

        assertTrue(outputEvents.isEmpty())
    }
}