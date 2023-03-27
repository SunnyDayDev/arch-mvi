package dev.sunnyday.arch.mvi.reducer

import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.State
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StateReducerTest {

    @Test
    fun `state reducer reduces update`() {
        val reducer = StateReducer.from<State, Event> { _, _ -> State("reduced") }

        val update = reducer.reduce(State("initial"), Event())

        assertEquals(Update.state(State("reduced")), update)
    }
}