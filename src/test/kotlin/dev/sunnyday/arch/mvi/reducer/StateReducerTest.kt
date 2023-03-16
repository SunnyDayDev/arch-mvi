package dev.sunnyday.arch.mvi.reducer

import dev.sunnyday.arch.mvi.Update
import dev.sunnyday.arch.mvi.test.Event
import dev.sunnyday.arch.mvi.test.State
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StateReducerTest {

    @Test
    fun `state reducer reduces update`() {
        val reducer = createReducer { _, _ -> State("reduced") }

        val update = reducer.reduce(State("initial"), Event())

        assertEquals(Update.state(State("reduced")), update)
    }

    private fun createReducer(reducer: (State, Event) -> State): StateReducer<State, Event> {
        return object : StateReducer<State, Event>() {
            override fun reduceState(state: State, event: Event): State = reducer.invoke(state, event)
        }
    }
}