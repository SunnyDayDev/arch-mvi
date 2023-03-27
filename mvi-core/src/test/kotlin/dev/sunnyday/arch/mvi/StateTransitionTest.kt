package dev.sunnyday.arch.mvi

import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.SideEffect
import dev.sunnyday.arch.mvi.test.State
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StateTransitionTest {

    @Test
    fun `state transition destructurization`() {
        val (previousState, newState, triggerEvent, sideEffects) = StateTransition(
            previousState = State("prev"),
            newState = State("new"),
            triggerEvent = Event(),
            sideEffects = listOf(SideEffect())
        )

        assertEquals(State("prev"), previousState)
        assertEquals(State("new"), newState)
        assertEquals(Event(), triggerEvent)
        assertEquals(listOf(SideEffect()), sideEffects)
    }
}